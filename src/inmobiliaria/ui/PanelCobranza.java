package inmobiliaria.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import inmobiliaria.cobranza.modelo.Cuota;
import inmobiliaria.cobranza.modelo.Recibo;
import inmobiliaria.cobranza.servicio.MoraService;
import inmobiliaria.cobranza.servicio.PagoService;
import inmobiliaria.contratos.modelo.Contrato;
import inmobiliaria.contratos.modelo.EstadoContrato;
import inmobiliaria.contratos.servicio.ContratoService;
import inmobiliaria.nucleo.estructuras.ListaSimple;
import inmobiliaria.nucleo.util.Fechas;
import inmobiliaria.propiedades.modelo.Edificio;
import inmobiliaria.propiedades.modelo.Unidad;
import inmobiliaria.propiedades.servicio.PropiedadService;

/**
 * Cobro de rentas y mora (2.5) de la propiedad seleccionada: cuotas de un contrato,
 * pagos totales o parciales, recibos, dias de atraso, recargo y si el arrendatario es moroso.
 * Usa PagoService y MoraService (Integrante 4).
 * Ventana no modal (se pueden abrir varias) con refresco automatico cada segundo; no se muestran codigos.
 */
public class PanelCobranza extends JDialog {
    private final ContratoService contratos;
    private final PagoService pagos;
    private final MoraService mora;
    private final PropiedadService propiedades;
    private final Edificio edificio;

    private final JComboBox<Contrato> comboContratos = new JComboBox<>();
    private final DefaultTableModel modelo = Dialogos.modelo(
            "Cuota", "Vencimiento", "Monto (S/)", "Saldo (S/)", "Atraso (dias)", "Estado", "Recargo (S/)");
    private final JTable tabla = Dialogos.tabla(modelo);
    private final JLabel resumen = new JLabel(" ");
    private boolean ajustandoCombo = false;   // evita refrescar dos veces al reconstruir el combo

    public PanelCobranza(Frame dueno, ContratoService contratos, PagoService pagos, MoraService mora,
                         PropiedadService propiedades, Edificio edificio) {
        super(dueno, "Cobro de rentas y mora - " + edificio.getNombre(), false);
        this.contratos = contratos;
        this.pagos = pagos;
        this.mora = mora;
        this.propiedades = propiedades;
        this.edificio = edificio;

        setLayout(new BorderLayout(8, 8));

        JPanel norte = new JPanel(new FlowLayout(FlowLayout.LEFT));
        norte.add(new JLabel("Contrato:"));
        Dialogos.mostrarComo(comboContratos, c -> c.getDocumentoArrendatario() + " - " + direccionDe(c.getCodigoUnidad()));
        norte.add(comboContratos);
        add(norte, BorderLayout.NORTH);

        JPanel centro = new JPanel(new BorderLayout(4, 4));
        centro.add(new JScrollPane(tabla), BorderLayout.CENTER);
        resumen.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 6, 2, 6));
        centro.add(resumen, BorderLayout.SOUTH);
        add(centro, BorderLayout.CENTER);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton pagar = new JButton("Registrar pago");
        pagar.addActionListener(e -> registrarPago());
        JButton verRecibos = new JButton("Ver recibos");
        verRecibos.addActionListener(e -> verRecibos());
        JButton cerrar = new JButton("Cerrar");
        cerrar.addActionListener(e -> dispose());
        botones.add(pagar);
        botones.add(verRecibos);
        botones.add(Dialogos.botonRefrescar(this::actualizar));
        botones.add(cerrar);
        add(botones, BorderLayout.SOUTH);

        sincronizarContratos();
        comboContratos.addActionListener(e -> {
            if (ajustandoCombo) return;
            tabla.clearSelection();   // la fila elegida era de otro contrato
            refrescar();
        });
        refrescar();
        Dialogos.autoRefrescar(this, this::actualizar);
        setSize(780, 400);
        setLocationRelativeTo(dueno);
    }

    private LocalDate hoy() {
        return LocalDate.now();
    }

    private boolean esDeLaPropiedad(String codigoUnidad) {
        ListaSimple<Unidad> unidades = propiedades.unidadesDe(edificio.getCodigo());
        for (int i = 0; i < unidades.getTamano(); i++) {
            if (unidades.obtener(i).getCodigo().equals(codigoUnidad)) return true;
        }
        return false;
    }

    private String direccionDe(String codigoUnidad) {
        Unidad u = propiedades.buscarUnidad(codigoUnidad);
        return u == null ? "(unidad dada de baja)" : u.getDireccion();
    }

    // Boton y timer: primero la lista de contratos (puede haber activaciones nuevas) y luego la tabla
    private void actualizar() {
        sincronizarContratos();
        refrescar();
    }

    // Solo contratos activados (con cuotas): los pendientes todavia no cobran.
    // Si la lista no cambio no se toca el combo; si cambio, se conserva el contrato elegido.
    private void sincronizarContratos() {
        List<Contrato> nuevos = new ArrayList<>();
        ListaSimple<Contrato> lista = contratos.listar();
        for (int i = 0; i < lista.getTamano(); i++) {
            Contrato c = lista.obtener(i);
            if (c.getEstado() != EstadoContrato.PENDIENTE && esDeLaPropiedad(c.getCodigoUnidad())) nuevos.add(c);
        }
        boolean igual = nuevos.size() == comboContratos.getItemCount();
        for (int i = 0; igual && i < nuevos.size(); i++) {
            igual = nuevos.get(i) == comboContratos.getItemAt(i);
        }
        if (igual) return;

        Contrato elegido = (Contrato) comboContratos.getSelectedItem();
        ajustandoCombo = true;
        comboContratos.removeAllItems();
        for (Contrato c : nuevos) comboContratos.addItem(c);
        if (elegido != null && nuevos.contains(elegido)) comboContratos.setSelectedItem(elegido);
        ajustandoCombo = false;
    }

    // Recibo sin codigo: fecha, cuota, pagado y saldo
    private String textoRecibo(Recibo r) {
        return Fechas.convertirAString(r.getFecha()) + " | cuota " + r.getNumeroCuota()
                + " | pagado S/ " + Dialogos.dinero(r.getMontoPagado())
                + " | saldo de la cuota S/ " + Dialogos.dinero(r.getSaldoCuota());
    }

    private String estado(Cuota c) {
        LocalDate hoy = hoy();
        if (c.estaPagada()) return "Pagada";
        if (mora.enMora(c, hoy)) return "EN MORA";
        if (mora.diasAtraso(c, hoy) > 0) return "Vencida (en gracia)";
        return "Pendiente";
    }

    private void refrescar() {
        Contrato c = (Contrato) comboContratos.getSelectedItem();
        if (c == null) {
            Dialogos.poblar(tabla, modelo, new ArrayList<>());
            resumen.setText("No hay contratos activos en esta propiedad. Active uno en el modulo Contratos.");
            return;
        }
        List<Object[]> filas = new ArrayList<>();
        ListaSimple<Cuota> cuotas = pagos.cuotasDe(c.getNumero());
        for (int i = 0; i < cuotas.getTamano(); i++) {
            Cuota q = cuotas.obtener(i);
            filas.add(new Object[] { q.getNumero(), Fechas.convertirAString(q.getVencimiento()),
                    Dialogos.dinero(q.getMonto()), Dialogos.dinero(q.getSaldo()),
                    mora.diasAtraso(q, hoy()), estado(q), Dialogos.dinero(mora.recargo(q, hoy())) });
        }
        Dialogos.poblar(tabla, modelo, filas);
        boolean moroso = mora.esMoroso(c.getDocumentoArrendatario(), contratos.listar(), hoy());
        resumen.setText("Deuda de renta: S/ " + Dialogos.dinero(pagos.deudaPendiente(c.getNumero()))
                + "   |   Recargo por mora: S/ " + Dialogos.dinero(mora.totalMora(c.getNumero(), hoy()))
                + "   |   Arrendatario " + c.getDocumentoArrendatario() + (moroso ? ": MOROSO" : ": al dia")
                + "   |   Gracia: " + MoraService.DIAS_GRACIA + " dias, recargo " + MoraService.PORCENTAJE_RECARGO + "%");
    }

    // ---------- Registrar pago total o parcial y emitir recibo (RN-09, RN-10) ----------
    private void registrarPago() {
        Contrato c = (Contrato) comboContratos.getSelectedItem();
        Object numero = Dialogos.seleccionado(tabla, 0);
        if (c == null || numero == null) {
            Dialogos.error(this, "Seleccione una cuota de la tabla.");
            return;
        }
        Cuota cuota = pagos.buscarCuota(c.getNumero(), (Integer) numero);
        if (cuota.estaPagada()) {
            Dialogos.error(this, "La cuota " + cuota.getNumero() + " ya esta pagada.");
            return;
        }
        JTextField monto = Dialogos.texto(Dialogos.dinero(cuota.getSaldo()));
        JTextField fecha = Dialogos.texto(Fechas.convertirAString(hoy()));
        if (!Dialogos.formulario(this, "Pago de la cuota " + cuota.getNumero(),
                new String[] { "Saldo de la cuota (S/)", "Monto a pagar (S/)", "Fecha (AAAA-MM-DD)" },
                new JComponent[] { soloLectura(Dialogos.dinero(cuota.getSaldo())), monto, fecha })) return;
        try {
            Recibo r = pagos.registrarPago(c.getNumero(), cuota.getNumero(),
                    Dialogos.numero(monto, "Monto"), Dialogos.fecha(fecha, "Fecha"));
            refrescar();
            Dialogos.info(this, "Recibo emitido", textoRecibo(r));
        } catch (IllegalArgumentException ex) {
            Dialogos.error(this, ex.getMessage());
        }
    }

    private void verRecibos() {
        Contrato c = (Contrato) comboContratos.getSelectedItem();
        if (c == null) {
            Dialogos.error(this, "No hay un contrato seleccionado.");
            return;
        }
        StringBuilder sb = new StringBuilder();
        ListaSimple<Recibo> todos = pagos.listarRecibos();
        for (int i = 0; i < todos.getTamano(); i++) {
            if (todos.obtener(i).getNumeroContrato().equals(c.getNumero()))
                sb.append(textoRecibo(todos.obtener(i))).append("\n");
        }
        if (sb.length() == 0) sb.append("Este contrato todavia no tiene recibos.");
        JTextArea area = new JTextArea(sb.toString(), 10, 60);
        area.setEditable(false);
        javax.swing.JOptionPane.showMessageDialog(this, new JScrollPane(area),
                "Recibos de " + c.getDocumentoArrendatario(), javax.swing.JOptionPane.PLAIN_MESSAGE);
    }

    private JTextField soloLectura(String texto) {
        JTextField f = Dialogos.texto(texto);
        f.setEditable(false);
        return f;
    }
}

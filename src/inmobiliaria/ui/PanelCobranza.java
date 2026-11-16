package inmobiliaria.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import java.io.File;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
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
import inmobiliaria.nucleo.util.Archivos;
import inmobiliaria.nucleo.util.Fechas;
import inmobiliaria.propiedades.modelo.Edificio;
import inmobiliaria.propiedades.modelo.Unidad;
import inmobiliaria.propiedades.servicio.PropiedadService;

/**
 * Cobro de rentas y mora (2.5) de la propiedad seleccionada: cuotas de un contrato (la primera fila,
 * "Garantia", es la cuota 0: el deposito que se cobra al empezar el contrato),
 * pagos totales o parciales, recibos, dias de atraso, recargo y si el arrendatario es moroso.
 * Usa PagoService y MoraService (Integrante 4).
 * Es un panel que MenuPrincipal muestra a la derecha de la ventana, con refresco automatico cada segundo;
 * no se muestran codigos.
 */
public class PanelCobranza extends JPanel {
    private final ContratoService contratos;
    private final PagoService pagos;
    private final MoraService mora;
    private final PropiedadService propiedades;
    private final Edificio edificio;

    private final JComboBox<Contrato> comboContratos = new JComboBox<>();
    private final DefaultTableModel modelo = Dialogos.modelo(
            "Concepto", "Vencimiento", "Monto (S/)", "Saldo (S/)", "Atraso (dias)", "Estado", "Recargo (S/)");
    private final JTable tabla = Dialogos.tabla(modelo);
    private final JLabel resumen = new JLabel(" ");
    private boolean ajustandoCombo = false;   // evita refrescar dos veces al reconstruir el combo

    public PanelCobranza(ContratoService contratos, PagoService pagos, MoraService mora,
                         PropiedadService propiedades, Edificio edificio) {
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
        // Fila: verde si esta pagada, amarilla si esta atrasada. En mora, el estado va en rojo y los dias
        // de atraso en un gradiente de rojos (rojo fuerte desde los 45 dias).
        Dialogos.colorear(tabla, (fila, col) -> {
            String e = String.valueOf(modelo.getValueAt(fila, 5));
            if (e.equals("Pagada")) return Dialogos.VERDE;
            if (e.equals("EN MORA")) {
                if (col == 5) return Dialogos.ROJO;
                if (col == 4) return Dialogos.rojoPorDias(((Number) modelo.getValueAt(fila, 4)).intValue());
                return Dialogos.AMARILLO;
            }
            if (e.startsWith("Vencida")) return Dialogos.AMARILLO;
            return null;
        });
        centro.add(new JScrollPane(tabla), BorderLayout.CENTER);
        resumen.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 6, 2, 6));
        centro.add(resumen, BorderLayout.SOUTH);
        add(centro, BorderLayout.CENTER);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton pagar = new JButton("Registrar pago");
        pagar.addActionListener(e -> registrarPago());
        JButton verRecibos = new JButton("Ver recibos");
        verRecibos.addActionListener(e -> verRecibos());
        botones.add(pagar);
        botones.add(verRecibos);
        JButton eliminar = new JButton("Eliminar");
        eliminar.addActionListener(e -> eliminarCuota());
        botones.add(eliminar);
        add(botones, BorderLayout.SOUTH);

        sincronizarContratos();
        comboContratos.addActionListener(e -> {
            if (ajustandoCombo) return;
            tabla.clearSelection();   // la fila elegida era de otro contrato
            refrescar();
        });
        refrescar();
        Dialogos.autoRefrescar(this, this::actualizar);
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
        return Fechas.convertirAString(r.getFecha()) + " | " + concepto(r.getNumeroCuota())
                + " | pagado S/ " + Dialogos.dinero(r.getMontoPagado())
                + " | saldo de la cuota S/ " + Dialogos.dinero(r.getSaldoCuota())
                + (r.getComprobante().isEmpty() ? "" : " | comprobante " + Archivos.nombre(r.getComprobante()));
    }

    // La cuota 0 es la garantia
    private String concepto(int numeroCuota) {
        return numeroCuota == Cuota.NUMERO_GARANTIA ? "garantia" : "cuota " + numeroCuota;
    }

    private Object etiqueta(int numeroCuota) {
        return numeroCuota == Cuota.NUMERO_GARANTIA ? "Garantia" : (Object) numeroCuota;
    }

    private String estado(Cuota c) {
        if (c.esGarantia()) return c.estaPagada() ? "Pagada" : "Pendiente";
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
            filas.add(new Object[] { etiqueta(q.getNumero()), Fechas.convertirAString(q.getVencimiento()),
                    Dialogos.dinero(q.getMonto()), Dialogos.dinero(q.getSaldo()),
                    mora.diasAtraso(q, hoy()), estado(q), Dialogos.dinero(mora.recargo(q, hoy())) });
        }
        Dialogos.poblar(tabla, modelo, filas);
        boolean moroso = mora.esMoroso(c.getDocumentoArrendatario(), contratos.listar(), hoy());
        double garantiaPendiente = pagos.garantiaPendiente(c.getNumero());
        resumen.setText("Deuda de renta: S/ " + Dialogos.dinero(pagos.deudaPendiente(c.getNumero()))
                + (garantiaPendiente > 0 ? "   |   Garantia pendiente: S/ " + Dialogos.dinero(garantiaPendiente) : "")
                + "   |   Recargo por mora: S/ " + Dialogos.dinero(mora.totalMora(c.getNumero(), hoy()))
                + "   |   Arrendatario " + c.getDocumentoArrendatario() + (moroso ? ": MOROSO" : ": al dia")
                + "   |   Gracia: " + MoraService.DIAS_GRACIA + " dias, recargo " + MoraService.PORCENTAJE_RECARGO + "%");
    }

    // ---------- Registrar pago total o parcial y emitir recibo (RN-09, RN-10) ----------
    // Elimina la cuota (o la garantia) elegida, solo si no tiene pagos
    private void eliminarCuota() {
        Contrato c = (Contrato) comboContratos.getSelectedItem();
        Object numero = Dialogos.seleccionado(tabla, 0);
        if (c == null || numero == null) {
            Dialogos.error(this, "Seleccione una cuota o la garantia de la tabla.");
            return;
        }
        int n = numero instanceof Integer i ? i : Cuota.NUMERO_GARANTIA;
        String que = n == Cuota.NUMERO_GARANTIA ? "la garantia" : "la cuota " + n;
        if (!Dialogos.confirmar(this, "Eliminar", "Eliminar " + que + "?")) return;
        try {
            pagos.eliminarCuota(c.getNumero(), n);
        } catch (IllegalArgumentException ex) {
            Dialogos.error(this, ex.getMessage());
            return;
        }
        refrescar();
    }

    private void registrarPago() {
        Contrato c = (Contrato) comboContratos.getSelectedItem();
        Object numero = Dialogos.seleccionado(tabla, 0);
        if (c == null || numero == null) {
            Dialogos.error(this, "Seleccione una cuota o la garantia de la tabla.");
            return;
        }
        Cuota cuota = pagos.buscarCuota(c.getNumero(), numero instanceof Integer n ? n : Cuota.NUMERO_GARANTIA);
        if (cuota.estaPagada()) {
            Dialogos.error(this, (cuota.esGarantia() ? "La garantia" : "La cuota " + cuota.getNumero()) + " ya esta pagada.");
            return;
        }
        JTextField monto = Dialogos.texto(Dialogos.dinero(cuota.getSaldo()));
        JTextField fecha = Dialogos.texto(Fechas.convertirAString(hoy()));
        JTextField comprobante = Dialogos.texto("");
        if (!Dialogos.formulario(this, cuota.esGarantia() ? "Pago de la garantia" : "Pago de la cuota " + cuota.getNumero(),
                new String[] { cuota.esGarantia() ? "Saldo de la garantia (S/)" : "Saldo de la cuota (S/)", "Monto a pagar (S/)", "Fecha (AAAA-MM-DD)",
                        "Comprobante (foto o PDF, opcional)" },
                new JComponent[] { soloLectura(Dialogos.dinero(cuota.getSaldo())), monto, fecha,
                        Dialogos.campoArchivo(this, comprobante) })) return;
        try {
            Recibo r = pagos.registrarPago(c.getNumero(), cuota.getNumero(),
                    Dialogos.numero(monto, "Monto"), Dialogos.fecha(fecha, "Fecha"));
            String aviso = "";
            if (!comprobante.getText().isEmpty()) {
                try {
                    pagos.anexarComprobante(r.getNumero(), new File(comprobante.getText()));
                } catch (IllegalArgumentException ex) {
                    aviso = "\n\nEl pago quedo registrado, pero el comprobante no se anexo: " + ex.getMessage()
                            + "\nPuede anexarlo despues desde \"Ver recibos\".";
                }
            }
            refrescar();
            Dialogos.info(this, "Recibo emitido", textoRecibo(r) + aviso);
        } catch (IllegalArgumentException ex) {
            Dialogos.error(this, ex.getMessage());
        }
    }

    // ---------- Recibos del contrato: doble clic en una fila abre el comprobante ----------
    private void verRecibos() {
        Contrato c = (Contrato) comboContratos.getSelectedItem();
        if (c == null) {
            Dialogos.error(this, "No hay un contrato seleccionado.");
            return;
        }
        DefaultTableModel modeloRecibos = Dialogos.modelo("Numero", "Fecha", "Concepto", "Pagado (S/)",
                "Saldo de la cuota (S/)", "Comprobante");
        JTable tablaRecibos = Dialogos.tabla(modeloRecibos);
        Dialogos.ocultarColumna(tablaRecibos, 0);
        Runnable cargar = () -> {
            modeloRecibos.setRowCount(0);
            ListaSimple<Recibo> todos = pagos.listarRecibos();
            for (int i = 0; i < todos.getTamano(); i++) {
                Recibo r = todos.obtener(i);
                if (!r.getNumeroContrato().equals(c.getNumero())) continue;
                modeloRecibos.addRow(new Object[] { r.getNumero(), Fechas.convertirAString(r.getFecha()),
                        etiqueta(r.getNumeroCuota()), Dialogos.dinero(r.getMontoPagado()),
                        Dialogos.dinero(r.getSaldoCuota()),
                        r.getComprobante().isEmpty() ? "-" : Archivos.nombre(r.getComprobante()) });
            }
        };
        cargar.run();

        Runnable abrir = () -> accion(() -> {
            Recibo r = reciboSeleccionado(tablaRecibos);
            if (r.getComprobante().isEmpty())
                throw new IllegalArgumentException("Este recibo todavia no tiene comprobante. Use \"Anexar comprobante\".");
            Archivos.abrir(r.getComprobante());
        });
        Dialogos.alDobleClick(tablaRecibos, abrir);

        JDialog dialogo = new JDialog(SwingUtilities.getWindowAncestor(this),
                "Recibos de " + c.getDocumentoArrendatario(), java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        JButton anexar = new JButton("Anexar comprobante");
        anexar.addActionListener(e -> accion(() -> {
            Recibo r = reciboSeleccionado(tablaRecibos);
            File f = Dialogos.elegirArchivo(dialogo);
            if (f == null) return;
            pagos.anexarComprobante(r.getNumero(), f);
            cargar.run();
        }));
        JButton cerrar = new JButton("Cerrar");
        cerrar.addActionListener(e -> dialogo.dispose());
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.CENTER));
        botones.add(anexar);
        botones.add(cerrar);

        dialogo.setLayout(new BorderLayout(6, 6));
        dialogo.add(new JLabel("  Doble clic en un recibo para abrir su comprobante."), BorderLayout.NORTH);
        dialogo.add(new JScrollPane(tablaRecibos), BorderLayout.CENTER);
        dialogo.add(botones, BorderLayout.SOUTH);
        dialogo.setSize(620, 320);
        dialogo.setLocationRelativeTo(this);
        dialogo.setVisible(true);
    }

    private Recibo reciboSeleccionado(JTable t) {
        Object numero = Dialogos.seleccionado(t, 0);
        if (numero == null) throw new IllegalArgumentException("Seleccione un recibo de la tabla.");
        return pagos.buscarRecibo((Integer) numero);
    }

    private void accion(Runnable r) {
        try {
            r.run();
        } catch (IllegalArgumentException ex) {
            Dialogos.error(this, ex.getMessage());
        }
    }

    private JTextField soloLectura(String texto) {
        JTextField f = Dialogos.texto(texto);
        f.setEditable(false);
        return f;
    }
}

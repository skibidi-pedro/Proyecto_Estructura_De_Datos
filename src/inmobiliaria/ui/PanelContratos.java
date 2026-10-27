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
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import inmobiliaria.cobranza.modelo.Cuota;
import inmobiliaria.cobranza.servicio.MoraService;
import inmobiliaria.cobranza.servicio.PagoService;
import inmobiliaria.contratos.modelo.Contrato;
import inmobiliaria.contratos.servicio.ContratoService;
import inmobiliaria.contratos.servicio.LiquidacionGarantia;
import inmobiliaria.nucleo.estructuras.ListaSimple;
import inmobiliaria.nucleo.util.Fechas;
import inmobiliaria.nucleo.util.GeneradorCodigo;
import inmobiliaria.propiedades.modelo.Edificio;
import inmobiliaria.propiedades.modelo.EstadoUnidad;
import inmobiliaria.propiedades.modelo.Unidad;
import inmobiliaria.propiedades.servicio.PropiedadService;

/**
 * Contratos de la propiedad seleccionada: alta, activacion, renovacion y finalizacion.
 *
 * ContratoService (Integrante 2) no conoce a los demas modulos, asi que aqui se
 * hace el pegado que sus comentarios piden:
 *  - unidad disponible       -> PropiedadService (RN-02)
 *  - arrendatario moroso     -> MoraService (RN-11)
 *  - cambiar estado unidad   -> PropiedadService (ALQUILADA al activar, DISPONIBLE al finalizar)
 *  - cuotas al activar/renovar -> PagoService (RN-07)
 *  - deuda y mora al finalizar -> PagoService y MoraService (RN-13)
 *  - el numero de contrato se asigna solo (CON-001, ...) y no se muestra (columna oculta en la tabla)
 *  - ventana no modal (se pueden abrir varias) con refresco automatico cada segundo
 *  - expediente completo (RN-06): el modulo Documentos esta en proceso, por ahora se confirma a mano.
 */
public class PanelContratos extends JDialog {
    private final ContratoService contratos;
    private final PropiedadService propiedades;
    private final PagoService pagos;
    private final MoraService mora;
    private final Edificio edificio;
    private final DefaultTableModel modelo = Dialogos.modelo(
            "Numero", "Unidad", "Arrendatario", "Inicio", "Fin", "Renta (S/)", "Estado");
    private final JTable tabla = Dialogos.tabla(modelo);

    public PanelContratos(Frame dueno, ContratoService contratos, PropiedadService propiedades,
                          PagoService pagos, MoraService mora, Edificio edificio) {
        super(dueno, "Contratos - " + edificio.getNombre(), false);
        this.contratos = contratos;
        this.propiedades = propiedades;
        this.pagos = pagos;
        this.mora = mora;
        this.edificio = edificio;

        setLayout(new BorderLayout(8, 8));
        add(new JLabel("  Propiedad: " + edificio), BorderLayout.NORTH);
        Dialogos.ocultarColumna(tabla, 0);
        add(new JScrollPane(tabla), BorderLayout.CENTER);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.CENTER));
        botones.add(boton("Nuevo contrato", () -> nuevo()));
        botones.add(boton("Activar", () -> activar()));
        botones.add(boton("Renovar", () -> renovar()));
        botones.add(boton("Finalizar", () -> finalizar()));
        botones.add(Dialogos.botonRefrescar(this::refrescar));
        botones.add(boton("Cerrar", () -> dispose()));
        add(botones, BorderLayout.SOUTH);

        refrescar();
        Dialogos.autoRefrescar(this, this::refrescar);
        setSize(760, 380);
        setLocationRelativeTo(dueno);
    }

    private JButton boton(String texto, Runnable accion) {
        JButton b = new JButton(texto);
        b.addActionListener(e -> {
            try {
                accion.run();
            } catch (IllegalArgumentException ex) {
                Dialogos.error(this, ex.getMessage());
            }
        });
        return b;
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

    // En la tabla se muestra la direccion de la unidad, no su codigo
    private String direccionDe(String codigoUnidad) {
        Unidad u = propiedades.buscarUnidad(codigoUnidad);
        return u == null ? "(unidad dada de baja)" : u.getDireccion();
    }

    private void refrescar() {
        List<Object[]> filas = new ArrayList<>();
        ListaSimple<Contrato> lista = contratos.listar();
        for (int i = 0; i < lista.getTamano(); i++) {
            Contrato c = lista.obtener(i);
            if (!esDeLaPropiedad(c.getCodigoUnidad())) continue;
            filas.add(new Object[] { c.getNumero(), direccionDe(c.getCodigoUnidad()), c.getDocumentoArrendatario(),
                    Fechas.convertirAString(c.getFechaInicio()), Fechas.convertirAString(c.getFechaFin()),
                    Dialogos.dinero(c.getRentaMensual()), c.getEstado() });
        }
        Dialogos.poblar(tabla, modelo, filas);
    }

    private Contrato seleccionado() {
        Object numero = Dialogos.seleccionado(tabla, 0);
        if (numero == null) throw new IllegalArgumentException("Seleccione un contrato de la tabla.");
        return contratos.buscar(numero.toString());
    }

    // ---------- Nuevo (RN-01, 02, 03, 04, 05, 11) ----------
    private void nuevo() {
        ListaSimple<Unidad> todas = propiedades.unidadesDe(edificio.getCodigo());
        JComboBox<Unidad> unidades = new JComboBox<>();
        for (int i = 0; i < todas.getTamano(); i++) {
            if (todas.obtener(i).getEstado() == EstadoUnidad.DISPONIBLE) unidades.addItem(todas.obtener(i));
        }
        if (unidades.getItemCount() == 0)
            throw new IllegalArgumentException(
                    "No hay unidades disponibles en esta propiedad. Registrelas en el modulo Unidades.");

        Dialogos.mostrarComo(unidades, u -> u.getDireccion() + " | " + u.getTipo() + " | S/ " + Dialogos.dinero(u.getPrecio()));
        JTextField documento = Dialogos.texto("");
        JTextField inicio = Dialogos.texto(Fechas.convertirAString(hoy()));
        JTextField fin = Dialogos.texto(Fechas.convertirAString(hoy().plusYears(1)));
        JTextField renta = Dialogos.texto(Dialogos.dinero(((Unidad) unidades.getSelectedItem()).getPrecio()));
        JTextField diaPago = Dialogos.texto("5");
        JTextField garantia = Dialogos.texto(renta.getText());
        JTextField clausulas = Dialogos.texto("");
        unidades.addActionListener(e -> {
            Unidad u = (Unidad) unidades.getSelectedItem();
            if (u != null) {
                renta.setText(Dialogos.dinero(u.getPrecio()));
                garantia.setText(renta.getText());
            }
        });

        if (!Dialogos.formulario(this, "Nuevo contrato",
                new String[] { "Documento del arrendatario", "Unidad",
                        "Inicio (AAAA-MM-DD)", "Fin (AAAA-MM-DD)", "Renta mensual (S/)",
                        "Dia de pago (1-28)", "Garantia (S/)", "Clausulas" },
                new JComponent[] { documento, unidades, inicio, fin, renta, diaPago, garantia, clausulas }))
            return;

        String doc = documento.getText().trim();
        Unidad unidad = (Unidad) unidades.getSelectedItem();
        String numero = GeneradorCodigo.siguiente("CON",
                Dialogos.codigos(contratos.listar(), Contrato::getNumero));
        Contrato c = contratos.crear(numero, doc, unidad.getCodigo(),
                Dialogos.fecha(inicio, "Inicio"), Dialogos.fecha(fin, "Fin"),
                Dialogos.numero(renta, "Renta"), Dialogos.entero(diaPago, "Dia de pago"),
                Dialogos.numero(garantia, "Garantia"), clausulas.getText(),
                propiedades.unidadDisponible(unidad.getCodigo()),
                mora.esMoroso(doc, contratos.listar(), hoy()));
        GeneradorCodigo.marcarUsado(numero);
        refrescar();
        Dialogos.info(this, "Contrato", "Contrato creado para " + doc + " (pendiente de activar).");
    }

    // ---------- Activar (RN-06) + estado de unidad + cuotas (RN-07) ----------
    private void activar() {
        Contrato c = seleccionado();
        // TODO: reemplazar por ExpedienteService cuando el modulo Documentos este terminado (RN-06)
        boolean completo = Dialogos.confirmar(this, "Expediente",
                "El expediente del arrendatario " + c.getDocumentoArrendatario() + " esta completo?\n"
                + "(documento de identidad y contrato firmado)\n\n"
                + "El modulo Documentos esta en proceso, por eso se confirma a mano.");
        contratos.activar(c.getNumero(), completo);
        propiedades.cambiarEstadoUnidad(c.getCodigoUnidad(), EstadoUnidad.ALQUILADA);
        ListaSimple<Cuota> cuotas = pagos.generarCuotas(c);
        refrescar();
        Dialogos.info(this, "Contrato", "Contrato de " + c.getDocumentoArrendatario() + " activado.\n"
                + "Se generaron " + cuotas.getTamano() + " cuotas mensuales.");
    }

    // ---------- Renovar (RN-12) + cuotas del periodo nuevo ----------
    private void renovar() {
        Contrato c = seleccionado();
        JTextField nuevaFin = Dialogos.texto(Fechas.convertirAString(c.getFechaFin().plusYears(1)));
        JTextField porcentaje = Dialogos.texto("0");
        if (!Dialogos.formulario(this, "Renovar contrato de " + c.getDocumentoArrendatario(),
                new String[] { "Nueva fecha de fin (AAAA-MM-DD)", "Ajuste de renta (%)" },
                new JComponent[] { nuevaFin, porcentaje })) return;

        LocalDate finAnterior = c.getFechaFin();
        contratos.renovar(c.getNumero(), Dialogos.fecha(nuevaFin, "Nueva fecha de fin"),
                Dialogos.numero(porcentaje, "Ajuste"),
                mora.esMoroso(c.getDocumentoArrendatario(), contratos.listar(), hoy()));
        ListaSimple<Cuota> nuevas = pagos.generarCuotasRenovacion(c, finAnterior);
        refrescar();
        Dialogos.info(this, "Contrato", "Contrato de " + c.getDocumentoArrendatario() + " renovado.\n"
                + "Se generaron " + nuevas.getTamano() + " cuotas nuevas.");
    }

    // ---------- Finalizar (RN-03, RN-13): liquida la garantia y libera la unidad ----------
    private void finalizar() {
        Contrato c = seleccionado();
        JTextField danos = Dialogos.texto("0");
        double deuda = pagos.deudaPendiente(c.getNumero());
        double recargo = mora.totalMora(c.getNumero(), hoy());
        if (!Dialogos.formulario(this, "Finalizar contrato de " + c.getDocumentoArrendatario(),
                new String[] { "Garantia (S/)", "Renta adeudada (S/)", "Recargo por mora (S/)", "Danos a descontar (S/)" },
                new JComponent[] { solo(Dialogos.dinero(c.getGarantia())), solo(Dialogos.dinero(deuda)),
                        solo(Dialogos.dinero(recargo)), danos })) return;

        LiquidacionGarantia liq = contratos.finalizar(c.getNumero(), deuda, recargo,
                Dialogos.numero(danos, "Danos"));
        propiedades.cambiarEstadoUnidad(c.getCodigoUnidad(), EstadoUnidad.DISPONIBLE);
        refrescar();
        Dialogos.info(this, "Liquidacion de garantia", liq.toString());
    }

    // Campo de solo lectura para mostrar un dato en el formulario
    private JTextField solo(String texto) {
        JTextField f = Dialogos.texto(texto);
        f.setEditable(false);
        return f;
    }
}

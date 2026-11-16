package inmobiliaria.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import inmobiliaria.arrendatarios.modelo.Arrendatario;
import inmobiliaria.arrendatarios.servicio.ArrendatarioService;
import inmobiliaria.cobranza.modelo.Cuota;
import inmobiliaria.cobranza.servicio.MoraService;
import inmobiliaria.cobranza.servicio.PagoService;
import inmobiliaria.contratos.modelo.Contrato;
import inmobiliaria.contratos.modelo.EstadoContrato;
import inmobiliaria.contratos.servicio.ContratoService;
import inmobiliaria.contratos.servicio.LiquidacionGarantia;
import inmobiliaria.documentos.servicio.ExpedienteService;
import inmobiliaria.nucleo.estructuras.ListaSimple;
import inmobiliaria.nucleo.util.Archivos;
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
 *  - cambiar estado unidad   -> PropiedadService (ALQUILADA al activar, NO_ALQUILADA al finalizar)
 *  - cuotas al activar/renovar -> PagoService (RN-07)
 *  - deuda y mora al finalizar -> PagoService y MoraService (RN-13)
 *  - el numero de contrato se asigna solo (CON-001, ...) y no se muestra (columna oculta en la tabla)
 *  - es un panel que MenuPrincipal muestra a la derecha de la ventana, con refresco automatico cada segundo
 *  - arrendatario            -> se elige de ArrendatarioService (tambien los inactivos)
 *  - documentos (RN-06)      -> identidad del arrendatario (ExpedienteService) y documento del
 *    contrato. Se puede activar sin ellos: pide confirmacion y queda resaltado en amarillo (ver Activacion)
 *  - el documento del contrato (PDF, Word o foto) se anexa al crear el contrato o despues con
 *    "Anexar documento"; la columna Archivo muestra su nombre (Contrato.pdf) y con doble clic se abre
 *  - cuando los documentos quedan completos se pregunta si activar el contrato (Si / No)
 */
public class PanelContratos extends JPanel {
    private final ContratoService contratos;
    private final PropiedadService propiedades;
    private final PagoService pagos;
    private final MoraService mora;
    private final ArrendatarioService arrendatarios;
    private final ExpedienteService expedientes;
    private final Edificio edificio;
    private final DefaultTableModel modelo = Dialogos.modelo(
            "Numero", "Unidad", "Arrendatario", "Inicio", "Fin", "Renta (S/)", "Archivo", "Estado");
    private final JTable tabla = Dialogos.tabla(modelo);

    public PanelContratos(ContratoService contratos, PropiedadService propiedades,
                          PagoService pagos, MoraService mora, ArrendatarioService arrendatarios,
                          ExpedienteService expedientes, Edificio edificio) {
        this.contratos = contratos;
        this.propiedades = propiedades;
        this.pagos = pagos;
        this.mora = mora;
        this.arrendatarios = arrendatarios;
        this.expedientes = expedientes;
        this.edificio = edificio;

        setLayout(new BorderLayout(8, 8));
        add(new JLabel("  Propiedad: " + edificio), BorderLayout.NORTH);
        Dialogos.ocultarColumna(tabla, 0);
        add(new JScrollPane(tabla), BorderLayout.CENTER);
        Dialogos.alDobleClick(tabla, () -> ejecutar(this::abrirDocumento));
        Dialogos.colorear(tabla, this::colorDe);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.CENTER));
        botones.add(boton("Nuevo contrato", () -> nuevo()));
        botones.add(boton("Anexar documento", () -> anexarDocumento()));
        botones.add(boton("Activar", () -> activar()));
        botones.add(boton("Renovar", () -> renovar()));
        botones.add(boton("Finalizar", () -> finalizar()));
        add(botones, BorderLayout.SOUTH);

        refrescar();
        Dialogos.autoRefrescar(this, this::refrescar);
    }

    private JButton boton(String texto, Runnable accion) {
        JButton b = new JButton(texto);
        b.addActionListener(e -> ejecutar(accion));
        return b;
    }

    private void ejecutar(Runnable accion) {
        try {
            accion.run();
        } catch (IllegalArgumentException ex) {
            Dialogos.error(this, ex.getMessage());
        }
    }

    // Nombre del arrendatario para los mensajes (si se dio de baja, su documento)
    private String nombreDe(String documento) {
        Arrendatario a = arrendatarios.buscar(documento);
        return a == null ? documento : a.getNombresCompletos();
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

    // Archivo: verde con documento del contrato, amarillo si falta.
    // Estado: VIGENTE verde (amarillo si le faltan documentos), PENDIENTE amarillo, FINALIZADO rojo.
    private java.awt.Color colorDe(int fila, int columna) {
        Contrato c = contratos.buscar(modelo.getValueAt(fila, 0).toString());
        if (c == null) return null;
        if (columna == 6) return c.getDocumento().isEmpty() ? Dialogos.AMARILLO : Dialogos.VERDE;
        if (columna != 7) return null;
        switch (c.getEstado()) {
            case VIGENTE: return Activacion.faltantes(c, expedientes).isEmpty() ? Dialogos.VERDE : Dialogos.AMARILLO;
            case PENDIENTE: return Dialogos.AMARILLO;
            default: return Dialogos.ROJO;   // FINALIZADO
        }
    }

    private void refrescar() {
        List<Object[]> filas = new ArrayList<>();
        ListaSimple<Contrato> lista = contratos.listar();
        for (int i = 0; i < lista.getTamano(); i++) {
            Contrato c = lista.obtener(i);
            if (!esDeLaPropiedad(c.getCodigoUnidad())) continue;
            filas.add(new Object[] { c.getNumero(), direccionDe(c.getCodigoUnidad()), c.getDocumentoArrendatario(),
                    Fechas.convertirAString(c.getFechaInicio()), Fechas.convertirAString(c.getFechaFin()),
                    Dialogos.dinero(c.getRentaMensual()),
                    c.getDocumento().isEmpty() ? "Falta" : Archivos.nombre(c.getDocumento()), c.getEstado() });
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
            if (todas.obtener(i).getEstado() == EstadoUnidad.NO_ALQUILADA) unidades.addItem(todas.obtener(i));
        }
        if (unidades.getItemCount() == 0)
            throw new IllegalArgumentException(
                    "No hay unidades sin alquilar en esta propiedad. Registrelas en el modulo Unidades.");

        Dialogos.mostrarComo(unidades, u -> u.getDireccion() + " | " + u.getTipo() + " | S/ " + Dialogos.dinero(u.getPrecio()));

        JComboBox<Arrendatario> inquilinos = new JComboBox<>();
        ListaSimple<Arrendatario> registrados = arrendatarios.listar();
        for (int i = 0; i < registrados.getTamano(); i++) {
            inquilinos.addItem(registrados.obtener(i));
        }
        if (inquilinos.getItemCount() == 0)
            throw new IllegalArgumentException(
                    "No hay arrendatarios activos. Registrelos en el modulo Arrendatarios.");
        Dialogos.mostrarComo(inquilinos, a -> a.getNombresCompletos() + " (" + a.getDocumentoIdentidad() + ")");
        JTextField inicio = Dialogos.texto(Fechas.convertirAString(hoy()));
        JTextField fin = Dialogos.texto(Fechas.convertirAString(hoy().plusYears(1)));
        JTextField renta = Dialogos.texto(Dialogos.dinero(((Unidad) unidades.getSelectedItem()).getPrecio()));
        JTextField diaPago = Dialogos.texto("5");
        JTextField garantia = Dialogos.texto(renta.getText());
        JTextField archivo = Dialogos.texto("");
        unidades.addActionListener(e -> {
            Unidad u = (Unidad) unidades.getSelectedItem();
            if (u != null) {
                renta.setText(Dialogos.dinero(u.getPrecio()));
                garantia.setText(renta.getText());
            }
        });

        if (!Dialogos.formulario(this, "Nuevo contrato",
                new String[] { "Arrendatario", "Unidad",
                        "Inicio (AAAA-MM-DD)", "Fin (AAAA-MM-DD)", "Renta mensual (S/)",
                        "Dia de pago (1-28)", "Garantia (S/)", "Documento del contrato (archivo)" },
                new JComponent[] { inquilinos, unidades, inicio, fin, renta, diaPago, garantia,
                        Dialogos.campoArchivo(this, archivo) }))
            return;
        File documento = archivoElegido(archivo);   // se revisa antes de crear el contrato

        String doc = ((Arrendatario) inquilinos.getSelectedItem()).getDocumentoIdentidad();
        Unidad unidad = (Unidad) unidades.getSelectedItem();
        String numero = GeneradorCodigo.siguiente("CON",
                Dialogos.codigos(contratos.listar(), Contrato::getNumero));
        Contrato c = contratos.crear(numero, doc, unidad.getCodigo(),
                Dialogos.fecha(inicio, "Inicio"), Dialogos.fecha(fin, "Fin"),
                Dialogos.numero(renta, "Renta"), Dialogos.entero(diaPago, "Dia de pago"),
                Dialogos.numero(garantia, "Garantia"), "",
                propiedades.unidadDisponible(unidad.getCodigo()),
                mora.esMoroso(doc, contratos.listar(), hoy()));
        GeneradorCodigo.marcarUsado(numero);
        if (documento != null) {
            try {
                contratos.anexarDocumento(numero, documento);
            } catch (IllegalArgumentException ex) {
                Dialogos.error(this, "El contrato se creo, pero no se pudo anexar el documento: " + ex.getMessage()
                        + "\nUse \"Anexar documento\" para intentarlo de nuevo.");
            }
        }
        refrescar();
        ofrecerActivacion(c);
    }

    // Revisa el archivo elegido ANTES de guardar nada
    private File archivoElegido(JTextField ruta) {
        if (ruta.getText().isEmpty()) return null;
        File f = new File(ruta.getText());
        if (!f.isFile()) throw new IllegalArgumentException("No se encuentra el archivo elegido.");
        if (!Archivos.extensionValida(f.getName()))
            throw new IllegalArgumentException("Formato no permitido. Use PDF, Word (doc, docx) o imagen (jpg, png).");
        return f;
    }

    // Si ya estan todos los documentos pregunta si activar; si no, avisa cuales faltan
    private void ofrecerActivacion(Contrato c) {
        String falta = Activacion.faltantes(c, expedientes);
        if (falta.isEmpty()) {
            Activacion.ofrecer(this, c.getDocumentoArrendatario(), nombreDe(c.getDocumentoArrendatario()),
                    contratos, propiedades, pagos, expedientes);
        } else {
            Dialogos.info(this, "Contrato", "Contrato creado para " + nombreDe(c.getDocumentoArrendatario())
                    + " (pendiente de activar).\nPara activarlo falta " + falta + ".");
        }
    }

    // ---------- Anexar o reemplazar el documento del contrato ----------
    private void anexarDocumento() {
        Contrato c = seleccionado();
        File f = Dialogos.elegirArchivo(this);
        if (f == null) return;
        contratos.anexarDocumento(c.getNumero(), f);
        refrescar();
        ofrecerActivacion(c);
    }

    // Doble clic: abre el documento del contrato
    private void abrirDocumento() {
        Contrato c = seleccionado();
        if (c.getDocumento().isEmpty())
            throw new IllegalArgumentException("Este contrato todavia no tiene documento. Use \"Anexar documento\".");
        Archivos.abrir(c.getDocumento());
    }

    // ---------- Activar (RN-06) + estado de unidad + cuotas (RN-07) ----------
    private void activar() {
        activarContrato(seleccionado());
    }

    private void activarContrato(Contrato c) {
        ListaSimple<Cuota> cuotas = Activacion.activar(c, contratos, propiedades, pagos);
        refrescar();
        Dialogos.info(this, "Contrato", "Contrato de " + nombreDe(c.getDocumentoArrendatario()) + " activado.\n"
                + Activacion.resumen(c, cuotas));
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
                + "Se generaron " + Activacion.mensuales(nuevas) + " cuotas nuevas.");
    }

    // ---------- Finalizar (RN-03, RN-13): liquida la garantia y libera la unidad ----------
    private void finalizar() {
        Contrato c = seleccionado();
        JTextField danos = Dialogos.texto("0");
        double deuda = pagos.deudaPendiente(c.getNumero());
        double recargo = mora.totalMora(c.getNumero(), hoy());
        double garantiaPagada = pagos.garantiaPagada(c);   // lo realmente entregado (cuota 0)
        if (!Dialogos.formulario(this, "Finalizar contrato de " + c.getDocumentoArrendatario(),
                new String[] { "Garantia pagada (S/)", "Renta adeudada (S/)", "Recargo por mora (S/)", "Danos a descontar (S/)" },
                new JComponent[] { solo(Dialogos.dinero(garantiaPagada)), solo(Dialogos.dinero(deuda)),
                        solo(Dialogos.dinero(recargo)), danos })) return;

        LiquidacionGarantia liq = contratos.finalizar(c.getNumero(), deuda, recargo,
                Dialogos.numero(danos, "Danos"), garantiaPagada);
        propiedades.cambiarEstadoUnidad(c.getCodigoUnidad(), EstadoUnidad.NO_ALQUILADA);
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

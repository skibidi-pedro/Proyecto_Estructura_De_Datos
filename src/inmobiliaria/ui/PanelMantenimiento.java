package inmobiliaria.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import inmobiliaria.mantenimiento.modelo.SolicitudMantenimiento;
import inmobiliaria.mantenimiento.modelo.Urgencia;
import inmobiliaria.mantenimiento.servicio.MantenimientoService;
import inmobiliaria.nucleo.estructuras.ListaSimple;
import inmobiliaria.nucleo.util.GeneradorCodigo;
import inmobiliaria.propiedades.modelo.Edificio;
import inmobiliaria.propiedades.modelo.Unidad;
import inmobiliaria.propiedades.servicio.PropiedadService;

/**
 * Solicitudes de mantenimiento de la propiedad seleccionada (2.6).
 * "Atender siguiente" saca de la cola de prioridad la solicitud mas urgente de esta propiedad
 * (y la mas antigua entre las de igual urgencia); queda EN_PROCESO hasta pulsar "Terminar".
 * El codigo de la solicitud (MAN-001, ...) se asigna solo y no se muestra.
 * Es un panel que MenuPrincipal muestra a la derecha de la ventana; se refresca solo cada segundo.
 */
public class PanelMantenimiento extends JPanel {
    private final MantenimientoService mantenimiento;
    private final PropiedadService propiedades;
    private final Edificio edificio;
    private final DefaultTableModel modelo = Dialogos.modelo(
            "Codigo", "Unidad", "Descripcion", "Urgencia", "Estado", "Fecha");
    private final JTable tabla = Dialogos.tabla(modelo);

    public PanelMantenimiento(MantenimientoService mantenimiento, PropiedadService propiedades, Edificio edificio) {
        this.mantenimiento = mantenimiento;
        this.propiedades = propiedades;
        this.edificio = edificio;

        setLayout(new BorderLayout(8, 8));
        add(new JLabel("  Propiedad: " + edificio), BorderLayout.NORTH);
        Dialogos.ocultarColumna(tabla, 0);
        add(new JScrollPane(tabla), BorderLayout.CENTER);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.CENTER));
        botones.add(boton("Nueva solicitud", this::nueva));
        botones.add(boton("Atender siguiente", this::atenderSiguiente));
        botones.add(boton("Terminar", this::terminar));
        botones.add(Dialogos.botonRefrescar(this::refrescar));
        add(botones, BorderLayout.SOUTH);

        refrescar();
        Dialogos.autoRefrescar(this, this::refrescar);
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

    private ListaSimple<String> codigosDeUnidades() {
        return Dialogos.codigos(propiedades.unidadesDe(edificio.getCodigo()), Unidad::getCodigo);
    }

    private boolean esDeLaPropiedad(String codigoUnidad) {
        ListaSimple<String> codigos = codigosDeUnidades();
        for (int i = 0; i < codigos.getTamano(); i++) {
            if (codigos.obtener(i).equals(codigoUnidad)) return true;
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
        ListaSimple<SolicitudMantenimiento> lista = mantenimiento.listar();
        for (int i = 0; i < lista.getTamano(); i++) {
            SolicitudMantenimiento s = lista.obtener(i);
            if (!esDeLaPropiedad(s.getIdentificadorUnidad())) continue;
            filas.add(new Object[] { s.getCodigo(), direccionDe(s.getIdentificadorUnidad()), s.getDescripcion(),
                    s.getNivelUrgencia(), s.getEstado(), s.getFechaRegistro() });
        }
        Dialogos.poblar(tabla, modelo, filas);
    }

    private void nueva() {
        ListaSimple<Unidad> todas = propiedades.unidadesDe(edificio.getCodigo());
        if (todas.getTamano() == 0)
            throw new IllegalArgumentException("Esta propiedad no tiene unidades. Registrelas en el modulo Unidades.");
        JComboBox<Unidad> unidades = new JComboBox<>();
        for (int i = 0; i < todas.getTamano(); i++) unidades.addItem(todas.obtener(i));
        Dialogos.mostrarComo(unidades, u -> u.getDireccion() + " | " + u.getTipo());
        JTextField descripcion = Dialogos.texto("");
        JComboBox<Urgencia> urgencia = new JComboBox<>(Urgencia.values());
        urgencia.setSelectedItem(Urgencia.MEDIA);

        if (!Dialogos.formulario(this, "Nueva solicitud de mantenimiento",
                new String[] { "Unidad", "Descripcion", "Urgencia" },
                new JComponent[] { unidades, descripcion, urgencia })) return;

        String codigo = GeneradorCodigo.siguiente("MAN",
                Dialogos.codigos(mantenimiento.listar(), SolicitudMantenimiento::getCodigo));
        mantenimiento.registrar(codigo, ((Unidad) unidades.getSelectedItem()).getCodigo(),
                descripcion.getText(), (Urgencia) urgencia.getSelectedItem(), LocalDate.now());
        GeneradorCodigo.marcarUsado(codigo);
        refrescar();
    }

    private void atenderSiguiente() {
        SolicitudMantenimiento s = mantenimiento.atenderSiguiente(codigosDeUnidades());
        refrescar();
        Dialogos.info(this, "Mantenimiento", "En proceso: " + s.getDescripcion() + "\n"
                + "Unidad: " + direccionDe(s.getIdentificadorUnidad()) + "\nUrgencia: " + s.getNivelUrgencia());
    }

    private void terminar() {
        Object codigo = Dialogos.seleccionado(tabla, 0);
        if (codigo == null) throw new IllegalArgumentException("Seleccione una solicitud de la tabla.");
        mantenimiento.terminar(codigo.toString());
        refrescar();
    }
}

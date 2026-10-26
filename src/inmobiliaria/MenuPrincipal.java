package inmobiliaria;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import inmobiliaria.nucleo.estructuras.ListaSimple;
import inmobiliaria.nucleo.persistencia.GestorCSV;
import inmobiliaria.nucleo.persistencia.Persistible;
import inmobiliaria.nucleo.historial.Historial;

/**
 * Menu principal en Swing. Al iniciar hay que seleccionar o registrar una
 * propiedad; recien despues se habilitan los modulos (arrendatarios,
 * contratos, documentos, cobro, mantenimiento) y los reportes.
 * El historial tiene su propio boton, siempre visible abajo.
 *
 * Responsable: Integrante 1 - Francia Ramos Johan Mitziu
 */
public class MenuPrincipal extends JFrame {

    private static final String CARD_SELECCION = "seleccion";
    private static final String CARD_MODULOS = "modulos";

    private record Opcion(String titulo, String descripcion) {}

    private final CardLayout cartas = new CardLayout();
    private final JPanel contenedor = new JPanel(cartas);
    // TODO: reemplazar por las propiedades cargadas desde CSV / PropiedadService
    private final DefaultComboBoxModel<String> propiedades = new DefaultComboBoxModel<>();
    private final JComboBox<String> comboPropiedades = new JComboBox<>(propiedades);
    private final JLabel lblPropiedad = new JLabel(" ", SwingConstants.CENTER);
    private final JMenu menuModulos = new JMenu("Módulos");
    private final JMenu menuReportes = new JMenu("Reportes");
    private final JMenuItem itemCambiar = new JMenuItem("Cambiar propiedad");
    // Instancia del historial para manejar la pila de operaciones y el deshacer
    private final Historial historial = new Historial();

    // Modulos que dependen de la propiedad seleccionada
    private final Opcion[] opciones = {
        new Opcion("Arrendatarios", "Datos personales y avales"),
        new Opcion("Contratos", "Alta, renovación y finalización"),
        new Opcion("Documentos", "Expediente por arrendatario"),
        new Opcion("Cobro de rentas y mora", "Cuotas, pagos y recibos"),
        new Opcion("Mantenimiento", "Solicitudes de reparación"),
    };

    // Reportes y alertas: pendientes de implementar
    private final String[] reportes = {
        "Ingresos", "Morosidad", "Ocupación", "Alertas",
    };

    public MenuPrincipal() {
        super("Gestión Inmobiliaria - Grupo 6");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { salir(); }
        });

        setJMenuBar(crearBarra());
        contenedor.add(crearPanelSeleccion(), CARD_SELECCION);
        contenedor.add(crearPanelModulos(), CARD_MODULOS);
        add(contenedor, BorderLayout.CENTER);
        add(crearPanelHistorial(), BorderLayout.SOUTH);

        mostrarSeleccion();
        setSize(640, 460);
        setLocationRelativeTo(null);
    }

    // ---------- Pantalla inicial: seleccionar o registrar una propiedad ----------
    private JPanel crearPanelSeleccion() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);
        c.gridx = 0; c.gridy = 0; c.gridwidth = 2;
        JLabel l = new JLabel("Seleccione o registre una propiedad", SwingConstants.CENTER);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 18f));
        p.add(l, c);

        c.gridy = 1; c.fill = GridBagConstraints.HORIZONTAL;
        comboPropiedades.setPreferredSize(new Dimension(320, 30));
        p.add(comboPropiedades, c);

        c.gridy = 2; c.gridwidth = 1; c.fill = GridBagConstraints.NONE;
        JButton nueva = new JButton("Nueva propiedad");
        nueva.addActionListener(e -> nuevaPropiedad());
        p.add(nueva, c);

        c.gridx = 1;
        JButton continuar = new JButton("Continuar");
        continuar.addActionListener(e -> continuarConPropiedad());
        p.add(continuar, c);
        return p;
    }

    private void nuevaPropiedad() {
        // TODO: delegar el alta a PropiedadService (RN-01: identificador unico)
        String nombre = JOptionPane.showInputDialog(this, "Nombre o dirección de la propiedad:");
        if (nombre != null && !nombre.isBlank()) {
            String limpio = nombre.trim();
            if (propiedades.getIndexOf(limpio) >= 0) {
                JOptionPane.showMessageDialog(this, "Ya existe la propiedad \"" + limpio + "\".",
                    "Propiedad", JOptionPane.WARNING_MESSAGE);
                return;
            }
            propiedades.addElement(limpio);
            propiedades.setSelectedItem(limpio);
            // Registramos la operacion en la pila del historial
            historial.registrarOperacion("Nueva propiedad registrada: " + limpio);
        }
    }

    private void continuarConPropiedad() {
        Object sel = comboPropiedades.getSelectedItem();
        if (sel == null) {
            JOptionPane.showMessageDialog(this, "Seleccione o registre una propiedad.",
                "Propiedad", JOptionPane.WARNING_MESSAGE);
            return;
        }
        lblPropiedad.setText("Propiedad: " + sel);
        menuModulos.setEnabled(true);
        menuReportes.setEnabled(true);
        itemCambiar.setEnabled(true);
        cartas.show(contenedor, CARD_MODULOS);
    }

    private void mostrarSeleccion() {
        menuModulos.setEnabled(false);
        menuReportes.setEnabled(false);
        itemCambiar.setEnabled(false);
        cartas.show(contenedor, CARD_SELECCION);
    }

    // ---------- Pantalla de modulos (tras elegir la propiedad) ----------
    private JPanel crearPanelModulos() {
        JPanel p = new JPanel(new BorderLayout());
        lblPropiedad.setFont(lblPropiedad.getFont().deriveFont(Font.BOLD, 16f));
        lblPropiedad.setBorder(BorderFactory.createEmptyBorder(15, 10, 5, 10));
        p.add(lblPropiedad, BorderLayout.NORTH);

        JPanel rejilla = new JPanel(new GridLayout(0, 2, 12, 12));
        rejilla.setBorder(BorderFactory.createEmptyBorder(10, 30, 20, 30));
        for (Opcion o : opciones) {
            JButton b = new JButton("<html><center><b>" + o.titulo() + "</b><br><small>"
                + o.descripcion() + "</small></center></html>");
            b.addActionListener(e -> abrir(o.titulo()));
            rejilla.add(b);
        }
        p.add(rejilla, BorderLayout.CENTER);
        return p;
    }

    // ---------- Boton de historial, aparte de los modulos ----------
    private JPanel crearPanelHistorial() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER));
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        JButton b = new JButton("Historial y deshacer");
        b.addActionListener(e -> ejecutarDeshacer());
        p.add(b);
        return p;
    }

    private JMenuBar crearBarra() {
        JMenuBar barra = new JMenuBar();

        JMenu archivo = new JMenu("Archivo");
        itemCambiar.addActionListener(e -> mostrarSeleccion());
        JMenuItem guardar = new JMenuItem("Guardar datos (CSV)");
        guardar.addActionListener(e -> guardarDatosCSV());

        //Opcion de deshacer en la barra de menu
        JMenuItem itemDeshacer = new JMenuItem("Deshacer ultima operacion");
        itemDeshacer.addActionListener(e -> ejecutarDeshacer());

        archivo.add(itemCambiar);
        archivo.add(guardar);
        archivo.add(itemDeshacer);
        barra.add(archivo);

        for (Opcion o : opciones) {
            JMenuItem item = new JMenuItem(o.titulo());
            item.addActionListener(e -> abrir(o.titulo()));
            menuModulos.add(item);
        }
        barra.add(menuModulos);

        // Reportes ocupa el lugar que tenia Ayuda
        for (String r : reportes) {
            JMenuItem item = new JMenuItem(r);
            item.addActionListener(e -> reportePendiente(r));
            menuReportes.add(item);
        }
        barra.add(menuReportes);
        return barra;
    }

    private void ejecutarDeshacer() {
        Object ultimaOp = historial.deshacer();
        if (ultimaOp != null) {
            JOptionPane.showMessageDialog(this, "Operacion deshecha con exito:\n" + ultimaOp.toString(),
            "Deshacer", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "No hay operaciones registradas para deshacer.",
                "Deshacer", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void guardarDatosCSV() {
        ListaSimple<Persistible> listaPersistible = new ListaSimple<>();
        GestorCSV.guardar("propietarios.csv", listaPersistible);
        JOptionPane.showMessageDialog(this, "Datos guardados exitosamente en CSV.",
            "Persistencia", JOptionPane.INFORMATION_MESSAGE);
    }

    /** TODO: reemplazar por la apertura del panel real de cada modulo. */
    private void abrir(String modulo) {
        JOptionPane.showMessageDialog(this, "Módulo \"" + modulo + "\" aún no implementado.",
            modulo, JOptionPane.INFORMATION_MESSAGE);
    }

    /** TODO: conectar con ReporteService y AlertaService. */
    private void reportePendiente(String reporte) {
        JOptionPane.showMessageDialog(this, "El reporte \"" + reporte + "\" está pendiente.",
            "Reportes", JOptionPane.INFORMATION_MESSAGE);
    }

    private void salir() {
        int r = JOptionPane.showConfirmDialog(this, "¿Desea salir?", "Salir",
            JOptionPane.YES_NO_OPTION);
        if (r == JOptionPane.YES_OPTION) {
            // TODO: guardar datos en CSV antes de cerrar (2.8)
            ListaSimple<Persistible> listaPersistible = new ListaSimple<>();
            GestorCSV.guardar("propietarios.csv", listaPersistible);
            dispose();
            System.exit(0);
        }
    }
}

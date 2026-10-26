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
 * Menu principal en Swing. Primero se selecciona un propietario; en la pantalla
 * siguiente se selecciona o registra una de sus propiedades (panel pequeno,
 * arriba a la izquierda). Los modulos y reportes se habilitan recien cuando hay
 * una propiedad seleccionada. El historial tiene su propio boton, siempre visible.
 *
 * Responsable: Integrante 1 - Francia Ramos Johan Mitziu
 */
public class MenuPrincipal extends JFrame {

    private static final String CARD_SELECCION = "seleccion";
    private static final String CARD_MODULOS = "modulos";

    private record Opcion(String titulo, String descripcion) {}

    private final CardLayout cartas = new CardLayout();
    private final JPanel contenedor = new JPanel(cartas);

    // TODO: reemplazar por los propietarios cargados desde CSV / PropiedadService
    private final DefaultComboBoxModel<String> propietarios = new DefaultComboBoxModel<>();
    private final JComboBox<String> comboPropietarios = new JComboBox<>(propietarios);
    private final JLabel lblPropietario = new JLabel(" ");

    // TODO: reemplazar por las propiedades del propietario (PropiedadService.unidadesDePropietario)
    private final DefaultComboBoxModel<String> propiedades = new DefaultComboBoxModel<>();
    private final JComboBox<String> comboPropiedades = new JComboBox<>(propiedades);
    private final JLabel lblPropiedad = new JLabel("Ninguna propiedad seleccionada");
    private String propietarioActual = null;
    private boolean propiedadElegida = false;

    private final JButton[] botonesModulos;
    private final JMenu menuModulos = new JMenu("Módulos");
    private final JMenu menuReportes = new JMenu("Reportes");
    private final JMenuItem itemCambiar = new JMenuItem("Cambiar propietario");
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

        botonesModulos = new JButton[opciones.length];
        setJMenuBar(crearBarra());
        contenedor.add(crearPanelSeleccion(), CARD_SELECCION);
        contenedor.add(crearPanelModulos(), CARD_MODULOS);
        add(contenedor, BorderLayout.CENTER);
        add(crearPanelHistorial(), BorderLayout.SOUTH);

        mostrarSeleccion();
        setSize(640, 460);
        setLocationRelativeTo(null);
    }

    // ---------- Pantalla inicial: seleccionar propietario ----------
    private JPanel crearPanelSeleccion() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);
        c.gridx = 0; c.gridy = 0; c.gridwidth = 2;
        JLabel l = new JLabel("Seleccione un propietario", SwingConstants.CENTER);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 18f));
        p.add(l, c);

        c.gridy = 1; c.fill = GridBagConstraints.HORIZONTAL;
        comboPropietarios.setPreferredSize(new Dimension(320, 30));
        p.add(comboPropietarios, c);

        c.gridy = 2; c.gridwidth = 1; c.fill = GridBagConstraints.NONE;
        JButton nuevo = new JButton("Nuevo propietario");
        nuevo.addActionListener(e -> nuevoPropietario());
        p.add(nuevo, c);

        c.gridx = 1;
        JButton continuar = new JButton("Continuar");
        continuar.addActionListener(e -> continuarConPropietario());
        p.add(continuar, c);
        return p;
    }

    private void nuevoPropietario() {
        // TODO: delegar el alta a PropiedadService (RN-01: identificador unico)
        String nombre = JOptionPane.showInputDialog(this, "Nombre del propietario:");
        if (nombre != null && !nombre.isBlank()) {
            propietarios.addElement(nombre.trim());
            propietarios.setSelectedItem(nombre.trim());
            // Registramos la operacion en la pila del historial
            historial.registrarOperacion("Nuevo propietario registrado: " + nombre.trim());
        }
    }

    private void continuarConPropietario() {
        Object sel = comboPropietarios.getSelectedItem();
        if (sel == null) {
            JOptionPane.showMessageDialog(this, "Seleccione o cree un propietario.",
                "Propietario", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String propietario = sel.toString();
        // Si cambia de propietario, la propiedad elegida deja de valer
        if (!propietario.equals(propietarioActual)) {
            propietarioActual = propietario;
            propiedades.removeAllElements();
            ponerPropiedadElegida(false, null);
        }
        lblPropietario.setText("Propietario: " + propietario);
        itemCambiar.setEnabled(true);
        cartas.show(contenedor, CARD_MODULOS);
    }

    private void mostrarSeleccion() {
        itemCambiar.setEnabled(false);
        menuModulos.setEnabled(false);
        menuReportes.setEnabled(false);
        cartas.show(contenedor, CARD_SELECCION);
    }

    // ---------- Pantalla principal: propiedad (pequena, arriba a la izquierda) y modulos ----------
    private JPanel crearPanelModulos() {
        JPanel p = new JPanel(new BorderLayout());

        JPanel norte = new JPanel();
        norte.setLayout(new BoxLayout(norte, BoxLayout.Y_AXIS));
        norte.setBorder(BorderFactory.createEmptyBorder(10, 12, 0, 12));

        lblPropietario.setFont(lblPropietario.getFont().deriveFont(Font.BOLD, 14f));
        lblPropietario.setAlignmentX(Component.LEFT_ALIGNMENT);
        norte.add(lblPropietario);
        norte.add(Box.createVerticalStrut(6));

        JPanel panelPropiedad = crearPanelPropiedad();
        panelPropiedad.setAlignmentX(Component.LEFT_ALIGNMENT);
        norte.add(panelPropiedad);
        p.add(norte, BorderLayout.NORTH);

        JPanel rejilla = new JPanel(new GridLayout(0, 2, 12, 12));
        rejilla.setBorder(BorderFactory.createEmptyBorder(10, 30, 20, 30));
        for (int i = 0; i < opciones.length; i++) {
            Opcion o = opciones[i];
            JButton b = new JButton("<html><center><b>" + o.titulo() + "</b><br><small>"
                + o.descripcion() + "</small></center></html>");
            b.addActionListener(e -> abrir(o.titulo()));
            b.setEnabled(false);
            botonesModulos[i] = b;
            rejilla.add(b);
        }
        p.add(rejilla, BorderLayout.CENTER);
        return p;
    }

    // Panel compacto: combo + Nueva + Elegir
    private JPanel crearPanelPropiedad() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder("Propiedad"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 4, 2, 4);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0; c.gridy = 0; c.gridwidth = 3;
        comboPropiedades.setPreferredSize(new Dimension(220, 24));
        p.add(comboPropiedades, c);

        c.gridy = 1; c.gridwidth = 1;
        JButton nueva = new JButton("Nueva");
        nueva.setMargin(new Insets(1, 8, 1, 8));
        nueva.addActionListener(e -> nuevaPropiedad());
        p.add(nueva, c);

        c.gridx = 1;
        JButton elegir = new JButton("Elegir");
        elegir.setMargin(new Insets(1, 8, 1, 8));
        elegir.addActionListener(e -> elegirPropiedad());
        p.add(elegir, c);

        c.gridx = 0; c.gridy = 2; c.gridwidth = 3;
        lblPropiedad.setFont(lblPropiedad.getFont().deriveFont(Font.ITALIC, 11f));
        p.add(lblPropiedad, c);

        // Que el panel no se estire: queda chico en la esquina
        p.setMaximumSize(p.getPreferredSize());
        return p;
    }

    private void nuevaPropiedad() {
        // TODO: delegar el alta a PropiedadService (RN-01: identificador unico)
        String nombre = JOptionPane.showInputDialog(this, "Nombre o dirección de la propiedad:");
        if (nombre == null || nombre.isBlank()) return;
        String limpio = nombre.trim();
        if (propiedades.getIndexOf(limpio) >= 0) {
            JOptionPane.showMessageDialog(this, "Ya existe la propiedad \"" + limpio + "\".",
                "Propiedad", JOptionPane.WARNING_MESSAGE);
            return;
        }
        propiedades.addElement(limpio);
        propiedades.setSelectedItem(limpio);
        historial.registrarOperacion("Nueva propiedad registrada: " + limpio
            + " (propietario " + propietarioActual + ")");
        ponerPropiedadElegida(true, limpio);
    }

    private void elegirPropiedad() {
        Object sel = comboPropiedades.getSelectedItem();
        if (sel == null) {
            JOptionPane.showMessageDialog(this, "Seleccione o registre una propiedad.",
                "Propiedad", JOptionPane.WARNING_MESSAGE);
            return;
        }
        ponerPropiedadElegida(true, sel.toString());
    }

    // Habilita o deshabilita modulos y reportes segun haya propiedad elegida
    private void ponerPropiedadElegida(boolean elegida, String nombre) {
        propiedadElegida = elegida;
        lblPropiedad.setText(elegida ? "Elegida: " + nombre : "Ninguna propiedad seleccionada");
        for (JButton b : botonesModulos) {
            b.setEnabled(elegida);
        }
        menuModulos.setEnabled(elegida);
        menuReportes.setEnabled(elegida);
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
        if (!propiedadElegida) return;
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

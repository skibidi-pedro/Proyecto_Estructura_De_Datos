package inmobiliaria;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Menu principal en Swing. Al iniciar solo se puede seleccionar un propietario;
 * los demas modulos aparecen una vez seleccionado.
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
    private final JLabel lblPropietario = new JLabel(" ", SwingConstants.CENTER);
    private final JMenu menuModulos = new JMenu("Módulos");
    private final JMenuItem itemCambiar = new JMenuItem("Cambiar propietario");

    private final Opcion[] opciones = {
        new Opcion("Propiedades", "Edificios y unidades"),
        new Opcion("Arrendatarios", "Datos personales y avales"),
        new Opcion("Contratos", "Alta, renovación y finalización"),
        new Opcion("Documentos", "Expediente por arrendatario"),
        new Opcion("Cobro de rentas y mora", "Cuotas, pagos y recibos"),
        new Opcion("Mantenimiento", "Solicitudes de reparación"),
        new Opcion("Historial y deshacer", "Bitácora de operaciones"),
        new Opcion("Reportes y alertas", "Ingresos, morosidad y ocupación"),
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
        add(contenedor);

        mostrarSeleccion();
        setSize(640, 420);
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
        }
    }

    private void continuarConPropietario() {
        Object sel = comboPropietarios.getSelectedItem();
        if (sel == null) {
            JOptionPane.showMessageDialog(this, "Seleccione o cree un propietario.",
                "Propietario", JOptionPane.WARNING_MESSAGE);
            return;
        }
        lblPropietario.setText("Propietario: " + sel);
        menuModulos.setEnabled(true);
        itemCambiar.setEnabled(true);
        cartas.show(contenedor, CARD_MODULOS);
    }

    private void mostrarSeleccion() {
        menuModulos.setEnabled(false);
        itemCambiar.setEnabled(false);
        cartas.show(contenedor, CARD_SELECCION);
    }

    // ---------- Pantalla de modulos (tras elegir propietario) ----------
    private JPanel crearPanelModulos() {
        JPanel p = new JPanel(new BorderLayout());
        lblPropietario.setFont(lblPropietario.getFont().deriveFont(Font.BOLD, 16f));
        lblPropietario.setBorder(BorderFactory.createEmptyBorder(15, 10, 5, 10));
        p.add(lblPropietario, BorderLayout.NORTH);

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

    private JMenuBar crearBarra() {
        JMenuBar barra = new JMenuBar();

        JMenu archivo = new JMenu("Archivo");
        itemCambiar.addActionListener(e -> mostrarSeleccion());
        JMenuItem guardar = new JMenuItem("Guardar datos (CSV)");
        guardar.addActionListener(e -> abrir("Guardar datos (CSV)"));
        archivo.add(itemCambiar);
        archivo.add(guardar);
        barra.add(archivo);

        for (Opcion o : opciones) {
            JMenuItem item = new JMenuItem(o.titulo());
            item.addActionListener(e -> abrir(o.titulo()));
            menuModulos.add(item);
        }
        barra.add(menuModulos);

        JMenu ayuda = new JMenu("Ayuda");
        JMenuItem acerca = new JMenuItem("Acerca de");
        acerca.addActionListener(e -> JOptionPane.showMessageDialog(this,
            "Proyecto de Estructura de Datos - Grupo 6\nDocente: Arredondo Castillo Gustavo",
            "Acerca de", JOptionPane.INFORMATION_MESSAGE));
        ayuda.add(acerca);
        barra.add(ayuda);
        return barra;
    }

    /** TODO: reemplazar por la apertura del panel real de cada modulo. */
    private void abrir(String modulo) {
        JOptionPane.showMessageDialog(this, "Módulo \"" + modulo + "\" aún no implementado.",
            modulo, JOptionPane.INFORMATION_MESSAGE);
    }

    private void salir() {
        int r = JOptionPane.showConfirmDialog(this, "¿Desea salir?", "Salir",
            JOptionPane.YES_NO_OPTION);
        if (r == JOptionPane.YES_OPTION) {
            // TODO: guardar datos en CSV antes de cerrar (2.8)
            dispose();
            System.exit(0);
        }
    }
}

package inmobiliaria;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import inmobiliaria.cobranza.servicio.MoraService;
import inmobiliaria.cobranza.servicio.PagoService;
import inmobiliaria.contratos.servicio.ContratoService;
import inmobiliaria.nucleo.estructuras.ListaSimple;
import inmobiliaria.nucleo.util.GeneradorCodigo;
import inmobiliaria.propiedades.modelo.Edificio;
import inmobiliaria.propiedades.modelo.Propietario;
import inmobiliaria.propiedades.servicio.PropiedadService;
import inmobiliaria.ui.Dialogos;
import inmobiliaria.ui.PanelCobranza;
import inmobiliaria.ui.PanelContratos;
import inmobiliaria.ui.PanelUnidades;

public class MenuPrincipal extends JFrame {

    private static final String CARD_SELECCION = "seleccion";
    private static final String CARD_MODULOS = "modulos";

    private record Opcion(String titulo, String descripcion) {}

    private final CardLayout cartas = new CardLayout();
    private final JPanel contenedor = new JPanel(cartas);

    private final PropiedadService propiedades;
    private final ContratoService contratos;
    private final PagoService pagos;
    private final MoraService mora;

    private final DefaultComboBoxModel<Propietario> modeloPropietarios = new DefaultComboBoxModel<>();
    private final JComboBox<Propietario> comboPropietarios = new JComboBox<>(modeloPropietarios);
    private final JLabel lblPropietario = new JLabel(" ");

    private final DefaultComboBoxModel<Edificio> modeloPropiedades = new DefaultComboBoxModel<>();
    private final JComboBox<Edificio> comboPropiedades = new JComboBox<>(modeloPropiedades);
    private final JLabel lblPropiedad = new JLabel("Ninguna propiedad seleccionada");
    private Propietario propietarioActual = null;
    private Edificio propiedadActual = null;

    private final JButton[] botonesModulos;
    private final JMenu menuModulos = new JMenu("Módulos");
    private final JMenu menuReportes = new JMenu("Reportes");
    private final JMenuItem itemCambiar = new JMenuItem("Cambiar propietario");

    private final Opcion[] opciones = {
        new Opcion("Unidades", "Departamentos, casas, locales"),
        new Opcion("Arrendatarios", "Datos y avales (en proceso)"),
        new Opcion("Contratos", "Alta, renovación y finalización"),
        new Opcion("Documentos", "Expediente (en proceso)"),
        new Opcion("Cobro de rentas y mora", "Cuotas, pagos y recibos"),
        new Opcion("Mantenimiento", "Solicitudes (en proceso)"),
    };

    private final String[] reportes = {
        "Ingresos", "Morosidad", "Ocupación", "Alertas",
    };

    public MenuPrincipal(PropiedadService propiedades, ContratoService contratos,
                         PagoService pagos, MoraService mora) {
        super("Gestión Inmobiliaria - Grupo 6");
        this.propiedades = propiedades;
        this.contratos = contratos;
        this.pagos = pagos;
        this.mora = mora;

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { salir(); }
        });

        botonesModulos = new JButton[opciones.length];
        setJMenuBar(crearBarra());
        contenedor.add(crearPanelSeleccion(), CARD_SELECCION);
        contenedor.add(crearPanelModulos(), CARD_MODULOS);
        add(contenedor, BorderLayout.CENTER);

        recargarPropietarios();
        mostrarSeleccion();
        setSize(660, 520);
        setLocationRelativeTo(null);
    }

    private JPanel crearPanelSeleccion() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);
        c.gridx = 0; c.gridy = 0; c.gridwidth = 2;
        JLabel l = new JLabel("Seleccione un propietario", SwingConstants.CENTER);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 18f));
        p.add(l, c);

        c.gridy = 1; c.fill = GridBagConstraints.HORIZONTAL;
        Dialogos.mostrarComo(comboPropietarios, Propietario::getNombre);
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

    private void recargarPropietarios() {
        modeloPropietarios.removeAllElements();
        ListaSimple<Propietario> lista = propiedades.listarPropietarios();
        for (int i = 0; i < lista.getTamano(); i++) {
            modeloPropietarios.addElement(lista.obtener(i));
        }
    }

    private void nuevoPropietario() {
        JTextField nombre = Dialogos.texto("");
        if (!Dialogos.formulario(this, "Nuevo propietario",
                new String[] { "Nombre" }, new JComponent[] { nombre })) return;
        try {
            String codigo = GeneradorCodigo.siguiente("PRO",
                    Dialogos.codigos(propiedades.listarPropietarios(), Propietario::getCodigo));
            Propietario p = propiedades.registrarPropietario(codigo, nombre.getText());
            GeneradorCodigo.marcarUsado(codigo);
            recargarPropietarios();
            modeloPropietarios.setSelectedItem(propiedades.buscarPropietario(p.getCodigo()));
        } catch (IllegalArgumentException ex) {
            Dialogos.error(this, ex.getMessage());
        }
    }

    private void continuarConPropietario() {
        Propietario sel = (Propietario) comboPropietarios.getSelectedItem();
        if (sel == null) {
            JOptionPane.showMessageDialog(this, "Seleccione o cree un propietario.",
                "Propietario", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (propietarioActual == null || !sel.getCodigo().equals(propietarioActual.getCodigo())) {
            propietarioActual = sel;
            ponerPropiedadElegida(null);
        }
        recargarPropiedades();
        lblPropietario.setText("Propietario: " + sel.getNombre());
        itemCambiar.setEnabled(true);
        cartas.show(contenedor, CARD_MODULOS);
    }

    private void mostrarSeleccion() {
        itemCambiar.setEnabled(false);
        menuModulos.setEnabled(false);
        menuReportes.setEnabled(false);
        cartas.show(contenedor, CARD_SELECCION);
    }

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

    private JPanel crearPanelPropiedad() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder("Propiedad"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 4, 2, 4);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0; c.gridy = 0; c.gridwidth = 3;
        Dialogos.mostrarComo(comboPropiedades, e -> e.getNombre() + " (" + e.getDireccion() + ")");
        comboPropiedades.setPreferredSize(new Dimension(260, 24));
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

        p.setMaximumSize(p.getPreferredSize());
        return p;
    }

    private void recargarPropiedades() {
        modeloPropiedades.removeAllElements();
        ListaSimple<Edificio> lista = propiedades.edificiosDe(propietarioActual.getCodigo());
        for (int i = 0; i < lista.getTamano(); i++) {
            modeloPropiedades.addElement(lista.obtener(i));
        }
        if (propiedadActual != null) modeloPropiedades.setSelectedItem(propiedadActual);
    }

    private void nuevaPropiedad() {
        JTextField nombre = Dialogos.texto("");
        JTextField direccion = Dialogos.texto("");
        if (!Dialogos.formulario(this, "Nueva propiedad",
                new String[] { "Nombre", "Dirección" },
                new JComponent[] { nombre, direccion })) return;
        try {
            String codigo = GeneradorCodigo.siguiente("EDI",
                    Dialogos.codigos(propiedades.listarEdificios(), Edificio::getCodigo));
            Edificio e = propiedades.registrarEdificio(propietarioActual.getCodigo(),
                    codigo, nombre.getText(), direccion.getText());
            GeneradorCodigo.marcarUsado(codigo);
            recargarPropiedades();
            comboPropiedades.setSelectedItem(propiedades.buscarEdificio(e.getCodigo()));
            ponerPropiedadElegida(propiedades.buscarEdificio(e.getCodigo()));
        } catch (IllegalArgumentException ex) {
            Dialogos.error(this, ex.getMessage());
        }
    }

    private void elegirPropiedad() {
        Edificio sel = (Edificio) comboPropiedades.getSelectedItem();
        if (sel == null) {
            JOptionPane.showMessageDialog(this, "Seleccione o registre una propiedad.",
                "Propiedad", JOptionPane.WARNING_MESSAGE);
            return;
        }
        ponerPropiedadElegida(sel);
    }

    private void ponerPropiedadElegida(Edificio elegida) {
        propiedadActual = elegida;
        lblPropiedad.setText(elegida != null ? "Elegida: " + elegida.getNombre()
                                             : "Ninguna propiedad seleccionada");
        for (JButton b : botonesModulos) {
            b.setEnabled(elegida != null);
        }
        menuModulos.setEnabled(elegida != null);
        menuReportes.setEnabled(elegida != null);
    }

    private JMenuBar crearBarra() {
        JMenuBar barra = new JMenuBar();

        JMenu archivo = new JMenu("Archivo");
        itemCambiar.addActionListener(e -> mostrarSeleccion());
        JMenuItem guardar = new JMenuItem("Guardar datos (CSV)");
        guardar.addActionListener(e -> {
            guardarTodo();
            JOptionPane.showMessageDialog(this, "Datos guardados exitosamente en CSV.",
                "Persistencia", JOptionPane.INFORMATION_MESSAGE);
        });

        archivo.add(itemCambiar);
        archivo.add(guardar);
        barra.add(archivo);

        for (Opcion o : opciones) {
            JMenuItem item = new JMenuItem(o.titulo());
            item.addActionListener(e -> abrir(o.titulo()));
            menuModulos.add(item);
        }
        barra.add(menuModulos);

        for (String r : reportes) {
            JMenuItem item = new JMenuItem(r);
            item.addActionListener(e -> reportePendiente(r));
            menuReportes.add(item);
        }
        barra.add(menuReportes);
        return barra;
    }

    private void guardarTodo() {
        propiedades.guardar();
        contratos.guardar();
        pagos.guardar();
    }

    private void abrir(String modulo) {
        if (propiedadActual == null) return;
        switch (modulo) {
            case "Unidades" ->
                new PanelUnidades(this, propiedades, propiedadActual).setVisible(true);
            case "Contratos" ->
                new PanelContratos(this, contratos, propiedades, pagos, mora, propiedadActual).setVisible(true);
            case "Cobro de rentas y mora" ->
                new PanelCobranza(this, contratos, pagos, mora, propiedades, propiedadActual).setVisible(true);
            default -> JOptionPane.showMessageDialog(this,
                "El módulo \"" + modulo + "\" está en proceso: sus modelos existen pero falta su servicio.",
                modulo, JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void reportePendiente(String reporte) {
        JOptionPane.showMessageDialog(this, "El reporte \"" + reporte + "\" está pendiente.",
            "Reportes", JOptionPane.INFORMATION_MESSAGE);
    }

    private void salir() {
        int r = JOptionPane.showConfirmDialog(this, "¿Desea salir? Se guardarán los datos.", "Salir",
            JOptionPane.YES_NO_OPTION);
        if (r == JOptionPane.YES_OPTION) {
            guardarTodo();
            dispose();
            System.exit(0);
        }
    }
}

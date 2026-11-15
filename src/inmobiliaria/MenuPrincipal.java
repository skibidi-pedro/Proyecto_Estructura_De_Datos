package inmobiliaria;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import inmobiliaria.alertas.AlertaService;
import inmobiliaria.arrendatarios.servicio.ArrendatarioService;
import inmobiliaria.cobranza.servicio.MoraService;
import inmobiliaria.cobranza.servicio.PagoService;
import inmobiliaria.contratos.servicio.ContratoService;
import inmobiliaria.documentos.servicio.ExpedienteService;
import inmobiliaria.mantenimiento.servicio.MantenimientoService;
import inmobiliaria.nucleo.estructuras.ListaSimple;
import inmobiliaria.nucleo.util.Fechas;
import inmobiliaria.nucleo.util.GeneradorCodigo;
import inmobiliaria.propiedades.modelo.Edificio;
import inmobiliaria.propiedades.modelo.Propietario;
import inmobiliaria.propiedades.servicio.PropiedadService;
import inmobiliaria.reportes.ReporteService;
import inmobiliaria.ui.Dialogos;
import inmobiliaria.ui.PanelArrendatarios;
import inmobiliaria.ui.PanelCobranza;
import inmobiliaria.ui.PanelContratos;
import inmobiliaria.ui.PanelDocumentos;
import inmobiliaria.ui.PanelMantenimiento;
import inmobiliaria.ui.PanelUnidades;

/**
 * Menu principal en Swing. Primero se selecciona un propietario; en la pantalla
 * siguiente se selecciona o registra una de sus propiedades (panel pequeno,
 * arriba a la izquierda). Los modulos y reportes se habilitan recien cuando hay
 * una propiedad seleccionada. Los codigos (propietario, propiedad, unidad, contrato) se asignan
 * solos y no se muestran. Los modulos son botones toggle: al activar uno, la ventana se expande hacia la
 * derecha y lo muestra ahi (solo uno a la vez); al volver a pulsarlo, se cierra y la ventana se contrae.
 * "Historial y deshacer" se quito: queda como opcion de refinamiento posterior.
 *
 * Una "propiedad" es un edificio del propietario (Propietario > Edificio > Unidad).
 *
 * Modulos conectados: Unidades, Arrendatarios, Contratos, Documentos, Cobro de rentas y mora, Mantenimiento.
 * Menu Reportes: Ingresos, Morosidad, Ocupacion (ReporteService) y Alertas (AlertaService).
 *
 * Responsable: Integrante 1 - Francia Ramos Johan Mitziu
 */
public class MenuPrincipal extends JFrame {

    private static final String CARD_SELECCION = "seleccion";
    private static final String CARD_MODULOS = "modulos";
    private static final int ANCHO_MODULO = 760;   // ancho que gana la ventana al abrir un modulo

    private record Opcion(String titulo) {}

    private final CardLayout cartas = new CardLayout();
    private final JPanel contenedor = new JPanel(cartas);

    // Zona a la derecha donde se muestra el modulo activo (solo esta en la ventana mientras hay uno abierto)
    private final JPanel vistaModulo = new JPanel(new BorderLayout());
    private boolean expandida = false;
    private int anchoVentanaMenu = 0;   // ancho de la ventana antes de expandirse, para volver a el

    // Servicios compartidos (los crea Main y carga los CSV)
    private final PropiedadService propiedades;
    private final ContratoService contratos;
    private final PagoService pagos;
    private final MoraService mora;
    private final ArrendatarioService arrendatarios;
    private final ExpedienteService expedientes;
    private final MantenimientoService mantenimiento;
    private final ReporteService reporteService;
    private final AlertaService alertaService;

    private final DefaultComboBoxModel<Propietario> modeloPropietarios = new DefaultComboBoxModel<>();
    private final JComboBox<Propietario> comboPropietarios = new JComboBox<>(modeloPropietarios);
    private final JLabel lblPropietario = new JLabel(" ");

    private final DefaultComboBoxModel<Edificio> modeloPropiedades = new DefaultComboBoxModel<>();
    private final JComboBox<Edificio> comboPropiedades = new JComboBox<>(modeloPropiedades);
    private final JLabel lblPropiedad = new JLabel("Ninguna propiedad seleccionada");
    private Propietario propietarioActual = null;
    private Edificio propiedadActual = null;

    private final JToggleButton[] botonesModulos;
    private final JMenu menuModulos = new JMenu("Módulos");
    private final JMenu menuReportes = new JMenu("Reportes");
    private final JMenuItem itemCambiar = new JMenuItem("Cambiar propietario");

    // Modulos que dependen de la propiedad seleccionada
    private final Opcion[] opciones = {
        new Opcion("Unidades"),
        new Opcion("Arrendatarios"),
        new Opcion("Contratos"),
        new Opcion("Documentos"),
        new Opcion("Cobro de rentas y mora"),
        new Opcion("Mantenimiento"),
    };

    // Reportes y alertas (se muestran en un dialogo de texto)
    private final String[] reportes = {
        "Ingresos", "Morosidad", "Ocupación", "Alertas",
    };

    public MenuPrincipal(PropiedadService propiedades, ContratoService contratos,
                         PagoService pagos, MoraService mora, ArrendatarioService arrendatarios,
                         ExpedienteService expedientes, MantenimientoService mantenimiento,
                         ReporteService reporteService, AlertaService alertaService) {
        super("Gestión Inmobiliaria - Grupo 6");
        this.propiedades = propiedades;
        this.contratos = contratos;
        this.pagos = pagos;
        this.mora = mora;
        this.arrendatarios = arrendatarios;
        this.expedientes = expedientes;
        this.mantenimiento = mantenimiento;
        this.reporteService = reporteService;
        this.alertaService = alertaService;

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { salir(); }
        });

        vistaModulo.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Color.GRAY));
        botonesModulos = new JToggleButton[opciones.length];
        setJMenuBar(crearBarra());
        contenedor.add(crearPanelSeleccion(), CARD_SELECCION);
        contenedor.add(crearPanelModulos(), CARD_MODULOS);
        add(contenedor, BorderLayout.CENTER);

        recargarPropietarios();
        mostrarSeleccion();
        setSize(372, 625);
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
            // El código se asigna solo y queda en memoria; RN-01 (código único) la valida PropiedadService
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
        // Si cambia de propietario, la propiedad elegida deja de valer
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
        cerrarModulo();
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

        // Una sola columna: cada modulo es un boton ancho, uno debajo del otro
        JPanel rejilla = new JPanel(new GridLayout(0, 1, 0, 10));
        rejilla.setBorder(BorderFactory.createEmptyBorder(10, 30, 20, 30));
        for (int i = 0; i < opciones.length; i++) {
            final int indice = i;
            JToggleButton b = new JToggleButton("<html><center><b>" + opciones[i].titulo() + "</b></center></html>");
            b.addActionListener(e -> alternarModulo(indice));
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

        // Que el panel no se estire: queda chico en la esquina
        p.setMaximumSize(p.getPreferredSize());
        return p;
    }

    // Edificios del propietario actual
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

    // Habilita o deshabilita modulos y reportes segun haya propiedad elegida (null = ninguna)
    private void ponerPropiedadElegida(Edificio elegida) {
        if (elegida != propiedadActual) cerrarModulo();   // el modulo abierto era de la otra propiedad
        propiedadActual = elegida;
        lblPropiedad.setText(elegida != null ? "Elegida: " + elegida.getNombre()
                                             : "Ninguna propiedad seleccionada");
        for (JToggleButton b : botonesModulos) {
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

        for (int i = 0; i < opciones.length; i++) {
            final int indice = i;
            JMenuItem item = new JMenuItem(opciones[i].titulo());
            item.addActionListener(e -> abrirModulo(indice));
            menuModulos.add(item);
        }
        barra.add(menuModulos);

        // Reportes ocupa el lugar que tenia Ayuda
        for (String r : reportes) {
            JMenuItem item = new JMenuItem(r);
            item.addActionListener(e -> mostrarReporte(r));
            menuReportes.add(item);
        }
        barra.add(menuReportes);
        return barra;
    }

    private void guardarTodo() {
        propiedades.guardar();
        contratos.guardar();
        pagos.guardar();
        arrendatarios.guardar();
        expedientes.guardar();
        mantenimiento.guardar();
    }

    // ---------- Modulos: toggle, uno a la vez, se muestran a la derecha ----------

    // Pulsar el boton: si quedo activado abre ese modulo; si quedo apagado, cierra
    private void alternarModulo(int indice) {
        if (botonesModulos[indice].isSelected()) abrirModulo(indice);
        else cerrarModulo();
    }

    // Abre el modulo (cambiando el que hubiera) y deja encendido solo su boton
    private void abrirModulo(int indice) {
        if (propiedadActual == null) return;
        for (int j = 0; j < botonesModulos.length; j++) {
            botonesModulos[j].setSelected(j == indice);
        }
        vistaModulo.removeAll();   // el modulo anterior (si habia) deja de refrescarse
        vistaModulo.add(crearVistaModulo(opciones[indice].titulo()), BorderLayout.CENTER);
        expandir();
        vistaModulo.revalidate();
        vistaModulo.repaint();
    }

    private JComponent crearVistaModulo(String modulo) {
        return switch (modulo) {
            case "Unidades" -> new PanelUnidades(propiedades, propiedadActual);
            case "Arrendatarios" -> new PanelArrendatarios(arrendatarios, contratos);
            case "Contratos" -> new PanelContratos(contratos, propiedades, pagos, mora,
                    arrendatarios, expedientes, propiedadActual);
            case "Documentos" -> new PanelDocumentos(expedientes, arrendatarios, contratos, propiedades, pagos);
            case "Cobro de rentas y mora" -> new PanelCobranza(contratos, pagos, mora, propiedades, propiedadActual);
            case "Mantenimiento" -> new PanelMantenimiento(mantenimiento, propiedades, propiedadActual);
            default -> throw new IllegalArgumentException("Módulo desconocido: " + modulo);
        };
    }

    // La ventana crece hacia la derecha; el menu conserva su ancho
    private void expandir() {
        if (expandida) return;
        expandida = true;
        anchoVentanaMenu = getWidth();
        contenedor.setPreferredSize(new Dimension(contenedor.getWidth(), 0));
        getContentPane().remove(contenedor);
        add(contenedor, BorderLayout.WEST);
        add(vistaModulo, BorderLayout.CENTER);
        setSize(getWidth() + ANCHO_MODULO, getHeight());
        acomodarEnPantalla();
        revalidate();
        repaint();
    }

    // Apaga los botones, quita el modulo y devuelve la ventana a su ancho de menu
    private void cerrarModulo() {
        for (JToggleButton b : botonesModulos) b.setSelected(false);
        if (!expandida) return;
        expandida = false;
        vistaModulo.removeAll();
        getContentPane().remove(vistaModulo);
        getContentPane().remove(contenedor);
        contenedor.setPreferredSize(null);
        add(contenedor, BorderLayout.CENTER);
        setSize(anchoVentanaMenu, getHeight());
        revalidate();
        repaint();
    }

    // Si al crecer se sale por la derecha de la pantalla, se corre a la izquierda lo justo
    private void acomodarEnPantalla() {
        GraphicsConfiguration gc = getGraphicsConfiguration();
        if (gc == null) return;
        Rectangle pantalla = gc.getBounds();
        Insets bordes = Toolkit.getDefaultToolkit().getScreenInsets(gc);
        int derecha = pantalla.x + pantalla.width - bordes.right;
        if (getX() + getWidth() > derecha) {
            setLocation(Math.max(pantalla.x + bordes.left, derecha - getWidth()), getY());
        }
    }

    // ---------- Reportes y alertas ----------
    private void mostrarReporte(String reporte) {
        try {
            LocalDate hoy = LocalDate.now();
            String texto;
            switch (reporte) {
                case "Ingresos" -> {
                    JTextField desde = Dialogos.texto(Fechas.convertirAString(hoy.withDayOfMonth(1)));
                    JTextField hasta = Dialogos.texto(Fechas.convertirAString(hoy));
                    if (!Dialogos.formulario(this, "Reporte de ingresos",
                            new String[] { "Desde (AAAA-MM-DD)", "Hasta (AAAA-MM-DD)" },
                            new JComponent[] { desde, hasta })) return;
                    LocalDate d = Dialogos.fecha(desde, "Desde");
                    LocalDate h = Dialogos.fecha(hasta, "Hasta");
                    if (h.isBefore(d))
                        throw new IllegalArgumentException("La fecha final no puede ser anterior a la inicial");
                    texto = reporteService.reporteIngresos(d, h);
                }
                case "Morosidad" -> texto = reporteService.reporteMorosidad(hoy);
                case "Ocupación" -> texto = reporteService.reporteOcupacion();
                default -> texto = textoAlertas(hoy);
            }
            mostrarTexto(reporte, texto);
        } catch (IllegalArgumentException ex) {
            Dialogos.error(this, ex.getMessage());
        }
    }

    private String textoAlertas(LocalDate hoy) {
        ListaSimple<String> alertas = alertaService.todas(hoy);
        if (alertas.estaVacia()) return "ALERTAS al " + Fechas.convertirAString(hoy) + "\n  No hay alertas.";
        StringBuilder sb = new StringBuilder("ALERTAS al " + Fechas.convertirAString(hoy)
                + " (" + alertas.getTamano() + ")\n");
        for (int i = 0; i < alertas.getTamano(); i++) {
            sb.append("  - ").append(alertas.obtener(i)).append("\n");
        }
        return sb.toString();
    }

    private void mostrarTexto(String titulo, String texto) {
        JTextArea area = new JTextArea(texto, 16, 60);
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setCaretPosition(0);
        JOptionPane.showMessageDialog(this, new JScrollPane(area), titulo, JOptionPane.PLAIN_MESSAGE);
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

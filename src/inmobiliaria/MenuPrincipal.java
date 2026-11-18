package inmobiliaria;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import inmobiliaria.alertas.AlertaService;
import inmobiliaria.arrendatarios.servicio.ArrendatarioService;
import inmobiliaria.cobranza.servicio.MoraService;
import inmobiliaria.cobranza.servicio.PagoService;
import inmobiliaria.contratos.servicio.ContratoService;
import inmobiliaria.documentos.servicio.ExpedienteService;
import inmobiliaria.nucleo.estructuras.ListaSimple;
import inmobiliaria.nucleo.historial.Historial;
import inmobiliaria.nucleo.historial.Operacion;
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
 * Modulos: Unidades, Arrendatarios (con su documento de identidad), Contratos (con su documento) y Cobro de rentas y mora.
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
    private final ReporteService reporteService;
    private final Historial historial;
    private final AlertaService alertaService;

    private final DefaultComboBoxModel<Propietario> modeloPropietarios = new DefaultComboBoxModel<>();
    private final JComboBox<Propietario> comboPropietarios = new JComboBox<>(modeloPropietarios);
    private final JLabel lblPropietario = new JLabel(" ");

    private final DefaultComboBoxModel<Edificio> modeloPropiedades = new DefaultComboBoxModel<>();
    private final JComboBox<Edificio> comboPropiedades = new JComboBox<>(modeloPropiedades);
    private Propietario propietarioActual = null;
    private Edificio propiedadActual = null;

    private final JToggleButton[] botonesModulos;
    // Panel ya creado de cada modulo: al volver a abrirlo se muestra el mismo, con su arrendatario,
    // contrato o fila elegidos, en vez de uno nuevo que empieza desde el primero.
    private JComponent[] vistasModulos;
    private final JMenu menuModulos = new JMenu("Módulos");
    private final JMenu menuReportes = new JMenu("Reportes");
    private final JMenuItem itemCambiar = new JMenuItem("Cambiar propietario");

    // Modulos que dependen de la propiedad seleccionada
    private final Opcion[] opciones = {
        new Opcion("Unidades"),
        new Opcion("Arrendatarios"),
        new Opcion("Contratos"),
        new Opcion("Cobro de rentas y mora"),
    };

    // Reportes y alertas (se muestran en un dialogo de texto)
    private final String[] reportes = {
        "Ingresos", "Morosidad", "Ocupación", "Alertas",
    };

    public MenuPrincipal(PropiedadService propiedades, ContratoService contratos,
                         PagoService pagos, MoraService mora, ArrendatarioService arrendatarios,
                         ExpedienteService expedientes,
                         ReporteService reporteService, AlertaService alertaService, Historial historial) {
        super("Gestión Inmobiliaria - Grupo 6");
        this.propiedades = propiedades;
        this.contratos = contratos;
        this.pagos = pagos;
        this.mora = mora;
        this.arrendatarios = arrendatarios;
        this.expedientes = expedientes;
        this.reporteService = reporteService;
        this.historial = historial;
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
        Dialogos.mostrarComo(comboPropiedades, e -> e.getNombre());
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
        if (elegida != propiedadActual) {
            cerrarModulo();   // el modulo abierto era de la otra propiedad
            olvidarVistasDePropiedad();
        }
        propiedadActual = elegida;
        for (JToggleButton b : botonesModulos) {
            b.setEnabled(elegida != null);
        }
        menuModulos.setEnabled(elegida != null);
        menuReportes.setEnabled(elegida != null);
    }

    // Al cambiar de propiedad se descartan los paneles que dependen de ella; Arrendatarios
    // no depende de la propiedad, asi que conserva lo que estaba elegido.
    private void olvidarVistasDePropiedad() {
        if (vistasModulos == null) return;
        for (int i = 0; i < vistasModulos.length; i++) {
            String titulo = opciones[i].titulo();
            if (!titulo.equals("Arrendatarios")) vistasModulos[i] = null;
        }
    }

    private JMenuBar crearBarra() {
        JMenuBar barra = new JMenuBar();

        JMenu archivo = new JMenu("Archivo");
        itemCambiar.addActionListener(e -> mostrarSeleccion());
        JMenuItem guardar = new JMenuItem("Guardar datos (CSV)");
        guardar.addActionListener(e -> {
            try {
                guardarTodo();
                JOptionPane.showMessageDialog(this, "Datos guardados exitosamente en CSV.",
                    "Persistencia", JOptionPane.INFORMATION_MESSAGE);
            } catch (RuntimeException ex) {
                Dialogos.error(this, "No se pudieron guardar los datos: " + ex.getMessage());
            }
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

        // Historial de operaciones: a la derecha de Reportes
        JMenu menuHistorial = new JMenu("Historial");
        JMenuItem verHistorial = new JMenuItem("Ver historial de operaciones");
        verHistorial.addActionListener(e -> mostrarHistorial());
        menuHistorial.add(verHistorial);
        barra.add(menuHistorial);
        return barra;
    }

    private void guardarTodo() {
        propiedades.guardar();
        contratos.guardar();
        pagos.guardar();
        arrendatarios.guardar();
        expedientes.guardar();
        historial.guardar();
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
        if (vistasModulos == null) vistasModulos = new JComponent[opciones.length];
        if (vistasModulos[indice] == null) vistasModulos[indice] = crearVistaModulo(opciones[indice].titulo());
        vistaModulo.removeAll();   // el modulo anterior (si habia) se oculta y deja de refrescarse
        vistaModulo.add(vistasModulos[indice], BorderLayout.CENTER);
        expandir();
        vistaModulo.revalidate();
        vistaModulo.repaint();
    }

    private JComponent crearVistaModulo(String modulo) {
        return switch (modulo) {
            case "Unidades" -> new PanelUnidades(propiedades, propiedadActual);
            case "Arrendatarios" -> new PanelArrendatarios(arrendatarios, contratos, expedientes, propiedades, pagos, mora);
            case "Contratos" -> new PanelContratos(contratos, propiedades, pagos, mora,
                    arrendatarios, expedientes, propiedadActual);
            case "Cobro de rentas y mora" -> new PanelCobranza(contratos, pagos, mora, propiedades, propiedadActual);
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
            String csv = null;   // el reporte de alertas no se exporta
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
                    csv = reporteService.csvIngresos(d, h);
                }
                case "Morosidad" -> {
                    texto = reporteService.reporteMorosidad(hoy);
                    csv = reporteService.csvMorosidad(hoy);
                }
                case "Ocupación" -> {
                    texto = reporteService.reporteOcupacion();
                    csv = reporteService.csvOcupacion();
                }
                default -> texto = textoAlertas(hoy);
            }
            mostrarTexto(reporte, texto, csv);
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

    // Muestra el reporte; si tiene version CSV ofrece exportarla
    private void mostrarTexto(String titulo, String texto, String csv) {
        JTextArea area = new JTextArea(texto, 16, 60);
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setCaretPosition(0);
        if (csv == null) {
            JOptionPane.showMessageDialog(this, new JScrollPane(area), titulo, JOptionPane.PLAIN_MESSAGE);
            return;
        }
        int r = JOptionPane.showOptionDialog(this, new JScrollPane(area), titulo, JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE, null, new String[] { "Exportar CSV", "Cerrar" }, "Cerrar");
        if (r == 0) exportarCsv(titulo, csv);
    }

    // Guarda el reporte en un .csv (UTF-8 con marca BOM para que Excel respete los acentos)
    private void exportarCsv(String reporte, String csv) {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("CSV (*.csv)", "csv"));
        fc.setSelectedFile(new File("reporte-" + reporte.toLowerCase().replace("ó", "o") + ".csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        if (!f.getName().toLowerCase().endsWith(".csv")) f = new File(f.getPath() + ".csv");
        if (f.exists() && !Dialogos.confirmar(this, "Exportar", "El archivo ya existe. Reemplazarlo?")) return;
        try {
            Files.write(f.toPath(), ("\uFEFF" + csv).getBytes(StandardCharsets.UTF_8));
            Dialogos.info(this, "Exportar", "Reporte guardado en " + f.getName());
        } catch (IOException ex) {
            Dialogos.error(this, "No se pudo guardar el archivo: " + ex.getMessage());
        }
    }

    // ---------- Historial de operaciones (la mas reciente primero) ----------
    private void mostrarHistorial() {
        ListaSimple<Operacion> ops = historial.listar();
        if (ops.estaVacia()) {
            Dialogos.info(this, "Historial de operaciones", "Todavia no hay operaciones registradas.");
            return;
        }
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DefaultTableModel modelo = Dialogos.modelo("Fecha", "Operacion");
        for (int i = 0; i < ops.getTamano(); i++)
            modelo.addRow(new Object[] { formato.format(ops.obtener(i).getFechaHora()), ops.obtener(i).getDescripcion() });
        JTable tabla = Dialogos.tabla(modelo);
        tabla.getColumnModel().getColumn(0).setPreferredWidth(150);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(550);
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.add(Dialogos.barraBusqueda(ops.getTamano() + " operaciones", tabla, null), BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setPreferredSize(new Dimension(720, 360));
        panel.add(scroll, BorderLayout.CENTER);
        JOptionPane.showMessageDialog(this, panel, "Historial de operaciones", JOptionPane.PLAIN_MESSAGE);
    }

    private void salir() {
        int r = JOptionPane.showConfirmDialog(this, "¿Desea salir? Se guardarán los datos.", "Salir",
            JOptionPane.YES_NO_OPTION);
        if (r == JOptionPane.YES_OPTION) {
            try {
                guardarTodo();
                dispose();
                System.exit(0);
            } catch (RuntimeException ex) {
                Dialogos.error(this, "Fallo el guardado: " + ex.getMessage()
                    + ". La aplicacion seguira abierta para evitar perder datos.");
            }
        }
    }
}

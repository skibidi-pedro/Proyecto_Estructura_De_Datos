package inmobiliaria.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.awt.event.HierarchyEvent;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.BiFunction;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.Timer;
import inmobiliaria.nucleo.estructuras.ListaSimple;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.util.Locale;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import inmobiliaria.nucleo.util.Archivos;
import inmobiliaria.nucleo.util.Fechas;

/**
 * Utilidades de Swing compartidas por los paneles: mensajes, formularios,
 * lectura de campos con validacion y tablas de solo lectura.
 * Los metodos de lectura lanzan IllegalArgumentException con un mensaje
 * entendible, igual que los servicios, para que los paneles las capturen juntas.
 */
public class Dialogos {

    public static void error(Component padre, String mensaje) {
        JOptionPane.showMessageDialog(padre, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void info(Component padre, String titulo, String mensaje) {
        JOptionPane.showMessageDialog(padre, mensaje, titulo, JOptionPane.INFORMATION_MESSAGE);
    }

    public static boolean confirmar(Component padre, String titulo, String mensaje) {
        return JOptionPane.showConfirmDialog(padre, mensaje, titulo, JOptionPane.YES_NO_OPTION)
                == JOptionPane.YES_OPTION;
    }

    // Formulario de etiqueta + campo por fila. Devuelve true si se pulso Aceptar.
    public static boolean formulario(Component padre, String titulo, String[] etiquetas, JComponent[] campos) {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 4, 3, 4);
        c.anchor = GridBagConstraints.WEST;
        for (int i = 0; i < etiquetas.length; i++) {
            c.gridy = i;
            c.gridx = 0; c.fill = GridBagConstraints.NONE; c.weightx = 0;
            p.add(new JLabel(etiquetas[i]), c);
            c.gridx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1;
            p.add(campos[i], c);
        }
        return JOptionPane.showConfirmDialog(padre, p, titulo,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION;
    }

    public static JTextField texto(String inicial) {
        return new JTextField(inicial, 18);
    }

    // ---------- Lectura validada de campos ----------
    public static double numero(JTextField campo, String nombre) {
        try {
            return Double.parseDouble(campo.getText().trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("\"" + nombre + "\" debe ser un numero");
        }
    }

    public static int entero(JTextField campo, String nombre) {
        try {
            return Integer.parseInt(campo.getText().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("\"" + nombre + "\" debe ser un numero entero");
        }
    }

    public static LocalDate fecha(JTextField campo, String nombre) {
        LocalDate f = Fechas.convertirALocalDate(campo.getText());
        if (f == null)
            throw new IllegalArgumentException("\"" + nombre + "\" no es una fecha valida (use AAAA-MM-DD)");
        return f;
    }

    public static String dinero(double valor) {
        return String.format(Locale.US, "%.2f", valor);
    }

    // ---------- Tablas ----------
    public static DefaultTableModel modelo(String... columnas) {
        return new DefaultTableModel(columnas, 0) {
            @Override public boolean isCellEditable(int fila, int columna) { return false; }
        };
    }

    // Colores de resaltado (tonos claros): verde = bien / pagado, amarillo = falta o atrasado, rojo = inactivo
    public static final Color VERDE = new Color(198, 239, 206);
    public static final Color AMARILLO = new Color(255, 235, 132);
    public static final Color ROJO = new Color(255, 199, 206);

    // Pinta celdas de la tabla. El criterio recibe (fila del MODELO, columna del MODELO) y devuelve
    // el color, o null para dejar la celda como esta. La fila seleccionada conserva su color de seleccion.
    public static void colorear(JTable tabla, BiFunction<Integer, Integer, Color> criterio) {
        tabla.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object valor, boolean seleccionada,
                                                           boolean foco, int fila, int columna) {
                Component c = super.getTableCellRendererComponent(t, valor, seleccionada, foco, fila, columna);
                Color color = seleccionada ? null
                        : criterio.apply(t.convertRowIndexToModel(fila), t.convertColumnIndexToModel(columna));
                c.setBackground(color != null ? color : seleccionada ? t.getSelectionBackground() : t.getBackground());
                return c;
            }
        });
    }

    public static JTable tabla(DefaultTableModel modelo) {
        JTable t = new JTable(modelo);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setRowHeight(22);
        t.getTableHeader().setReorderingAllowed(false);
        return t;
    }

    // Valor de la fila seleccionada en la columna del MODELO (sirve aunque la columna este oculta), o null
    public static Object seleccionado(JTable tabla, int columna) {
        int fila = tabla.getSelectedRow();
        return fila < 0 ? null : tabla.getModel().getValueAt(tabla.convertRowIndexToModel(fila), columna);
    }

    // Oculta una columna de la vista pero sigue en el modelo (para codigos que el usuario no ve)
    public static void ocultarColumna(JTable tabla, int columna) {
        tabla.removeColumn(tabla.getColumnModel().getColumn(columna));
    }

    // Vuelca las filas al modelo SOLO si cambiaron, y vuelve a seleccionar la fila (por la columna 0)
    public static void poblar(JTable tabla, DefaultTableModel modelo, List<Object[]> filas) {
        if (modelo.getRowCount() == filas.size()) {
            boolean igual = true;
            for (int i = 0; i < filas.size() && igual; i++) {
                for (int j = 0; j < modelo.getColumnCount(); j++) {
                    if (!Objects.equals(modelo.getValueAt(i, j), filas.get(i)[j])) { igual = false; break; }
                }
            }
            if (igual) return;
        }
        Object clave = seleccionado(tabla, 0);
        modelo.setRowCount(0);
        for (Object[] f : filas) modelo.addRow(f);
        if (clave == null) return;
        for (int i = 0; i < modelo.getRowCount(); i++) {
            if (clave.equals(modelo.getValueAt(i, 0))) { tabla.setRowSelectionInterval(i, i); break; }
        }
    }

    // ---------- Archivos adjuntos ----------
    // Cuadro para elegir un archivo (PDF, Word o imagen). Devuelve null si se cancela.
    public static File elegirArchivo(Component padre) {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter(
                "Documentos e imagenes (pdf, doc, docx, jpg, png)", Archivos.EXTENSIONES));
        return fc.showOpenDialog(padre) == JFileChooser.APPROVE_OPTION ? fc.getSelectedFile() : null;
    }

    // Campo de solo lectura + boton "Examinar..."; la ruta elegida queda en 'destino'
    public static JPanel campoArchivo(Component padre, JTextField destino) {
        destino.setEditable(false);
        JButton examinar = new JButton("Examinar...");
        examinar.addActionListener(e -> {
            File f = elegirArchivo(padre);
            if (f != null) destino.setText(f.getAbsolutePath());
        });
        JPanel p = new JPanel(new BorderLayout(4, 0));
        p.add(destino, BorderLayout.CENTER);
        p.add(examinar, BorderLayout.EAST);
        return p;
    }

    // Doble clic sobre una fila de la tabla (el dato se lee despues con seleccionado())
    public static void alDobleClick(JTable tabla, Runnable accion) {
        tabla.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tabla.rowAtPoint(e.getPoint()) >= 0) accion.run();
            }
        });
    }

    // ---------- Refresco ----------
    public static JButton botonRefrescar(Runnable accion) {
        JButton b = new JButton("Refrescar");
        b.addActionListener(e -> accion.run());
        return b;
    }

    // Llama a accion cada segundo mientras el panel este visible; se detiene cuando el panel
    // se oculta (al cerrar el modulo o cambiar a otro).
    // Como poblar() no toca la tabla si nada cambio, el costo es leer los datos una vez por segundo.
    public static void autoRefrescar(JComponent panel, Runnable accion) {
        Timer t = new Timer(1000, e -> refrescarSeguro(accion));
        // El panel se conserva entre aperturas del modulo (MenuPrincipal), asi que el timer corre solo
        // mientras se ve: al mostrarse refresca de inmediato y al ocultarse se detiene.
        panel.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == 0) return;
            if (panel.isShowing()) {
                refrescarSeguro(accion);
                t.start();
            } else {
                t.stop();
            }
        });
    }

    private static void refrescarSeguro(Runnable accion) {
        try {
            accion.run();
        } catch (RuntimeException ex) {
            // un dato pudo cambiar a medio refresco (p. ej. una baja): el siguiente tick reintenta
        }
    }

    // ---------- Textos sin codigos ----------
    @SuppressWarnings("unchecked")
    public static <T> void mostrarComo(JComboBox<T> combo, Function<T, String> texto) {
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> lista, Object valor, int indice,
                                                          boolean seleccionado, boolean foco) {
                super.getListCellRendererComponent(lista, valor, indice, seleccionado, foco);
                if (valor != null) setText(texto.apply((T) valor));
                return this;
            }
        });
    }

    public static <T> ListaSimple<String> codigos(ListaSimple<T> lista, Function<T, String> codigo) {
        ListaSimple<String> r = new ListaSimple<>();
        for (int i = 0; i < lista.getTamano(); i++) r.agregar(codigo.apply(lista.obtener(i)));
        return r;
    }
}

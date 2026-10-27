package inmobiliaria.ui;

import java.awt.Component;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.Timer;
import inmobiliaria.nucleo.estructuras.ListaSimple;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.util.Locale;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
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

    // ---------- Refresco ----------
    public static JButton botonRefrescar(Runnable accion) {
        JButton b = new JButton("Refrescar");
        b.addActionListener(e -> accion.run());
        return b;
    }

    // Llama a accion cada segundo mientras la ventana este visible; se detiene al cerrarla.
    // Como poblar() no toca la tabla si nada cambio, el costo es leer los datos una vez por segundo.
    public static void autoRefrescar(JDialog ventana, Runnable accion) {
        Timer t = new Timer(1000, e -> {
            if (!ventana.isShowing()) return;
            try {
                accion.run();
            } catch (RuntimeException ex) {
                // un dato pudo cambiar a medio refresco (p. ej. una baja en otra ventana): el siguiente tick reintenta
            }
        });
        ventana.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        ventana.addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) { t.stop(); }
        });
        t.start();
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

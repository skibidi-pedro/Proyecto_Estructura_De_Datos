package inmobiliaria;

import javax.swing.*;
import java.awt.*;

/**
 * Menu principal en Swing. Solo navegacion: cada opcion queda lista para
 * enlazarse al panel/servicio de su modulo.
 *
 * Responsable: Integrante 1 - Francia Ramos Johan Mitziu
 */
public class MenuPrincipal extends JFrame {

    /** Opcion del menu: titulo, descripcion y accion al presionarla. */
    private record Opcion(String titulo, String descripcion, Runnable accion) {}

    public MenuPrincipal() {
        super("Sistema de Gestión Inmobiliaria y Alquileres - Grupo 6");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) { salir(); }
        });

        Opcion[] opciones = {
            new Opcion("Propietarios y propiedades", "Propietarios, edificios y unidades (2.1)", () -> abrir("Propietarios y propiedades")),
            new Opcion("Arrendatarios", "Datos personales y avales (2.2)", () -> abrir("Arrendatarios")),
            new Opcion("Contratos", "Alta, renovación y finalización (2.3)", () -> abrir("Contratos")),
            new Opcion("Documentos", "Expediente por arrendatario (2.4)", () -> abrir("Documentos")),
            new Opcion("Cobro de rentas y mora", "Cuotas, pagos y recibos (2.5)", () -> abrir("Cobro de rentas y mora")),
            new Opcion("Mantenimiento", "Solicitudes de reparación (2.6)", () -> abrir("Mantenimiento")),
            new Opcion("Historial y deshacer", "Bitácora de operaciones (2.7)", () -> abrir("Historial y deshacer")),
            new Opcion("Reportes y alertas", "Ingresos, morosidad y ocupación", () -> abrir("Reportes y alertas")),
        };

        setJMenuBar(crearBarra(opciones));

        JLabel titulo = new JLabel("Sistema de Gestión Inmobiliaria y Alquileres", SwingConstants.CENTER);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 22f));
        titulo.setBorder(BorderFactory.createEmptyBorder(20, 10, 10, 10));

        JPanel rejilla = new JPanel(new GridLayout(0, 2, 12, 12));
        rejilla.setBorder(BorderFactory.createEmptyBorder(10, 30, 20, 30));
        for (Opcion o : opciones) {
            JButton b = new JButton("<html><center><b>" + o.titulo() + "</b><br><small>" + o.descripcion() + "</small></center></html>");
            b.setPreferredSize(new Dimension(280, 70));
            b.addActionListener(e -> o.accion().run());
            rejilla.add(b);
        }

        JButton btnSalir = new JButton("Salir");
        btnSalir.addActionListener(e -> salir());
        JPanel pie = new JPanel();
        pie.add(btnSalir);

        setLayout(new BorderLayout());
        add(titulo, BorderLayout.NORTH);
        add(rejilla, BorderLayout.CENTER);
        add(pie, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
    }

    private JMenuBar crearBarra(Opcion[] opciones) {
        JMenuBar barra = new JMenuBar();

        JMenu archivo = new JMenu("Archivo");
        JMenuItem guardar = new JMenuItem("Guardar datos (CSV)");
        guardar.addActionListener(e -> abrir("Guardar datos (CSV)"));
        JMenuItem salir = new JMenuItem("Salir");
        salir.addActionListener(e -> salir());
        archivo.add(guardar);
        archivo.addSeparator();
        archivo.add(salir);
        barra.add(archivo);

        JMenu modulos = new JMenu("Módulos");
        for (Opcion o : opciones) {
            JMenuItem item = new JMenuItem(o.titulo());
            item.addActionListener(e -> o.accion().run());
            modulos.add(item);
        }
        barra.add(modulos);

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

package inmobiliaria.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import inmobiliaria.nucleo.estructuras.ListaSimple;
import inmobiliaria.propiedades.modelo.Edificio;
import inmobiliaria.propiedades.modelo.EstadoUnidad;
import inmobiliaria.propiedades.modelo.TipoUnidad;
import inmobiliaria.propiedades.modelo.Unidad;
import inmobiliaria.propiedades.servicio.PropiedadService;

/**
 * Unidades (departamentos, casas, locales, cuartos) de la propiedad seleccionada.
 * Usa PropiedadService (Integrante 2). Las unidades nacen DISPONIBLE o EN_MANTENIMIENTO;
 * ALQUILADA solo la pone el modulo de contratos al activar uno.
 */
public class PanelUnidades extends JDialog {
    private final PropiedadService propiedades;
    private final Edificio edificio;
    private final DefaultTableModel modelo =
            Dialogos.modelo("Codigo", "Direccion", "Tipo", "Precio (S/)", "Estado");
    private final JTable tabla = Dialogos.tabla(modelo);

    public PanelUnidades(Frame dueno, PropiedadService propiedades, Edificio edificio) {
        super(dueno, "Unidades - " + edificio.getNombre(), true);
        this.propiedades = propiedades;
        this.edificio = edificio;

        setLayout(new BorderLayout(8, 8));
        add(new JLabel("  Propiedad: " + edificio), BorderLayout.NORTH);
        add(new JScrollPane(tabla), BorderLayout.CENTER);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton nueva = new JButton("Nueva unidad");
        nueva.addActionListener(e -> nueva());
        JButton estado = new JButton("Cambiar estado");
        estado.addActionListener(e -> cambiarEstado());
        JButton baja = new JButton("Dar de baja");
        baja.addActionListener(e -> baja());
        JButton cerrar = new JButton("Cerrar");
        cerrar.addActionListener(e -> dispose());
        botones.add(nueva);
        botones.add(estado);
        botones.add(baja);
        botones.add(cerrar);
        add(botones, BorderLayout.SOUTH);

        refrescar();
        setSize(680, 360);
        setLocationRelativeTo(dueno);
    }

    private void refrescar() {
        modelo.setRowCount(0);
        ListaSimple<Unidad> lista = propiedades.unidadesDe(edificio.getCodigo());
        for (int i = 0; i < lista.getTamano(); i++) {
            Unidad u = lista.obtener(i);
            modelo.addRow(new Object[] { u.getCodigo(), u.getDireccion(), u.getTipo(),
                    Dialogos.dinero(u.getPrecio()), u.getEstado() });
        }
    }

    private void nueva() {
        JTextField codigo = Dialogos.texto("");
        JTextField direccion = Dialogos.texto(edificio.getDireccion());
        JComboBox<TipoUnidad> tipo = new JComboBox<>(TipoUnidad.values());
        JTextField precio = Dialogos.texto("");
        JComboBox<EstadoUnidad> estado = new JComboBox<>(
                new EstadoUnidad[] { EstadoUnidad.DISPONIBLE, EstadoUnidad.EN_MANTENIMIENTO });

        if (!Dialogos.formulario(this, "Nueva unidad",
                new String[] { "Codigo", "Direccion", "Tipo", "Precio mensual (S/)", "Estado inicial" },
                new javax.swing.JComponent[] { codigo, direccion, tipo, precio, estado })) return;
        try {
            propiedades.registrarUnidad(edificio.getCodigo(), codigo.getText(), direccion.getText(),
                    (TipoUnidad) tipo.getSelectedItem(), Dialogos.numero(precio, "Precio"),
                    (EstadoUnidad) estado.getSelectedItem());
            refrescar();
        } catch (IllegalArgumentException ex) {
            Dialogos.error(this, ex.getMessage());
        }
    }

    private void cambiarEstado() {
        Object codigo = Dialogos.seleccionado(tabla, 0);
        if (codigo == null) {
            Dialogos.error(this, "Seleccione una unidad de la tabla.");
            return;
        }
        Unidad u = propiedades.buscarUnidad(codigo.toString());
        if (u.getEstado() == EstadoUnidad.ALQUILADA) {
            Dialogos.error(this, "La unidad esta alquilada: vuelve a DISPONIBLE al finalizar su contrato.");
            return;
        }
        Object nuevo = JOptionPane.showInputDialog(this, "Nuevo estado de " + u.getCodigo() + ":",
                "Cambiar estado", JOptionPane.PLAIN_MESSAGE, null,
                new EstadoUnidad[] { EstadoUnidad.DISPONIBLE, EstadoUnidad.EN_MANTENIMIENTO }, u.getEstado());
        if (nuevo == null) return;
        try {
            propiedades.cambiarEstadoUnidad(u.getCodigo(), (EstadoUnidad) nuevo);
            refrescar();
        } catch (IllegalArgumentException ex) {
            Dialogos.error(this, ex.getMessage());
        }
    }

    private void baja() {
        Object codigo = Dialogos.seleccionado(tabla, 0);
        if (codigo == null) {
            Dialogos.error(this, "Seleccione una unidad de la tabla.");
            return;
        }
        if (!Dialogos.confirmar(this, "Dar de baja", "Dar de baja la unidad " + codigo + "?")) return;
        try {
            propiedades.bajaUnidad(codigo.toString());
            refrescar();
        } catch (IllegalArgumentException ex) {
            Dialogos.error(this, ex.getMessage());
        }
    }
}

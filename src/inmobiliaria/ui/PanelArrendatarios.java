package inmobiliaria.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import inmobiliaria.arrendatarios.modelo.Arrendatario;
import inmobiliaria.arrendatarios.modelo.EstadoArrendatario;
import inmobiliaria.arrendatarios.servicio.ArrendatarioService;
import inmobiliaria.contratos.modelo.Contrato;
import inmobiliaria.contratos.servicio.ContratoService;
import inmobiliaria.nucleo.estructuras.ListaSimple;

/**
 * Arrendatarios. Usa ArrendatarioService (Integrante 3).
 * El documento de identidad es la clave (RN-01) y se muestra: lo escribe el usuario.
 * Es un panel que MenuPrincipal muestra a la derecha de la ventana; se refresca solo cada segundo.
 * Un arrendatario con contratos no se puede eliminar (se puede marcar INACTIVO).
 */
public class PanelArrendatarios extends JPanel {
    private final ArrendatarioService arrendatarios;
    private final ContratoService contratos;
    private final DefaultTableModel modelo = Dialogos.modelo(
            "Documento", "Nombres", "Telefono", "Correo", "Estado");
    private final JTable tabla = Dialogos.tabla(modelo);
    private final JTextField buscar = new JTextField(10);

    public PanelArrendatarios(ArrendatarioService arrendatarios, ContratoService contratos) {
        this.arrendatarios = arrendatarios;
        this.contratos = contratos;

        setLayout(new BorderLayout(8, 8));
        JPanel norte = new JPanel(new FlowLayout(FlowLayout.LEFT));
        norte.add(new JLabel("Buscar por documento:"));
        norte.add(buscar);
        JButton ir = new JButton("Buscar");
        ir.addActionListener(e -> buscarDocumento());
        norte.add(ir);
        add(norte, BorderLayout.NORTH);
        add(new JScrollPane(tabla), BorderLayout.CENTER);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.CENTER));
        botones.add(boton("Nuevo", this::nuevo));
        botones.add(boton("Editar", this::editar));
        botones.add(boton("Estado", this::estado));
        botones.add(boton("Eliminar", this::eliminar));
        botones.add(Dialogos.botonRefrescar(this::refrescar));
        add(botones, BorderLayout.SOUTH);

        refrescar();
        Dialogos.autoRefrescar(this, this::refrescar);
    }

    private JButton boton(String texto, Runnable accion) {
        JButton b = new JButton(texto);
        b.addActionListener(e -> {
            try {
                accion.run();
            } catch (IllegalArgumentException ex) {
                Dialogos.error(this, ex.getMessage());
            }
        });
        return b;
    }

    private void refrescar() {
        List<Object[]> filas = new ArrayList<>();
        ListaSimple<Arrendatario> lista = arrendatarios.listar();
        for (int i = 0; i < lista.getTamano(); i++) {
            Arrendatario a = lista.obtener(i);
            filas.add(new Object[] { a.getDocumentoIdentidad(), a.getNombresCompletos(), a.getTelefono(),
                    a.getCorreo(), a.getEstado() });
        }
        Dialogos.poblar(tabla, modelo, filas);
    }

    private Arrendatario seleccionado() {
        Object doc = Dialogos.seleccionado(tabla, 0);
        if (doc == null) throw new IllegalArgumentException("Seleccione un arrendatario de la tabla.");
        return arrendatarios.buscar(doc.toString());
    }

    // Busqueda por documento de identidad (tabla hash): deja seleccionada la fila
    private void buscarDocumento() {
        Arrendatario a = arrendatarios.buscar(buscar.getText());
        if (a == null) {
            Dialogos.error(this, "No hay un arrendatario con documento \"" + buscar.getText().trim() + "\".");
            return;
        }
        for (int i = 0; i < modelo.getRowCount(); i++) {
            if (a.getDocumentoIdentidad().equals(modelo.getValueAt(i, 0))) {
                tabla.setRowSelectionInterval(i, i);
                tabla.scrollRectToVisible(tabla.getCellRect(i, 0, true));
                return;
            }
        }
    }

    // ---------- Nuevo (RN-01) ----------
    private void nuevo() {
        JTextField documento = Dialogos.texto("");
        JTextField nombres = Dialogos.texto("");
        JTextField telefono = Dialogos.texto("");
        JTextField correo = Dialogos.texto("");
        if (!Dialogos.formulario(this, "Nuevo arrendatario",
                new String[] { "Documento de identidad", "Nombres completos", "Telefono", "Correo" },
                new JComponent[] { documento, nombres, telefono, correo })) return;
        arrendatarios.registrar(documento.getText(), nombres.getText(), telefono.getText(),
                correo.getText());
        refrescar();
    }

    private void editar() {
        Arrendatario a = seleccionado();
        JTextField nombres = Dialogos.texto(a.getNombresCompletos());
        JTextField telefono = Dialogos.texto(a.getTelefono());
        JTextField correo = Dialogos.texto(a.getCorreo());
        if (!Dialogos.formulario(this, "Editar " + a.getDocumentoIdentidad(),
                new String[] { "Nombres completos", "Telefono", "Correo" },
                new JComponent[] { nombres, telefono, correo })) return;
        arrendatarios.actualizar(a.getDocumentoIdentidad(), nombres.getText(), telefono.getText(),
                correo.getText());
        refrescar();
    }

    private void estado() {
        Arrendatario a = seleccionado();
        Object nuevo = JOptionPane.showInputDialog(this, "Nuevo estado de " + a.getNombresCompletos() + ":",
                "Cambiar estado", JOptionPane.PLAIN_MESSAGE, null, EstadoArrendatario.values(), a.getEstado());
        if (nuevo == null) return;
        arrendatarios.cambiarEstado(a.getDocumentoIdentidad(), (EstadoArrendatario) nuevo);
        refrescar();
    }

    private void eliminar() {
        Arrendatario a = seleccionado();
        ListaSimple<Contrato> lista = contratos.listar();
        for (int i = 0; i < lista.getTamano(); i++) {
            if (lista.obtener(i).getDocumentoArrendatario().equals(a.getDocumentoIdentidad()))
                throw new IllegalArgumentException(
                        "El arrendatario tiene contratos registrados: marquelo INACTIVO en lugar de eliminarlo.");
        }
        if (!Dialogos.confirmar(this, "Eliminar", "Eliminar a " + a.getNombresCompletos() + "?")) return;
        arrendatarios.eliminar(a.getDocumentoIdentidad());
        refrescar();
    }
}

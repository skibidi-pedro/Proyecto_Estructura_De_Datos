package inmobiliaria.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import inmobiliaria.arrendatarios.modelo.Arrendatario;
import inmobiliaria.arrendatarios.servicio.ArrendatarioService;
import inmobiliaria.documentos.modelo.Documento;
import inmobiliaria.documentos.modelo.EstadoDocumento;
import inmobiliaria.documentos.modelo.Expediente;
import inmobiliaria.documentos.modelo.TipoDocumento;
import inmobiliaria.documentos.servicio.ExpedienteService;
import inmobiliaria.nucleo.estructuras.ListaSimple;
import inmobiliaria.nucleo.util.Fechas;

/**
 * Expediente de un arrendatario: documento de identidad y contrato firmado (RN-06).
 * Se elige al arrendatario arriba y la tabla muestra el estado de cada documento requerido.
 * Un contrato solo se activa cuando el expediente esta completo (lo verifica PanelContratos).
 * Es un panel que MenuPrincipal muestra a la derecha de la ventana; se refresca solo cada segundo.
 */
public class PanelDocumentos extends JPanel {
    private final ExpedienteService expedientes;
    private final ArrendatarioService arrendatarios;
    private final JComboBox<Arrendatario> comboArrendatarios = new JComboBox<>();
    private final DefaultTableModel modelo = Dialogos.modelo("Documento", "Estado", "Fecha", "Archivo");
    private final JTable tabla = Dialogos.tabla(modelo);
    private final JLabel resumen = new JLabel(" ");
    private int cantidadAnterior = -1;

    public PanelDocumentos(ExpedienteService expedientes, ArrendatarioService arrendatarios) {
        this.expedientes = expedientes;
        this.arrendatarios = arrendatarios;

        setLayout(new BorderLayout(8, 8));
        Dialogos.mostrarComo(comboArrendatarios, a -> a.getNombresCompletos() + " (" + a.getDocumentoIdentidad() + ")");
        JPanel norte = new JPanel(new FlowLayout(FlowLayout.LEFT));
        norte.add(new JLabel("Arrendatario:"));
        norte.add(comboArrendatarios);
        add(norte, BorderLayout.NORTH);

        JPanel centro = new JPanel(new BorderLayout(4, 4));
        centro.add(new JScrollPane(tabla), BorderLayout.CENTER);
        centro.add(resumen, BorderLayout.SOUTH);
        add(centro, BorderLayout.CENTER);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.CENTER));
        botones.add(boton("Registrar documento", this::registrar));
        botones.add(boton("Marcar vencido", this::marcarVencido));
        botones.add(Dialogos.botonRefrescar(this::refrescar));
        add(botones, BorderLayout.SOUTH);

        comboArrendatarios.addActionListener(e -> refrescar());
        cargarArrendatarios();
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

    // El combo se vuelve a llenar solo si cambio la cantidad de arrendatarios (alta o baja en su modulo)
    private void cargarArrendatarios() {
        ListaSimple<Arrendatario> lista = arrendatarios.listar();
        if (lista.getTamano() == cantidadAnterior) return;
        cantidadAnterior = lista.getTamano();
        Arrendatario previo = (Arrendatario) comboArrendatarios.getSelectedItem();
        comboArrendatarios.removeAllItems();
        for (int i = 0; i < lista.getTamano(); i++) comboArrendatarios.addItem(lista.obtener(i));
        if (previo != null) comboArrendatarios.setSelectedItem(arrendatarios.buscar(previo.getDocumentoIdentidad()));
    }

    private Arrendatario elegido() {
        Arrendatario a = (Arrendatario) comboArrendatarios.getSelectedItem();
        if (a == null)
            throw new IllegalArgumentException("No hay arrendatarios. Registrelos en el modulo Arrendatarios.");
        return a;
    }

    private void refrescar() {
        cargarArrendatarios();
        List<Object[]> filas = new ArrayList<>();
        Arrendatario a = (Arrendatario) comboArrendatarios.getSelectedItem();
        if (a == null) {
            resumen.setText("  No hay arrendatarios registrados.");
            Dialogos.poblar(tabla, modelo, filas);
            return;
        }
        Expediente e = expedientes.buscarExpediente(a.getDocumentoIdentidad());
        for (TipoDocumento t : TipoDocumento.values()) {
            Documento d = e == null ? null : e.buscarPorTipo(t);
            if (d == null) filas.add(new Object[] { t, "FALTA", "", "" });
            else filas.add(new Object[] { t, d.getEstado(), d.getFechaRegistro(), d.getRutaArchivo() });
        }
        Dialogos.poblar(tabla, modelo, filas);
        resumen.setText(expedientes.expedienteCompleto(a.getDocumentoIdentidad())
                ? "  Expediente COMPLETO: ya se puede activar su contrato."
                : "  Expediente INCOMPLETO. Falta: " + expedientes.faltantes(a.getDocumentoIdentidad()));
    }

    private TipoDocumento tipoSeleccionado() {
        Object t = Dialogos.seleccionado(tabla, 0);
        if (t == null) throw new IllegalArgumentException("Seleccione un documento de la tabla.");
        return (TipoDocumento) t;
    }

    // ---------- Registrar documento recibido ----------
    private void registrar() {
        Arrendatario a = elegido();
        JComboBox<TipoDocumento> tipo = new JComboBox<>(TipoDocumento.values());
        Object sel = Dialogos.seleccionado(tabla, 0);
        if (sel != null) tipo.setSelectedItem(sel);
        JTextField ruta = Dialogos.texto("");
        JTextField fecha = Dialogos.texto(Fechas.convertirAString(LocalDate.now()));
        if (!Dialogos.formulario(this, "Documento de " + a.getNombresCompletos(),
                new String[] { "Tipo", "Archivo (ruta o nombre)", "Fecha (AAAA-MM-DD)" },
                new JComponent[] { tipo, ruta, fecha })) return;
        expedientes.registrarDocumento(a.getDocumentoIdentidad(), (TipoDocumento) tipo.getSelectedItem(),
                ruta.getText(), Dialogos.fecha(fecha, "Fecha"));
        refrescar();
    }

    private void marcarVencido() {
        Arrendatario a = elegido();
        expedientes.cambiarEstado(a.getDocumentoIdentidad(), tipoSeleccionado(), EstadoDocumento.VENCIDO);
        refrescar();
    }
}

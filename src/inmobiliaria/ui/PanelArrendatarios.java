package inmobiliaria.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import java.util.ArrayList;
import java.time.LocalDate;
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
import inmobiliaria.cobranza.servicio.MoraService;
import inmobiliaria.cobranza.servicio.PagoService;
import inmobiliaria.contratos.servicio.ContratoService;
import inmobiliaria.documentos.modelo.TipoDocumento;
import inmobiliaria.documentos.servicio.ExpedienteService;
import inmobiliaria.nucleo.estructuras.ListaSimple;
import inmobiliaria.nucleo.util.Archivos;
import inmobiliaria.propiedades.servicio.PropiedadService;

/**
 * Arrendatarios. Usa ArrendatarioService (Integrante 3).
 * El documento de identidad es la clave (RN-01) y se muestra: lo escribe el usuario.
 * Es un panel que MenuPrincipal muestra a la derecha de la ventana; se refresca solo cada segundo.
 * Un arrendatario con contratos no se puede eliminar .
 * Al crear o editar se anexa su documento de identidad (PDF, Word o foto): la columna Archivo
 * muestra el nombre (Identidad.jpg) y con doble clic en la fila se abre. Si con ese documento
 * quedan completos los de un contrato pendiente, se pregunta si activarlo (ver Activacion).
 */
public class PanelArrendatarios extends JPanel {
    private final ArrendatarioService arrendatarios;
    private final ContratoService contratos;
    private final ExpedienteService expedientes;
    private final PropiedadService propiedades;
    private final PagoService pagos;
    private final MoraService mora;
    private final DefaultTableModel modelo = Dialogos.modelo(
            "Documento", "Nombres", "Telefono", "Correo", "Archivo", "Estado");
    private final JTable tabla = Dialogos.tabla(modelo);
    private final JTextField buscar = new JTextField(10);

    public PanelArrendatarios(ArrendatarioService arrendatarios, ContratoService contratos,
                              ExpedienteService expedientes, PropiedadService propiedades, PagoService pagos, MoraService mora) {
        this.mora = mora;
        this.arrendatarios = arrendatarios;
        this.contratos = contratos;
        this.expedientes = expedientes;
        this.propiedades = propiedades;
        this.pagos = pagos;

        setLayout(new BorderLayout(8, 8));
        JPanel norte = new JPanel(new FlowLayout(FlowLayout.LEFT));
        norte.add(new JLabel("Buscar por documento:"));
        norte.add(buscar);
        JButton ir = new JButton("Buscar");
        ir.addActionListener(e -> buscarDocumento());
        norte.add(ir);
        add(norte, BorderLayout.NORTH);
        add(new JScrollPane(tabla), BorderLayout.CENTER);
        Dialogos.alDobleClick(tabla, () -> ejecutar(this::abrirDocumento));
        Dialogos.colorear(tabla, this::colorDe);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.CENTER));
        botones.add(boton("Nuevo", this::nuevo));
        botones.add(boton("Editar", this::editar));
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
        Activacion.sincronizarEstados(arrendatarios, contratos, mora);   // estado automatico
        List<Object[]> filas = new ArrayList<>();
        ListaSimple<Arrendatario> lista = arrendatarios.listar();
        for (int i = 0; i < lista.getTamano(); i++) {
            Arrendatario a = lista.obtener(i);
            filas.add(new Object[] { a.getDocumentoIdentidad(), a.getNombresCompletos(), a.getTelefono(),
                    a.getCorreo(), nombreArchivo(a), a.getEstado() });
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

    // Archivo: verde si el documento de identidad esta recibido, amarillo si falta.
    // Estado: ACTIVO verde, MOROSO amarillo, INACTIVO rojo.
    private java.awt.Color colorDe(int fila, int columna) {
        if (columna == 4)
            return expedientes.expedienteCompleto(modelo.getValueAt(fila, 0).toString())
                    ? Dialogos.VERDE : Dialogos.AMARILLO;
        if (columna == 5) {
            switch ((EstadoArrendatario) modelo.getValueAt(fila, 5)) {
                case ACTIVO: return Dialogos.VERDE;
                case MOROSO: return Dialogos.AMARILLO;
                default: return Dialogos.ROJO;
            }
        }
        return null;
    }

    private String nombreArchivo(Arrendatario a) {
        String ruta = expedientes.rutaIdentidad(a.getDocumentoIdentidad());
        return ruta.isEmpty() ? "Falta" : Archivos.nombre(ruta);
    }

    private void ejecutar(Runnable accion) {
        try {
            accion.run();
        } catch (IllegalArgumentException ex) {
            Dialogos.error(this, ex.getMessage());
        }
    }

    // Revisa el archivo elegido ANTES de guardar nada, para no dejar un arrendatario a medias
    private File archivoElegido(JTextField ruta) {
        if (ruta.getText().isEmpty()) return null;
        File f = new File(ruta.getText());
        if (!f.isFile()) throw new IllegalArgumentException("No se encuentra el archivo elegido.");
        if (!Archivos.extensionValida(f.getName()))
            throw new IllegalArgumentException("Formato no permitido. Use PDF, Word o imagen jpg o png.");
        return f;
    }

    // Anexa el documento de identidad y, si con eso quedan completos los documentos de un contrato
    // pendiente, pregunta si activarlo
    private void anexarIdentidad(Arrendatario a, File archivo) {
        if (archivo == null) return;
        expedientes.anexarArchivo(a.getDocumentoIdentidad(), TipoDocumento.IDENTIDAD, archivo, LocalDate.now());
        refrescar();
        Activacion.ofrecer(this, a.getDocumentoIdentidad(), a.getNombresCompletos(),
                contratos, propiedades, pagos, expedientes);
    }

    // ---------- Nuevo (RN-01) ----------
    private void nuevo() {
        JTextField documento = Dialogos.texto("");
        JTextField nombres = Dialogos.texto("");
        JTextField telefono = Dialogos.texto("");
        JTextField correo = Dialogos.texto("");
        JTextField archivo = Dialogos.texto("");
        if (!Dialogos.formulario(this, "Nuevo arrendatario",
                new String[] { "Numero de documento", "Nombres completos", "Telefono", "Correo",
                        "Documento de identidad" },
                new JComponent[] { documento, nombres, telefono, correo, Dialogos.campoArchivo(this, archivo) })) return;
        File f = archivoElegido(archivo);
        Arrendatario a = arrendatarios.registrar(documento.getText(), nombres.getText(), telefono.getText(),
                correo.getText());
        refrescar();
        anexarIdentidad(a, f);
    }

    private void editar() {
        Arrendatario a = seleccionado();
        JTextField nombres = Dialogos.texto(a.getNombresCompletos());
        JTextField telefono = Dialogos.texto(a.getTelefono());
        JTextField correo = Dialogos.texto(a.getCorreo());
        JTextField archivo = Dialogos.texto("");
        if (!Dialogos.formulario(this, "Editar " + a.getDocumentoIdentidad(),
                new String[] { "Nombres completos", "Telefono", "Correo",
                        "Documento de identidad" },
                new JComponent[] { nombres, telefono, correo, Dialogos.campoArchivo(this, archivo) })) return;
        File f = archivoElegido(archivo);
        arrendatarios.actualizar(a.getDocumentoIdentidad(), nombres.getText(), telefono.getText(),
                correo.getText());
        refrescar();
        anexarIdentidad(a, f);
    }

    // Doble clic en la fila: abre el documento de identidad anexado
    private void abrirDocumento() {
        Arrendatario a = seleccionado();
        String ruta = expedientes.rutaIdentidad(a.getDocumentoIdentidad());
        if (ruta.isEmpty())
            throw new IllegalArgumentException("Este arrendatario todavia no tiene documento de identidad anexado. "
                    + "Use Editar para anexarlo.");
        Archivos.abrir(ruta);
    }

    private void eliminar() {
        Arrendatario a = seleccionado();
        ListaSimple<Contrato> lista = contratos.listar();
        for (int i = 0; i < lista.getTamano(); i++) {
            if (lista.obtener(i).getDocumentoArrendatario().equals(a.getDocumentoIdentidad()))
                throw new IllegalArgumentException(
                        "El arrendatario tiene contratos registrados: no se puede eliminar.");
        }
        if (!Dialogos.confirmar(this, "Eliminar", "Eliminar a " + a.getNombresCompletos() + "?")) return;
        arrendatarios.eliminar(a.getDocumentoIdentidad());
        expedientes.eliminarExpediente(a.getDocumentoIdentidad());
        refrescar();
    }
}

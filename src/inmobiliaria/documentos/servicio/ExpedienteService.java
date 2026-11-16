package inmobiliaria.documentos.servicio;

import java.io.File;
import java.time.LocalDate;
import inmobiliaria.documentos.modelo.Documento;
import inmobiliaria.documentos.modelo.EstadoDocumento;
import inmobiliaria.documentos.modelo.Expediente;
import inmobiliaria.documentos.modelo.TipoDocumento;
import inmobiliaria.nucleo.estructuras.ListaSimple;
import inmobiliaria.nucleo.historial.Historial;
import inmobiliaria.nucleo.persistencia.GestorCSV;
import inmobiliaria.nucleo.persistencia.Persistible;
import inmobiliaria.nucleo.util.Archivos;
import inmobiliaria.nucleo.util.Fechas;
import inmobiliaria.nucleo.util.Validador;

/**
 * Documento de identidad de cada arrendatario (archivo anexado) y verificacion del expediente (2.4).
 * Por parte del arrendatario, el expediente esta completo (RN-06) cuando tiene RECIBIDO su documento
 * de identidad; el documento del contrato se anexa al Contrato y Activacion revisa ambos.
 *
 * El servicio no valida que el arrendatario exista: quien lo llama (el panel) ya lo eligio
 * de la lista de arrendatarios.
 *
 * Responsable: Integrante 3 - Quispe Rodriguez Carlos Manuel Ignacio
 * Reglas: RN-06
 */
public class ExpedienteService {
    private static final String ARCHIVO = "documentos.csv";

    private ListaSimple<Expediente> expedientes = new ListaSimple<>();   // no es final: eliminarExpediente la reemplaza
    private Historial historial;

    public ExpedienteService(Historial historial) {
        this.historial = historial;
    }

    // ---------- Registrar un documento recibido ----------
    // Si ya habia uno de ese tipo (pendiente o vencido) se reemplaza, no se duplica.
    public Documento registrarDocumento(String documentoArrendatario, TipoDocumento tipo,
                                        String rutaArchivo, LocalDate fecha) {
        if (!Validador.esTextoValido(documentoArrendatario))
            throw new IllegalArgumentException("Falta el documento del arrendatario");
        if (tipo == null)
            throw new IllegalArgumentException("Falta el tipo de documento");
        if (fecha == null)
            throw new IllegalArgumentException("Falta la fecha de registro");

        Expediente e = expedienteODefault(documentoArrendatario.trim());
        Documento d = e.buscarPorTipo(tipo);
        if (d == null) {
            d = new Documento(e.getDocumentoIdentidadArrendatario(), tipo, EstadoDocumento.RECIBIDO,
                    Fechas.convertirAString(fecha), rutaArchivo);
            e.agregarDocumento(d);
        } else {
            d.setEstado(EstadoDocumento.RECIBIDO);
            d.setFechaRegistro(Fechas.convertirAString(fecha));
            d.setRutaArchivo(rutaArchivo);
        }
        historial.registrarOperacion("Documento " + tipo + " recibido de " + e.getDocumentoIdentidadArrendatario());
        return d;
    }

    // ---------- Anexar un archivo (PDF, Word o foto) como documento recibido ----------
    // Se copia a data/archivos/<documento del arrendatario>/ con el nombre del tipo:
    // Identidad.jpg, Identidad.pdf, ... Si ya habia uno, se reemplaza.
    public Documento anexarArchivo(String documentoArrendatario, TipoDocumento tipo,
                                   File origen, LocalDate fecha) {
        if (!Validador.esTextoValido(documentoArrendatario))
            throw new IllegalArgumentException("Falta el documento del arrendatario");
        if (tipo == null)
            throw new IllegalArgumentException("Falta el tipo de documento");

        Expediente e = buscarExpediente(documentoArrendatario);
        Documento previo = e == null ? null : e.buscarPorTipo(tipo);
        String rutaAnterior = previo == null ? "" : previo.getRutaArchivo();

        String ruta = Archivos.copiar(origen, documentoArrendatario.trim(), "Identidad");
        Documento d = registrarDocumento(documentoArrendatario, tipo, ruta, fecha);
        if (!rutaAnterior.isEmpty() && !rutaAnterior.equals(ruta)) Archivos.eliminar(rutaAnterior);
        return d;
    }

    // ---------- Cambiar estado (p. ej. marcar vencido un documento) ----------
    public void cambiarEstado(String documentoArrendatario, TipoDocumento tipo, EstadoDocumento estado) {
        Expediente e = buscarExpediente(documentoArrendatario);
        Documento d = e == null ? null : e.buscarPorTipo(tipo);
        if (d == null)
            throw new IllegalArgumentException("El arrendatario no tiene el documento " + tipo);
        d.setEstado(estado);
        historial.registrarOperacion("Documento " + tipo + " de " + documentoArrendatario + " pasa a " + estado);
    }

    // ---------- RN-06 ----------
    public boolean expedienteCompleto(String documentoArrendatario) {
        Expediente e = buscarExpediente(documentoArrendatario);
        return e != null && e.estaCompleto();
    }

    // Ruta del documento de identidad anexado ("" si no tiene)
    public String rutaIdentidad(String documentoArrendatario) {
        Expediente e = buscarExpediente(documentoArrendatario);
        Documento d = e == null ? null : e.buscarPorTipo(TipoDocumento.IDENTIDAD);
        return d == null ? "" : d.getRutaArchivo();
    }

    // Borra el expediente (y sus archivos) al dar de baja al arrendatario
    public void eliminarExpediente(String documentoArrendatario) {
        ListaSimple<Expediente> quedan = new ListaSimple<>();
        for (int i = 0; i < expedientes.getTamano(); i++) {
            Expediente e = expedientes.obtener(i);
            if (e.getDocumentoIdentidadArrendatario().equals(documentoArrendatario)) {
                for (int j = 0; j < e.getDocumentos().getTamano(); j++)
                    Archivos.eliminar(e.getDocumentos().obtener(j).getRutaArchivo());
            } else {
                quedan.agregar(e);
            }
        }
        expedientes = quedan;
    }

    // Que le falta al expediente, en texto (para mostrar al activar un contrato)
    public String faltantes(String documentoArrendatario) {
        Expediente e = buscarExpediente(documentoArrendatario);
        StringBuilder sb = new StringBuilder();
        for (TipoDocumento t : TipoDocumento.values()) {
            Documento d = e == null ? null : e.buscarPorTipo(t);
            if (d == null || d.getEstado() != EstadoDocumento.RECIBIDO) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(t);
            }
        }
        return sb.toString();
    }

    // ---------- Consultas ----------
    public Expediente buscarExpediente(String documentoArrendatario) {
        if (documentoArrendatario == null) return null;
        for (int i = 0; i < expedientes.getTamano(); i++) {
            Expediente e = expedientes.obtener(i);
            if (e.getDocumentoIdentidadArrendatario().equals(documentoArrendatario.trim())) return e;
        }
        return null;
    }

    public ListaSimple<Documento> documentosDe(String documentoArrendatario) {
        Expediente e = buscarExpediente(documentoArrendatario);
        return e == null ? new ListaSimple<>() : e.getDocumentos();
    }

    private Expediente expedienteODefault(String documentoArrendatario) {
        Expediente e = buscarExpediente(documentoArrendatario);
        if (e == null) {
            e = new Expediente(documentoArrendatario);
            expedientes.agregar(e);
        }
        return e;
    }

    // ---------- Persistencia (data/documentos.csv) ----------
    public void cargar() {
        ListaSimple<String> lineas = new ListaSimple<>();
        GestorCSV.cargar(ARCHIVO, lineas);
        for (int i = 0; i < lineas.getTamano(); i++) {
            Documento d = new Documento();
            try {
                d.desdeLineaCSV(lineas.obtener(i));
            } catch (IllegalArgumentException ex) {
                continue;   // tipo que ya no existe (p. ej. CONTRATO_FIRMADO de una version anterior)
            }
            expedienteODefault(d.getDocumentoArrendatario()).agregarDocumento(d);
        }
    }

    public void guardar() {
        ListaSimple<Persistible> lista = new ListaSimple<>();
        for (int i = 0; i < expedientes.getTamano(); i++) {
            ListaSimple<Documento> docs = expedientes.obtener(i).getDocumentos();
            for (int j = 0; j < docs.getTamano(); j++) lista.agregar(docs.obtener(j));
        }
        GestorCSV.guardar(ARCHIVO, lista);
    }
}

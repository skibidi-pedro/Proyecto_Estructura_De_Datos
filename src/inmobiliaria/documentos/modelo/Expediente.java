package inmobiliaria.documentos.modelo;

import inmobiliaria.nucleo.estructuras.ListaSimple;

/**
 * Expediente de un arrendatario: su documento de identidad (archivo anexado).
 *
 * Responsable: Integrante 3 - Quispe Rodriguez Carlos Manuel Ignacio
 * Reglas: RN-06
 */
public class Expediente {
    private String documentoIdentidadArrendatario;
    private ListaSimple<Documento> documentosRequeridos;

    public Expediente(String documentoIdentidadArrendatario) {
        this.documentoIdentidadArrendatario = documentoIdentidadArrendatario;
        this.documentosRequeridos = new ListaSimple<>();
    }

    public String getDocumentoIdentidadArrendatario() { return documentoIdentidadArrendatario; }

    public ListaSimple<Documento> getDocumentos() { return documentosRequeridos; }

    public void agregarDocumento(Documento doc) {
        documentosRequeridos.agregar(doc);
    }

    // Un expediente tiene a lo sumo un documento por tipo
    public Documento buscarPorTipo(TipoDocumento tipo) {
        for (int i = 0; i < documentosRequeridos.getTamano(); i++) {
            Documento d = documentosRequeridos.obtener(i);
            if (d.getTipo() == tipo) return d;
        }
        return null;
    }

    // RN-06 (parte del arrendatario): tiene su documento de identidad recibido.
    // La otra parte, el documento del contrato, se revisa en el Contrato (ver Activacion).
    public boolean estaCompleto() {
        Documento id = buscarPorTipo(TipoDocumento.IDENTIDAD);
        return id != null && id.getEstado() == EstadoDocumento.RECIBIDO;
    }
}

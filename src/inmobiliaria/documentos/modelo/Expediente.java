package inmobiliaria.documentos.modelo;

import inmobiliaria.nucleo.estructuras.ListaSimple;

/**
 * Expediente de un arrendatario: los documentos que respaldan su contrato.
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

    // RN-06: Verificación automática del expediente completo antes de activar contrato
    public boolean estaCompleto() {
        boolean tieneIdentidad = false;
        boolean tieneContrato = false;

        for (int i = 0; i < documentosRequeridos.getTamano(); i++) {
            Documento doc = documentosRequeridos.obtener(i);
            if (doc.getEstado() == EstadoDocumento.RECIBIDO) {
                if (doc.getTipo() == TipoDocumento.IDENTIDAD) tieneIdentidad = true;
                if (doc.getTipo() == TipoDocumento.CONTRATO_FIRMADO) tieneContrato = true;
            }
        }
        return tieneIdentidad && tieneContrato;
    }
}

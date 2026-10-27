package inmobiliaria.documentos.modelo;

import java.util.ArrayList;
import java.util.List;

public class Expediente {
    private String documentoIdentidadArrendatario;
    private List<Documento> documentosRequeridos;

    public Expediente(String documentoIdentidadArrendatario) {
        this.documentoIdentidadArrendatario = documentoIdentidadArrendatario;
        this.documentosRequeridos = new ArrayList<>();
    }

    public void agregarDocumento(Documento doc) {
        documentosRequeridos.add(doc);
    }

    public boolean estaCompleto() {
        boolean tieneIdentidad = false;
        boolean tieneContrato = false;

        for (Documento doc : documentosRequeridos) {
            if (doc.getEstado() == EstadoDocumento.RECIBIDO) {
                if (doc.getTipo() == TipoDocumento.IDENTIDAD) tieneIdentidad = true;
                if (doc.getTipo() == TipoDocumento.CONTRATO_FIRMADO) tieneContrato = true;
            }
        }
        return tieneIdentidad && tieneContrato;
    }
}
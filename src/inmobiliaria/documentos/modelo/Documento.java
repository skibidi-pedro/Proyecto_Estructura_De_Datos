package inmobiliaria.documentos.modelo;

import inmobiliaria.nucleo.persistencia.Persistible;

/**
 * Documento del expediente de un arrendatario. Se guarda en data/documentos.csv.
 *
 * Responsable: Integrante 2 - Zavaleta Calderon Steven Jack
 */
public class Documento implements Persistible {
    private String documentoArrendatario;
    private TipoDocumento tipo;
    private EstadoDocumento estado;
    private String fechaRegistro;
    private String rutaArchivo;

    // Vacio, para poder usar desdeLineaCSV al cargar
    public Documento() {
    }

    public Documento(String documentoArrendatario, TipoDocumento tipo, EstadoDocumento estado,
                     String fechaRegistro, String rutaArchivo) {
        this.documentoArrendatario = documentoArrendatario;
        this.tipo = tipo;
        this.estado = estado;
        this.fechaRegistro = fechaRegistro;
        this.rutaArchivo = limpiar(rutaArchivo);
    }

    public String getDocumentoArrendatario() { return documentoArrendatario; }
    public TipoDocumento getTipo() { return tipo; }
    public EstadoDocumento getEstado() { return estado; }
    public String getFechaRegistro() { return fechaRegistro; }
    public String getRutaArchivo() { return rutaArchivo; }

    public void setEstado(EstadoDocumento estado) { this.estado = estado; }
    public void setFechaRegistro(String fechaRegistro) { this.fechaRegistro = fechaRegistro; }
    public void setRutaArchivo(String rutaArchivo) { this.rutaArchivo = limpiar(rutaArchivo); }

    // El separador del CSV es ';', asi que no puede aparecer en la ruta
    private String limpiar(String texto) {
        return texto == null ? "" : texto.trim().replace(";", ",");
    }

    @Override
    public String aLineaCSV() {
        return documentoArrendatario + ";" + tipo + ";" + estado + ";" + fechaRegistro + ";" + rutaArchivo;
    }

    @Override
    public void desdeLineaCSV(String linea) {
        String[] p = linea.split(";", -1);
        documentoArrendatario = p[0];
        tipo = TipoDocumento.valueOf(p[1]);
        estado = EstadoDocumento.valueOf(p[2]);
        fechaRegistro = p[3];
        rutaArchivo = p[4];
    }

    @Override
    public String toString() {
        return tipo + " | " + estado + " | " + fechaRegistro + " | " + rutaArchivo;
    }
}

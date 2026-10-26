package inmobiliaria.documentos.modelo;

public class Documento {
    private TipoDocumento tipo;
    private EstadoDocumento estado;
    private String fechaRegistro;
    private String rutaArchivo;

    public Documento(TipoDocumento tipo, EstadoDocumento estado, String fechaRegistro, String rutaArchivo) {
        this.tipo = tipo;
        this.estado = estado;
        this.fechaRegistro = fechaRegistro;
        this.rutaArchivo = rutaArchivo;
    }

    public TipoDocumento getTipo() { return tipo; }
    public EstadoDocumento getEstado() { return estado; }
    public String getFechaRegistro() { return fechaRegistro; }
    public String getRutaArchivo() { return rutaArchivo; }
}
package inmobiliaria.mantenimiento.modelo;

public class SolicitudMantenimiento implements Comparable<SolicitudMantenimiento> {
    private String identificadorUnidad;
    private String descripcion;
    private Urgencia nivelUrgencia;
    private EstadoSolicitud estado;

    public SolicitudMantenimiento(String identificadorUnidad, String descripcion, Urgencia nivelUrgencia) {
        this.identificadorUnidad = identificadorUnidad;
        this.descripcion = descripcion;
        this.nivelUrgencia = nivelUrgencia;
        this.estado = EstadoSolicitud.PENDIENTE;
    }

    public Urgencia getNivelUrgencia() { return nivelUrgencia; }
    public String getIdentificadorUnidad() { return identificadorUnidad; }
    
    @Override
    public int compareTo(SolicitudMantenimiento otra) {
        return this.nivelUrgencia.compareTo(otra.nivelUrgencia);
    }
}
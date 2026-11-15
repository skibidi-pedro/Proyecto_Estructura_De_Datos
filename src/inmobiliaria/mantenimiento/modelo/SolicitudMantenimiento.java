package inmobiliaria.mantenimiento.modelo;

import inmobiliaria.nucleo.persistencia.Persistible;

/**
 * Solicitud de mantenimiento de una unidad. Se guarda en data/mantenimiento.csv;
 * el orden del archivo es el orden de llegada.
 *
 * Responsable: Integrante 3 - Quispe Rodriguez Carlos Manuel Ignacio
 */
public class SolicitudMantenimiento implements Persistible, Comparable<SolicitudMantenimiento> {
    private String codigo;
    private String identificadorUnidad;
    private String descripcion;
    private Urgencia nivelUrgencia;
    private EstadoSolicitud estado;
    private String fechaRegistro;

    // Vacio, para poder usar desdeLineaCSV al cargar
    public SolicitudMantenimiento() {
    }

    public SolicitudMantenimiento(String codigo, String identificadorUnidad, String descripcion,
                                  Urgencia nivelUrgencia, String fechaRegistro) {
        this.codigo = codigo;
        this.identificadorUnidad = identificadorUnidad;
        this.descripcion = limpiar(descripcion);
        this.nivelUrgencia = nivelUrgencia;
        this.estado = EstadoSolicitud.PENDIENTE;
        this.fechaRegistro = fechaRegistro;
    }

    public String getCodigo() { return codigo; }
    public Urgencia getNivelUrgencia() { return nivelUrgencia; }
    public String getIdentificadorUnidad() { return identificadorUnidad; }
    public String getDescripcion() { return descripcion; }
    public EstadoSolicitud getEstado() { return estado; }
    public String getFechaRegistro() { return fechaRegistro; }

    public void setEstado(EstadoSolicitud estado) { this.estado = estado; }

    // El separador del CSV es ';', asi que no puede aparecer en la descripcion
    private String limpiar(String texto) {
        return texto == null ? "" : texto.trim().replace(";", ",");
    }

    // ALTA primero (el orden del enum). Entre iguales, la cola respeta el orden de llegada.
    @Override
    public int compareTo(SolicitudMantenimiento otra) {
        return this.nivelUrgencia.compareTo(otra.nivelUrgencia);
    }

    @Override
    public String aLineaCSV() {
        return codigo + ";" + identificadorUnidad + ";" + descripcion + ";"
                + nivelUrgencia + ";" + estado + ";" + fechaRegistro;
    }

    @Override
    public void desdeLineaCSV(String linea) {
        String[] p = linea.split(";", -1);
        codigo = p[0];
        identificadorUnidad = p[1];
        descripcion = p[2];
        nivelUrgencia = Urgencia.valueOf(p[3]);
        estado = EstadoSolicitud.valueOf(p[4]);
        fechaRegistro = p[5];
    }

    @Override
    public String toString() {
        return codigo + " | unidad " + identificadorUnidad + " | " + nivelUrgencia + " | " + estado
                + " | " + descripcion;
    }
}

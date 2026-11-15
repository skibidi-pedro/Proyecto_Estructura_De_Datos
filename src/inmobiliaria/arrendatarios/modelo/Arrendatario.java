package inmobiliaria.arrendatarios.modelo;

import inmobiliaria.nucleo.persistencia.Persistible;

/**
 * Entidad arrendatario. Se guarda en data/arrendatarios.csv; su aval va aparte en data/avales.csv.
 *
 * Responsable: Integrante 3 - Quispe Rodriguez Carlos Manuel Ignacio
 * Reglas: RN-01
 */
public class Arrendatario implements Persistible {
    private String documentoIdentidad;
    private String nombresCompletos;
    private String telefono;
    private String correo;
    private String ocupacion;
    private Aval aval;
    private EstadoArrendatario estado;

    // Vacio, para poder usar desdeLineaCSV al cargar
    public Arrendatario() {
    }

    public Arrendatario(String documentoIdentidad, String nombresCompletos, String telefono,
                        String correo, String ocupacion, Aval aval) {
        this.documentoIdentidad = documentoIdentidad;
        this.nombresCompletos = limpiar(nombresCompletos);
        this.telefono = limpiar(telefono);
        this.correo = limpiar(correo);
        this.ocupacion = limpiar(ocupacion);
        this.aval = aval;
        this.estado = EstadoArrendatario.ACTIVO;
    }

    public String getDocumentoIdentidad() { return documentoIdentidad; }
    public String getNombresCompletos() { return nombresCompletos; }
    public String getTelefono() { return telefono; }
    public String getCorreo() { return correo; }
    public String getOcupacion() { return ocupacion; }
    public Aval getAval() { return aval; }
    public EstadoArrendatario getEstado() { return estado; }

    public void setNombresCompletos(String v) { this.nombresCompletos = limpiar(v); }
    public void setTelefono(String v) { this.telefono = limpiar(v); }
    public void setCorreo(String v) { this.correo = limpiar(v); }
    public void setOcupacion(String v) { this.ocupacion = limpiar(v); }
    public void setAval(Aval aval) { this.aval = aval; }
    public void setEstado(EstadoArrendatario estado) { this.estado = estado; }

    // El separador del CSV es ';', asi que no puede aparecer en los textos
    private String limpiar(String texto) {
        return texto == null ? "" : texto.trim().replace(";", ",");
    }

    @Override
    public String aLineaCSV() {
        return documentoIdentidad + ";" + nombresCompletos + ";" + telefono + ";"
                + correo + ";" + ocupacion + ";" + estado;
    }

    @Override
    public void desdeLineaCSV(String linea) {
        String[] p = linea.split(";", -1);
        documentoIdentidad = p[0];
        nombresCompletos = p[1];
        telefono = p[2];
        correo = p[3];
        ocupacion = p[4];
        estado = EstadoArrendatario.valueOf(p[5]);
    }

    @Override
    public String toString() {
        return nombresCompletos + " (" + documentoIdentidad + ")";
    }
}

package inmobiliaria.arrendatarios.modelo;

public class Arrendatario {
    private String documentoIdentidad;
    private String nombresCompletos;
    private String telefono;
    private String correo;
    private String ocupacion;
    private Aval aval;

    public Arrendatario(String documentoIdentidad, String nombresCompletos, String telefono, String correo, String ocupacion, Aval aval) {
        this.documentoIdentidad = documentoIdentidad;
        this.nombresCompletos = nombresCompletos;
        this.telefono = telefono;
        this.correo = correo;
        this.ocupacion = ocupacion;
        this.aval = aval;
    }

    public String getDocumentoIdentidad() { return documentoIdentidad; }
    public String getNombresCompletos() { return nombresCompletos; }
}

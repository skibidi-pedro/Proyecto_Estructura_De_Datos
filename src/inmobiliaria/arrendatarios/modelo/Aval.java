package inmobiliaria.arrendatarios.modelo;

public class Aval {
    private String documentoIdentidad;
    private String nombresCompletos;
    private double ingresosComprobados;

    public Aval(String documentoIdentidad, String nombresCompletos, double ingresosComprobados) {
        this.documentoIdentidad = documentoIdentidad;
        this.nombresCompletos = nombresCompletos;
        this.ingresosComprobados = ingresosComprobados;
    }

    public String getDocumentoIdentidad() { return documentoIdentidad; }
    public String getNombresCompletos() { return nombresCompletos; }
    public double getIngresosComprobados() { return ingresosComprobados; }
}
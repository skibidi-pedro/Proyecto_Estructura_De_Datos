package inmobiliaria.arrendatarios.modelo;

/**
 * Aval (garante) de un arrendatario. Se guarda en data/avales.csv junto al
 * documento del arrendatario al que respalda.
 */
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

    @Override
    public String toString() {
        return nombresCompletos + " (" + documentoIdentidad + ")";
    }
}

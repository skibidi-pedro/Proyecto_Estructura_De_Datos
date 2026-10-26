package inmobiliaria.propiedades.modelo;

import inmobiliaria.nucleo.persistencia.Persistible;

/**
 * Departamento, casa, local o cuarto.
 *
 * Responsable: Integrante 2 - Zavaleta Calderon Steven Jack
 * Reglas: RN-01, RN-02
 */
public class Unidad implements Persistible {
    private String codigo;
    private String codigoEdificio;
    private String direccion;
    private TipoUnidad tipo;
    private double precio;
    private EstadoUnidad estado;

    public Unidad() {
    }

    public Unidad(String codigo, String codigoEdificio, String direccion,
                  TipoUnidad tipo, double precio, EstadoUnidad estado) {
        this.codigo = codigo;
        this.codigoEdificio = codigoEdificio;
        this.direccion = limpiar(direccion);
        this.tipo = tipo;
        this.precio = precio;
        this.estado = estado;
    }

    public String getCodigo() { return codigo; }
    public String getCodigoEdificio() { return codigoEdificio; }
    public String getDireccion() { return direccion; }
    public TipoUnidad getTipo() { return tipo; }
    public double getPrecio() { return precio; }
    public EstadoUnidad getEstado() { return estado; }

    public void setDireccion(String direccion) { this.direccion = limpiar(direccion); }
    public void setTipo(TipoUnidad tipo) { this.tipo = tipo; }
    public void setPrecio(double precio) { this.precio = precio; }
    public void setEstado(EstadoUnidad estado) { this.estado = estado; }

    private String limpiar(String texto) {
        return texto == null ? "" : texto.trim().replace(";", ",");
    }

    @Override
    public String aLineaCSV() {
        return codigo + ";" + codigoEdificio + ";" + direccion + ";" + tipo + ";" + precio + ";" + estado;
    }

    @Override
    public void desdeLineaCSV(String linea) {
        String[] p = linea.split(";", -1);
        codigo = p[0];
        codigoEdificio = p[1];
        direccion = p[2];
        tipo = TipoUnidad.valueOf(p[3]);
        precio = Double.parseDouble(p[4]);
        estado = EstadoUnidad.valueOf(p[5]);
    }

    @Override
    public String toString() {
        return codigo + " | " + tipo + " | " + direccion + " | S/ " + precio + " | " + estado;
    }
}
package inmobiliaria.propiedades.modelo;

import inmobiliaria.nucleo.persistencia.Persistible;

/**
 * Entidad edificio.
 *
 * Responsable: Integrante 2 - Zavaleta Calderon Steven Jack
 */
public class Edificio implements Persistible {
    private String codigo;
    private String codigoPropietario;
    private String nombre;
    private String direccion;

    public Edificio() {
    }

    public Edificio(String codigo, String codigoPropietario, String nombre, String direccion) {
        this.codigo = codigo;
        this.codigoPropietario = codigoPropietario;
        this.nombre = limpiar(nombre);
        this.direccion = limpiar(direccion);
    }

    public String getCodigo() { return codigo; }
    public String getCodigoPropietario() { return codigoPropietario; }
    public String getNombre() { return nombre; }
    public String getDireccion() { return direccion; }

    public void setNombre(String nombre) { this.nombre = limpiar(nombre); }
    public void setDireccion(String direccion) { this.direccion = limpiar(direccion); }

    private String limpiar(String texto) {
        return texto == null ? "" : texto.trim().replace(";", ",");
    }

    @Override
    public String aLineaCSV() {
        return codigo + ";" + codigoPropietario + ";" + nombre + ";" + direccion;
    }

    @Override
    public void desdeLineaCSV(String linea) {
        String[] p = linea.split(";", -1);
        codigo = p[0];
        codigoPropietario = p[1];
        nombre = p[2];
        direccion = p[3];
    }

    @Override
    public String toString() {
        return codigo + " - " + nombre + " (" + direccion + ")";
    }
}
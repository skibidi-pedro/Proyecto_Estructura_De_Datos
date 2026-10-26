package inmobiliaria.propiedades.modelo;

import inmobiliaria.nucleo.persistencia.Persistible;

/**
 * Entidad propietario.
 *
 * Responsable: Integrante 2 - Zavaleta Calderon Steven Jack
 * Reglas: RN-01
 */
public class Propietario implements Persistible {
    private String codigo;
    private String nombre;

    // Vacio, para usar desdeLineaCSV al cargar
    public Propietario() {
    }

    public Propietario(String codigo, String nombre) {
        this.codigo = codigo;
        this.nombre = limpiar(nombre);
    }

    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = limpiar(nombre); }

    // El separador del CSV es ';'
    private String limpiar(String texto) {
        return texto == null ? "" : texto.trim().replace(";", ",");
    }

    @Override
    public String aLineaCSV() {
        return codigo + ";" + nombre;
    }

    @Override
    public void desdeLineaCSV(String linea) {
        String[] p = linea.split(";", -1);
        codigo = p[0];
        nombre = p[1];
    }

    @Override
    public String toString() {
        return codigo + " - " + nombre;
    }
}
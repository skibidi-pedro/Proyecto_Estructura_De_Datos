package inmobiliaria.nucleo.persistencia;

public interface Persistible {
    String aLineaCSV();
    void desdeLineaCSV(String linea);
}

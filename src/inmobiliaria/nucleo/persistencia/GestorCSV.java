package inmobiliaria.nucleo.persistencia;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import inmobiliaria.nucleo.estructuras.ListaSimple;
/**
 * Lectura y escritura de archivos CSV de todos los modulos (2.8).
 *
 * Responsable: Integrante 1 - Francia Ramos Johan Mitziu
 */


public class GestorCSV {
    private static final String RUTA_DIRECTORIO = "data/";

    public static void guardar(String nombreArchivo, ListaSimple<Persistible> elementos) {
        File directorio = new File(RUTA_DIRECTORIO);
        if (!directorio.exists()) {
            directorio.mkdirs();
        }

        File archivo = new File(RUTA_DIRECTORIO + nombreArchivo);
        try (BufferedWriter escritor = new BufferedWriter(new FileWriter(archivo))) {
            for (int i = 0; i < elementos.getTamano(); i++) {
                Persistible item = elementos.obtener(i);
                escritor.write(item.aLineaCSV());
                escritor.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error al guardar el archivo: " + e.getMessage());
        }
    }

    public static void cargar(String nombreArchivo, ListaSimple<String> lineas) {
        File archivo = new File(RUTA_DIRECTORIO + nombreArchivo);
        if (!archivo.exists()) {
            return;
        }

        try (BufferedReader lector = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                if (!linea.trim().isEmpty()) {
                    lineas.agregar(linea);
                }
            }
        } catch (IOException e) {
            System.err.println("Error al leer el archivo: " + e.getMessage());
        }
    }
}

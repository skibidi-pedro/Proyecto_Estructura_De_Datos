package inmobiliaria.nucleo.persistencia;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import inmobiliaria.nucleo.estructuras.ListaSimple;

/** Persistencia CSV: escritura temporal y errores visibles al usuario. */
public class GestorCSV {
    private static final Path DIRECTORIO = Paths.get("data");

    public static void guardar(String nombreArchivo, ListaSimple<Persistible> elementos) {
        Path temporal = null;
        try {
            Files.createDirectories(DIRECTORIO);
            Path destino = DIRECTORIO.resolve(nombreArchivo);
            temporal = Files.createTempFile(DIRECTORIO, "guardado-", ".tmp");
            try (BufferedWriter escritor = Files.newBufferedWriter(temporal, StandardCharsets.UTF_8)) {
                for (Persistible item : elementos) {
                    escritor.write(item.aLineaCSV());
                    escritor.newLine();
                }
            }
            try {
                Files.move(temporal, destino, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Error al guardar " + nombreArchivo, e);
        } finally {
            if (temporal != null) {
                try { Files.deleteIfExists(temporal); } catch (IOException ignored) { }
            }
        }
    }

    public static void cargar(String nombreArchivo, ListaSimple<String> lineas) {
        Path archivo = DIRECTORIO.resolve(nombreArchivo);
        if (!Files.exists(archivo)) return;
        try (BufferedReader lector = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                if (!linea.trim().isEmpty()) lineas.agregar(linea);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Error al leer " + nombreArchivo, e);
        }
    }
}

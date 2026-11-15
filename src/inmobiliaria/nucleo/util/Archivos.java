package inmobiliaria.nucleo.util;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

/**
 * Archivos adjuntos (documentos del expediente, comprobantes de pago).
 *
 * Al anexar, el archivo se COPIA a data/archivos/<subcarpeta>/<nombre>.<ext>, asi el sistema
 * no depende de que el original siga en su lugar. En los CSV solo se guarda la ruta relativa
 * (con '/'), que se interpreta desde la carpeta donde se ejecuta el programa, igual que data/.
 * Formatos permitidos: PDF, Word (doc, docx) e imagenes (jpg, jpeg, png).
 */
public class Archivos {
    public static final String[] EXTENSIONES = { "pdf", "doc", "docx", "jpg", "jpeg", "png" };
    private static final String CARPETA = "data/archivos/";

    public static String extension(String nombre) {
        int punto = nombre == null ? -1 : nombre.lastIndexOf('.');
        return punto < 0 ? "" : nombre.substring(punto + 1).toLowerCase(Locale.ROOT);
    }

    public static boolean extensionValida(String nombre) {
        String ext = extension(nombre);
        for (String e : EXTENSIONES) {
            if (e.equals(ext)) return true;
        }
        return false;
    }

    // Copia 'origen' a data/archivos/<subcarpeta>/<nombreBase>.<ext> (reemplaza si ya existia)
    // y devuelve la ruta relativa que se guarda en el CSV.
    public static String copiar(File origen, String subcarpeta, String nombreBase) {
        if (origen == null || !origen.isFile())
            throw new IllegalArgumentException("Seleccione un archivo valido");
        if (!extensionValida(origen.getName()))
            throw new IllegalArgumentException("Formato no permitido. Use PDF, Word (doc, docx) o imagen (jpg, png).");

        String sub = subcarpeta.replaceAll("[^A-Za-z0-9_.-]", "_");
        String nombre = nombreBase + "." + extension(origen.getName());
        Path destino = Paths.get(CARPETA + sub, nombre);
        try {
            Files.createDirectories(destino.getParent());
            boolean mismo = origen.toPath().toAbsolutePath().normalize()
                    .equals(destino.toAbsolutePath().normalize());
            if (!mismo) Files.copy(origen.toPath(), destino, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalArgumentException("No se pudo copiar el archivo: " + e.getMessage());
        }
        return CARPETA + sub + "/" + nombre;
    }

    // Solo el nombre (Contrato.pdf), que es lo que se muestra en las tablas
    public static String nombre(String ruta) {
        if (ruta == null || ruta.isEmpty()) return "";
        return new File(ruta).getName();
    }

    // Borra un adjunto que ya no se usa (por ejemplo, al reemplazar Contrato.pdf por Contrato.docx)
    public static void eliminar(String ruta) {
        if (ruta == null || ruta.isEmpty()) return;
        try {
            Files.deleteIfExists(Paths.get(ruta));
        } catch (IOException e) {
            // no es grave: queda un archivo suelto en data/archivos
        }
    }

    // Lo abre con el programa que el sistema tenga asociado (visor de PDF, Word, fotos)
    public static void abrir(String ruta) {
        if (ruta == null || ruta.isEmpty())
            throw new IllegalArgumentException("Todavia no hay un archivo anexado");
        File archivo = new File(ruta);
        if (!archivo.isFile())
            throw new IllegalArgumentException("No se encuentra el archivo " + ruta
                    + ". Vuelva a anexarlo.");
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN))
            throw new IllegalArgumentException("Este equipo no permite abrir archivos desde la aplicacion. "
                    + "Esta en: " + archivo.getAbsolutePath());
        try {
            Desktop.getDesktop().open(archivo.getAbsoluteFile());
        } catch (IOException e) {
            throw new IllegalArgumentException("No se pudo abrir el archivo: " + e.getMessage());
        }
    }
}

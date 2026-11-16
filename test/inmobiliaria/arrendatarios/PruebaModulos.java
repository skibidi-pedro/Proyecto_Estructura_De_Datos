package inmobiliaria.arrendatarios;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import inmobiliaria.arrendatarios.modelo.Arrendatario;
import inmobiliaria.arrendatarios.modelo.EstadoArrendatario;
import inmobiliaria.arrendatarios.servicio.ArrendatarioService;
import inmobiliaria.contratos.modelo.Contrato;
import inmobiliaria.contratos.modelo.EstadoContrato;
import inmobiliaria.propiedades.modelo.EstadoUnidad;
import inmobiliaria.contratos.servicio.ContratoService;
import inmobiliaria.documentos.modelo.EstadoDocumento;
import inmobiliaria.documentos.modelo.TipoDocumento;
import inmobiliaria.documentos.servicio.ExpedienteService;
import inmobiliaria.nucleo.historial.Historial;
import inmobiliaria.nucleo.util.Archivos;
import inmobiliaria.ui.Activacion;

/**
 * Pruebas de arrendatarios y documentos sin JUnit.
 * Incluye el documento anexado al contrato.
 *
 * Ejecutar desde una carpeta vacia, porque las pruebas de archivos y de persistencia
 * escriben en ./data/ :
 *   javac -d out $(find src test -name '*.java')
 *   java -cp out inmobiliaria.arrendatarios.PruebaModulos
 */
public class PruebaModulos {
    private static int fallos = 0;

    private static void verificar(String nombre, boolean ok) {
        System.out.println((ok ? "[OK]    " : "[FALLO] ") + nombre);
        if (!ok) fallos++;
    }

    private static boolean lanza(Runnable r) {
        try { r.run(); return false; } catch (IllegalArgumentException e) { return true; }
    }

    public static void main(String[] args) throws IOException {
        Historial historial = new Historial();
        ArrendatarioService arrendatarios = new ArrendatarioService(historial);
        ExpedienteService expedientes = new ExpedienteService(historial);
        ContratoService contratos = new ContratoService(historial);
        LocalDate hoy = LocalDate.of(2026, 3, 1);

        // ---- Arrendatarios (tabla hash, RN-01) ----
        arrendatarios.registrar("1001", "Ana Perez", "999", "ana@mail.com");
        arrendatarios.registrar("1002", "Beto Ruiz", "888", "");
        verificar("Se busca por documento", arrendatarios.buscar("1001").getNombresCompletos().equals("Ana Perez"));
        verificar("Documento inexistente da null", arrendatarios.buscar("9999") == null);
        verificar("RN-01: documento repetido se rechaza", lanza(() ->
                arrendatarios.registrar("1001", "Otra Persona", "", "")));
        verificar("Documento y nombres obligatorios", lanza(() ->
                arrendatarios.registrar("1003", " ", "", "")));
        verificar("Listado ordenado por documento", arrendatarios.listar().getTamano() == 2
                && arrendatarios.listar().obtener(0).getDocumentoIdentidad().equals("1001"));
        arrendatarios.actualizar("1002", "Beto R. Ruiz", "777", "beto@mail.com");
        verificar("Actualizar cambia los datos", arrendatarios.buscar("1002").getTelefono().equals("777")
                && arrendatarios.buscar("1002").getCorreo().equals("beto@mail.com"));
        arrendatarios.cambiarEstado("1002", EstadoArrendatario.MOROSO);
        verificar("Cambiar estado", arrendatarios.buscar("1002").getEstado() == EstadoArrendatario.MOROSO);
        arrendatarios.registrar("1003", "Carla Soto", "", "");
        arrendatarios.eliminar("1003");
        verificar("Eliminar quita de la tabla hash", arrendatarios.buscar("1003") == null
                && arrendatarios.listar().getTamano() == 2);
        verificar("Eliminar uno inexistente falla", lanza(() -> arrendatarios.eliminar("1003")));

        // ---- Documento de identidad anexado al arrendatario ----
        File fotoId = File.createTempFile("dni", ".JPG");
        File exe = File.createTempFile("virus", ".exe");
        verificar("Extensiones permitidas: pdf, doc, docx, jpg, png",
                Archivos.extensionValida("a.PDF") && Archivos.extensionValida("a.docx")
                && Archivos.extensionValida("a.jpeg") && !Archivos.extensionValida("a.exe"));
        verificar("Sin archivo el expediente esta incompleto", !expedientes.expedienteCompleto("1001")
                && expedientes.rutaIdentidad("1001").isEmpty());
        verificar("Un .exe no se puede anexar", lanza(() ->
                expedientes.anexarArchivo("1001", TipoDocumento.IDENTIDAD, exe, hoy)));
        expedientes.anexarArchivo("1001", TipoDocumento.IDENTIDAD, fotoId, hoy);
        String rutaId = expedientes.rutaIdentidad("1001");
        verificar("El archivo se copia como Identidad.jpg", Archivos.nombre(rutaId).equals("Identidad.jpg")
                && new File(rutaId).isFile());
        verificar("Con el documento de identidad el expediente esta completo", expedientes.expedienteCompleto("1001"));
        expedientes.cambiarEstado("1001", TipoDocumento.IDENTIDAD, EstadoDocumento.VENCIDO);
        verificar("Documento vencido deja incompleto el expediente", !expedientes.expedienteCompleto("1001"));
        File otraFoto = File.createTempFile("dni2", ".png");
        expedientes.anexarArchivo("1001", TipoDocumento.IDENTIDAD, otraFoto, hoy);
        verificar("Volver a anexar reemplaza (no duplica) y borra el archivo anterior",
                expedientes.documentosDe("1001").getTamano() == 1 && expedientes.expedienteCompleto("1001")
                && !new File(rutaId).exists() && new File("data/archivos/1001/Identidad.png").isFile());
        verificar("Cambiar estado de un documento inexistente falla", lanza(() ->
                expedientes.cambiarEstado("1002", TipoDocumento.IDENTIDAD, EstadoDocumento.VENCIDO)));
        verificar("Abrir un archivo que no existe avisa", lanza(() -> Archivos.abrir("data/archivos/no_existe.pdf")));

        // ---- Documento anexado al contrato ----
        Contrato c = contratos.crear("CON-001", "1001", "U1", hoy, hoy.plusMonths(6), 1000, 5, 1000,
                "", true, false);
        verificar("Un contrato nuevo no tiene documento", c.getDocumento().isEmpty());
        verificar("Falta el documento del contrato para activar",
                Activacion.faltantes(c, expedientes).contains("documento del contrato")
                && !Activacion.faltantes(c, expedientes).contains("identidad"));
        File pdf = File.createTempFile("firmado", ".pdf");
        verificar("Un .exe no se anexa al contrato", lanza(() -> contratos.anexarDocumento("CON-001", exe)));
        contratos.anexarDocumento("CON-001", pdf);
        verificar("El documento se copia como Contrato.pdf", Archivos.nombre(c.getDocumento()).equals("Contrato.pdf")
                && new File(c.getDocumento()).isFile());
        verificar("Con identidad y contrato no falta nada", Activacion.faltantes(c, expedientes).isEmpty());
        File docx = File.createTempFile("firmado2", ".docx");
        String rutaAnterior = c.getDocumento();
        contratos.anexarDocumento("CON-001", docx);
        verificar("Reemplazar con otro formato borra el anterior", Archivos.nombre(c.getDocumento()).equals("Contrato.docx")
                && !new File(rutaAnterior).exists());
        verificar("No se anexa a un contrato inexistente", lanza(() -> contratos.anexarDocumento("CON-999", pdf)));

        Contrato c2 = contratos.crear("CON-002", "1002", "U2", hoy, hoy.plusMonths(6), 900, 5, 0,
                "", true, false);
        verificar("Sin ningun documento faltan los dos",
                Activacion.faltantes(c2, expedientes).contains("identidad")
                && Activacion.faltantes(c2, expedientes).contains("contrato"));
        verificar("La activacion estricta (con bandera) sigue exigiendo documentos",
                lanza(() -> contratos.activar("CON-002", false)));
        contratos.activar("CON-001", Activacion.faltantes(c, expedientes).isEmpty());
        verificar("Con los dos documentos el contrato se activa", c.getEstado().toString().equals("VIGENTE"));

        // Se puede activar sin documentos: queda VIGENTE y la interfaz lo resalta en amarillo
        contratos.activar("CON-002");
        verificar("Un contrato sin documentos se activa igual (sale en amarillo)",
                c2.getEstado() == EstadoContrato.VIGENTE && !Activacion.faltantes(c2, expedientes).isEmpty());

        // ---- Estados de unidad ----
        verificar("Estados de unidad: NO ALQUILADA y ALQUILADA",
                EstadoUnidad.values().length == 2
                && EstadoUnidad.NO_ALQUILADA.toString().equals("NO ALQUILADA")
                && EstadoUnidad.ALQUILADA.toString().equals("ALQUILADA"));
        verificar("Un estado antiguo (DISPONIBLE, MANTENIMIENTO) se lee como NO ALQUILADA",
                EstadoUnidad.desdeTexto("DISPONIBLE") == EstadoUnidad.NO_ALQUILADA
                && EstadoUnidad.desdeTexto("EN_MANTENIMIENTO") == EstadoUnidad.NO_ALQUILADA
                && EstadoUnidad.desdeTexto("ALQUILADA") == EstadoUnidad.ALQUILADA);

        // ---- Persistencia (escribe en ./data/) ----
        arrendatarios.guardar();
        expedientes.guardar();
        contratos.guardar();

        ArrendatarioService a2 = new ArrendatarioService(new Historial());
        a2.cargar();
        verificar("CSV: se recargan los 2 arrendatarios", a2.listar().getTamano() == 2);
        Arrendatario ana = a2.buscar("1001");
        verificar("CSV: se conservan los datos", ana != null && ana.getCorreo().equals("ana@mail.com"));
        verificar("CSV: se conserva el estado", a2.buscar("1002").getEstado() == EstadoArrendatario.MOROSO);

        ExpedienteService e2 = new ExpedienteService(new Historial());
        e2.cargar();
        verificar("CSV: el expediente sigue completo", e2.expedienteCompleto("1001"));
        verificar("CSV: se conserva el nombre del documento de identidad",
                Archivos.nombre(e2.rutaIdentidad("1001")).equals("Identidad.png"));
        e2.eliminarExpediente("1001");
        verificar("Eliminar el expediente borra sus archivos", e2.rutaIdentidad("1001").isEmpty()
                && !new File("data/archivos/1001/Identidad.png").exists());

        ContratoService c3 = new ContratoService(new Historial());
        c3.cargar();
        verificar("CSV: se conserva el documento del contrato",
                Archivos.nombre(c3.buscar("CON-001").getDocumento()).equals("Contrato.docx"));
        verificar("CSV: un contrato sin documento carga vacio", c3.buscar("CON-002").getDocumento().isEmpty());

        System.out.println(fallos == 0 ? "\nTodas las pruebas pasaron." : "\nFallaron " + fallos + " pruebas.");
        if (fallos > 0) System.exit(1);
    }
}

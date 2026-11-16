package inmobiliaria.arrendatarios;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import inmobiliaria.arrendatarios.modelo.Arrendatario;
import inmobiliaria.arrendatarios.modelo.EstadoArrendatario;
import inmobiliaria.arrendatarios.servicio.ArrendatarioService;
import inmobiliaria.documentos.modelo.EstadoDocumento;
import inmobiliaria.documentos.modelo.TipoDocumento;
import inmobiliaria.documentos.servicio.ExpedienteService;
import inmobiliaria.mantenimiento.modelo.EstadoSolicitud;
import inmobiliaria.mantenimiento.modelo.SolicitudMantenimiento;
import inmobiliaria.mantenimiento.modelo.Urgencia;
import inmobiliaria.mantenimiento.servicio.MantenimientoService;
import inmobiliaria.nucleo.estructuras.ListaSimple;
import inmobiliaria.nucleo.historial.Historial;
import inmobiliaria.nucleo.util.Archivos;

/**
 * Pruebas del integrante 3 (arrendatarios, documentos, mantenimiento) sin JUnit.
 *
 * Ejecutar desde una carpeta vacia, porque la prueba de persistencia
 * escribe en ./data/ :
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
        MantenimientoService mantenimiento = new MantenimientoService(historial);
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

        // ---- Documentos (RN-06) ----
        verificar("Sin documentos el expediente esta incompleto", !expedientes.expedienteCompleto("1001"));
        expedientes.registrarDocumento("1001", TipoDocumento.IDENTIDAD, "dni_ana.pdf", hoy);
        verificar("Solo identidad: incompleto", !expedientes.expedienteCompleto("1001"));
        verificar("Faltantes indica el contrato firmado", expedientes.faltantes("1001").equals("CONTRATO_FIRMADO"));
        expedientes.registrarDocumento("1001", TipoDocumento.CONTRATO_FIRMADO, "contrato_ana.pdf", hoy);
        verificar("Identidad + contrato firmado: completo", expedientes.expedienteCompleto("1001"));
        expedientes.cambiarEstado("1001", TipoDocumento.IDENTIDAD, EstadoDocumento.VENCIDO);
        verificar("Documento vencido deja incompleto el expediente", !expedientes.expedienteCompleto("1001"));
        expedientes.registrarDocumento("1001", TipoDocumento.IDENTIDAD, "dni_nuevo.pdf", hoy);
        verificar("Volver a registrar reemplaza (no duplica)", expedientes.documentosDe("1001").getTamano() == 2
                && expedientes.expedienteCompleto("1001"));
        verificar("Cambiar estado de un documento inexistente falla", lanza(() ->
                expedientes.cambiarEstado("1002", TipoDocumento.IDENTIDAD, EstadoDocumento.VENCIDO)));

        // ---- Archivos anexados ----
        File fotoId = File.createTempFile("dni", ".JPG");
        File pdfContrato = File.createTempFile("firmado", ".pdf");
        File exe = File.createTempFile("virus", ".exe");
        verificar("Extensiones permitidas: pdf, doc, docx, jpg, png",
                Archivos.extensionValida("a.PDF") && Archivos.extensionValida("a.docx")
                && Archivos.extensionValida("a.jpeg") && !Archivos.extensionValida("a.exe"));
        verificar("Un .exe no se puede anexar", lanza(() ->
                expedientes.anexarArchivo("1002", TipoDocumento.IDENTIDAD, exe, hoy)));
        expedientes.anexarArchivo("1002", TipoDocumento.IDENTIDAD, fotoId, hoy);
        String rutaId = expedientes.buscarExpediente("1002").buscarPorTipo(TipoDocumento.IDENTIDAD).getRutaArchivo();
        verificar("El archivo se copia con el nombre del tipo (Identidad.jpg)", Archivos.nombre(rutaId).equals("Identidad.jpg")
                && new File(rutaId).isFile());
        verificar("Solo con identidad el expediente sigue incompleto", !expedientes.expedienteCompleto("1002"));
        expedientes.anexarArchivo("1002", TipoDocumento.CONTRATO_FIRMADO, pdfContrato, hoy);
        verificar("El contrato se guarda como Contrato.pdf", Archivos.nombre(expedientes.buscarExpediente("1002")
                .buscarPorTipo(TipoDocumento.CONTRATO_FIRMADO).getRutaArchivo()).equals("Contrato.pdf"));
        verificar("Con ambos archivos el expediente esta completo", expedientes.expedienteCompleto("1002"));
        File otraFoto = File.createTempFile("dni2", ".png");
        expedientes.anexarArchivo("1002", TipoDocumento.IDENTIDAD, otraFoto, hoy);
        verificar("Reemplazar con otro formato borra el archivo anterior", !new File(rutaId).exists()
                && new File("data/archivos/1002/Identidad.png").isFile());
        verificar("Abrir un archivo que no existe avisa", lanza(() -> Archivos.abrir("data/archivos/no_existe.pdf")));

        // ---- Mantenimiento (cola de prioridad) ----
        mantenimiento.registrar("MAN-001", "U1", "Gotera en el bano", Urgencia.BAJA, hoy);
        mantenimiento.registrar("MAN-002", "U2", "Fuga de gas", Urgencia.ALTA, hoy);
        mantenimiento.registrar("MAN-003", "U1", "Puerta rota", Urgencia.MEDIA, hoy);
        mantenimiento.registrar("MAN-004", "U1", "Corto circuito", Urgencia.ALTA, hoy);
        verificar("Registro valida la descripcion", lanza(() ->
                mantenimiento.registrar("MAN-005", "U1", " ", Urgencia.BAJA, hoy)));
        verificar("Codigo de solicitud no se repite", lanza(() ->
                mantenimiento.registrar("MAN-001", "U1", "Otra", Urgencia.BAJA, hoy)));
        verificar("4 pendientes en la cola", mantenimiento.cantidadPendientes() == 4);
        verificar("Primero la urgencia ALTA mas antigua", mantenimiento.atenderSiguiente().getCodigo().equals("MAN-002"));
        verificar("Luego la otra ALTA (orden de llegada)", mantenimiento.atenderSiguiente().getCodigo().equals("MAN-004"));

        ListaSimple<String> soloU2 = new ListaSimple<>();
        soloU2.agregar("U2");
        verificar("Atender filtrando por unidades sin solicitudes pendientes falla", lanza(() ->
                mantenimiento.atenderSiguiente(soloU2)));
        verificar("El intento fallido no pierde solicitudes de la cola", mantenimiento.cantidadPendientes() == 2);
        verificar("Sigue la MEDIA antes que la BAJA", mantenimiento.atenderSiguiente().getCodigo().equals("MAN-003"));

        mantenimiento.registrar("MAN-006", "U2", "Foco", Urgencia.BAJA, hoy);
        ListaSimple<String> unidadesU2 = new ListaSimple<>();
        unidadesU2.agregar("U2");
        verificar("Filtro por unidad salta las solicitudes de otras unidades",
                mantenimiento.atenderSiguiente(unidadesU2).getCodigo().equals("MAN-006"));
        verificar("La solicitud saltada sigue en la cola", mantenimiento.cantidadPendientes() == 1
                && mantenimiento.atenderSiguiente().getCodigo().equals("MAN-001"));
        verificar("Cola vacia no se puede atender", lanza(() -> mantenimiento.atenderSiguiente()));
        verificar("Terminar una solicitud en proceso", mantenimiento.terminar("MAN-002").getEstado() == EstadoSolicitud.ATENDIDA);
        verificar("No se termina una ya atendida", lanza(() -> mantenimiento.terminar("MAN-002")));

        // ---- Persistencia (escribe en ./data/) ----
        mantenimiento.registrar("MAN-007", "U3", "Ventana", Urgencia.MEDIA, hoy);
        mantenimiento.registrar("MAN-008", "U3", "Lavadero", Urgencia.ALTA, hoy);
        arrendatarios.guardar();
        expedientes.guardar();
        mantenimiento.guardar();

        ArrendatarioService a2 = new ArrendatarioService(new Historial());
        a2.cargar();
        verificar("CSV: se recargan los 2 arrendatarios", a2.listar().getTamano() == 2);
        Arrendatario ana = a2.buscar("1001");
        verificar("CSV: se conservan los datos", ana != null && ana.getCorreo().equals("ana@mail.com"));
        verificar("CSV: se conserva el estado", a2.buscar("1002").getEstado() == EstadoArrendatario.MOROSO);

        ExpedienteService e2 = new ExpedienteService(new Historial());
        e2.cargar();
        verificar("CSV: el expediente sigue completo", e2.expedienteCompleto("1001"));
        verificar("CSV: se recargan los 2 documentos", e2.documentosDe("1001").getTamano() == 2);
        verificar("CSV: se conserva la ruta del archivo anexado", Archivos.nombre(e2.buscarExpediente("1002")
                .buscarPorTipo(TipoDocumento.IDENTIDAD).getRutaArchivo()).equals("Identidad.png"));

        MantenimientoService m2 = new MantenimientoService(new Historial());
        m2.cargar();
        verificar("CSV: se recargan las 7 solicitudes", m2.listar().getTamano() == 7);
        verificar("CSV: solo las pendientes vuelven a la cola", m2.cantidadPendientes() == 2);
        SolicitudMantenimiento s = m2.atenderSiguiente();
        verificar("CSV: la cola recargada respeta urgencia", s.getCodigo().equals("MAN-008"));

        System.out.println(fallos == 0 ? "\nTodas las pruebas pasaron." : "\nFallaron " + fallos + " pruebas.");
        if (fallos > 0) System.exit(1);
    }
}

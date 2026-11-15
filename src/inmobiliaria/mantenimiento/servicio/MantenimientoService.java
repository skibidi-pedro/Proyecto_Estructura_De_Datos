package inmobiliaria.mantenimiento.servicio;

import java.time.LocalDate;
import inmobiliaria.mantenimiento.modelo.EstadoSolicitud;
import inmobiliaria.mantenimiento.modelo.SolicitudMantenimiento;
import inmobiliaria.mantenimiento.modelo.Urgencia;
import inmobiliaria.nucleo.estructuras.ColaPrioridad;
import inmobiliaria.nucleo.estructuras.ListaSimple;
import inmobiliaria.nucleo.historial.Historial;
import inmobiliaria.nucleo.persistencia.GestorCSV;
import inmobiliaria.nucleo.persistencia.Persistible;
import inmobiliaria.nucleo.util.Fechas;
import inmobiliaria.nucleo.util.Validador;

/**
 * Cola de prioridad: urgentes primero y luego por orden de llegada (2.6).
 *
 * Las solicitudes PENDIENTES esperan en una ColaPrioridad (prioridad = orden del enum Urgencia,
 * ALTA = 0). Aparte se guarda la lista completa para consultar el historial y para el CSV.
 * Flujo: PENDIENTE -> (atenderSiguiente) EN_PROCESO -> (terminar) ATENDIDA.
 *
 * Responsable: Integrante 3 - Quispe Rodriguez Carlos Manuel Ignacio
 */
public class MantenimientoService {
    private static final String ARCHIVO = "mantenimiento.csv";

    private ListaSimple<SolicitudMantenimiento> solicitudes = new ListaSimple<>();
    private ColaPrioridad<SolicitudMantenimiento> pendientes = new ColaPrioridad<>();
    private Historial historial;

    public MantenimientoService(Historial historial) {
        this.historial = historial;
    }

    // ---------- Registrar ----------
    public SolicitudMantenimiento registrar(String codigo, String codigoUnidad, String descripcion,
                                            Urgencia urgencia, LocalDate fecha) {
        if (!Validador.esTextoValido(codigo))
            throw new IllegalArgumentException("Falta el codigo de la solicitud");
        if (!Validador.esTextoValido(codigoUnidad))
            throw new IllegalArgumentException("Seleccione la unidad");
        if (!Validador.esTextoValido(descripcion))
            throw new IllegalArgumentException("La descripcion es obligatoria");
        if (urgencia == null)
            throw new IllegalArgumentException("Seleccione el nivel de urgencia");
        if (fecha == null)
            throw new IllegalArgumentException("Falta la fecha");
        if (buscar(codigo) != null)
            throw new IllegalArgumentException("Ya existe la solicitud " + codigo);

        SolicitudMantenimiento s = new SolicitudMantenimiento(codigo, codigoUnidad, descripcion,
                urgencia, Fechas.convertirAString(fecha));
        solicitudes.agregar(s);
        pendientes.encolar(s, urgencia.ordinal());
        historial.registrarOperacion("Solicitud de mantenimiento registrada: " + codigo);
        return s;
    }

    // ---------- Atender: saca de la cola la mas urgente (y la mas antigua entre iguales) ----------
    public SolicitudMantenimiento atenderSiguiente() {
        return atenderSiguiente(null);
    }

    // Igual, pero solo entre las unidades dadas (las de la propiedad abierta en pantalla).
    // Las solicitudes de otras unidades vuelven a la cola en su mismo orden de llegada.
    public SolicitudMantenimiento atenderSiguiente(ListaSimple<String> codigosUnidad) {
        SolicitudMantenimiento s = null;
        ListaSimple<SolicitudMantenimiento> aparte = new ListaSimple<>();
        // Se vacia toda la cola y se reinsertan los demas en el mismo orden: asi no se altera
        // el orden de llegada entre solicitudes de igual urgencia.
        while (!pendientes.estaVacia()) {
            SolicitudMantenimiento actual = pendientes.desencolar();
            if (s == null && (codigosUnidad == null || contiene(codigosUnidad, actual.getIdentificadorUnidad()))) s = actual;
            else aparte.agregar(actual);
        }
        for (int i = 0; i < aparte.getTamano(); i++) {
            SolicitudMantenimiento x = aparte.obtener(i);
            pendientes.encolar(x, x.getNivelUrgencia().ordinal());
        }
        if (s == null)
            throw new IllegalArgumentException("No hay solicitudes pendientes");
        s.setEstado(EstadoSolicitud.EN_PROCESO);
        historial.registrarOperacion("Solicitud de mantenimiento en proceso: " + s.getCodigo());
        return s;
    }

    private boolean contiene(ListaSimple<String> lista, String valor) {
        for (int i = 0; i < lista.getTamano(); i++) {
            if (lista.obtener(i).equals(valor)) return true;
        }
        return false;
    }

    // ---------- Terminar: EN_PROCESO -> ATENDIDA ----------
    public SolicitudMantenimiento terminar(String codigo) {
        SolicitudMantenimiento s = buscar(codigo);
        if (s == null)
            throw new IllegalArgumentException("No existe la solicitud " + codigo);
        if (s.getEstado() != EstadoSolicitud.EN_PROCESO)
            throw new IllegalArgumentException("Solo se puede terminar una solicitud en proceso");
        s.setEstado(EstadoSolicitud.ATENDIDA);
        historial.registrarOperacion("Solicitud de mantenimiento atendida: " + codigo);
        return s;
    }

    // ---------- Consultas ----------
    public SolicitudMantenimiento buscar(String codigo) {
        for (int i = 0; i < solicitudes.getTamano(); i++) {
            if (solicitudes.obtener(i).getCodigo().equals(codigo)) return solicitudes.obtener(i);
        }
        return null;
    }

    public ListaSimple<SolicitudMantenimiento> listar() {
        return solicitudes;
    }

    public int cantidadPendientes() {
        return pendientes.getTamano();
    }

    // ---------- Persistencia (data/mantenimiento.csv) ----------
    // Al cargar se vuelve a armar la cola con las PENDIENTES, en el orden del archivo (= de llegada).
    public void cargar() {
        ListaSimple<String> lineas = new ListaSimple<>();
        GestorCSV.cargar(ARCHIVO, lineas);
        for (int i = 0; i < lineas.getTamano(); i++) {
            SolicitudMantenimiento s = new SolicitudMantenimiento();
            s.desdeLineaCSV(lineas.obtener(i));
            solicitudes.agregar(s);
            if (s.getEstado() == EstadoSolicitud.PENDIENTE) pendientes.encolar(s, s.getNivelUrgencia().ordinal());
        }
    }

    public void guardar() {
        ListaSimple<Persistible> lista = new ListaSimple<>();
        for (int i = 0; i < solicitudes.getTamano(); i++) {
            lista.agregar(solicitudes.obtener(i));
        }
        GestorCSV.guardar(ARCHIVO, lista);
    }
}

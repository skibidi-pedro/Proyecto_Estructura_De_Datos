package inmobiliaria.arrendatarios.servicio;

import inmobiliaria.arrendatarios.estructuras.TablaHash;
import inmobiliaria.arrendatarios.modelo.Arrendatario;
import inmobiliaria.arrendatarios.modelo.EstadoArrendatario;
import inmobiliaria.nucleo.estructuras.ListaSimple;
import inmobiliaria.nucleo.historial.Historial;
import inmobiliaria.nucleo.persistencia.GestorCSV;
import inmobiliaria.nucleo.persistencia.Persistible;
import inmobiliaria.nucleo.util.Validador;

/**
 * CRUD de arrendatarios (el aval es un dato de texto); busqueda por documento de identidad (2.2).
 * Los arrendatarios viven en una tabla hash cuya clave es el documento (RN-01: no se repite).
 *
 * Responsable: Integrante 3 - Quispe Rodriguez Carlos Manuel Ignacio
 * Reglas: RN-01
 */
public class ArrendatarioService {
    private static final String ARCHIVO = "arrendatarios.csv";
    private static final int CAPACIDAD = 101;

    private TablaHash tabla = new TablaHash(CAPACIDAD);
    private Historial historial;

    public ArrendatarioService(Historial historial) {
        this.historial = historial;
    }

    // ---------- Registrar (RN-01) ----------
    public Arrendatario registrar(String documento, String nombres, String telefono,
                                  String correo, String aval) {
        if (!Validador.esTextoValido(documento) || !Validador.esTextoValido(nombres))
            throw new IllegalArgumentException("Documento y nombres son obligatorios");
        documento = documento.trim();
        if (documento.contains(";") || documento.contains(" "))
            throw new IllegalArgumentException("El documento no puede tener espacios ni ';'");
        if (tabla.buscar(documento) != null)                            // RN-01
            throw new IllegalArgumentException("Ya existe un arrendatario con documento " + documento);

        Arrendatario a = new Arrendatario(documento, nombres, telefono, correo, aval);
        tabla.insertar(a);
        historial.registrarOperacion("Arrendatario registrado: " + documento);
        return a;
    }

    // ---------- Modificar datos de contacto ----------
    public Arrendatario actualizar(String documento, String nombres, String telefono,
                                   String correo, String aval) {
        Arrendatario a = buscarObligatorio(documento);
        if (!Validador.esTextoValido(nombres))
            throw new IllegalArgumentException("Los nombres son obligatorios");
        a.setNombresCompletos(nombres);
        a.setTelefono(telefono);
        a.setCorreo(correo);
        a.setAval(aval);
        historial.registrarOperacion("Arrendatario actualizado: " + documento);
        return a;
    }

    // ---------- Estado (ACTIVO / MOROSO / INACTIVO) ----------
    public void cambiarEstado(String documento, EstadoArrendatario estado) {
        Arrendatario a = buscarObligatorio(documento);
        a.setEstado(estado);
        historial.registrarOperacion("Arrendatario " + documento + " pasa a " + estado);
    }

    // ---------- Baja ----------
    public void eliminar(String documento) {
        buscarObligatorio(documento);
        tabla.eliminar(documento);
        historial.registrarOperacion("Arrendatario eliminado: " + documento);
    }

    // ---------- Consultas ----------
    public Arrendatario buscar(String documento) {
        if (documento == null) return null;
        return tabla.buscar(documento.trim());
    }

    public ListaSimple<Arrendatario> listar() {
        return tabla.todos();
    }

    private Arrendatario buscarObligatorio(String documento) {
        Arrendatario a = buscar(documento);
        if (a == null) throw new IllegalArgumentException("No existe el arrendatario " + documento);
        return a;
    }

    // ---------- Persistencia (data/arrendatarios.csv) ----------
    public void cargar() {
        ListaSimple<String> lineas = new ListaSimple<>();
        GestorCSV.cargar(ARCHIVO, lineas);
        for (int i = 0; i < lineas.getTamano(); i++) {
            Arrendatario a = new Arrendatario();
            a.desdeLineaCSV(lineas.obtener(i));
            tabla.insertar(a);
        }
    }

    public void guardar() {
        ListaSimple<Arrendatario> todos = tabla.todos();
        ListaSimple<Persistible> lista = new ListaSimple<>();
        for (int i = 0; i < todos.getTamano(); i++) lista.agregar(todos.obtener(i));
        GestorCSV.guardar(ARCHIVO, lista);
    }
}

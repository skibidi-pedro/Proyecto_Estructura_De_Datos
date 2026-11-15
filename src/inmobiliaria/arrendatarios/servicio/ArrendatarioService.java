package inmobiliaria.arrendatarios.servicio;

import inmobiliaria.arrendatarios.estructuras.TablaHash;
import inmobiliaria.arrendatarios.modelo.Arrendatario;
import inmobiliaria.arrendatarios.modelo.Aval;
import inmobiliaria.arrendatarios.modelo.EstadoArrendatario;
import inmobiliaria.nucleo.estructuras.ListaSimple;
import inmobiliaria.nucleo.historial.Historial;
import inmobiliaria.nucleo.persistencia.GestorCSV;
import inmobiliaria.nucleo.persistencia.Persistible;
import inmobiliaria.nucleo.util.Validador;

/**
 * CRUD de arrendatarios y avales; busqueda por documento de identidad (2.2).
 * Los arrendatarios viven en una tabla hash cuya clave es el documento (RN-01: no se repite).
 *
 * Responsable: Integrante 3 - Quispe Rodriguez Carlos Manuel Ignacio
 * Reglas: RN-01
 */
public class ArrendatarioService {
    private static final String ARCHIVO = "arrendatarios.csv";
    private static final String ARCHIVO_AVALES = "avales.csv";
    private static final int CAPACIDAD = 101;

    private TablaHash tabla = new TablaHash(CAPACIDAD);
    private Historial historial;

    public ArrendatarioService(Historial historial) {
        this.historial = historial;
    }

    // ---------- Registrar (RN-01) ----------
    public Arrendatario registrar(String documento, String nombres, String telefono,
                                  String correo, String ocupacion, Aval aval) {
        if (!Validador.esTextoValido(documento) || !Validador.esTextoValido(nombres))
            throw new IllegalArgumentException("Documento y nombres son obligatorios");
        documento = documento.trim();
        if (documento.contains(";") || documento.contains(" "))
            throw new IllegalArgumentException("El documento no puede tener espacios ni ';'");
        if (tabla.buscar(documento) != null)                            // RN-01
            throw new IllegalArgumentException("Ya existe un arrendatario con documento " + documento);
        validarAval(aval, documento);

        Arrendatario a = new Arrendatario(documento, nombres, telefono, correo, ocupacion, aval);
        tabla.insertar(a);
        historial.registrarOperacion("Arrendatario registrado: " + documento);
        return a;
    }

    // ---------- Modificar datos de contacto ----------
    public Arrendatario actualizar(String documento, String nombres, String telefono,
                                   String correo, String ocupacion) {
        Arrendatario a = buscarObligatorio(documento);
        if (!Validador.esTextoValido(nombres))
            throw new IllegalArgumentException("Los nombres son obligatorios");
        a.setNombresCompletos(nombres);
        a.setTelefono(telefono);
        a.setCorreo(correo);
        a.setOcupacion(ocupacion);
        historial.registrarOperacion("Arrendatario actualizado: " + documento);
        return a;
    }

    // ---------- Aval ----------
    public void asignarAval(String documento, Aval aval) {
        Arrendatario a = buscarObligatorio(documento);
        validarAval(aval, documento);
        a.setAval(aval);
        historial.registrarOperacion("Aval asignado al arrendatario " + documento);
    }

    private void validarAval(Aval aval, String documentoArrendatario) {
        if (aval == null) return;   // el aval es opcional
        if (!Validador.esTextoValido(aval.getDocumentoIdentidad()) || !Validador.esTextoValido(aval.getNombresCompletos()))
            throw new IllegalArgumentException("El aval necesita documento y nombres");
        if (aval.getDocumentoIdentidad().trim().equals(documentoArrendatario))
            throw new IllegalArgumentException("El aval no puede ser el mismo arrendatario");
        if (!Validador.esMontoValido(aval.getIngresosComprobados()))
            throw new IllegalArgumentException("Los ingresos comprobados del aval deben ser mayores a cero");
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

    // ---------- Persistencia (data/arrendatarios.csv y data/avales.csv) ----------
    public void cargar() {
        ListaSimple<String> lineas = new ListaSimple<>();
        GestorCSV.cargar(ARCHIVO, lineas);
        for (int i = 0; i < lineas.getTamano(); i++) {
            Arrendatario a = new Arrendatario();
            a.desdeLineaCSV(lineas.obtener(i));
            tabla.insertar(a);
        }

        // avales.csv: documentoArrendatario;documentoAval;nombres;ingresos
        ListaSimple<String> avales = new ListaSimple<>();
        GestorCSV.cargar(ARCHIVO_AVALES, avales);
        for (int i = 0; i < avales.getTamano(); i++) {
            String[] p = avales.obtener(i).split(";", -1);
            Arrendatario a = tabla.buscar(p[0]);
            if (a != null) a.setAval(new Aval(p[1], p[2], Double.parseDouble(p[3])));
        }
    }

    public void guardar() {
        ListaSimple<Arrendatario> todos = tabla.todos();
        ListaSimple<Persistible> lista = new ListaSimple<>();
        ListaSimple<Persistible> avales = new ListaSimple<>();
        for (int i = 0; i < todos.getTamano(); i++) {
            Arrendatario a = todos.obtener(i);
            lista.agregar(a);
            if (a.getAval() != null) avales.agregar(lineaAval(a));
        }
        GestorCSV.guardar(ARCHIVO, lista);
        GestorCSV.guardar(ARCHIVO_AVALES, avales);
    }

    // Persistible solo para escribir la linea del aval junto al documento de su arrendatario
    private Persistible lineaAval(Arrendatario a) {
        Aval v = a.getAval();
        String linea = a.getDocumentoIdentidad() + ";" + v.getDocumentoIdentidad() + ";"
                + v.getNombresCompletos().replace(";", ",") + ";" + v.getIngresosComprobados();
        return new Persistible() {
            @Override public String aLineaCSV() { return linea; }
            @Override public void desdeLineaCSV(String l) { }
        };
    }
}

package inmobiliaria.nucleo.historial;
import inmobiliaria.nucleo.estructuras.ListaSimple;
import inmobiliaria.nucleo.estructuras.Pila;
import inmobiliaria.nucleo.persistencia.GestorCSV;
import inmobiliaria.nucleo.persistencia.Persistible;

/**
 * Bitacora de ultimas operaciones y deshacer la ultima (2.7).
 *
 * Responsable: Integrante 1 - Francia Ramos Johan Mitziu
 */

public class Historial {
    private Pila<Operacion> pilaOperaciones;

    public Historial() {
        this.pilaOperaciones = new Pila<>();
    }

    public void registrarOperacion(String descripcion) {
        Operacion op = new Operacion(descripcion);
        pilaOperaciones.apilar(op);
    }

    public Operacion deshacer() {
        if (pilaOperaciones.estaVacia()) {
            return null;
        }
        return pilaOperaciones.desapilar();
    }

    // Todas las operaciones, de la mas reciente a la mas antigua (la pila queda como estaba)
    public ListaSimple<Operacion> listar() {
        ListaSimple<Operacion> recientes = new ListaSimple<>();
        while (!pilaOperaciones.estaVacia()) recientes.agregar(pilaOperaciones.desapilar());
        for (int i = recientes.getTamano() - 1; i >= 0; i--) pilaOperaciones.apilar(recientes.obtener(i));
        return recientes;
    }

    // ---------- Persistencia (data/historial.csv), de la mas antigua a la mas reciente ----------
    public void guardar() {
        ListaSimple<Operacion> recientes = listar();
        ListaSimple<Persistible> lista = new ListaSimple<>();
        for (int i = recientes.getTamano() - 1; i >= 0; i--) lista.agregar(recientes.obtener(i));
        GestorCSV.guardar("historial.csv", lista);
    }

    public void cargar() {
        ListaSimple<String> lineas = new ListaSimple<>();
        GestorCSV.cargar("historial.csv", lineas);
        for (int i = 0; i < lineas.getTamano(); i++) {
            Operacion op = new Operacion();
            op.desdeLineaCSV(lineas.obtener(i));
            pilaOperaciones.apilar(op);
        }
    }

    public boolean estaVacio() {
        return pilaOperaciones.estaVacia();
    }

    public int getCantidadOperaciones() {
        return pilaOperaciones.getTamano();
    }
}

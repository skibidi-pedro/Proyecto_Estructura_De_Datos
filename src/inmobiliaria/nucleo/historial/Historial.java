package inmobiliaria.nucleo.historial;
import inmobiliaria.nucleo.estructuras.Pila;

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

    public boolean estaVacio() {
        return pilaOperaciones.estaVacia();
    }

    public int getCantidadOperaciones() {
        return pilaOperaciones.getTamano();
    }
}

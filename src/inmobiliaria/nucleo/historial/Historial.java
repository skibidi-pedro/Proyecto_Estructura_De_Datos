package inmobiliaria.nucleo.historial;
import inmobiliaria.nucleo.estructuras.Pila;

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

package inmobiliaria.nucleo.estructuras;

public class ColaPrioridad<T> {
    private static class NodoPrioridad<E> {
        E dato;
        int prioridad;
        NodoPrioridad<E> siguiente;

        NodoPrioridad(E dato, int prioridad) {
            this.dato = dato;
            this.prioridad = prioridad;
            this.siguiente = null;
        }
    }

    private NodoPrioridad<T> cabeza;
    private int tamano;

    public ColaPrioridad() {
        this.cabeza = null;
        this.tamano = 0;
    }

    public boolean estaVacia() {
        return cabeza == null;
    }

    public void encolar(T dato, int prioridad) {
        NodoPrioridad<T> nuevo = new NodoPrioridad<>(dato, prioridad);
        if (estaVacia() || prioridad < cabeza.prioridad) {
            nuevo.siguiente = cabeza;
            cabeza = nuevo;
        } else {
            NodoPrioridad<T> actual = cabeza;
            while (actual.siguiente != null && actual.siguiente.prioridad <= prioridad) {
                actual = actual.siguiente;
            }
            nuevo.siguiente = actual.siguiente;
            actual.siguiente = nuevo;
        }
        tamano++;
    }

    public T desencolar() {
        if (estaVacia()) {
            return null;
        }
        T dato = cabeza.dato;
        cabeza = cabeza.siguiente;
        tamano--;
        return dato;
    }

    public int getTamano() {
        return tamano;
    }
}

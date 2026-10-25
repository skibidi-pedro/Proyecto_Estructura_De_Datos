package inmobiliaria.nucleo.estructuras;

/**
 * Nodo de la lista enlazada simple.
 *
 * Responsable: Integrante 1 - Francia Ramos Johan Mitziu
 */
public class NodoSimple<T> {
    private T dato;
    private NodoSimple<T> siguiente;

    public NodoSimple(T dato) {
        this.dato = dato;
        this.siguiente = null;
    }

    public T getDato() {
        return dato;
    }

    public void setDato(T dato) {
        this.dato = dato;
    }

    public NodoSimple<T> getSiguiente() {
        return siguiente;
    }

    public void setSiguiente(NodoSimple<T> siguiente) {
        this.siguiente = siguiente;
    }
}
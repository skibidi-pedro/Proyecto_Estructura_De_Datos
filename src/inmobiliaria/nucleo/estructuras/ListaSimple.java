package inmobiliaria.nucleo.estructuras;

/**
 * Lista enlazada simple generica.
 *
 * Responsable: Integrante 1 - Francia Ramos Johan Mitziu
 */
public class ListaSimple<T> implements Iterable<T> {
    private NodoSimple<T> cabeza;
    private NodoSimple<T> cola;
    private int tamano;

    public ListaSimple() {
        this.cabeza = null;
        this.tamano = 0;
    }

    public boolean estaVacia() {
        return cabeza == null;
    }

    public void agregar(T dato) {
        NodoSimple<T> nuevo = new NodoSimple<>(dato);
        if (estaVacia()) {
            cabeza = nuevo;
            cola = nuevo;
        } else {
            cola.setSiguiente(nuevo);
            cola = nuevo;
        }
        tamano++;
    }

    public T obtener(int indice) {
        if (indice < 0 || indice >= tamano) {
            throw new IndexOutOfBoundsException("Indice fuera de rango");
        }
        NodoSimple<T> actual = cabeza;
        for (int i = 0; i < indice; i++) {
            actual = actual.getSiguiente();
        }
        return actual.getDato();
    }

    @Override
    public java.util.Iterator<T> iterator() {
        return new java.util.Iterator<T>() {
            private NodoSimple<T> actual = cabeza;
            public boolean hasNext() { return actual != null; }
            public T next() {
                if (actual == null) throw new java.util.NoSuchElementException();
                T dato = actual.getDato();
                actual = actual.getSiguiente();
                return dato;
            }
        };
    }

    public int getTamano() {
        return tamano;
    }
}

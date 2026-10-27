package inmobiliaria.nucleo.estructuras;

public class ListaSimple<T> {
    private NodoSimple<T> cabeza;
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
        } else {
            NodoSimple<T> actual = cabeza;
            while (actual.getSiguiente() != null) {
                actual = actual.getSiguiente();
            }
            actual.setSiguiente(nuevo);
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

    public int getTamano() {
        return tamano;
    }
}

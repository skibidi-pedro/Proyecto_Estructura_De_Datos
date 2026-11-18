package inmobiliaria.nucleo.estructuras;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Lista enlazada simple generica. Agregar al final es O(1).
 * Implementa Iterable para recorridos lineales sin obtener(i).
 *
 * Responsable: Integrante 1
 */
public class ListaSimple<T> implements Iterable<T> {
    private NodoSimple<T> cabeza;
    private NodoSimple<T> cola;
    private int tamano;

    public ListaSimple() {
        cabeza = null;
        cola = null;
        tamano = 0;
    }

    public boolean estaVacia() { return cabeza == null; }

    public void agregar(T dato) {
        NodoSimple<T> nuevo = new NodoSimple<>(dato);
        if (estaVacia()) cabeza = nuevo;
        else cola.setSiguiente(nuevo);
        cola = nuevo;
        tamano++;
    }

    public T obtener(int indice) {
        if (indice < 0 || indice >= tamano)
            throw new IndexOutOfBoundsException("Indice fuera de rango");
        NodoSimple<T> actual = cabeza;
        for (int i = 0; i < indice; i++) actual = actual.getSiguiente();
        return actual.getDato();
    }

    public int getTamano() { return tamano; }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private NodoSimple<T> actual = cabeza;

            @Override
            public boolean hasNext() { return actual != null; }

            @Override
            public T next() {
                if (actual == null) throw new NoSuchElementException();
                T dato = actual.getDato();
                actual = actual.getSiguiente();
                return dato;
            }
        };
    }
}

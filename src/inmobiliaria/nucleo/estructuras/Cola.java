package inmobiliaria.nucleo.estructuras;

public class Cola<T> {
    private NodoSimple<T> frente;
    private NodoSimple<T> fin;
    private int tamano;

    public Cola() {
        this.frente = null;
        this.fin = null;
        this.tamano = 0;
    }

    public boolean estaVacia() {
        return frente == null;
    }

    public void encolar(T dato) {
        NodoSimple<T> nuevo = new NodoSimple<>(dato);
        if (estaVacia()) {
            frente = nuevo;
            fin = nuevo;
        } else {
            fin.setSiguiente(nuevo);
            fin = nuevo;
        }
        tamano++;
    }

    public T desencolar() {
        if (estaVacia()) {
            return null;
        }
        T dato = frente.getDato();
        frente = frente.getSiguiente();
        if (frente == null) {
            fin = null;
        }
        tamano--;
        return dato;
    }

    public T verFrente() {
        if (estaVacia()) {
            return null;
        }
        return frente.getDato();
    }

    public int getTamano() {
        return tamano;
    }
}

package inmobiliaria.nucleo.estructuras;

/**
 * Pila generica (base del historial / deshacer).
 *
 * Responsable: Integrante 1 - Francia Ramos Johan Mitziu
 */
public class Pila<T> {
    private NodoSimple<T> cima;
    private int tamano;

    public Pila() {
        this.cima = null;
        this.tamano = 0;
    }

    public boolean estaVacia() {
        return cima == null;
    }

    public void apilar(T dato) {
        NodoSimple<T> nuevo = new NodoSimple<>(dato);
        nuevo.setSiguiente(cima);
        cima = nuevo;
        tamano++;
    }

    public T desapilar() {
        if (estaVacia()) {
            return null;
        }
        T dato = cima.getDato();
        cima = cima.getSiguiente();
        tamano--;
        return dato;
    }

    public T verCima() {
        if (estaVacia()) {
            return null;
        }
        return cima.getDato();
    }

    public int getTamano() {
        return tamano;
    }
}

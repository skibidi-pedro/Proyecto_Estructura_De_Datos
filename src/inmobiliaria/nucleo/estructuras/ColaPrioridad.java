package inmobiliaria.nucleo.estructuras;

/**
 * Heap binario minimo. Encolar y desencolar: O(log n).
 * En prioridades iguales mantiene orden de llegada.
 */
public class ColaPrioridad<T> {
    private static class Elemento<E> {
        E dato;
        int prioridad;
        long orden;
        Elemento(E dato, int prioridad, long orden) {
            this.dato = dato;
            this.prioridad = prioridad;
            this.orden = orden;
        }
    }
    private Object[] elementos = new Object[16];
    private int tamano;
    private long secuencia;

    public boolean estaVacia() { return tamano == 0; }
    public int getTamano() { return tamano; }

    @SuppressWarnings("unchecked")
    private Elemento<T> en(int i) { return (Elemento<T>) elementos[i]; }

    private int comparar(Elemento<T> a, Elemento<T> b) {
        int resultado = Integer.compare(a.prioridad, b.prioridad);
        return resultado != 0 ? resultado : Long.compare(a.orden, b.orden);
    }

    public void encolar(T dato, int prioridad) {
        if (tamano == elementos.length) {
            Object[] copia = new Object[elementos.length * 2];
            System.arraycopy(elementos, 0, copia, 0, tamano);
            elementos = copia;
        }
        Elemento<T> nuevo = new Elemento<>(dato, prioridad, secuencia++);
        int i = tamano++;
        while (i > 0) {
            int padre = (i - 1) / 2;
            if (comparar(nuevo, en(padre)) >= 0) break;
            elementos[i] = elementos[padre];
            i = padre;
        }
        elementos[i] = nuevo;
    }

    public T desencolar() {
        if (tamano == 0) return null;
        T dato = en(0).dato;
        Elemento<T> ultimo = en(--tamano);
        elementos[tamano] = null;
        if (tamano == 0) return dato;
        int i = 0;
        while (2 * i + 1 < tamano) {
            int hijo = 2 * i + 1;
            if (hijo + 1 < tamano && comparar(en(hijo + 1), en(hijo)) < 0) hijo++;
            if (comparar(ultimo, en(hijo)) <= 0) break;
            elementos[i] = elementos[hijo];
            i = hijo;
        }
        elementos[i] = ultimo;
        return dato;
    }
}

package inmobiliaria.arrendatarios.estructuras;

import inmobiliaria.arrendatarios.modelo.Arrendatario;
import inmobiliaria.nucleo.estructuras.ListaSimple;

/**
 * Tabla hash con encadenamiento; la clave es el documento de identidad.
 *
 * Responsable: Integrante 1 - Francia Ramos Johan Mitziu
 */
public class TablaHash {
    // Clase interna para el encadenamiento
    private class Nodo {
        Arrendatario arrendatario;
        Nodo siguiente;

        public Nodo(Arrendatario arrendatario) {
            this.arrendatario = arrendatario;
            this.siguiente = null;
        }
    }

    private Nodo[] tabla;
    private int capacidad;
    private int tamano;

    public TablaHash(int capacidad) {
        this.capacidad = capacidad;
        this.tabla = new Nodo[capacidad];
        this.tamano = 0;
    }

    private int funcionHash(String documentoIdentidad) {
        return (documentoIdentidad.hashCode() & 0x7fffffff) % capacidad;
    }

    public void insertar(Arrendatario arrendatario) {
        int indice = funcionHash(arrendatario.getDocumentoIdentidad());
        Nodo nuevoNodo = new Nodo(arrendatario);

        if (tabla[indice] == null) {
            tabla[indice] = nuevoNodo;
        } else {
            Nodo actual = tabla[indice];
            while (actual.siguiente != null) {
                actual = actual.siguiente;
            }
            actual.siguiente = nuevoNodo;
        }
        tamano++;
    }

    public Arrendatario buscar(String documentoIdentidad) {
        int indice = funcionHash(documentoIdentidad);
        Nodo actual = tabla[indice];

        while (actual != null) {
            if (actual.arrendatario.getDocumentoIdentidad().equals(documentoIdentidad)) {
                return actual.arrendatario;
            }
            actual = actual.siguiente;
        }
        return null; // No encontrado
    }

    // Quita al arrendatario de su cadena. Devuelve true si estaba.
    public boolean eliminar(String documentoIdentidad) {
        int indice = funcionHash(documentoIdentidad);
        Nodo anterior = null;
        Nodo actual = tabla[indice];
        while (actual != null) {
            if (actual.arrendatario.getDocumentoIdentidad().equals(documentoIdentidad)) {
                if (anterior == null) tabla[indice] = actual.siguiente;
                else anterior.siguiente = actual.siguiente;
                tamano--;
                return true;
            }
            anterior = actual;
            actual = actual.siguiente;
        }
        return false;
    }

    // Todos los arrendatarios, ordenados por documento para que el listado sea estable
    public ListaSimple<Arrendatario> todos() {
        Arrendatario[] arreglo = new Arrendatario[tamano];
        int n = 0;
        for (int i = 0; i < capacidad; i++) {
            for (Nodo actual = tabla[i]; actual != null; actual = actual.siguiente) {
                arreglo[n++] = actual.arrendatario;
            }
        }
        // insercion simple: la cantidad de arrendatarios es pequena
        for (int i = 1; i < n; i++) {
            Arrendatario x = arreglo[i];
            int j = i - 1;
            while (j >= 0 && arreglo[j].getDocumentoIdentidad().compareTo(x.getDocumentoIdentidad()) > 0) {
                arreglo[j + 1] = arreglo[j];
                j--;
            }
            arreglo[j + 1] = x;
        }
        ListaSimple<Arrendatario> lista = new ListaSimple<>();
        for (int i = 0; i < n; i++) lista.agregar(arreglo[i]);
        return lista;
    }

    public int getTamano() {
        return tamano;
    }
}

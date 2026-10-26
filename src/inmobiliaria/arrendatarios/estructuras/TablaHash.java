package inmobiliaria.arrendatarios.estructuras;

import inmobiliaria.arrendatarios.modelo.Arrendatario;

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

    public TablaHash(int capacidad) {
        this.capacidad = capacidad;
        this.tabla = new Nodo[capacidad];
    }

    private int funcionHash(String documentoIdentidad) {
        return Math.abs(documentoIdentidad.hashCode()) % capacidad;
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
}
package inmobiliaria.propiedades.estructuras;

import inmobiliaria.nucleo.estructuras.ListaSimple;

/**
 * Arbol AVL generico (busquedas por codigo, precio, etc.).
 * K es la clave (ej. el codigo) y V lo que se guarda.
 *
 * Responsable: Integrante 2 - Zavaleta Calderon Steven Jack
 */
public class ArbolAVL<K extends Comparable<K>, V> {

    private static class Nodo<K, V> {
        K clave;
        V valor;
        Nodo<K, V> izq, der;
        int altura = 1;

        Nodo(K clave, V valor) {
            this.clave = clave;
            this.valor = valor;
        }
    }

    private Nodo<K, V> raiz;
    private int tamano;

    public int getTamano() {
        return tamano;
    }

    public boolean estaVacio() {
        return raiz == null;
    }

    // ---------- Buscar ----------
    public V buscar(K clave) {
        Nodo<K, V> actual = raiz;
        while (actual != null) {
            int cmp = clave.compareTo(actual.clave);
            if (cmp == 0) return actual.valor;
            actual = (cmp < 0) ? actual.izq : actual.der;
        }
        return null;
    }

    // ---------- Insertar (false si la clave ya existe) ----------
    public boolean insertar(K clave, V valor) {
        if (buscar(clave) != null) return false;
        raiz = insertar(raiz, clave, valor);
        tamano++;
        return true;
    }

    private Nodo<K, V> insertar(Nodo<K, V> n, K clave, V valor) {
        if (n == null) return new Nodo<>(clave, valor);
        if (clave.compareTo(n.clave) < 0) n.izq = insertar(n.izq, clave, valor);
        else n.der = insertar(n.der, clave, valor);
        return balancear(n);
    }

    // ---------- Eliminar (false si no existe) ----------
    public boolean eliminar(K clave) {
        if (buscar(clave) == null) return false;
        raiz = eliminar(raiz, clave);
        tamano--;
        return true;
    }

    private Nodo<K, V> eliminar(Nodo<K, V> n, K clave) {
        int cmp = clave.compareTo(n.clave);
        if (cmp < 0) {
            n.izq = eliminar(n.izq, clave);
        } else if (cmp > 0) {
            n.der = eliminar(n.der, clave);
        } else {
            if (n.izq == null) return n.der;
            if (n.der == null) return n.izq;
            // dos hijos: lo reemplazo por el menor de la derecha
            Nodo<K, V> menor = n.der;
            while (menor.izq != null) menor = menor.izq;
            n.clave = menor.clave;
            n.valor = menor.valor;
            n.der = eliminar(n.der, menor.clave);
        }
        return balancear(n);
    }

    // ---------- Recorrido en orden (ordenado por clave) ----------
    public ListaSimple<V> enOrden() {
        ListaSimple<V> lista = new ListaSimple<>();
        enOrden(raiz, lista);
        return lista;
    }

    private void enOrden(Nodo<K, V> n, ListaSimple<V> lista) {
        if (n == null) return;
        enOrden(n.izq, lista);
        lista.agregar(n.valor);
        enOrden(n.der, lista);
    }

    // ---------- Balanceo ----------
    private int altura(Nodo<K, V> n) {
        return n == null ? 0 : n.altura;
    }

    private int balance(Nodo<K, V> n) {
        return altura(n.izq) - altura(n.der);
    }

    private void actualizarAltura(Nodo<K, V> n) {
        n.altura = 1 + Math.max(altura(n.izq), altura(n.der));
    }

    private Nodo<K, V> rotarDerecha(Nodo<K, V> y) {
        Nodo<K, V> x = y.izq;
        y.izq = x.der;
        x.der = y;
        actualizarAltura(y);
        actualizarAltura(x);
        return x;
    }

    private Nodo<K, V> rotarIzquierda(Nodo<K, V> x) {
        Nodo<K, V> y = x.der;
        x.der = y.izq;
        y.izq = x;
        actualizarAltura(x);
        actualizarAltura(y);
        return y;
    }

    private Nodo<K, V> balancear(Nodo<K, V> n) {
        actualizarAltura(n);
        int b = balance(n);
        if (b > 1) {                                   // pesado a la izquierda
            if (balance(n.izq) < 0) n.izq = rotarIzquierda(n.izq);
            return rotarDerecha(n);
        }
        if (b < -1) {                                  // pesado a la derecha
            if (balance(n.der) > 0) n.der = rotarDerecha(n.der);
            return rotarIzquierda(n);
        }
        return n;
    }
}
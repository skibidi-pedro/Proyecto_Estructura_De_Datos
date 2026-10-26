package inmobiliaria.propiedades.estructuras;

import inmobiliaria.nucleo.estructuras.ListaSimple;

/**
 * Arbol general Propietario > Edificio > Unidad.
 * La raiz no guarda ningun dato; sus hijos son los propietarios.
 * Los datos se buscan por referencia (el mismo objeto), no por contenido.
 *
 * Responsable: Integrante 2 - Zavaleta Calderon Steven Jack
 */
public class ArbolGeneral<T> {

    private static class Nodo<T> {
        T dato;
        ListaSimple<Nodo<T>> hijos = new ListaSimple<>();

        Nodo(T dato) {
            this.dato = dato;
        }

        // ListaSimple no tiene quitar, asi que armo la lista de nuevo sin ese hijo
        void quitarHijo(Nodo<T> hijo) {
            ListaSimple<Nodo<T>> nueva = new ListaSimple<>();
            for (int i = 0; i < hijos.getTamano(); i++) {
                Nodo<T> h = hijos.obtener(i);
                if (h != hijo) nueva.agregar(h);
            }
            hijos = nueva;
        }
    }

    private Nodo<T> raiz = new Nodo<>(null);

    // Agrega 'dato' como hijo de 'padre'. Si padre es null, cuelga de la raiz.
    public boolean agregar(T padre, T dato) {
        Nodo<T> nodoPadre = (padre == null) ? raiz : buscarNodo(raiz, padre);
        if (nodoPadre == null) return false;
        nodoPadre.hijos.agregar(new Nodo<>(dato));
        return true;
    }

    // Quita un dato, solo si no tiene hijos
    public boolean quitar(T dato) {
        Nodo<T> padre = buscarPadre(raiz, dato);
        if (padre == null) return false;
        for (int i = 0; i < padre.hijos.getTamano(); i++) {
            Nodo<T> hijo = padre.hijos.obtener(i);
            if (hijo.dato == dato) {
                if (hijo.hijos.getTamano() > 0) return false;
                padre.quitarHijo(hijo);
                return true;
            }
        }
        return false;
    }

    // Hijos directos de 'padre' (si padre es null, los de la raiz)
    public ListaSimple<T> hijosDe(T padre) {
        ListaSimple<T> resultado = new ListaSimple<>();
        Nodo<T> nodo = (padre == null) ? raiz : buscarNodo(raiz, padre);
        if (nodo == null) return resultado;
        for (int i = 0; i < nodo.hijos.getTamano(); i++) {
            resultado.agregar(nodo.hijos.obtener(i).dato);
        }
        return resultado;
    }

    public boolean contiene(T dato) {
        return buscarNodo(raiz, dato) != null;
    }

    private Nodo<T> buscarNodo(Nodo<T> actual, T dato) {
        if (actual.dato == dato && dato != null) return actual;
        for (int i = 0; i < actual.hijos.getTamano(); i++) {
            Nodo<T> r = buscarNodo(actual.hijos.obtener(i), dato);
            if (r != null) return r;
        }
        return null;
    }

    private Nodo<T> buscarPadre(Nodo<T> actual, T dato) {
        for (int i = 0; i < actual.hijos.getTamano(); i++) {
            Nodo<T> hijo = actual.hijos.obtener(i);
            if (hijo.dato == dato) return actual;
            Nodo<T> r = buscarPadre(hijo, dato);
            if (r != null) return r;
        }
        return null;
    }
}
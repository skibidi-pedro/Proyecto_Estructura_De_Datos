package inmobiliaria.nucleo.util;

import java.util.HashMap;
import java.util.Map;
import inmobiliaria.nucleo.estructuras.ListaSimple;

/**
 * Asigna codigos en orden (PRO-001, PRO-002, ...) para que el usuario no los escriba ni los vea.
 * Los codigos viven en los objetos en memoria y de ahi salen al escribir los CSV.
 *
 * siguiente() solo propone el codigo; marcarUsado() lo confirma cuando el registro salio bien,
 * asi un alta que falla no deja huecos en la numeracion.
 */
public class GeneradorCodigo {
    private static final Map<String, Integer> ultimos = new HashMap<>();

    public static String siguiente(String prefijo, ListaSimple<String> existentes) {
        int max = ultimos.getOrDefault(prefijo, 0);
        String inicio = prefijo + "-";
        for (int i = 0; i < existentes.getTamano(); i++) {
            String c = existentes.obtener(i);
            if (c == null || !c.startsWith(inicio)) continue;
            try {
                max = Math.max(max, Integer.parseInt(c.substring(inicio.length())));
            } catch (NumberFormatException e) {
                // codigo con otro formato (de un CSV viejo): no cuenta para la numeracion
            }
        }
        return String.format("%s-%03d", prefijo, max + 1);
    }

    public static void marcarUsado(String codigo) {
        int guion = codigo.indexOf('-');
        String prefijo = codigo.substring(0, guion);
        int numero = Integer.parseInt(codigo.substring(guion + 1));
        ultimos.merge(prefijo, numero, Math::max);
    }
}

package inmobiliaria.nucleo.util;

import java.util.HashMap;
import java.util.Map;
import inmobiliaria.nucleo.estructuras.ListaSimple;

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

package inmobiliaria.nucleo.util;

/**
 * Validaciones generales y unicidad de identificadores.
 *
 * Responsable: Integrante 1 - Francia Ramos Johan Mitziu
 * Reglas: RN-01
 */
import inmobiliaria.nucleo.estructuras.ListaSimple;

public class Validador {

    public static boolean esTextoValido(String texto) {
        return texto != null && !texto.trim().isEmpty();
    }

    public static boolean esMontoValido(double monto) {
        return monto > 0;
    }

    public static boolean esDiaPagoValido(int dia) {
        return dia >= 1 && dia <= 28;
    }

    public static boolean idUnico(ListaSimple<String> codigosExistentes, String nuevoCodigo) {
        if (nuevoCodigo == null || nuevoCodigo.trim().isEmpty()) {
            return false;
        }
        for (int i = 0; i < codigosExistentes.getTamano(); i++) {
            if (codigosExistentes.obtener(i).equalsIgnoreCase(nuevoCodigo.trim())) {
                return false;
            }
        }
        return true;
    }
}

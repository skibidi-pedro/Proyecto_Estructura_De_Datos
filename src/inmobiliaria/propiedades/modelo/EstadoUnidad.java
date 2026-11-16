package inmobiliaria.propiedades.modelo;

/**
 * NO_ALQUILADA o ALQUILADA. Se muestran como "NO ALQUILADA" y "ALQUILADA".
 * ALQUILADA la pone el modulo de contratos al activar uno y vuelve a NO_ALQUILADA al finalizarlo.
 *
 * En el CSV se guarda el nombre del enum (name). Los archivos anteriores usaban DISPONIBLE y
 * EN_MANTENIMIENTO: al leerlos ambos pasan a NO_ALQUILADA (ver desdeTexto).
 *
 * Responsable: Integrante 2 - Zavaleta Calderon Steven Jack
 * Reglas: RN-02
 */
public enum EstadoUnidad {
    NO_ALQUILADA("NO ALQUILADA"),
    ALQUILADA("ALQUILADA");

    private final String etiqueta;

    EstadoUnidad(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    // Lee el valor guardado en el CSV, incluidos los de versiones anteriores
    public static EstadoUnidad desdeTexto(String texto) {
        if (texto.equals("ALQUILADA")) return ALQUILADA;
        return NO_ALQUILADA;   // NO_ALQUILADA, DISPONIBLE, EN_MANTENIMIENTO
    }

    @Override
    public String toString() {
        return etiqueta;   // lo que ve el usuario en tablas y listas
    }
}

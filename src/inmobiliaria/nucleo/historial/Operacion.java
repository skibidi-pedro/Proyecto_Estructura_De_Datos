package inmobiliaria.nucleo.historial;
import java.time.LocalDateTime;
/**
 * Operacion registrada y reversible (pago, alta, cambio de estado).
 *
 * Responsable: Integrante 1 - Francia Ramos Johan Mitziu
 */
public class Operacion {
    private String descripcion;
    private LocalDateTime fechaHora;

    public Operacion(String descripcion) {
        this.descripcion = descripcion;
        this.fechaHora = LocalDateTime.now();
    }

    public String getDescripcion() {
        return descripcion;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    @Override
    public String toString() {
        return "[" + fechaHora + "] " + descripcion;
    }
}

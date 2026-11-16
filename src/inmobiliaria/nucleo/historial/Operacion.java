package inmobiliaria.nucleo.historial;
import java.time.LocalDateTime;
import inmobiliaria.nucleo.persistencia.Persistible;
/**
 * Operacion registrada y reversible (pago, alta, cambio de estado).
 * Se guarda en data/historial.csv para verla en la proxima sesion.
 *
 * Responsable: Integrante 1 - Francia Ramos Johan Mitziu
 */
public class Operacion implements Persistible {
    private String descripcion;
    private LocalDateTime fechaHora;

    public Operacion() {
    }

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
    public String aLineaCSV() {
        return fechaHora + ";" + descripcion.replace(";", ",");
    }

    @Override
    public void desdeLineaCSV(String linea) {
        String[] p = linea.split(";", 2);
        this.fechaHora = LocalDateTime.parse(p[0]);
        this.descripcion = p.length > 1 ? p[1] : "";
    }

    @Override
    public String toString() {
        return "[" + fechaHora + "] " + descripcion;
    }
}

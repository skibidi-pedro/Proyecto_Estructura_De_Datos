package inmobiliaria.reportes;

import java.time.LocalDate;
import java.util.Locale;
import inmobiliaria.cobranza.modelo.Pago;
import inmobiliaria.cobranza.servicio.MoraService;
import inmobiliaria.cobranza.servicio.PagoService;
import inmobiliaria.contratos.modelo.Contrato;
import inmobiliaria.contratos.servicio.ContratoService;
import inmobiliaria.nucleo.estructuras.ListaSimple;
import inmobiliaria.nucleo.util.Fechas;
import inmobiliaria.propiedades.modelo.EstadoUnidad;
import inmobiliaria.propiedades.modelo.Unidad;
import inmobiliaria.propiedades.servicio.PropiedadService;

public class ReporteService {
    private ContratoService contratos;
    private PagoService pagos;
    private MoraService mora;
    private PropiedadService propiedades;

    public ReporteService(ContratoService contratos, PagoService pagos,
                          MoraService mora, PropiedadService propiedades) {
        this.contratos = contratos;
        this.pagos = pagos;
        this.mora = mora;
        this.propiedades = propiedades;
    }

    public double totalIngresos(LocalDate desde, LocalDate hasta) {
        double total = 0;
        ListaSimple<Pago> lista = pagos.listarPagos();
        for (int i = 0; i < lista.getTamano(); i++) {
            Pago p = lista.obtener(i);
            if (!p.getFecha().isBefore(desde) && !p.getFecha().isAfter(hasta)) total += p.getMonto();
        }
        return Math.round(total * 100.0) / 100.0;
    }

    public String reporteIngresos(LocalDate desde, LocalDate hasta) {
        StringBuilder sb = new StringBuilder();
        sb.append("REPORTE DE INGRESOS del ").append(Fechas.convertirAString(desde))
          .append(" al ").append(Fechas.convertirAString(hasta)).append("\n");

        int cantidad = 0;
        ListaSimple<Contrato> lista = contratos.listar();
        for (int i = 0; i < lista.getTamano(); i++) {
            String numero = lista.obtener(i).getNumero();
            double subtotal = 0;
            int n = 0;
            ListaSimple<Pago> delContrato = pagos.pagosDe(numero);
            for (int j = 0; j < delContrato.getTamano(); j++) {
                Pago p = delContrato.obtener(j);
                if (!p.getFecha().isBefore(desde) && !p.getFecha().isAfter(hasta)) {
                    subtotal += p.getMonto();
                    n++;
                }
            }
            if (n > 0) {
                sb.append(String.format(Locale.US, "  Contrato %s: %d pagos, S/ %.2f%n", numero, n, subtotal));
                cantidad += n;
            }
        }
        sb.append(String.format(Locale.US, "TOTAL: %d pagos, S/ %.2f", cantidad, totalIngresos(desde, hasta)));
        return sb.toString();
    }

    public String reporteMorosidad(LocalDate hoy) {
        StringBuilder sb = new StringBuilder();
        sb.append("REPORTE DE MOROSIDAD al ").append(Fechas.convertirAString(hoy)).append("\n");

        double totalDeuda = 0;
        double totalMora = 0;
        int morosos = 0;
        ListaSimple<Contrato> lista = contratos.listar();
        for (int i = 0; i < lista.getTamano(); i++) {
            Contrato c = lista.obtener(i);
            if (!mora.contratoEnMora(c.getNumero(), hoy)) continue;

            double deuda = pagos.deudaPendiente(c.getNumero());
            double recargo = mora.totalMora(c.getNumero(), hoy);
            sb.append(String.format(Locale.US,
                    "  Arrendatario %s | contrato %s | unidad %s | deuda S/ %.2f | recargo S/ %.2f%n",
                    c.getDocumentoArrendatario(), c.getNumero(), c.getCodigoUnidad(), deuda, recargo));
            totalDeuda += deuda;
            totalMora += recargo;
            morosos++;
        }
        if (morosos == 0) {
            sb.append("  No hay contratos en mora.");
        } else {
            sb.append(String.format(Locale.US, "TOTAL: %d contratos en mora | deuda S/ %.2f | recargos S/ %.2f",
                    morosos, totalDeuda, totalMora));
        }
        return sb.toString();
    }

    public String reporteOcupacion() {
        int disponibles = 0, alquiladas = 0, mantenimiento = 0;
        ListaSimple<Unidad> lista = propiedades.listarUnidades();
        for (int i = 0; i < lista.getTamano(); i++) {
            EstadoUnidad e = lista.obtener(i).getEstado();
            if (e == EstadoUnidad.DISPONIBLE) disponibles++;
            else if (e == EstadoUnidad.ALQUILADA) alquiladas++;
            else mantenimiento++;
        }
        int total = lista.getTamano();
        double porcentaje = total == 0 ? 0 : alquiladas * 100.0 / total;

        return "REPORTE DE OCUPACION\n"
                + "  Unidades: " + total + "\n"
                + "  Alquiladas: " + alquiladas + "\n"
                + "  Disponibles: " + disponibles + "\n"
                + "  En mantenimiento: " + mantenimiento + "\n"
                + String.format(Locale.US, "OCUPACION: %.1f%%", porcentaje);
    }
}

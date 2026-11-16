package inmobiliaria.reportes;

import java.time.LocalDate;
import java.util.Locale;
import inmobiliaria.cobranza.modelo.Cuota;
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

/**
 * Reportes de ingresos, morosidad y ocupacion.
 *
 * Cada reporte devuelve el texto listo para mostrar. Los totales numericos
 * tambien estan disponibles por separado.
 *
 * Responsable: Integrante 4 - Pucho Lizana Pedro Eduardo Mar
 */
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

    // ---------- Ingresos: pagos cobrados entre dos fechas (inclusive) ----------
    public double totalIngresos(LocalDate desde, LocalDate hasta) {
        double total = 0;
        ListaSimple<Pago> lista = pagos.listarPagos();
        for (int i = 0; i < lista.getTamano(); i++) {
            Pago p = lista.obtener(i);
            if (p.getNumeroCuota() == Cuota.NUMERO_GARANTIA) continue;   // la garantia es un deposito, no ingreso
            if (!p.getFecha().isBefore(desde) && !p.getFecha().isAfter(hasta)) total += p.getMonto();
        }
        return Math.round(total * 100.0) / 100.0;
    }

    // Garantias cobradas en el periodo; se informan aparte porque se devuelven al terminar el contrato
    public double garantiasRecibidas(LocalDate desde, LocalDate hasta) {
        double total = 0;
        ListaSimple<Pago> lista = pagos.listarPagos();
        for (int i = 0; i < lista.getTamano(); i++) {
            Pago p = lista.obtener(i);
            if (p.getNumeroCuota() == Cuota.NUMERO_GARANTIA
                    && !p.getFecha().isBefore(desde) && !p.getFecha().isAfter(hasta)) total += p.getMonto();
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
                if (p.getNumeroCuota() == Cuota.NUMERO_GARANTIA) continue;
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
        double garantias = garantiasRecibidas(desde, hasta);
        if (garantias > 0)
            sb.append(String.format(Locale.US, "%nGarantias recibidas (depositos, no son ingreso): S/ %.2f", garantias));
        return sb.toString();
    }

    // ---------- Morosidad: contratos con cuotas en mora ----------
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

    // ---------- Ocupacion: unidades por estado ----------
    public String reporteOcupacion() {
        int alquiladas = 0;
        ListaSimple<Unidad> lista = propiedades.listarUnidades();
        for (int i = 0; i < lista.getTamano(); i++) {
            if (lista.obtener(i).getEstado() == EstadoUnidad.ALQUILADA) alquiladas++;
        }
        int total = lista.getTamano();
        double porcentaje = total == 0 ? 0 : alquiladas * 100.0 / total;

        return "REPORTE DE OCUPACION\n"
                + "  Unidades: " + total + "\n"
                + "  Alquiladas: " + alquiladas + "\n"
                + "  No alquiladas: " + (total - alquiladas) + "\n"
                + String.format(Locale.US, "OCUPACION: %.1f%%", porcentaje);
    }
}

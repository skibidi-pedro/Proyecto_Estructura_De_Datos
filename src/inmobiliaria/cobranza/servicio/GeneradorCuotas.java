package inmobiliaria.cobranza.servicio;

import java.time.LocalDate;
import java.time.YearMonth;
import inmobiliaria.cobranza.modelo.Cuota;
import inmobiliaria.contratos.modelo.Contrato;
import inmobiliaria.nucleo.estructuras.ListaSimple;

/**
 * Genera cuotas mensuales al activar un contrato.
 *
 * Una cuota por mes, con vencimiento el dia de pago del contrato (1 a 28),
 * por el monto de la renta mensual, mientras el vencimiento no pase de la
 * fecha fin del contrato.
 *
 * Responsable: Integrante 4 - Pucho Lizana Pedro Eduardo Mar
 * Reglas: RN-07
 */
public class GeneradorCuotas {

    // Al activar: cuotas desde la fecha de inicio del contrato
    public static ListaSimple<Cuota> generar(Contrato c) {
        return generar(c, null, 1);
    }

    /**
     * Genera las cuotas cuyo vencimiento cae despues de 'despuesDe' (null = desde
     * la fecha de inicio, inclusive) y hasta la fecha fin del contrato.
     * Sirve tambien al renovar: despuesDe = fecha fin anterior, y la numeracion
     * continua desde 'numeroInicial'.
     */
    public static ListaSimple<Cuota> generar(Contrato c, LocalDate despuesDe, int numeroInicial) {
        ListaSimple<Cuota> resultado = new ListaSimple<>();
        YearMonth mes = YearMonth.from(despuesDe == null ? c.getFechaInicio() : despuesDe);
        int numero = numeroInicial;

        while (true) {
            LocalDate vencimiento = mes.atDay(c.getDiaPago());
            mes = mes.plusMonths(1);

            boolean antesDelPeriodo = (despuesDe == null)
                    ? vencimiento.isBefore(c.getFechaInicio())
                    : !vencimiento.isAfter(despuesDe);
            if (antesDelPeriodo) continue;
            if (vencimiento.isAfter(c.getFechaFin())) break;

            resultado.agregar(new Cuota(c.getNumero(), numero, vencimiento, c.getRentaMensual()));
            numero++;
        }
        return resultado;
    }
}

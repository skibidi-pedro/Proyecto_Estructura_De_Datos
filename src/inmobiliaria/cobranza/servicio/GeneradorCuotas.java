package inmobiliaria.cobranza.servicio;

import java.time.LocalDate;
import java.time.YearMonth;
import inmobiliaria.cobranza.modelo.Cuota;
import inmobiliaria.contratos.modelo.Contrato;
import inmobiliaria.nucleo.estructuras.ListaSimple;

public class GeneradorCuotas {

    public static ListaSimple<Cuota> generar(Contrato c) {
        return generar(c, null, 1);
    }

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

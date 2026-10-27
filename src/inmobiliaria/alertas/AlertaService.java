package inmobiliaria.alertas;

import java.time.LocalDate;
import inmobiliaria.cobranza.modelo.Cuota;
import inmobiliaria.cobranza.servicio.MoraService;
import inmobiliaria.cobranza.servicio.PagoService;
import inmobiliaria.contratos.modelo.Contrato;
import inmobiliaria.contratos.servicio.ContratoService;
import inmobiliaria.nucleo.estructuras.ColaPrioridad;
import inmobiliaria.nucleo.estructuras.ListaSimple;
import inmobiliaria.nucleo.util.Fechas;

public class AlertaService {
    public static final int DIAS_VENCIMIENTO_CONTRATO = 30;
    public static final int DIAS_AVISO_PAGO = 3;

    private ContratoService contratos;
    private PagoService pagos;
    private MoraService mora;

    public AlertaService(ContratoService contratos, PagoService pagos, MoraService mora) {
        this.contratos = contratos;
        this.pagos = pagos;
        this.mora = mora;
    }

    public ListaSimple<String> contratosPorVencer(LocalDate hoy) {
        ListaSimple<String> alertas = new ListaSimple<>();
        ListaSimple<Contrato> lista = contratos.porVencer(hoy, DIAS_VENCIMIENTO_CONTRATO);
        for (int i = 0; i < lista.getTamano(); i++) {
            Contrato c = lista.obtener(i);
            long dias = Fechas.calcularDiasDiferencia(hoy, c.getFechaFin());
            String cuando = dias < 0 ? "vencio hace " + (-dias) + " dias"
                          : dias == 0 ? "vence hoy"
                          : "vence en " + dias + " dias";
            alertas.agregar("Contrato " + c.getNumero() + " (unidad " + c.getCodigoUnidad()
                    + ", arrendatario " + c.getDocumentoArrendatario() + ") " + cuando
                    + " [" + Fechas.convertirAString(c.getFechaFin()) + "]");
        }
        return alertas;
    }

    public ListaSimple<String> pagosPendientes(LocalDate hoy) {
        ListaSimple<String> alertas = new ListaSimple<>();

        ColaPrioridad<Cuota> urgentes = mora.cobrosUrgentes(hoy);
        while (!urgentes.estaVacia()) {
            Cuota c = urgentes.desencolar();
            long atraso = mora.diasAtraso(c, hoy);
            String estado = mora.enMora(c, hoy)
                    ? "EN MORA, recargo S/ " + mora.recargo(c, hoy)
                    : "vencida, dentro de los " + MoraService.DIAS_GRACIA + " dias de gracia";
            alertas.agregar("Contrato " + c.getNumeroContrato() + " cuota " + c.getNumero()
                    + ": saldo S/ " + c.getSaldo() + ", " + atraso + " dias de atraso (" + estado + ")");
        }

        ListaSimple<Cuota> todas = pagos.listarCuotas();
        for (int i = 0; i < todas.getTamano(); i++) {
            Cuota c = todas.obtener(i);
            long faltan = Fechas.calcularDiasDiferencia(hoy, c.getVencimiento());
            if (!c.estaPagada() && faltan >= 0 && faltan <= DIAS_AVISO_PAGO) {
                alertas.agregar("Contrato " + c.getNumeroContrato() + " cuota " + c.getNumero()
                        + ": S/ " + c.getSaldo() + " vence "
                        + (faltan == 0 ? "hoy" : "en " + faltan + " dias"));
            }
        }
        return alertas;
    }

    public ListaSimple<String> todas(LocalDate hoy) {
        ListaSimple<String> resultado = contratosPorVencer(hoy);
        ListaSimple<String> pendientes = pagosPendientes(hoy);
        for (int i = 0; i < pendientes.getTamano(); i++) {
            resultado.agregar(pendientes.obtener(i));
        }
        return resultado;
    }
}

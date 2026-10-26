package inmobiliaria.cobranza.servicio;

import java.time.LocalDate;
import inmobiliaria.cobranza.modelo.Cuota;
import inmobiliaria.contratos.modelo.Contrato;
import inmobiliaria.contratos.modelo.EstadoContrato;
import inmobiliaria.nucleo.estructuras.ColaPrioridad;
import inmobiliaria.nucleo.estructuras.ListaSimple;
import inmobiliaria.nucleo.util.Fechas;

/**
 * Dias de gracia, recargo por mora y deteccion de arrendatarios morosos.
 *
 * Supuestos (cambiar las constantes si el enunciado dice otra cosa):
 *  - Una cuota entra en mora cuando tiene saldo y pasaron mas de DIAS_GRACIA
 *    dias desde su vencimiento (RN-08).
 *  - El recargo es PORCENTAJE_RECARGO % del saldo pendiente, por una sola vez.
 *    No se suma al saldo de la cuota: se calcula al consultar, y es el valor
 *    que se pasa como "mora" a ContratoService.finalizar.
 *  - Un arrendatario es moroso si algun contrato suyo tiene una cuota en mora (RN-11).
 *
 * Responsable: Integrante 4 - Pucho Lizana Pedro Eduardo Mar
 * Reglas: RN-08, RN-11
 */
public class MoraService {
    public static final int DIAS_GRACIA = 5;
    public static final double PORCENTAJE_RECARGO = 5.0;

    private PagoService pagos;

    public MoraService(PagoService pagos) {
        this.pagos = pagos;
    }

    // ---------- Por cuota (RN-08) ----------
    // Dias desde el vencimiento (0 si no vencio o ya esta pagada)
    public long diasAtraso(Cuota c, LocalDate hoy) {
        if (c.estaPagada() || !hoy.isAfter(c.getVencimiento())) return 0;
        return Fechas.calcularDiasDiferencia(c.getVencimiento(), hoy);
    }

    public boolean enMora(Cuota c, LocalDate hoy) {
        return diasAtraso(c, hoy) > DIAS_GRACIA;
    }

    public double recargo(Cuota c, LocalDate hoy) {
        if (!enMora(c, hoy)) return 0;
        return Math.round(c.getSaldo() * PORCENTAJE_RECARGO) / 100.0;
    }

    // ---------- Por contrato ----------
    // Suma de recargos de sus cuotas en mora (el "mora" de finalizar)
    public double totalMora(String numeroContrato, LocalDate hoy) {
        double total = 0;
        ListaSimple<Cuota> lista = pagos.cuotasDe(numeroContrato);
        for (int i = 0; i < lista.getTamano(); i++) {
            total += recargo(lista.obtener(i), hoy);
        }
        return Math.round(total * 100.0) / 100.0;
    }

    public boolean contratoEnMora(String numeroContrato, LocalDate hoy) {
        ListaSimple<Cuota> lista = pagos.cuotasDe(numeroContrato);
        for (int i = 0; i < lista.getTamano(); i++) {
            if (enMora(lista.obtener(i), hoy)) return true;
        }
        return false;
    }

    // ---------- Por arrendatario (RN-11) ----------
    // Los contratos se reciben por parametro (contratoService.listar())
    public boolean esMoroso(String documento, ListaSimple<Contrato> contratos, LocalDate hoy) {
        for (int i = 0; i < contratos.getTamano(); i++) {
            Contrato c = contratos.obtener(i);
            if (c.getDocumentoArrendatario().equals(documento)
                    && c.getEstado() != EstadoContrato.PENDIENTE
                    && contratoEnMora(c.getNumero(), hoy)) {
                return true;
            }
        }
        return false;
    }

    // Documentos de los arrendatarios morosos, sin repetir
    public ListaSimple<String> morosos(ListaSimple<Contrato> contratos, LocalDate hoy) {
        ListaSimple<String> resultado = new ListaSimple<>();
        for (int i = 0; i < contratos.getTamano(); i++) {
            String documento = contratos.obtener(i).getDocumentoArrendatario();
            if (!contiene(resultado, documento) && esMoroso(documento, contratos, hoy)) {
                resultado.agregar(documento);
            }
        }
        return resultado;
    }

    private boolean contiene(ListaSimple<String> lista, String valor) {
        for (int i = 0; i < lista.getTamano(); i++) {
            if (lista.obtener(i).equals(valor)) return true;
        }
        return false;
    }

    // ---------- Cobros urgentes: cola de prioridad ----------
    // Cuotas vencidas con saldo; sale primero la mas atrasada
    // (la cola saca la menor prioridad, por eso se usa -diasAtraso).
    public ColaPrioridad<Cuota> cobrosUrgentes(LocalDate hoy) {
        ColaPrioridad<Cuota> cola = new ColaPrioridad<>();
        ListaSimple<Cuota> todas = pagos.listarCuotas();
        for (int i = 0; i < todas.getTamano(); i++) {
            Cuota c = todas.obtener(i);
            long atraso = diasAtraso(c, hoy);
            if (atraso > 0) {
                cola.encolar(c, (int) -atraso);
            }
        }
        return cola;
    }
}

package inmobiliaria.cobranza.modelo;

import java.time.LocalDate;
import inmobiliaria.nucleo.persistencia.Persistible;
import inmobiliaria.nucleo.util.Fechas;

/**
 * Cuota mensual de un contrato (vencimiento, monto, saldo).
 *
 * La cuota numero 0 es la GARANTIA del contrato: se genera al activarlo, vence en la fecha de inicio
 * y se cobra igual que una cuota (pago total o parcial, con recibo). No es renta: no cuenta en la deuda
 * de renta, no genera recargo por mora y no es un ingreso.
 *
 * Responsable: Integrante 4 - Pucho Lizana Pedro Eduardo Mar
 * Reglas: RN-07, RN-09
 */
public class Cuota implements Persistible {
    public static final int NUMERO_GARANTIA = 0;

    private String numeroContrato;
    private int numero;              // 0 = garantia; 1, 2, 3... = renta mensual
    private LocalDate vencimiento;
    private double monto;
    private double saldo;            // lo que falta pagar (RN-09: puede haber pagos parciales)

    // Vacio, para poder usar desdeLineaCSV al cargar
    public Cuota() {
    }

    public Cuota(String numeroContrato, int numero, LocalDate vencimiento, double monto) {
        this.numeroContrato = numeroContrato;
        this.numero = numero;
        this.vencimiento = vencimiento;
        this.monto = monto;
        this.saldo = monto;
    }

    public String getNumeroContrato() { return numeroContrato; }
    public int getNumero() { return numero; }
    public LocalDate getVencimiento() { return vencimiento; }
    public double getMonto() { return monto; }
    public double getSaldo() { return saldo; }

    public boolean esGarantia() {
        return numero == NUMERO_GARANTIA;
    }

    public boolean estaPagada() {
        return saldo <= 0;
    }

    // La validacion (monto > 0 y no mayor al saldo) la hace PagoService
    public void aplicarPago(double importe) {
        saldo = Math.round((saldo - importe) * 100.0) / 100.0;
        if (saldo < 0.005) saldo = 0;
    }

    @Override
    public String aLineaCSV() {
        return numeroContrato + ";" + numero + ";" + Fechas.convertirAString(vencimiento) + ";"
                + monto + ";" + saldo;
    }

    @Override
    public void desdeLineaCSV(String linea) {
        String[] p = linea.split(";", -1);
        numeroContrato = p[0];
        numero = Integer.parseInt(p[1]);
        vencimiento = Fechas.convertirALocalDate(p[2]);
        monto = Double.parseDouble(p[3]);
        saldo = Double.parseDouble(p[4]);
    }

    @Override
    public String toString() {
        return "Cuota " + numero + " | contrato " + numeroContrato + " | vence "
                + Fechas.convertirAString(vencimiento) + " | monto S/ " + monto
                + " | saldo S/ " + saldo + (estaPagada() ? " | PAGADA" : "");
    }
}

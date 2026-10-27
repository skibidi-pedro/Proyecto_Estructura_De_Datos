package inmobiliaria.cobranza.modelo;

import java.time.LocalDate;
import inmobiliaria.nucleo.persistencia.Persistible;
import inmobiliaria.nucleo.util.Fechas;

public class Cuota implements Persistible {
    private String numeroContrato;
    private int numero;
    private LocalDate vencimiento;
    private double monto;
    private double saldo;

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

    public boolean estaPagada() {
        return saldo <= 0;
    }

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

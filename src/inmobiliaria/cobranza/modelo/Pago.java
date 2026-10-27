package inmobiliaria.cobranza.modelo;

import java.time.LocalDate;
import inmobiliaria.nucleo.persistencia.Persistible;
import inmobiliaria.nucleo.util.Fechas;

public class Pago implements Persistible {
    private String numeroContrato;
    private int numeroCuota;
    private LocalDate fecha;
    private double monto;
    private int numeroRecibo;

    public Pago() {
    }

    public Pago(String numeroContrato, int numeroCuota, LocalDate fecha, double monto, int numeroRecibo) {
        this.numeroContrato = numeroContrato;
        this.numeroCuota = numeroCuota;
        this.fecha = fecha;
        this.monto = monto;
        this.numeroRecibo = numeroRecibo;
    }

    public String getNumeroContrato() { return numeroContrato; }
    public int getNumeroCuota() { return numeroCuota; }
    public LocalDate getFecha() { return fecha; }
    public double getMonto() { return monto; }
    public int getNumeroRecibo() { return numeroRecibo; }

    @Override
    public String aLineaCSV() {
        return numeroContrato + ";" + numeroCuota + ";" + Fechas.convertirAString(fecha) + ";"
                + monto + ";" + numeroRecibo;
    }

    @Override
    public void desdeLineaCSV(String linea) {
        String[] p = linea.split(";", -1);
        numeroContrato = p[0];
        numeroCuota = Integer.parseInt(p[1]);
        fecha = Fechas.convertirALocalDate(p[2]);
        monto = Double.parseDouble(p[3]);
        numeroRecibo = Integer.parseInt(p[4]);
    }

    @Override
    public String toString() {
        return "Pago " + Fechas.convertirAString(fecha) + " | contrato " + numeroContrato
                + " cuota " + numeroCuota + " | S/ " + monto + " | recibo " + numeroRecibo;
    }
}

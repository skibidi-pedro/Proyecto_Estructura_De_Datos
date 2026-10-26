package inmobiliaria.cobranza.modelo;

import java.time.LocalDate;
import inmobiliaria.nucleo.persistencia.Persistible;
import inmobiliaria.nucleo.util.Fechas;

/**
 * Recibo con numero correlativo; inmutable.
 *
 * No tiene setters: una vez emitido no se modifica (RN-10). Los campos solo
 * se llenan en el constructor o al cargar desde el CSV.
 *
 * Responsable: Integrante 4 - Pucho Lizana Pedro Eduardo Mar
 * Reglas: RN-10
 */
public class Recibo implements Persistible {
    private int numero;
    private String numeroContrato;
    private int numeroCuota;
    private LocalDate fecha;
    private double montoPagado;
    private double saldoCuota;       // lo que quedo pendiente de la cuota tras este pago

    // Vacio, solo para desdeLineaCSV al cargar
    public Recibo() {
    }

    public Recibo(int numero, String numeroContrato, int numeroCuota, LocalDate fecha,
                  double montoPagado, double saldoCuota) {
        this.numero = numero;
        this.numeroContrato = numeroContrato;
        this.numeroCuota = numeroCuota;
        this.fecha = fecha;
        this.montoPagado = montoPagado;
        this.saldoCuota = saldoCuota;
    }

    public int getNumero() { return numero; }
    public String getNumeroContrato() { return numeroContrato; }
    public int getNumeroCuota() { return numeroCuota; }
    public LocalDate getFecha() { return fecha; }
    public double getMontoPagado() { return montoPagado; }
    public double getSaldoCuota() { return saldoCuota; }

    // Ej: REC-000001
    public String getCodigo() {
        return String.format("REC-%06d", numero);
    }

    @Override
    public String aLineaCSV() {
        return numero + ";" + numeroContrato + ";" + numeroCuota + ";"
                + Fechas.convertirAString(fecha) + ";" + montoPagado + ";" + saldoCuota;
    }

    @Override
    public void desdeLineaCSV(String linea) {
        String[] p = linea.split(";", -1);
        numero = Integer.parseInt(p[0]);
        numeroContrato = p[1];
        numeroCuota = Integer.parseInt(p[2]);
        fecha = Fechas.convertirALocalDate(p[3]);
        montoPagado = Double.parseDouble(p[4]);
        saldoCuota = Double.parseDouble(p[5]);
    }

    @Override
    public String toString() {
        return getCodigo() + " | " + Fechas.convertirAString(fecha) + " | contrato " + numeroContrato
                + " cuota " + numeroCuota + " | pagado S/ " + montoPagado
                + " | saldo de la cuota S/ " + saldoCuota;
    }
}

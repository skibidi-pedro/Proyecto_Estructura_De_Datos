package inmobiliaria.contratos.modelo;

import java.time.LocalDate;
import inmobiliaria.nucleo.persistencia.Persistible;
import inmobiliaria.nucleo.util.Fechas;

/**
 * Entidad contrato.
 *
 * Responsable: Integrante 2 - Zavaleta Calderon Steven Jack
 * Reglas: RN-01, RN-04, RN-05
 */
public class Contrato implements Persistible {
    private String numero;
    private String documentoArrendatario;
    private String codigoUnidad;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private double rentaMensual;
    private int diaPago;
    private double garantia;
    private String clausulas;
    private EstadoContrato estado;

    // Vacio, para poder usar desdeLineaCSV al cargar
    public Contrato() {
    }

    public Contrato(String numero, String documentoArrendatario, String codigoUnidad,
                    LocalDate fechaInicio, LocalDate fechaFin, double rentaMensual,
                    int diaPago, double garantia, String clausulas) {
        this.numero = numero;
        this.documentoArrendatario = documentoArrendatario;
        this.codigoUnidad = codigoUnidad;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.rentaMensual = rentaMensual;
        this.diaPago = diaPago;
        this.garantia = garantia;
        this.clausulas = limpiar(clausulas);
        this.estado = EstadoContrato.PENDIENTE;
    }

    public String getNumero() { return numero; }
    public String getDocumentoArrendatario() { return documentoArrendatario; }
    public String getCodigoUnidad() { return codigoUnidad; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public LocalDate getFechaFin() { return fechaFin; }
    public double getRentaMensual() { return rentaMensual; }
    public int getDiaPago() { return diaPago; }
    public double getGarantia() { return garantia; }
    public String getClausulas() { return clausulas; }
    public EstadoContrato getEstado() { return estado; }

    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }
    public void setRentaMensual(double rentaMensual) { this.rentaMensual = rentaMensual; }
    public void setEstado(EstadoContrato estado) { this.estado = estado; }

    // El separador del CSV es ';', asi que no puede aparecer en las clausulas
    private String limpiar(String texto) {
        return texto == null ? "" : texto.replace(";", ",");
    }

    @Override
    public String aLineaCSV() {
        return numero + ";" + documentoArrendatario + ";" + codigoUnidad + ";"
                + Fechas.convertirAString(fechaInicio) + ";" + Fechas.convertirAString(fechaFin) + ";"
                + rentaMensual + ";" + diaPago + ";" + garantia + ";" + clausulas + ";" + estado;
    }

    @Override
    public void desdeLineaCSV(String linea) {
        String[] p = linea.split(";", -1);
        numero = p[0];
        documentoArrendatario = p[1];
        codigoUnidad = p[2];
        fechaInicio = Fechas.convertirALocalDate(p[3]);
        fechaFin = Fechas.convertirALocalDate(p[4]);
        rentaMensual = Double.parseDouble(p[5]);
        diaPago = Integer.parseInt(p[6]);
        garantia = Double.parseDouble(p[7]);
        clausulas = p[8];
        estado = EstadoContrato.valueOf(p[9]);
    }

    @Override
    public String toString() {
        return "Contrato " + numero + " | unidad " + codigoUnidad + " | arrendatario "
                + documentoArrendatario + " | renta " + rentaMensual + " | " + estado;
    }
}
package inmobiliaria.contratos.servicio;

public class LiquidacionGarantia {
    private double garantia;
    private double deudaRenta;
    private double mora;
    private double danos;

    public LiquidacionGarantia(double garantia, double deudaRenta, double mora, double danos) {
        this.garantia = garantia;
        this.deudaRenta = deudaRenta;
        this.mora = mora;
        this.danos = danos;
    }

    public double getTotalDescuentos() {
        return deudaRenta + mora + danos;
    }

    public double getMontoADevolver() {
        return Math.max(0, garantia - getTotalDescuentos());
    }

    public double getSaldoPendiente() {
        return Math.max(0, getTotalDescuentos() - garantia);
    }

    @Override
    public String toString() {
        return "Garantia: " + garantia + " | Descuentos: " + getTotalDescuentos()
                + " (renta " + deudaRenta + ", mora " + mora + ", danos " + danos + ")"
                + " | A devolver: " + getMontoADevolver()
                + " | Saldo pendiente: " + getSaldoPendiente();
    }
}
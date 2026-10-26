package inmobiliaria.cobranza.servicio;

import java.time.LocalDate;
import inmobiliaria.cobranza.modelo.Cuota;
import inmobiliaria.cobranza.modelo.Pago;
import inmobiliaria.cobranza.modelo.Recibo;
import inmobiliaria.contratos.modelo.Contrato;
import inmobiliaria.contratos.modelo.EstadoContrato;
import inmobiliaria.nucleo.estructuras.ListaSimple;
import inmobiliaria.nucleo.historial.Historial;
import inmobiliaria.nucleo.persistencia.GestorCSV;
import inmobiliaria.nucleo.persistencia.Persistible;

/**
 * Registro de pagos y emision de recibos (2.5).
 *
 * Guarda las cuotas, los pagos y los recibos. Recibe el Contrato como
 * parametro (igual que ContratoService recibe datos de otros modulos), asi
 * no depende de ContratoService.
 *
 * Responsable: Integrante 4 - Pucho Lizana Pedro Eduardo Mar
 * Reglas: RN-07, RN-09, RN-10
 */
public class PagoService {
    private static final String ARCHIVO_CUOTAS = "cuotas.csv";
    private static final String ARCHIVO_PAGOS = "pagos.csv";
    private static final String ARCHIVO_RECIBOS = "recibos.csv";

    private ListaSimple<Cuota> cuotas = new ListaSimple<>();
    private ListaSimple<Pago> pagos = new ListaSimple<>();
    private ListaSimple<Recibo> recibos = new ListaSimple<>();
    private int ultimoRecibo = 0;      // el siguiente recibo es ultimoRecibo + 1 (RN-10)
    private Historial historial;

    public PagoService(Historial historial) {
        this.historial = historial;
    }

    // ---------- Generar cuotas (RN-07): al activar el contrato ----------
    public ListaSimple<Cuota> generarCuotas(Contrato c) {
        if (c.getEstado() != EstadoContrato.VIGENTE)
            throw new IllegalArgumentException("Solo se generan cuotas de un contrato vigente");
        if (!cuotasDe(c.getNumero()).estaVacia())
            throw new IllegalArgumentException("El contrato " + c.getNumero() + " ya tiene cuotas");

        ListaSimple<Cuota> nuevas = GeneradorCuotas.generar(c);
        agregarCuotas(nuevas);
        historial.registrarOperacion("Cuotas generadas: contrato " + c.getNumero()
                + " (" + nuevas.getTamano() + ")");
        return nuevas;
    }

    // Al renovar: cuotas del periodo nuevo. fechaFinAnterior es la fecha fin ANTES de renovar.
    public ListaSimple<Cuota> generarCuotasRenovacion(Contrato c, LocalDate fechaFinAnterior) {
        if (c.getEstado() != EstadoContrato.VIGENTE)
            throw new IllegalArgumentException("Solo se generan cuotas de un contrato vigente");

        int siguiente = cuotasDe(c.getNumero()).getTamano() + 1;
        ListaSimple<Cuota> nuevas = GeneradorCuotas.generar(c, fechaFinAnterior, siguiente);
        agregarCuotas(nuevas);
        historial.registrarOperacion("Cuotas de renovacion generadas: contrato " + c.getNumero()
                + " (" + nuevas.getTamano() + ")");
        return nuevas;
    }

    private void agregarCuotas(ListaSimple<Cuota> nuevas) {
        for (int i = 0; i < nuevas.getTamano(); i++) {
            cuotas.agregar(nuevas.obtener(i));
        }
    }

    // ---------- Registrar pago total o parcial y emitir recibo (RN-09, RN-10) ----------
    public Recibo registrarPago(String numeroContrato, int numeroCuota, double monto, LocalDate fecha) {
        Cuota cuota = buscarCuota(numeroContrato, numeroCuota);
        if (cuota == null)
            throw new IllegalArgumentException("No existe la cuota " + numeroCuota
                    + " del contrato " + numeroContrato);
        if (fecha == null)
            throw new IllegalArgumentException("Falta la fecha de pago");
        if (cuota.estaPagada())
            throw new IllegalArgumentException("La cuota ya esta pagada");

        monto = Math.round(monto * 100.0) / 100.0;
        if (monto <= 0)                                                // RN-09
            throw new IllegalArgumentException("El monto del pago debe ser mayor a cero");
        if (monto > cuota.getSaldo())                                  // RN-09
            throw new IllegalArgumentException("El pago (S/ " + monto
                    + ") supera el saldo de la cuota (S/ " + cuota.getSaldo() + ")");

        cuota.aplicarPago(monto);
        ultimoRecibo++;                                                // RN-10: correlativo
        Recibo recibo = new Recibo(ultimoRecibo, numeroContrato, numeroCuota, fecha,
                monto, cuota.getSaldo());
        recibos.agregar(recibo);
        pagos.agregar(new Pago(numeroContrato, numeroCuota, fecha, monto, ultimoRecibo));

        historial.registrarOperacion("Pago registrado: contrato " + numeroContrato + " cuota "
                + numeroCuota + " S/ " + monto + " (" + recibo.getCodigo() + ")");
        return recibo;
    }

    // ---------- Consultas ----------
    public Cuota buscarCuota(String numeroContrato, int numeroCuota) {
        for (int i = 0; i < cuotas.getTamano(); i++) {
            Cuota c = cuotas.obtener(i);
            if (c.getNumeroContrato().equals(numeroContrato) && c.getNumero() == numeroCuota) return c;
        }
        return null;
    }

    public ListaSimple<Cuota> cuotasDe(String numeroContrato) {
        ListaSimple<Cuota> resultado = new ListaSimple<>();
        for (int i = 0; i < cuotas.getTamano(); i++) {
            if (cuotas.obtener(i).getNumeroContrato().equals(numeroContrato))
                resultado.agregar(cuotas.obtener(i));
        }
        return resultado;
    }

    public ListaSimple<Pago> pagosDe(String numeroContrato) {
        ListaSimple<Pago> resultado = new ListaSimple<>();
        for (int i = 0; i < pagos.getTamano(); i++) {
            if (pagos.obtener(i).getNumeroContrato().equals(numeroContrato))
                resultado.agregar(pagos.obtener(i));
        }
        return resultado;
    }

    public Recibo buscarRecibo(int numero) {
        for (int i = 0; i < recibos.getTamano(); i++) {
            if (recibos.obtener(i).getNumero() == numero) return recibos.obtener(i);
        }
        return null;
    }

    // Suma de los saldos de las cuotas del contrato (solo renta, sin recargo por mora)
    public double deudaPendiente(String numeroContrato) {
        double total = 0;
        ListaSimple<Cuota> lista = cuotasDe(numeroContrato);
        for (int i = 0; i < lista.getTamano(); i++) {
            total += lista.obtener(i).getSaldo();
        }
        return Math.round(total * 100.0) / 100.0;
    }

    public ListaSimple<Cuota> listarCuotas() { return cuotas; }
    public ListaSimple<Pago> listarPagos() { return pagos; }
    public ListaSimple<Recibo> listarRecibos() { return recibos; }

    // ---------- Persistencia (cuotas.csv, pagos.csv, recibos.csv) ----------
    public void cargar() {
        ListaSimple<String> lineas = new ListaSimple<>();
        GestorCSV.cargar(ARCHIVO_CUOTAS, lineas);
        for (int i = 0; i < lineas.getTamano(); i++) {
            Cuota c = new Cuota();
            c.desdeLineaCSV(lineas.obtener(i));
            cuotas.agregar(c);
        }

        lineas = new ListaSimple<>();
        GestorCSV.cargar(ARCHIVO_PAGOS, lineas);
        for (int i = 0; i < lineas.getTamano(); i++) {
            Pago p = new Pago();
            p.desdeLineaCSV(lineas.obtener(i));
            pagos.agregar(p);
        }

        lineas = new ListaSimple<>();
        GestorCSV.cargar(ARCHIVO_RECIBOS, lineas);
        for (int i = 0; i < lineas.getTamano(); i++) {
            Recibo r = new Recibo();
            r.desdeLineaCSV(lineas.obtener(i));
            recibos.agregar(r);
            if (r.getNumero() > ultimoRecibo) ultimoRecibo = r.getNumero();   // sigue la numeracion
        }
    }

    public void guardar() {
        GestorCSV.guardar(ARCHIVO_CUOTAS, comoPersistibles(cuotas));
        GestorCSV.guardar(ARCHIVO_PAGOS, comoPersistibles(pagos));
        GestorCSV.guardar(ARCHIVO_RECIBOS, comoPersistibles(recibos));
    }

    private <T extends Persistible> ListaSimple<Persistible> comoPersistibles(ListaSimple<T> origen) {
        ListaSimple<Persistible> lista = new ListaSimple<>();
        for (int i = 0; i < origen.getTamano(); i++) {
            lista.agregar(origen.obtener(i));
        }
        return lista;
    }
}

package inmobiliaria.contratos.servicio;

import java.time.LocalDate;
import inmobiliaria.contratos.modelo.Contrato;
import inmobiliaria.contratos.modelo.EstadoContrato;
import inmobiliaria.nucleo.estructuras.ListaSimple;
import inmobiliaria.nucleo.historial.Historial;
import inmobiliaria.nucleo.persistencia.GestorCSV;
import inmobiliaria.nucleo.persistencia.Persistible;
import inmobiliaria.nucleo.util.Fechas;
import inmobiliaria.nucleo.util.Validador;

/**
 * Alta, renovacion, ajuste de renta y finalizacion (2.3).
 *
 * Los datos de otros modulos (unidad disponible, moroso, expediente completo)
 * se reciben como parametros, asi este servicio no depende de las clases de
 * los demas. Quien llame (menu o servicio de integracion) tambien se encarga
 * de cambiar el estado de la unidad (ALQUILADA al activar, DISPONIBLE al finalizar).
 *
 * Responsable: Integrante 2 - Zavaleta Calderon Steven Jack
 * Reglas: RN-02, RN-03, RN-04, RN-05, RN-12, RN-13
 */
public class ContratoService {
    private static final String ARCHIVO = "contratos.csv";

    private ListaSimple<Contrato> contratos = new ListaSimple<>();
    private Historial historial;

    public ContratoService(Historial historial) {
        this.historial = historial;
    }

    // ---------- Crear (RN-01, 02, 03, 04, 05, 11) ----------
    public Contrato crear(String numero, String documento, String codigoUnidad,
                          LocalDate inicio, LocalDate fin, double renta,
                          int diaPago, double garantia, String clausulas,
                          boolean unidadDisponible, boolean arrendatarioMoroso) {

        if (!Validador.esTextoValido(numero) || !Validador.esTextoValido(documento)
                || !Validador.esTextoValido(codigoUnidad))
            throw new IllegalArgumentException("Numero, arrendatario y unidad son obligatorios");
        if (buscar(numero) != null)                                    // RN-01
            throw new IllegalArgumentException("Ya existe el contrato " + numero);
        if (arrendatarioMoroso)                                        // RN-11
            throw new IllegalArgumentException("El arrendatario es moroso, no puede firmar");
        if (!unidadDisponible)                                         // RN-02
            throw new IllegalArgumentException("La unidad no esta disponible");
        if (contratoVigenteDeUnidad(codigoUnidad) != null)             // RN-03
            throw new IllegalArgumentException("La unidad ya tiene un contrato vigente");

        if (inicio == null || fin == null)                             // RN-04
            throw new IllegalArgumentException("Faltan las fechas del contrato");
        if (!Validador.esMontoValido(renta))
            throw new IllegalArgumentException("La renta debe ser mayor a cero");
        if (!Validador.esDiaPagoValido(diaPago))
            throw new IllegalArgumentException("El dia de pago debe estar entre 1 y 28");
        if (garantia < 0)
            throw new IllegalArgumentException("La garantia no puede ser negativa");
        if (!Fechas.esPosterior(inicio, fin))                          // RN-05
            throw new IllegalArgumentException("La fecha de fin debe ser posterior a la de inicio");

        Contrato c = new Contrato(numero, documento, codigoUnidad, inicio, fin,
                renta, diaPago, garantia, clausulas);
        contratos.agregar(c);
        historial.registrarOperacion("Contrato creado: " + numero);
        return c;
    }

    // ---------- Activar (RN-06). Despues hay que generar las cuotas (RN-07) ----------
    public Contrato activar(String numero, boolean expedienteCompleto) {
        Contrato c = buscarObligatorio(numero);
        if (c.getEstado() != EstadoContrato.PENDIENTE)
            throw new IllegalArgumentException("Solo se puede activar un contrato pendiente");
        if (!expedienteCompleto)                                       // RN-06
            throw new IllegalArgumentException("El expediente esta incompleto (identidad y contrato firmado)");
        if (contratoVigenteDeUnidad(c.getCodigoUnidad()) != null)      // RN-03
            throw new IllegalArgumentException("La unidad ya tiene un contrato vigente");

        c.setEstado(EstadoContrato.VIGENTE);
        historial.registrarOperacion("Contrato activado: " + numero);
        return c;
    }

    // ---------- Renovar (RN-12) ----------
    // porcentaje: 5 = sube 5%, 0 = misma renta
    public Contrato renovar(String numero, LocalDate nuevaFechaFin, double porcentaje,
                            boolean arrendatarioMoroso) {
        Contrato c = buscarObligatorio(numero);
        if (c.getEstado() != EstadoContrato.VIGENTE)
            throw new IllegalArgumentException("Solo se puede renovar un contrato vigente");
        if (arrendatarioMoroso)
            throw new IllegalArgumentException("No se puede renovar: el arrendatario es moroso");
        if (!Fechas.esPosterior(c.getFechaFin(), nuevaFechaFin))
            throw new IllegalArgumentException("La nueva fecha debe ser posterior a la actual");

        aplicarAjuste(c, porcentaje);
        c.setFechaFin(nuevaFechaFin);
        historial.registrarOperacion("Contrato renovado: " + numero);
        return c;   // cobranza debe generar las cuotas del periodo nuevo
    }

    // ---------- Ajustar renta ----------
    public void ajustarRenta(String numero, double porcentaje) {
        Contrato c = buscarObligatorio(numero);
        aplicarAjuste(c, porcentaje);
        historial.registrarOperacion("Renta ajustada en contrato " + numero);
    }

    private void aplicarAjuste(Contrato c, double porcentaje) {
        double nueva = c.getRentaMensual() * (1 + porcentaje / 100.0);
        if (!Validador.esMontoValido(nueva))
            throw new IllegalArgumentException("La renta resultante debe ser mayor a cero");
        c.setRentaMensual(nueva);
    }

    // ---------- Finalizar + liquidar garantia (RN-03, RN-13) ----------
    // deudaRenta y mora vienen de cobranza, los danos los registra el usuario
    public LiquidacionGarantia finalizar(String numero, double deudaRenta, double mora, double danos) {
        Contrato c = buscarObligatorio(numero);
        if (c.getEstado() != EstadoContrato.VIGENTE)
            throw new IllegalArgumentException("Solo se puede finalizar un contrato vigente");

        c.setEstado(EstadoContrato.FINALIZADO);
        historial.registrarOperacion("Contrato finalizado: " + numero);
        return new LiquidacionGarantia(c.getGarantia(), deudaRenta, mora, danos);
    }

    // ---------- Consultas ----------
    public Contrato buscar(String numero) {
        for (int i = 0; i < contratos.getTamano(); i++) {
            Contrato c = contratos.obtener(i);
            if (c.getNumero().equals(numero)) return c;
        }
        return null;
    }

    public Contrato contratoVigenteDeUnidad(String codigoUnidad) {
        for (int i = 0; i < contratos.getTamano(); i++) {
            Contrato c = contratos.obtener(i);
            if (c.getCodigoUnidad().equals(codigoUnidad) && c.getEstado() == EstadoContrato.VIGENTE)
                return c;
        }
        return null;
    }

    public ListaSimple<Contrato> listar() {
        return contratos;
    }

    // Vigentes que vencen dentro de 'dias' dias (alerta de 30 dias, RN-12)
    public ListaSimple<Contrato> porVencer(LocalDate hoy, int dias) {
        ListaSimple<Contrato> resultado = new ListaSimple<>();
        LocalDate limite = hoy.plusDays(dias);
        for (int i = 0; i < contratos.getTamano(); i++) {
            Contrato c = contratos.obtener(i);
            if (c.getEstado() == EstadoContrato.VIGENTE && !c.getFechaFin().isAfter(limite))
                resultado.agregar(c);
        }
        return resultado;
    }

    // ---------- Persistencia (data/contratos.csv) ----------
    public void cargar() {
        ListaSimple<String> lineas = new ListaSimple<>();
        GestorCSV.cargar(ARCHIVO, lineas);
        for (int i = 0; i < lineas.getTamano(); i++) {
            Contrato c = new Contrato();
            c.desdeLineaCSV(lineas.obtener(i));
            contratos.agregar(c);
        }
    }

    public void guardar() {
        ListaSimple<Persistible> lista = new ListaSimple<>();
        for (int i = 0; i < contratos.getTamano(); i++) {
            lista.agregar(contratos.obtener(i));
        }
        GestorCSV.guardar(ARCHIVO, lista);
    }

    private Contrato buscarObligatorio(String numero) {
        Contrato c = buscar(numero);
        if (c == null) throw new IllegalArgumentException("No existe el contrato " + numero);
        return c;
    }
}
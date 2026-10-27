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

public class ContratoService {
    private static final String ARCHIVO = "contratos.csv";

    private ListaSimple<Contrato> contratos = new ListaSimple<>();
    private Historial historial;

    public ContratoService(Historial historial) {
        this.historial = historial;
    }

    public Contrato crear(String numero, String documento, String codigoUnidad,
                          LocalDate inicio, LocalDate fin, double renta,
                          int diaPago, double garantia, String clausulas,
                          boolean unidadDisponible, boolean arrendatarioMoroso) {

        if (!Validador.esTextoValido(numero) || !Validador.esTextoValido(documento)
                || !Validador.esTextoValido(codigoUnidad))
            throw new IllegalArgumentException("Numero, arrendatario y unidad son obligatorios");
        if (buscar(numero) != null)
            throw new IllegalArgumentException("Ya existe el contrato " + numero);
        if (arrendatarioMoroso)
            throw new IllegalArgumentException("El arrendatario es moroso, no puede firmar");
        if (!unidadDisponible)
            throw new IllegalArgumentException("La unidad no esta disponible");
        if (contratoVigenteDeUnidad(codigoUnidad) != null)
            throw new IllegalArgumentException("La unidad ya tiene un contrato vigente");

        if (inicio == null || fin == null)
            throw new IllegalArgumentException("Faltan las fechas del contrato");
        if (!Validador.esMontoValido(renta))
            throw new IllegalArgumentException("La renta debe ser mayor a cero");
        if (!Validador.esDiaPagoValido(diaPago))
            throw new IllegalArgumentException("El dia de pago debe estar entre 1 y 28");
        if (garantia < 0)
            throw new IllegalArgumentException("La garantia no puede ser negativa");
        if (!Fechas.esPosterior(inicio, fin))
            throw new IllegalArgumentException("La fecha de fin debe ser posterior a la de inicio");

        Contrato c = new Contrato(numero, documento, codigoUnidad, inicio, fin,
                renta, diaPago, garantia, clausulas);
        contratos.agregar(c);
        historial.registrarOperacion("Contrato creado: " + numero);
        return c;
    }

    public Contrato activar(String numero, boolean expedienteCompleto) {
        Contrato c = buscarObligatorio(numero);
        if (c.getEstado() != EstadoContrato.PENDIENTE)
            throw new IllegalArgumentException("Solo se puede activar un contrato pendiente");
        if (!expedienteCompleto)
            throw new IllegalArgumentException("El expediente esta incompleto (identidad y contrato firmado)");
        if (contratoVigenteDeUnidad(c.getCodigoUnidad()) != null)
            throw new IllegalArgumentException("La unidad ya tiene un contrato vigente");

        c.setEstado(EstadoContrato.VIGENTE);
        historial.registrarOperacion("Contrato activado: " + numero);
        return c;
    }

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
        return c;
    }

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

    public LiquidacionGarantia finalizar(String numero, double deudaRenta, double mora, double danos) {
        Contrato c = buscarObligatorio(numero);
        if (c.getEstado() != EstadoContrato.VIGENTE)
            throw new IllegalArgumentException("Solo se puede finalizar un contrato vigente");

        c.setEstado(EstadoContrato.FINALIZADO);
        historial.registrarOperacion("Contrato finalizado: " + numero);
        return new LiquidacionGarantia(c.getGarantia(), deudaRenta, mora, danos);
    }

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
package inmobiliaria.cobranza;

import java.time.LocalDate;
import inmobiliaria.alertas.AlertaService;
import inmobiliaria.cobranza.modelo.Cuota;
import inmobiliaria.cobranza.modelo.Recibo;
import inmobiliaria.cobranza.servicio.MoraService;
import inmobiliaria.cobranza.servicio.PagoService;
import inmobiliaria.contratos.modelo.Contrato;
import inmobiliaria.contratos.servicio.ContratoService;
import inmobiliaria.nucleo.estructuras.ColaPrioridad;
import inmobiliaria.nucleo.estructuras.ListaSimple;
import inmobiliaria.nucleo.historial.Historial;
import inmobiliaria.propiedades.modelo.EstadoUnidad;
import inmobiliaria.propiedades.modelo.TipoUnidad;
import inmobiliaria.propiedades.servicio.PropiedadService;
import inmobiliaria.reportes.ReporteService;

public class PruebaCobranza {
    private static int fallos = 0;

    private static void verificar(String nombre, boolean ok) {
        System.out.println((ok ? "[OK]    " : "[FALLO] ") + nombre);
        if (!ok) fallos++;
    }

    private static boolean lanza(Runnable r) {
        try { r.run(); return false; } catch (IllegalArgumentException e) { return true; }
    }

    public static void main(String[] args) {
        Historial historial = new Historial();
        ContratoService contratos = new ContratoService(historial);
        PagoService pagos = new PagoService(historial);
        MoraService mora = new MoraService(pagos);
        AlertaService alertas = new AlertaService(contratos, pagos, mora);
        PropiedadService propiedades = new PropiedadService(historial);
        ReporteService reportes = new ReporteService(contratos, pagos, mora, propiedades);

        propiedades.registrarPropietario("P1", "Propietario Uno");
        propiedades.registrarEdificio("P1", "E1", "Edificio Uno", "Av. Lima 100");
        propiedades.registrarUnidad("E1", "U1", "Av. Lima 100 - 101", TipoUnidad.values()[0], 1000, EstadoUnidad.ALQUILADA);
        propiedades.registrarUnidad("E1", "U2", "Av. Lima 100 - 102", TipoUnidad.values()[0], 900, EstadoUnidad.DISPONIBLE);

        Contrato c = contratos.crear("C1", "DNI1", "U1", LocalDate.of(2026, 1, 10),
                LocalDate.of(2026, 6, 30), 1000, 10, 1000, "sin clausulas", true, false);

        verificar("No genera cuotas de un contrato pendiente", lanza(() -> pagos.generarCuotas(c)));
        contratos.activar("C1", true);
        ListaSimple<Cuota> generadas = pagos.generarCuotas(c);
        verificar("Genera 6 cuotas (ene a jun)", generadas.getTamano() == 6);
        verificar("Primer vencimiento 2026-01-10", generadas.obtener(0).getVencimiento().equals(LocalDate.of(2026, 1, 10)));
        verificar("Ultimo vencimiento 2026-06-10", generadas.obtener(5).getVencimiento().equals(LocalDate.of(2026, 6, 10)));
        verificar("No genera cuotas dos veces", lanza(() -> pagos.generarCuotas(c)));

        Recibo r1 = pagos.registrarPago("C1", 1, 1000, LocalDate.of(2026, 1, 10));
        verificar("Pago total deja la cuota pagada", pagos.buscarCuota("C1", 1).estaPagada());
        verificar("Primer recibo es el 1", r1.getNumero() == 1 && r1.getCodigo().equals("REC-000001"));
        Recibo r2 = pagos.registrarPago("C1", 2, 400, LocalDate.of(2026, 2, 12));
        verificar("Pago parcial deja saldo 600", pagos.buscarCuota("C1", 2).getSaldo() == 600);
        verificar("Recibo correlativo (2)", r2.getNumero() == 2 && r2.getSaldoCuota() == 600);
        verificar("No acepta pago mayor al saldo", lanza(() -> pagos.registrarPago("C1", 2, 700, LocalDate.of(2026, 2, 13))));
        verificar("No acepta pago de cero", lanza(() -> pagos.registrarPago("C1", 2, 0, LocalDate.of(2026, 2, 13))));
        verificar("No acepta pagar una cuota ya pagada", lanza(() -> pagos.registrarPago("C1", 1, 10, LocalDate.of(2026, 2, 13))));
        verificar("No acepta cuota inexistente", lanza(() -> pagos.registrarPago("C1", 99, 10, LocalDate.of(2026, 2, 13))));
        verificar("Pagos rechazados no emiten recibo", pagos.listarRecibos().getTamano() == 2);
        verificar("Deuda pendiente = 600 + 4 cuotas de 1000", pagos.deudaPendiente("C1") == 4600);

        LocalDate hoy = LocalDate.of(2026, 3, 20);
        Cuota c2 = pagos.buscarCuota("C1", 2);
        Cuota c3 = pagos.buscarCuota("C1", 3);
        verificar("Cuota 3 (vencio 10-mar) lleva 10 dias de atraso", mora.diasAtraso(c3, hoy) == 10);
        verificar("Cuota 3 esta en mora (pasaron los 5 dias de gracia)", mora.enMora(c3, hoy));
        verificar("Dentro de la gracia no hay mora", !mora.enMora(c3, LocalDate.of(2026, 3, 15)));
        verificar("Cuota pagada no tiene atraso", mora.diasAtraso(pagos.buscarCuota("C1", 1), hoy) == 0);
        verificar("Recargo cuota 2 = 5% de 600 = 30", mora.recargo(c2, hoy) == 30);
        verificar("Recargo cuota 3 = 5% de 1000 = 50", mora.recargo(c3, hoy) == 50);
        verificar("Total de mora del contrato = 80", mora.totalMora("C1", hoy) == 80);
        verificar("El arrendatario es moroso", mora.esMoroso("DNI1", contratos.listar(), hoy));
        verificar("Otro documento no es moroso", !mora.esMoroso("DNI2", contratos.listar(), hoy));
        verificar("Lista de morosos tiene 1 documento", mora.morosos(contratos.listar(), hoy).getTamano() == 1);
        verificar("Sin atrasos no es moroso", !mora.esMoroso("DNI1", contratos.listar(), LocalDate.of(2026, 1, 11)));

        ColaPrioridad<Cuota> urgentes = mora.cobrosUrgentes(hoy);
        verificar("Cobros urgentes: 2 cuotas vencidas", urgentes.getTamano() == 2);
        verificar("Sale primero la mas atrasada (cuota 2)", urgentes.desencolar().getNumero() == 2);

        LocalDate finAnterior = c.getFechaFin();
        contratos.renovar("C1", LocalDate.of(2026, 8, 31), 0, false);
        ListaSimple<Cuota> nuevas = pagos.generarCuotasRenovacion(c, finAnterior);
        verificar("Renovacion agrega 2 cuotas (jul, ago)", nuevas.getTamano() == 2);
        verificar("La numeracion continua en 7", nuevas.obtener(0).getNumero() == 7);
        verificar("Total de cuotas = 8", pagos.cuotasDe("C1").getTamano() == 8);

        LocalDate cerca = LocalDate.of(2026, 8, 10);
        verificar("Alerta de contrato por vencer (fin 31-ago)", alertas.contratosPorVencer(cerca).getTamano() == 1);
        verificar("Sin alerta de vencimiento lejos del fin", alertas.contratosPorVencer(hoy).getTamano() == 0);
        ListaSimple<String> pendientes = alertas.pagosPendientes(hoy);
        verificar("Alertas de pagos: 2 cuotas vencidas", pendientes.getTamano() == 2);
        verificar("La mas atrasada aparece primero", pendientes.obtener(0).contains("cuota 2"));
        verificar("Aviso de cuota por vencer en 3 dias",
                alertas.pagosPendientes(LocalDate.of(2026, 4, 8)).obtener(
                        alertas.pagosPendientes(LocalDate.of(2026, 4, 8)).getTamano() - 1).contains("cuota 4"));

        verificar("Ingresos de enero = 1000", reportes.totalIngresos(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)) == 1000);
        verificar("Ingresos ene-mar = 1400", reportes.totalIngresos(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31)) == 1400);
        verificar("Reporte de ingresos incluye el total", reportes.reporteIngresos(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31)).contains("S/ 1400.00"));
        verificar("Reporte de morosidad lista el contrato", reportes.reporteMorosidad(hoy).contains("contrato C1"));
        verificar("Morosidad sin mora lo indica", reportes.reporteMorosidad(LocalDate.of(2026, 1, 11)).contains("No hay contratos en mora"));
        verificar("Ocupacion 50%", reportes.reporteOcupacion().contains("OCUPACION: 50.0%"));

        pagos.guardar();
        PagoService recargado = new PagoService(new Historial());
        recargado.cargar();
        verificar("CSV: se recargan las 8 cuotas", recargado.listarCuotas().getTamano() == 8);
        verificar("CSV: se recargan los 2 pagos", recargado.listarPagos().getTamano() == 2);
        verificar("CSV: se conserva el saldo parcial", recargado.buscarCuota("C1", 2).getSaldo() == 600);
        Recibo r3 = recargado.registrarPago("C1", 2, 100, LocalDate.of(2026, 3, 21));
        verificar("CSV: el recibo sigue la numeracion (3)", r3.getNumero() == 3);

        System.out.println(fallos == 0 ? "\nTodas las pruebas pasaron." : "\nFallaron " + fallos + " pruebas.");
        if (fallos > 0) System.exit(1);
    }
}

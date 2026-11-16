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

/**
 * Pruebas del integrante 3 (cobranza, alertas, reportes) sin JUnit.
 *
 * Ejecutar desde una carpeta vacia, porque la prueba de persistencia
 * escribe en ./data/ :
 *   javac -d out $(find src test -name '*.java')
 *   java -cp out inmobiliaria.cobranza.PruebaCobranza
 */
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
        propiedades.registrarUnidad("E1", "U2", "Av. Lima 100 - 102", TipoUnidad.values()[0], 900, EstadoUnidad.NO_ALQUILADA);

        Contrato c = contratos.crear("C1", "DNI1", "U1", LocalDate.of(2026, 1, 10),
                LocalDate.of(2026, 6, 30), 1000, 10, 1000, "sin clausulas", true, false);

        // ---- RN-07: cuotas ----
        verificar("No genera cuotas de un contrato pendiente", lanza(() -> pagos.generarCuotas(c)));
        contratos.activar("C1", true);
        ListaSimple<Cuota> generadas = pagos.generarCuotas(c);
        verificar("Genera la garantia (cuota 0) y 6 cuotas mensuales (ene a jun)", generadas.getTamano() == 7);
        verificar("La primera es la garantia: cuota 0 por S/ 1000, vence al iniciar el contrato",
                generadas.obtener(0).esGarantia() && generadas.obtener(0).getMonto() == 1000
                && generadas.obtener(0).getVencimiento().equals(LocalDate.of(2026, 1, 10)));
        verificar("Primer vencimiento de renta 2026-01-10", generadas.obtener(1).getVencimiento().equals(LocalDate.of(2026, 1, 10)));
        verificar("Ultimo vencimiento 2026-06-10", generadas.obtener(6).getVencimiento().equals(LocalDate.of(2026, 6, 10)));
        verificar("La deuda de renta no incluye la garantia", pagos.deudaPendiente("C1") == 6000);
        verificar("Garantia pendiente = 1000", pagos.garantiaPendiente("C1") == 1000);
        verificar("No genera cuotas dos veces", lanza(() -> pagos.generarCuotas(c)));

        // ---- RN-09 / RN-10: pagos y recibos ----
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

        // ---- RN-08 / RN-11: mora ----
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

        // ---- Renovacion: cuotas del periodo nuevo ----
        LocalDate finAnterior = c.getFechaFin();
        contratos.renovar("C1", LocalDate.of(2026, 8, 31), 0, false);
        ListaSimple<Cuota> nuevas = pagos.generarCuotasRenovacion(c, finAnterior);
        verificar("Renovacion agrega 2 cuotas (jul, ago)", nuevas.getTamano() == 2);
        verificar("La numeracion continua en 7", nuevas.obtener(0).getNumero() == 7);
        verificar("Total de cuotas = 9 (garantia + 8 de renta)", pagos.cuotasDe("C1").getTamano() == 9);

        // ---- RN-12: alertas ----
        LocalDate cerca = LocalDate.of(2026, 8, 10);
        verificar("Alerta de contrato por vencer (fin 31-ago)", alertas.contratosPorVencer(cerca).getTamano() == 1);
        verificar("Sin alerta de vencimiento lejos del fin", alertas.contratosPorVencer(hoy).getTamano() == 0);
        ListaSimple<String> pendientes = alertas.pagosPendientes(hoy);
        verificar("Alertas de pagos: 2 cuotas vencidas", pendientes.getTamano() == 2);
        verificar("La mas atrasada aparece primero", pendientes.obtener(0).contains("cuota 2"));
        verificar("Aviso de cuota por vencer en 3 dias",
                alertas.pagosPendientes(LocalDate.of(2026, 4, 8)).obtener(
                        alertas.pagosPendientes(LocalDate.of(2026, 4, 8)).getTamano() - 1).contains("cuota 4"));

        // ---- Reportes ----
        verificar("Ingresos de enero = 1000", reportes.totalIngresos(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)) == 1000);
        verificar("Ingresos ene-mar = 1400", reportes.totalIngresos(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31)) == 1400);
        verificar("Reporte de ingresos incluye el total", reportes.reporteIngresos(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31)).contains("S/ 1400.00"));
        verificar("Reporte de morosidad lista el contrato", reportes.reporteMorosidad(hoy).contains("contrato C1"));
        verificar("Morosidad sin mora lo indica", reportes.reporteMorosidad(LocalDate.of(2026, 1, 11)).contains("No hay contratos en mora"));
        verificar("Ocupacion 50%", reportes.reporteOcupacion().contains("OCUPACION: 50.0%"));

        // ---- Persistencia (escribe en ./data/) ----
        pagos.guardar();
        PagoService recargado = new PagoService(new Historial());
        recargado.cargar();
        verificar("CSV: se recargan las 9 cuotas", recargado.listarCuotas().getTamano() == 9);
        verificar("CSV: se recargan los 2 pagos", recargado.listarPagos().getTamano() == 2);
        verificar("CSV: se conserva el saldo parcial", recargado.buscarCuota("C1", 2).getSaldo() == 600);
        Recibo r3 = recargado.registrarPago("C1", 2, 100, LocalDate.of(2026, 3, 21));
        verificar("CSV: el recibo sigue la numeracion (3)", r3.getNumero() == 3);

        // ---- Comprobante de pago (foto o PDF anexado al recibo) ----
        try {
            java.io.File foto = java.io.File.createTempFile("voucher", ".jpg");
            verificar("Un recibo nuevo no tiene comprobante", r3.getComprobante().isEmpty());
            recargado.anexarComprobante(r3.getNumero(), foto);
            verificar("El comprobante se copia como Recibo-000003.jpg",
                    inmobiliaria.nucleo.util.Archivos.nombre(r3.getComprobante()).equals("Recibo-000003.jpg")
                    && new java.io.File(r3.getComprobante()).isFile());
            verificar("Anexar el comprobante no cambia el monto ni el saldo (RN-10)",
                    r3.getMontoPagado() == 100 && r3.getSaldoCuota() == 500);
            verificar("No se anexa a un recibo inexistente", lanza(() -> recargado.anexarComprobante(99, foto)));
            recargado.guardar();
            PagoService otra = new PagoService(new Historial());
            otra.cargar();
            verificar("CSV: se conserva el comprobante", otra.buscarRecibo(3).getComprobante().equals(r3.getComprobante()));
            verificar("CSV: los recibos sin comprobante cargan vacios", otra.buscarRecibo(1).getComprobante().isEmpty());
        } catch (java.io.IOException e) {
            verificar("No se pudo crear el archivo temporal: " + e.getMessage(), false);
        }

        // ---- Garantia (cuota 0): se cobra como una cuota ----
        verificar("La garantia vencida no cuenta como atraso ni como mora",
                mora.diasAtraso(pagos.buscarCuota("C1", 0), hoy) == 0 && !mora.contratoEnMora("C0", hoy));
        Recibo rg = pagos.registrarPago("C1", 0, 400, LocalDate.of(2026, 1, 10));
        verificar("Pago parcial de la garantia deja saldo 600", pagos.garantiaPendiente("C1") == 600
                && rg.getNumeroCuota() == 0 && rg.getSaldoCuota() == 600);
        verificar("Garantia pagada = 400", pagos.garantiaPagada(c) == 400);
        verificar("No se paga mas que el saldo de la garantia", lanza(() -> pagos.registrarPago("C1", 0, 700, hoy)));
        verificar("El pago de la garantia no es ingreso (enero sigue en 1000)",
                reportes.totalIngresos(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)) == 1000);
        verificar("El reporte informa las garantias recibidas aparte",
                reportes.reporteIngresos(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)).contains("Garantias recibidas")
                && reportes.garantiasRecibidas(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)) == 400);
        pagos.registrarPago("C1", 0, 600, LocalDate.of(2026, 1, 12));
        verificar("Garantia pagada completa", pagos.garantiaPendiente("C1") == 0 && pagos.garantiaPagada(c) == 1000);
        verificar("Liquidacion con la garantia realmente pagada",
                contratos.finalizar("C1", 0, 0, 200, pagos.garantiaPagada(c)).getMontoADevolver() == 800);

        // ---- Eliminar cuota y estado automatico del arrendatario ----
        PagoService p2 = new PagoService(new Historial());
        ContratoService k2 = new ContratoService(new Historial());
        MoraService m2 = new MoraService(p2);
        propiedades.registrarUnidad("E1", "U3", "Av. Lima 100 - 103", TipoUnidad.values()[0], 800, EstadoUnidad.NO_ALQUILADA);
        Contrato d = k2.crear("D1", "DNI9", "U3", LocalDate.of(2026, 1, 10), LocalDate.of(2026, 3, 30), 800, 10, 800, "", true, false);
        k2.activar("D1");
        p2.generarCuotas(d);
        inmobiliaria.arrendatarios.servicio.ArrendatarioService ars = new inmobiliaria.arrendatarios.servicio.ArrendatarioService(new Historial());
        ars.registrar("DNI9", "Nueve", "", "");
        ars.registrar("DNI8", "Ocho", "", "");
        inmobiliaria.ui.Activacion.sincronizarEstados(ars, k2, m2);
        verificar("Con cuotas vencidas el arrendatario pasa a MOROSO solo",
                ars.buscar("DNI9").getEstado() == inmobiliaria.arrendatarios.modelo.EstadoArrendatario.MOROSO);
        verificar("Sin contratos queda INACTIVO",
                ars.buscar("DNI8").getEstado() == inmobiliaria.arrendatarios.modelo.EstadoArrendatario.INACTIVO);
        p2.registrarPago("D1", 1, 800, LocalDate.now());
        verificar("Eliminar borra todas las cuotas del contrato (garantia y 3 mensuales)", p2.eliminarCuotas("D1") == 4);
        verificar("Tambien borra sus pagos y recibos", p2.cuotasDe("D1").estaVacia()
                && p2.pagosDe("D1").estaVacia() && p2.listarRecibos().estaVacia());
        verificar("Vaciar un contrato sin cuotas no hace nada", p2.eliminarCuotas("D1") == 0);
        inmobiliaria.ui.Activacion.sincronizarEstados(ars, k2, m2);
        verificar("Sin mora y con contrato vigente vuelve a ACTIVO",
                ars.buscar("DNI9").getEstado() == inmobiliaria.arrendatarios.modelo.EstadoArrendatario.ACTIVO);
        k2.finalizar("D1", 0, 0, 0, 0);
        inmobiliaria.ui.Activacion.sincronizarEstados(ars, k2, m2);
        verificar("Con todos sus contratos finalizados queda INACTIVO",
                ars.buscar("DNI9").getEstado() == inmobiliaria.arrendatarios.modelo.EstadoArrendatario.INACTIVO);
        k2.eliminar("D1");
        verificar("Eliminar el contrato lo quita de la lista", k2.buscar("D1") == null
                && lanza(() -> k2.eliminar("D1")));

        // ---- Historial (pila) y su persistencia ----
        Historial hist = new Historial();
        hist.registrarOperacion("Primera");
        hist.registrarOperacion("Segunda; con punto y coma");
        verificar("El historial lista la mas reciente primero y no pierde operaciones",
                hist.listar().obtener(0).getDescripcion().startsWith("Segunda") && hist.getCantidadOperaciones() == 2);
        hist.guardar();
        Historial hist2 = new Historial();
        hist2.cargar();
        verificar("El historial se recarga en el mismo orden", hist2.getCantidadOperaciones() == 2
                && hist2.listar().obtener(0).getDescripcion().equals("Segunda, con punto y coma")
                && hist2.listar().obtener(1).getDescripcion().equals("Primera"));

        // ---- Exportar reportes a CSV ----
        String csvIng = reportes.csvIngresos(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        verificar("CSV de ingresos: encabezado y total", csvIng.startsWith("Contrato;Arrendatario;Pagos;Monto (S/)\n")
                && csvIng.contains("\nTOTAL;"));
        verificar("CSV de morosidad: encabezado y total", reportes.csvMorosidad(LocalDate.of(2026, 3, 20))
                .startsWith("Arrendatario;Contrato;Unidad;Deuda (S/);Recargo (S/)\n"));
        String csvOc = reportes.csvOcupacion();
        verificar("CSV de ocupacion", csvOc.startsWith("Concepto;Valor\n") && csvOc.contains("\nOcupacion (%);"));

        System.out.println(fallos == 0 ? "\nTodas las pruebas pasaron." : "\nFallaron " + fallos + " pruebas.");
        if (fallos > 0) System.exit(1);
    }
}

package inmobiliaria.nucleo;

import java.time.LocalDate;
import inmobiliaria.nucleo.estructuras.ColaPrioridad;
import inmobiliaria.nucleo.estructuras.ListaSimple;
import inmobiliaria.nucleo.util.Fechas;
import inmobiliaria.nucleo.util.Validador;
import inmobiliaria.nucleo.historial.Historial;
import inmobiliaria.contratos.modelo.Contrato;
import inmobiliaria.contratos.servicio.ContratoService;
import inmobiliaria.cobranza.servicio.PagoService;

/**
 * Pruebas de regresion sin librerias externas.
 * Ejecutar: javac -d out $(find src test -name '*.java')
 *           java -cp out inmobiliaria.nucleo.PruebaRegresion
 */
public class PruebaRegresion {
    private static int realizadas;

    private static void comprobar(boolean condicion, String mensaje) {
        realizadas++;
        if (!condicion) throw new AssertionError(mensaje);
        System.out.println("[OK] " + mensaje);
    }

    private static boolean falla(Runnable accion) {
        try { accion.run(); return false; }
        catch (IllegalArgumentException e) { return true; }
    }

    public static void main(String[] args) {
        ColaPrioridad<String> cola = new ColaPrioridad<>();
        cola.encolar("normal", 3);
        cola.encolar("urgente-1", -2);
        cola.encolar("urgente-2", -2);
        cola.encolar("media", 1);
        comprobar(cola.desencolar().equals("urgente-1"), "Prioridad minima");
        comprobar(cola.desencolar().equals("urgente-2"), "Empate por llegada");
        comprobar(cola.desencolar().equals("media"), "Orden intermedio");
        comprobar(cola.desencolar().equals("normal"), "Orden final");
        comprobar(cola.estaVacia() && cola.desencolar() == null, "Heap vacio");

        ListaSimple<Integer> lista = new ListaSimple<>();
        int suma = 0;
        for (int i = 1; i <= 100; i++) lista.agregar(i);
        for (int valor : lista) suma += valor;
        comprobar(suma == 5050 && lista.obtener(99) == 100, "Lista iterada y agregada");

        comprobar(!Validador.esMontoValido(Double.NaN), "Rechaza NaN");
        comprobar(!Validador.esMontoValido(Double.POSITIVE_INFINITY), "Rechaza infinito");
        comprobar(Fechas.convertirALocalDate("2026-02-30") == null, "Fecha invalida");
        comprobar(LocalDate.of(2024, 2, 29).equals(
                Fechas.convertirALocalDate("2024-02-29")), "Fecha bisiesta");

        Historial historial = new Historial();
        ContratoService cs = new ContratoService(historial);
        PagoService ps = new PagoService(historial);
        Contrato c = cs.crear("TEST-1", "DNI1", "U1", LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 3, 31), 1000, 5, 1000, "", true, false);
        cs.activar(c.getNumero(), true);
        ps.generarCuotas(c);
        comprobar(falla(() -> ps.registrarPago("TEST-1", 1, Double.NaN,
                LocalDate.of(2026, 1, 5))), "Pago NaN rechazado");
        comprobar(falla(() -> ps.registrarPago("TEST-1", 1, Double.POSITIVE_INFINITY,
                LocalDate.of(2026, 1, 5))), "Pago infinito rechazado");
        comprobar(ps.buscarCuota("TEST-1", 1).getSaldo() == 1000,
                "Un pago invalido no modifica el saldo");
        LocalDate finAnterior = c.getFechaFin();
        cs.renovar("TEST-1", LocalDate.of(2026, 5, 31), 0, false);
        comprobar(ps.generarCuotasRenovacion(c, finAnterior).getTamano() == 2,
                "Renovacion valida agrega dos cuotas");
        comprobar(falla(() -> ps.generarCuotasRenovacion(c, finAnterior)),
                "Renovacion repetida rechazada");
        comprobar(ps.cuotasDe("TEST-1").getTamano() == 5,
                "No se duplicaron cuotas");
        System.out.println("Pruebas completadas: " + realizadas);
    }
}

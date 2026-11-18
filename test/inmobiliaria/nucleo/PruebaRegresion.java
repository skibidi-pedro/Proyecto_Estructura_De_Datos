package inmobiliaria.nucleo;

import java.time.LocalDate;
import inmobiliaria.cobranza.modelo.Cuota;
import inmobiliaria.cobranza.servicio.PagoService;
import inmobiliaria.contratos.modelo.Contrato;
import inmobiliaria.contratos.modelo.EstadoContrato;
import inmobiliaria.contratos.servicio.ContratoService;
import inmobiliaria.nucleo.estructuras.ListaSimple;
import inmobiliaria.nucleo.historial.Historial;
import inmobiliaria.nucleo.persistencia.GestorCSV;
import inmobiliaria.nucleo.persistencia.Persistible;
import inmobiliaria.nucleo.util.Fechas;
import inmobiliaria.nucleo.util.Validador;
import inmobiliaria.propiedades.modelo.EstadoUnidad;
import inmobiliaria.propiedades.modelo.TipoUnidad;
import inmobiliaria.propiedades.modelo.Unidad;
import inmobiliaria.propiedades.servicio.PropiedadService;
import inmobiliaria.ui.Activacion;

/**
 * Regresiones de la revision de ajuste-menu-principal; sin librerias de pruebas.
 * Ejecutar desde un directorio temporal para no sobrescribir ./data de la aplicacion.
 */
public class PruebaRegresion {
    private static int verificaciones;

    private static void comprobar(boolean condicion, String descripcion) {
        verificaciones++;
        if (!condicion) throw new AssertionError(descripcion);
        System.out.println("[OK] " + descripcion);
    }

    private static boolean rechaza(Runnable accion) {
        try {
            accion.run();
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    public static void main(String[] args) {
        ListaSimple<Integer> lista = new ListaSimple<>();
        for (int i = 1; i <= 100; i++) lista.agregar(i);
        int suma = 0;
        for (int n : lista) suma += n;
        comprobar(suma == 5050 && lista.obtener(99) == 100, "Lista enlazada e iterador");

        comprobar(!Validador.esMontoValido(Double.NaN), "Monto NaN invalido");
        comprobar(!Validador.esMontoValido(Double.POSITIVE_INFINITY), "Monto infinito invalido");
        comprobar(Fechas.convertirALocalDate("2026-02-30") == null, "Fecha inexistente rechazada");
        comprobar(LocalDate.of(2024, 2, 29).equals(Fechas.convertirALocalDate("2024-02-29")),
                "Fecha bisiesta valida");

        Historial h = new Historial();
        PropiedadService propiedades = new PropiedadService(h);
        propiedades.registrarPropietario("PRO-1", "Propietario");
        propiedades.registrarEdificio("PRO-1", "EDI-1", "Edificio", "Av. Lima");
        Unidad unidad = propiedades.registrarUnidad("EDI-1", "UNI-1", "Unidad 1",
                TipoUnidad.values()[0], 1000, EstadoUnidad.NO_ALQUILADA);
        ContratoService contratos = new ContratoService(h);
        PagoService pagos = new PagoService(h);
        Contrato c = contratos.crear("CON-1", "DNI-1", "UNI-1", LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 3, 31), 1000, 5, 1000, "", true, false);

        // Simula un CSV inconsistente: un contrato pendiente con cuotas ya existentes.
        pagos.listarCuotas().agregar(new Cuota(c.getNumero(), 1, LocalDate.of(2026, 1, 5), 1000));
        comprobar(rechaza(() -> Activacion.activar(c, contratos, propiedades, pagos)),
                "No activa si ya existen cuotas");
        comprobar(c.getEstado() == EstadoContrato.PENDIENTE
                && unidad.getEstado() == EstadoUnidad.NO_ALQUILADA,
                "Activacion fallida deja estados intactos");
        pagos.eliminarCuotas(c.getNumero());

        ListaSimple<Cuota> generadas = Activacion.activar(c, contratos, propiedades, pagos);
        comprobar(c.getEstado() == EstadoContrato.VIGENTE
                && unidad.getEstado() == EstadoUnidad.ALQUILADA,
                "Activacion coherente");
        comprobar(generadas.getTamano() == 4, "Tres cuotas mensuales y garantia");

        comprobar(rechaza(() -> pagos.registrarPago("CON-1", 1, Double.NaN,
                LocalDate.of(2026, 1, 5))), "Pago NaN rechazado");
        comprobar(rechaza(() -> pagos.registrarPago("CON-1", 1, Double.POSITIVE_INFINITY,
                LocalDate.of(2026, 1, 5))), "Pago infinito rechazado");
        comprobar(pagos.buscarCuota("CON-1", 1).getSaldo() == 1000
                && pagos.listarRecibos().estaVacia(), "Pagos invalidos no modifican datos");

        LocalDate finAnterior = c.getFechaFin();
        contratos.renovar("CON-1", LocalDate.of(2026, 5, 31), 0, false);
        comprobar(pagos.generarCuotasRenovacion(c, finAnterior).getTamano() == 2,
                "Renovacion agrega cuotas nuevas");
        comprobar(rechaza(() -> pagos.generarCuotasRenovacion(c, finAnterior)),
                "No genera dos veces las cuotas de renovacion");
        comprobar(pagos.cuotasDe("CON-1").getTamano() == 6, "No hay cuotas duplicadas");
        comprobar(rechaza(() -> contratos.finalizar("CON-1", 0, 0, -1, 0)),
                "No liquida daños negativos");
        comprobar(c.getEstado() == EstadoContrato.VIGENTE,
                "Liquidacion invalida no finaliza contrato");

        ListaSimple<Persistible> paraGuardar = new ListaSimple<>();
        paraGuardar.agregar(c);
        GestorCSV.guardar("regresion-contratos.csv", paraGuardar);
        ListaSimple<String> lineas = new ListaSimple<>();
        GestorCSV.cargar("regresion-contratos.csv", lineas);
        comprobar(lineas.getTamano() == 1 && lineas.obtener(0).startsWith("CON-1;"),
                "Archivo CSV se guarda y carga");

        System.out.println("Pruebas de regresion superadas: " + verificaciones);
    }
}

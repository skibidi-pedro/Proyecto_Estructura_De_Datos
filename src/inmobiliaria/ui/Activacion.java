package inmobiliaria.ui;

import inmobiliaria.cobranza.modelo.Cuota;
import inmobiliaria.cobranza.servicio.PagoService;
import inmobiliaria.contratos.modelo.Contrato;
import inmobiliaria.contratos.modelo.EstadoContrato;
import inmobiliaria.contratos.servicio.ContratoService;
import inmobiliaria.documentos.servicio.ExpedienteService;
import inmobiliaria.nucleo.estructuras.ListaSimple;
import inmobiliaria.propiedades.modelo.EstadoUnidad;
import inmobiliaria.propiedades.servicio.PropiedadService;

/**
 * Activacion de un contrato (la comparten PanelContratos y PanelDocumentos).
 *
 * ContratoService no conoce a los demas modulos, asi que aqui se hace el pegado:
 *  - expediente completo (RN-06)  -> ExpedienteService
 *  - unidad pasa a ALQUILADA      -> PropiedadService
 *  - cuotas mensuales (RN-07)     -> PagoService
 */
public class Activacion {

    // Activa el contrato y devuelve las cuotas generadas. Lanza IllegalArgumentException si no se puede.
    public static ListaSimple<Cuota> activar(Contrato c, ContratoService contratos,
                                             PropiedadService propiedades, PagoService pagos,
                                             ExpedienteService expedientes) {
        String doc = c.getDocumentoArrendatario();
        if (!expedientes.expedienteCompleto(doc))
            throw new IllegalArgumentException("El expediente de " + doc + " esta incompleto. Falta: "
                    + expedientes.faltantes(doc) + ".\nRegistrelo en el modulo Documentos.");
        contratos.activar(c.getNumero(), true);
        propiedades.cambiarEstadoUnidad(c.getCodigoUnidad(), EstadoUnidad.ALQUILADA);
        return pagos.generarCuotas(c);
    }

    // Cuotas de renta de una lista (sin la garantia, que es la cuota 0)
    public static int mensuales(ListaSimple<Cuota> cuotas) {
        int n = 0;
        for (int i = 0; i < cuotas.getTamano(); i++) {
            if (!cuotas.obtener(i).esGarantia()) n++;
        }
        return n;
    }

    // Texto para el mensaje de contrato activado
    public static String resumen(Contrato c, ListaSimple<Cuota> cuotas) {
        String t = "Se generaron " + mensuales(cuotas) + " cuotas mensuales.";
        if (c.getGarantia() > 0)
            t += "\nLa garantia (S/ " + Dialogos.dinero(c.getGarantia())
                    + ") quedo pendiente de cobro como cuota 0 en Cobro de rentas y mora.";
        return t;
    }

    // Contratos PENDIENTES de un arrendatario (los que esperan su expediente)
    public static ListaSimple<Contrato> pendientesDe(String documentoArrendatario, ContratoService contratos) {
        ListaSimple<Contrato> r = new ListaSimple<>();
        ListaSimple<Contrato> todos = contratos.listar();
        for (int i = 0; i < todos.getTamano(); i++) {
            Contrato c = todos.obtener(i);
            if (c.getEstado() == EstadoContrato.PENDIENTE
                    && c.getDocumentoArrendatario().equals(documentoArrendatario)) r.agregar(c);
        }
        return r;
    }
}

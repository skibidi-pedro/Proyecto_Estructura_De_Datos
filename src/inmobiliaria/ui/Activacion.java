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

package inmobiliaria.ui;

import inmobiliaria.cobranza.servicio.MoraService;
import inmobiliaria.arrendatarios.servicio.ArrendatarioService;
import inmobiliaria.arrendatarios.modelo.EstadoArrendatario;
import inmobiliaria.arrendatarios.modelo.Arrendatario;
import java.awt.Component;
import inmobiliaria.cobranza.modelo.Cuota;
import inmobiliaria.cobranza.servicio.PagoService;
import inmobiliaria.contratos.modelo.Contrato;
import inmobiliaria.contratos.modelo.EstadoContrato;
import inmobiliaria.contratos.servicio.ContratoService;
import inmobiliaria.documentos.servicio.ExpedienteService;
import inmobiliaria.nucleo.estructuras.ListaSimple;
import inmobiliaria.propiedades.modelo.EstadoUnidad;
import inmobiliaria.propiedades.modelo.Unidad;
import inmobiliaria.propiedades.servicio.PropiedadService;

/**
 * Activacion de un contrato (la comparten PanelContratos y PanelArrendatarios).
 *
 * ContratoService no conoce a los demas modulos, asi que aqui se hace el pegado:
 *  - documentos (RN-06)           -> documento de identidad del arrendatario (ExpedienteService)
 *                                    y documento del contrato (Contrato.getDocumento). Ya no son
 *                                    obligatorios: se puede activar sin ellos, avisando, y la
 *                                    interfaz los resalta en amarillo mientras falten.
 *  - unidad pasa a ALQUILADA      -> PropiedadService
 *  - cuotas mensuales y garantia  -> PagoService (RN-07)
 */
public class Activacion {

    // Que documentos faltan para activar (texto vacio = nada, el contrato esta listo)
    public static String faltantes(Contrato c, ExpedienteService expedientes) {
        StringBuilder sb = new StringBuilder();
        if (!expedientes.expedienteCompleto(c.getDocumentoArrendatario()))
            sb.append("el documento de identidad del arrendatario, que se anexa en Arrendatarios");
        if (c.getDocumento().isEmpty()) {
            if (sb.length() > 0) sb.append(" y ");
            sb.append("el documento del contrato, que se anexa con Anexar documento en Contratos");
        }
        return sb.toString();
    }

    // Activa el contrato y devuelve las cuotas generadas. Lanza IllegalArgumentException si no se puede.
    // No exige documentos: quien llama ya pregunto al usuario si faltaban (ver PanelContratos).
    public static ListaSimple<Cuota> activar(Contrato c, ContratoService contratos,
                                             PropiedadService propiedades, PagoService pagos) {
        if (c == null || c.getEstado() != EstadoContrato.PENDIENTE)
            throw new IllegalArgumentException("Solo se puede activar un contrato pendiente");
        Unidad unidad = propiedades.buscarUnidad(c.getCodigoUnidad());
        if (unidad == null || unidad.getEstado() != EstadoUnidad.NO_ALQUILADA)
            throw new IllegalArgumentException("La unidad ya esta alquilada o no existe");

        // Todo lo que se pueda validar se comprueba ANTES de cambiar estados.
        pagos.validarGeneracionCuotas(c);
        contratos.activar(c.getNumero());
        try {
            ListaSimple<Cuota> nuevas = pagos.generarCuotas(c);
            propiedades.cambiarEstadoUnidad(c.getCodigoUnidad(), EstadoUnidad.ALQUILADA);
            return nuevas;
        } catch (RuntimeException ex) {
            // Evita dejar el contrato activo sin cuotas o la unidad alquilada a medias.
            try {
                if (!pagos.cuotasDe(c.getNumero()).estaVacia()) pagos.eliminarCuotas(c.getNumero());
            } catch (RuntimeException falloLimpieza) {
                ex.addSuppressed(falloLimpieza);
            }
            c.setEstado(EstadoContrato.PENDIENTE);
            unidad.setEstado(EstadoUnidad.NO_ALQUILADA);
            throw new IllegalArgumentException("No se completo la activacion: " + ex.getMessage(), ex);
        }
    }

    // Documentos registrados: por cada contrato PENDIENTE del arrendatario que ya tiene todos sus
    // documentos pregunta "activar? Si / No" y, si dice que si, lo activa.
    public static void ofrecer(Component padre, String documentoArrendatario, String nombreArrendatario,
                               ContratoService contratos, PropiedadService propiedades,
                               PagoService pagos, ExpedienteService expedientes) {
        ListaSimple<Contrato> pendientes = pendientesDe(documentoArrendatario, contratos);
        for (int i = 0; i < pendientes.getTamano(); i++) {
            Contrato c = pendientes.obtener(i);
            if (!faltantes(c, expedientes).isEmpty()) continue;
            Unidad u = propiedades.buscarUnidad(c.getCodigoUnidad());
            String donde = u == null ? "(unidad dada de baja)" : u.getDireccion();
            if (!Dialogos.confirmar(padre, "Documentos registrados",
                    "Los documentos de " + nombreArrendatario + " estan registrados.\n"
                    + "Activar su contrato de " + donde + "?")) continue;
            try {
                ListaSimple<Cuota> cuotas = activar(c, contratos, propiedades, pagos);
                Dialogos.info(padre, "Contrato", resumen(c, cuotas));
            } catch (IllegalArgumentException ex) {
                Dialogos.error(padre, ex.getMessage());
            }
        }
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
        return "Se generaron " + mensuales(cuotas) + " cuotas mensuales"
                + (c.getGarantia() > 0 ? " y la garantia" : "");
    }

    // Contratos PENDIENTES de un arrendatario (los que esperan sus documentos)
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

    // Asignacion automatica del estado de los arrendatarios segun sus contratos y su cobranza:
    // MOROSO si tiene cuotas en mora; ACTIVO si tiene un contrato pendiente o vigente;
    // INACTIVO si no tiene contratos o todos estan finalizados.
    public static void sincronizarEstados(ArrendatarioService arrendatarios, ContratoService contratos,
                                          MoraService mora) {
        ListaSimple<Contrato> todos = contratos.listar();
        ListaSimple<Arrendatario> lista = arrendatarios.listar();
        for (int i = 0; i < lista.getTamano(); i++) {
            Arrendatario a = lista.obtener(i);
            String doc = a.getDocumentoIdentidad();
            boolean tiene = false, abierto = false;
            for (int j = 0; j < todos.getTamano(); j++) {
                Contrato c = todos.obtener(j);
                if (!c.getDocumentoArrendatario().equals(doc)) continue;
                tiene = true;
                if (c.getEstado() != EstadoContrato.FINALIZADO) abierto = true;
            }
            EstadoArrendatario nuevo = mora.esMoroso(doc, todos, java.time.LocalDate.now()) ? EstadoArrendatario.MOROSO
                    : (tiene && abierto) ? EstadoArrendatario.ACTIVO : EstadoArrendatario.INACTIVO;
            if (a.getEstado() != nuevo) arrendatarios.cambiarEstado(doc, nuevo);
        }
    }
}

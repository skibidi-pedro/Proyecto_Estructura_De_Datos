package inmobiliaria.propiedades.servicio;

import inmobiliaria.nucleo.estructuras.ListaSimple;
import inmobiliaria.nucleo.historial.Historial;
import inmobiliaria.nucleo.persistencia.GestorCSV;
import inmobiliaria.nucleo.persistencia.Persistible;
import inmobiliaria.nucleo.util.Validador;
import inmobiliaria.propiedades.estructuras.ArbolAVL;
import inmobiliaria.propiedades.estructuras.ArbolGeneral;
import inmobiliaria.propiedades.modelo.Edificio;
import inmobiliaria.propiedades.modelo.EstadoUnidad;
import inmobiliaria.propiedades.modelo.Propietario;
import inmobiliaria.propiedades.modelo.TipoUnidad;
import inmobiliaria.propiedades.modelo.Unidad;

/**
 * CRUD de propietarios, edificios y unidades; busqueda por codigo, direccion, tipo, precio o estado (2.1).
 *
 * Se usan dos estructuras a la vez:
 *  - un AVL por cada entidad, para buscar por codigo en O(log n) y listar ordenado;
 *  - un arbol general (Propietario > Edificio > Unidad) para saber que cuelga de que.
 *
 * Responsable: Integrante 2 - Zavaleta Calderon Steven Jack
 * Reglas: RN-01, RN-02
 */
public class PropiedadService {
    private ArbolAVL<String, Propietario> propietarios = new ArbolAVL<>();
    private ArbolAVL<String, Edificio> edificios = new ArbolAVL<>();
    private ArbolAVL<String, Unidad> unidades = new ArbolAVL<>();
    private ArbolGeneral<Object> jerarquia = new ArbolGeneral<>();
    private Historial historial;

    public PropiedadService(Historial historial) {
        this.historial = historial;
    }

    // =====================================================
    //  PROPIETARIOS
    // =====================================================
    public Propietario registrarPropietario(String codigo, String nombre) {
        if (!Validador.esTextoValido(codigo) || !Validador.esTextoValido(nombre))
            throw new IllegalArgumentException("Codigo y nombre del propietario son obligatorios");
        codigo = codigo.trim();
        if (propietarios.buscar(codigo) != null)                       // RN-01
            throw new IllegalArgumentException("Ya existe el propietario " + codigo);

        Propietario p = new Propietario(codigo, nombre);
        propietarios.insertar(codigo, p);
        jerarquia.agregar(null, p);
        historial.registrarOperacion("Propietario registrado: " + codigo);
        return p;
    }

    public void modificarPropietario(String codigo, String nuevoNombre) {
        if (!Validador.esTextoValido(nuevoNombre))
            throw new IllegalArgumentException("El nombre es obligatorio");
        buscarPropietarioObligatorio(codigo).setNombre(nuevoNombre);
        historial.registrarOperacion("Propietario modificado: " + codigo);
    }

    public void bajaPropietario(String codigo) {
        Propietario p = buscarPropietarioObligatorio(codigo);
        if (jerarquia.hijosDe(p).getTamano() > 0)
            throw new IllegalArgumentException("El propietario todavia tiene edificios");
        jerarquia.quitar(p);
        propietarios.eliminar(p.getCodigo());
        historial.registrarOperacion("Propietario dado de baja: " + codigo);
    }

    public Propietario buscarPropietario(String codigo) {
        return propietarios.buscar(codigo);
    }

    public ListaSimple<Propietario> listarPropietarios() {
        return propietarios.enOrden();
    }

    // =====================================================
    //  EDIFICIOS
    // =====================================================
    public Edificio registrarEdificio(String codigoPropietario, String codigo,
                                      String nombre, String direccion) {
        Propietario p = buscarPropietarioObligatorio(codigoPropietario);
        if (!Validador.esTextoValido(codigo) || !Validador.esTextoValido(nombre)
                || !Validador.esTextoValido(direccion))
            throw new IllegalArgumentException("Codigo, nombre y direccion del edificio son obligatorios");
        codigo = codigo.trim();
        if (edificios.buscar(codigo) != null)
            throw new IllegalArgumentException("Ya existe el edificio " + codigo);

        Edificio e = new Edificio(codigo, p.getCodigo(), nombre, direccion);
        edificios.insertar(codigo, e);
        jerarquia.agregar(p, e);
        historial.registrarOperacion("Edificio registrado: " + codigo);
        return e;
    }

    public void modificarEdificio(String codigo, String nuevoNombre, String nuevaDireccion) {
        if (!Validador.esTextoValido(nuevoNombre) || !Validador.esTextoValido(nuevaDireccion))
            throw new IllegalArgumentException("Nombre y direccion son obligatorios");
        Edificio e = buscarEdificioObligatorio(codigo);
        e.setNombre(nuevoNombre);
        e.setDireccion(nuevaDireccion);
        historial.registrarOperacion("Edificio modificado: " + codigo);
    }

    public void bajaEdificio(String codigo) {
        Edificio e = buscarEdificioObligatorio(codigo);
        if (jerarquia.hijosDe(e).getTamano() > 0)
            throw new IllegalArgumentException("El edificio todavia tiene unidades");
        jerarquia.quitar(e);
        edificios.eliminar(e.getCodigo());
        historial.registrarOperacion("Edificio dado de baja: " + codigo);
    }

    public Edificio buscarEdificio(String codigo) {
        return edificios.buscar(codigo);
    }

    public ListaSimple<Edificio> listarEdificios() {
        return edificios.enOrden();
    }

    public ListaSimple<Edificio> edificiosDe(String codigoPropietario) {
        ListaSimple<Edificio> resultado = new ListaSimple<>();
        Propietario p = buscarPropietarioObligatorio(codigoPropietario);
        ListaSimple<Object> hijos = jerarquia.hijosDe(p);
        for (int i = 0; i < hijos.getTamano(); i++) {
            resultado.agregar((Edificio) hijos.obtener(i));
        }
        return resultado;
    }

    // =====================================================
    //  UNIDADES
    // =====================================================
    public Unidad registrarUnidad(String codigoEdificio, String codigo, String direccion,
                                  TipoUnidad tipo, double precio, EstadoUnidad estadoInicial) {
        Edificio e = buscarEdificioObligatorio(codigoEdificio);
        if (!Validador.esTextoValido(codigo) || !Validador.esTextoValido(direccion))
            throw new IllegalArgumentException("Codigo y direccion de la unidad son obligatorios");
        if (tipo == null || estadoInicial == null)
            throw new IllegalArgumentException("Falta el tipo o el estado de la unidad");
        if (!Validador.esMontoValido(precio))
            throw new IllegalArgumentException("El precio debe ser mayor a cero");
        codigo = codigo.trim();
        if (unidades.buscar(codigo) != null)                           // RN-01
            throw new IllegalArgumentException("Ya existe la unidad " + codigo);

        Unidad u = new Unidad(codigo, e.getCodigo(), direccion, tipo, precio, estadoInicial);
        unidades.insertar(codigo, u);
        jerarquia.agregar(e, u);
        historial.registrarOperacion("Unidad registrada: " + codigo);
        return u;
    }

    public void modificarUnidad(String codigo, String nuevaDireccion, TipoUnidad nuevoTipo, double nuevoPrecio) {
        if (!Validador.esTextoValido(nuevaDireccion) || nuevoTipo == null)
            throw new IllegalArgumentException("Direccion y tipo son obligatorios");
        if (!Validador.esMontoValido(nuevoPrecio))
            throw new IllegalArgumentException("El precio debe ser mayor a cero");
        Unidad u = buscarUnidadObligatoria(codigo);
        u.setDireccion(nuevaDireccion);
        u.setTipo(nuevoTipo);
        u.setPrecio(nuevoPrecio);
        historial.registrarOperacion("Unidad modificada: " + codigo);
    }

    // Lo usa el modulo de contratos: ALQUILADA al activar, DISPONIBLE al finalizar
    public void cambiarEstadoUnidad(String codigo, EstadoUnidad nuevoEstado) {
        if (nuevoEstado == null)
            throw new IllegalArgumentException("Falta el estado");
        buscarUnidadObligatoria(codigo).setEstado(nuevoEstado);
        historial.registrarOperacion("Unidad " + codigo + " ahora esta " + nuevoEstado);
    }

    public void bajaUnidad(String codigo) {
        Unidad u = buscarUnidadObligatoria(codigo);
        if (u.getEstado() == EstadoUnidad.ALQUILADA)
            throw new IllegalArgumentException("No se puede dar de baja una unidad alquilada");
        jerarquia.quitar(u);
        unidades.eliminar(u.getCodigo());
        historial.registrarOperacion("Unidad dada de baja: " + codigo);
    }

    public Unidad buscarUnidad(String codigo) {
        return unidades.buscar(codigo);
    }

    // RN-02: solo las disponibles pueden recibir un contrato nuevo
    public boolean unidadDisponible(String codigo) {
        Unidad u = unidades.buscar(codigo);
        return u != null && u.getEstado() == EstadoUnidad.DISPONIBLE;
    }

    public ListaSimple<Unidad> listarUnidades() {
        return unidades.enOrden();
    }

    public ListaSimple<Unidad> unidadesDe(String codigoEdificio) {
        ListaSimple<Unidad> resultado = new ListaSimple<>();
        Edificio e = buscarEdificioObligatorio(codigoEdificio);
        ListaSimple<Object> hijos = jerarquia.hijosDe(e);
        for (int i = 0; i < hijos.getTamano(); i++) {
            resultado.agregar((Unidad) hijos.obtener(i));
        }
        return resultado;
    }

    public ListaSimple<Unidad> unidadesDePropietario(String codigoPropietario) {
        ListaSimple<Unidad> resultado = new ListaSimple<>();
        ListaSimple<Edificio> lista = edificiosDe(codigoPropietario);
        for (int i = 0; i < lista.getTamano(); i++) {
            ListaSimple<Unidad> deEdificio = unidadesDe(lista.obtener(i).getCodigo());
            for (int j = 0; j < deEdificio.getTamano(); j++) {
                resultado.agregar(deEdificio.obtener(j));
            }
        }
        return resultado;
    }

    // =====================================================
    //  BUSQUEDAS DE UNIDADES (direccion, tipo, precio, estado)
    // =====================================================
    public ListaSimple<Unidad> buscarPorDireccion(String texto) {
        ListaSimple<Unidad> resultado = new ListaSimple<>();
        ListaSimple<Unidad> todas = unidades.enOrden();
        String buscado = texto.trim().toLowerCase();
        for (int i = 0; i < todas.getTamano(); i++) {
            Unidad u = todas.obtener(i);
            if (u.getDireccion().toLowerCase().contains(buscado)) resultado.agregar(u);
        }
        return resultado;
    }

    public ListaSimple<Unidad> buscarPorTipo(TipoUnidad tipo) {
        ListaSimple<Unidad> resultado = new ListaSimple<>();
        ListaSimple<Unidad> todas = unidades.enOrden();
        for (int i = 0; i < todas.getTamano(); i++) {
            if (todas.obtener(i).getTipo() == tipo) resultado.agregar(todas.obtener(i));
        }
        return resultado;
    }

    public ListaSimple<Unidad> buscarPorEstado(EstadoUnidad estado) {
        ListaSimple<Unidad> resultado = new ListaSimple<>();
        ListaSimple<Unidad> todas = unidades.enOrden();
        for (int i = 0; i < todas.getTamano(); i++) {
            if (todas.obtener(i).getEstado() == estado) resultado.agregar(todas.obtener(i));
        }
        return resultado;
    }

    // Unidades con precio entre min y max (inclusive)
    public ListaSimple<Unidad> buscarPorPrecio(double min, double max) {
        ListaSimple<Unidad> resultado = new ListaSimple<>();
        ListaSimple<Unidad> todas = unidades.enOrden();
        for (int i = 0; i < todas.getTamano(); i++) {
            Unidad u = todas.obtener(i);
            if (u.getPrecio() >= min && u.getPrecio() <= max) resultado.agregar(u);
        }
        return resultado;
    }

    // =====================================================
    //  PERSISTENCIA (propietarios.csv, edificios.csv, unidades.csv)
    // =====================================================
    public void cargar() {
        ListaSimple<String> lineas = new ListaSimple<>();
        GestorCSV.cargar("propietarios.csv", lineas);
        for (int i = 0; i < lineas.getTamano(); i++) {
            Propietario p = new Propietario();
            p.desdeLineaCSV(lineas.obtener(i));
            propietarios.insertar(p.getCodigo(), p);
            jerarquia.agregar(null, p);
        }

        lineas = new ListaSimple<>();
        GestorCSV.cargar("edificios.csv", lineas);
        for (int i = 0; i < lineas.getTamano(); i++) {
            Edificio e = new Edificio();
            e.desdeLineaCSV(lineas.obtener(i));
            Propietario p = propietarios.buscar(e.getCodigoPropietario());
            if (p == null) continue;               // linea huerfana, la ignoro
            edificios.insertar(e.getCodigo(), e);
            jerarquia.agregar(p, e);
        }

        lineas = new ListaSimple<>();
        GestorCSV.cargar("unidades.csv", lineas);
        for (int i = 0; i < lineas.getTamano(); i++) {
            Unidad u = new Unidad();
            u.desdeLineaCSV(lineas.obtener(i));
            Edificio e = edificios.buscar(u.getCodigoEdificio());
            if (e == null) continue;
            unidades.insertar(u.getCodigo(), u);
            jerarquia.agregar(e, u);
        }
    }

    public void guardar() {
        guardarLista("propietarios.csv", propietarios.enOrden());
        guardarLista("edificios.csv", edificios.enOrden());
        guardarLista("unidades.csv", unidades.enOrden());
    }

    // GestorCSV pide ListaSimple<Persistible>, asi que copio
    private <T extends Persistible> void guardarLista(String archivo, ListaSimple<T> origen) {
        ListaSimple<Persistible> lista = new ListaSimple<>();
        for (int i = 0; i < origen.getTamano(); i++) {
            lista.agregar(origen.obtener(i));
        }
        GestorCSV.guardar(archivo, lista);
    }

    // =====================================================
    private Propietario buscarPropietarioObligatorio(String codigo) {
        Propietario p = (codigo == null) ? null : propietarios.buscar(codigo.trim());
        if (p == null) throw new IllegalArgumentException("No existe el propietario " + codigo);
        return p;
    }

    private Edificio buscarEdificioObligatorio(String codigo) {
        Edificio e = (codigo == null) ? null : edificios.buscar(codigo.trim());
        if (e == null) throw new IllegalArgumentException("No existe el edificio " + codigo);
        return e;
    }

    private Unidad buscarUnidadObligatoria(String codigo) {
        Unidad u = (codigo == null) ? null : unidades.buscar(codigo.trim());
        if (u == null) throw new IllegalArgumentException("No existe la unidad " + codigo);
        return u;
    }
}
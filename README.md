# Sistema de Gestión Inmobiliaria y Alquileres — Grupo 6

Proyecto de Estructura de Datos (Java). Docente: Arredondo Castillo Gustavo.

## Estructura

```
src/inmobiliaria/
├── Main.java, MenuPrincipal.java        → Integrante 1
├── nucleo/        (estructuras, historial, persistencia, util)   → Integrante 1
├── propiedades/   (estructuras AVL/General, modelo, servicio)    → Integrante 2
├── contratos/     (modelo, servicio)                             → Integrante 2
├── arrendatarios/ (estructuras TablaHash, modelo, servicio)      → Integrante 1
├── documentos/    (modelo, servicio)                             → Integrante 2
├── cobranza/      (modelo, servicio)                             → Integrante 3
├── alertas/                                                      → Integrante 3
└── reportes/                                                     → Integrante 3
test/inmobiliaria/   → pruebas, misma división por módulo
data/                → archivos CSV (persistencia, sección 2.8)
docs/                → documentación y plan de pruebas
```

## Responsables

| # | Integrante | Paquetes | Secciones | Reglas |
|---|------------|----------|-----------|--------|
| 1 | Francia Ramos Johan Mitziu (líder técnico) | `Main`, `nucleo.*`, `arrendatarios.*` | 2.7, 2.8, 2.2 | RN-01 |
| 2 | Zavaleta Calderon Steven Jack | `propiedades.*`, `contratos.*`, `documentos.*` | 2.1, 2.3, 2.4 | RN-02, 03, 04, 05, 06, 12, 13 |
| 3 | Pucho Lizana Pedro Eduardo Mar | `cobranza.*`, `alertas`, `reportes` | 2.5 + alertas y reportes | RN-07, 08, 09, 10, 11 |

## Estructuras de datos por módulo

- **Integrante 1:** lista simple, lista doble, pila, cola, cola de prioridad (lista enlazada ordenada por prioridad), tabla hash con encadenamiento (arrendatarios).
- **Integrante 2:** árbol AVL genérico, árbol general (Propietario > Edificio > Unidad).
- **Integrante 3:** usa las estructuras del núcleo (cola de prioridad para cobros urgentes).

## Compilar

```
javac -d out $(find src -name '*.java')
java -cp out inmobiliaria.Main
```

## Ejecutar pruebas

Las pruebas escriben en `./data/`, asi que se ejecutan desde una carpeta vacia:

```
javac -d out $(find src test -name '*.java')
java -cp out inmobiliaria.cobranza.PruebaCobranza
java -cp out inmobiliaria.arrendatarios.PruebaModulos
```

## Modulos del menu

Unidades, Arrendatarios, Contratos y Cobro de rentas y mora se abren como paneles a la derecha
de la ventana. El menu Reportes muestra Ingresos, Morosidad, Ocupacion y Alertas.

- **Documentos anexados:** se aceptan PDF, Word (doc, docx) y fotos (jpg, png). Se copian a
  `data/archivos/`; en las tablas la columna **Archivo** muestra el nombre (`Identidad.jpg`,
  `Contrato.pdf`) y con **doble clic** en la fila se abre con el programa del sistema.
  - **Arrendatarios:** "Nuevo" y "Editar" permiten anexar el documento de identidad.
  - **Contratos:** "Nuevo contrato" permite anexar el documento del contrato; despues se puede
    agregar o cambiar con "Anexar documento".
- **Activacion (RN-06):** un contrato se activa cuando tiene el documento de identidad del
  arrendatario y el documento del contrato. Al completarse el segundo, el sistema pregunta
  "Los documentos estan registrados. Activar el contrato? Si / No". Tambien se puede activar
  a mano con el boton Activar. Tambien se puede activar SIN documentos: el sistema no
  advierte y el contrato queda resaltado en amarillo hasta que se anexen.
- **Estado del arrendatario (automatico):** MOROSO si tiene cuotas en mora, ACTIVO si tiene un contrato pendiente o vigente,
  INACTIVO si no tiene contratos o todos estan finalizados. Ya no se cambia a mano.
- **Eliminar contrato:** el boton Eliminar de Contratos borra el contrato con su documento, cuotas, pagos y
  recibos (con aviso breve) y libera la unidad. En Cobro solo se agregan comprobantes.
- **Busqueda y filtros:** Unidades y Contratos tienen un campo Buscar (en Contratos tambien por nombre del
  arrendatario) y Cobro tiene un filtro de cuotas por estado.
- **Historial de operaciones:** menu a la derecha de Reportes; la operacion mas reciente va primero, se guarda
  en `data/historial.csv` y se puede buscar.
- **Exportar reportes:** Ingresos, Morosidad y Ocupacion tienen el boton Exportar CSV (separador `;`, abre en Excel).
- **Estados de unidad:** `NO ALQUILADA` y `ALQUILADA` (cambian solos al activar o finalizar un contrato).
- **Colores en las tablas:**
  - Archivo (documento de identidad / del contrato): verde si esta, amarillo si falta.
  - Estado del arrendatario: ACTIVO verde, MOROSO amarillo, INACTIVO rojo.
  - Estado del contrato: VIGENTE verde (amarillo si le faltan documentos), PENDIENTE amarillo, FINALIZADO rojo.
  - Unidades: NO ALQUILADA naranja, ALQUILADA verde claro.
  - Cobro: Pagada verde, atrasada amarilla; EN MORA con estado rojo y los dias de atraso en gradiente de rojos.
  - Cobro de rentas y mora: fila verde (claro) si esta pagada, amarilla si esta atrasada.
- **Recibos:** al registrar un pago se puede anexar el comprobante (foto o PDF). En "Ver recibos",
  doble clic abre el comprobante y "Anexar comprobante" lo agrega o reemplaza despues.
- **Garantia:** al activar un contrato con garantia se crea la cuota 0 ("Garantia"), que vence el dia
  de inicio y se cobra en Cobro de rentas y mora como cualquier cuota (pago total o parcial, con
  recibo y comprobante). No cuenta como deuda de renta, ni genera recargo por mora, ni es ingreso
  (el reporte la informa aparte). Al finalizar el contrato se liquida con la garantia realmente pagada.
- **Pestañas:** cada modulo conserva lo que tenia elegido (arrendatario, contrato, fila) al cambiar
  de uno a otro. Al cambiar de propiedad se reinician, salvo Arrendatarios.
- `data/archivos/` esta en `.gitignore`: son documentos personales.

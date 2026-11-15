# Sistema de Gestión Inmobiliaria y Alquileres — Grupo 6

Proyecto de Estructura de Datos (Java). Docente: Arredondo Castillo Gustavo.

## Estructura

```
src/inmobiliaria/
├── Main.java, MenuPrincipal.java        → Integrante 1
├── nucleo/        (estructuras, historial, persistencia, util)   → Integrante 1
├── propiedades/   (estructuras AVL/General, modelo, servicio)    → Integrante 2
├── contratos/     (modelo, servicio)                             → Integrante 2
├── arrendatarios/ (estructuras TablaHash, modelo, servicio)      → Integrante 3
├── documentos/    (modelo, servicio)                             → Integrante 3
├── mantenimiento/ (modelo, servicio)                             → Integrante 3
├── cobranza/      (modelo, servicio)                             → Integrante 4
├── alertas/                                                      → Integrante 4
└── reportes/                                                     → Integrante 4
test/inmobiliaria/   → pruebas, misma división por módulo
data/                → archivos CSV (persistencia, sección 2.8)
docs/                → documentación y plan de pruebas
```

## Responsables

| # | Integrante | Paquetes | Secciones | Reglas |
|---|------------|----------|-----------|--------|
| 1 | Francia Ramos Johan Mitziu (líder técnico) | `Main`, `nucleo.*` | 2.7, 2.8 | RN-01 |
| 2 | Zavaleta Calderon Steven Jack | `propiedades.*`, `contratos.*` | 2.1, 2.3 | RN-02, 03, 04, 05, 12, 13 |
| 3 | Quispe Rodriguez Carlos Manuel Ignacio | `arrendatarios.*`, `documentos.*`, `mantenimiento.*` | 2.2, 2.4, 2.6 | RN-06 |
| 4 | Pucho Lizana Pedro Eduardo Mar | `cobranza.*`, `alertas`, `reportes` | 2.5 + alertas y reportes | RN-07, 08, 09, 10, 11 |

## Estructuras de datos por módulo

- **Integrante 1:** lista simple, lista doble, pila, cola, cola de prioridad (heap).
- **Integrante 2:** árbol AVL genérico, árbol general (Propietario > Edificio > Unidad).
- **Integrante 3:** tabla hash con encadenamiento; usa la cola de prioridad del núcleo.
- **Integrante 4:** usa las estructuras del núcleo (cola de prioridad para cobros urgentes).

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

Unidades, Arrendatarios, Contratos, Documentos, Cobro de rentas y mora y Mantenimiento
se abren como paneles a la derecha de la ventana. El menu Reportes muestra Ingresos,
Morosidad, Ocupacion y Alertas. Un contrato solo se activa con el expediente completo
(identidad + contrato firmado, RN-06).

- **Documentos:** cada documento es un archivo anexado (PDF, Word o foto). Se copia a
  `data/archivos/<documento>/` como `Identidad.jpg`, `Contrato.pdf`, etc.; la columna Archivo
  muestra el nombre y con doble clic se abre. Al completar el expediente, si el arrendatario
  tiene un contrato pendiente, el sistema pregunta si activarlo (Si / No).
- **Recibos:** al registrar un pago se puede anexar el comprobante (foto o PDF). En
  "Ver recibos", doble clic abre el comprobante y "Anexar comprobante" lo agrega o reemplaza
  despues. Se guardan en `data/archivos/recibos/`.
- **Arrendatarios:** el aval es un solo campo de texto opcional.
- `data/archivos/` esta en `.gitignore`: son documentos personales.

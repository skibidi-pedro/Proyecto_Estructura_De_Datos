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

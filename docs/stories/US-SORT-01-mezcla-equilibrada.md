# US-SORT-01: Ordenamiento externo del indice

- **Estado de implementacion:** Implementada
- **Estado de revision:** Approved
- **Aprobado por:** Integrante del equipo que solicito la tarea
- **Fecha de aprobacion:** 2026-11-16
- **Revision del profesor:** Pendiente
- **Spec:** [SPEC-SORT-01](../specs/11-mezcla-equilibrada.md)

## Historia

Como integrante del equipo, quiero ordenar el indice por nombre mediante
Balanced Merge Sort externo para preparar su futura busqueda binaria sin cargar
todos sus registros en memoria.

## Criterios de aceptacion

1. Se distribuyen y mezclan bloques de registros usando archivos en disco.
2. Se ordenan los nombres con String.compareTo y cada posicion se conserva
   junto a su nombre. Los nombres iguales mantienen su orden relativo.
3. La memoria utilizada no crece con la cantidad de registros.
4. El indice original no se modifica durante las pasadas. Se reemplaza solo
   mediante movimiento atomico cuando el sistema lo permite.
5. Se manejan errores explicitamente y se limpian solo temporales propios.
6. Se prueban indices vacios, simples, impares, ordenados y desordenados,
   duplicados, corrupcion y errores de acceso.

No incluye la busqueda binaria ni activar automaticamente el ordenamiento
despues de grabar. Esas tareas siguen pendientes.

## Verificacion

`mvn -B -Dtest=BalancedMergeSortTest,IndexFileManagerTest test` ejecuto 18
casos sin fallos, errores ni omisiones: 6 del algoritmo y 12 del indice.
Las pruebas usaron archivos temporales.

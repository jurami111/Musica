# US-SEARCH-01: Busqueda binaria del indice en disco

- **Estado de implementacion:** Implementada
- **Estado de revision:** Approved
- **Aprobado por:** Integrante del equipo que solicito la tarea
- **Fecha de aprobacion:** 2026-11-16
- **Revision del profesor:** Pendiente
- **Spec:** [SPEC-SEARCH-01](../specs/12-busqueda-binaria-disco.md)

## Historia

Como usuario, quiero encontrar una grabacion por nombre con busqueda binaria
directamente en recordings.index, para obtener la posicion de su encabezado en
recordings.music sin cargar el indice completo en memoria.

## Criterios de aceptacion

1. Cada inspeccion usa seek con numero de registro por 256 bytes y lee un solo
   registro.
2. Se compara con String.compareTo y solo se devuelve coincidencia exacta.
3. La busqueda funciona para indices vacios, de uno y de varios registros,
   siempre que ya esten ordenados.
4. No modifica ni ordena el indice durante la busqueda.
5. Se rechazan nombres invalidos y archivos truncados; los errores de disco y
   corrupcion se propagan con su tipo.
6. La posicion devuelta permite localizar el encabezado en recordings.music.

No incluye verificar que todos los registros estan ordenados, ordenar el indice
durante la busqueda ni conectar esta operacion a la interfaz de carga.

## Verificacion

`mvn -B -Dtest=DiskBinarySearchTest,BalancedMergeSortTest,IndexFileManagerTest test`
compilo el proyecto y ejecuto 26 casos sin fallos, errores ni omisiones:
8 de busqueda, 6 de ordenamiento y 12 de registros del indice.

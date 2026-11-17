# SPEC-SORT-01: Balanced Merge Sort externo

- **Historia:** [US-SORT-01](../stories/US-SORT-01-mezcla-equilibrada.md)
- **Estado de implementacion:** Implementada
- **Estado de revision:** Approved
- **Aprobado por:** Integrante del equipo que solicito la tarea
- **Fecha de aprobacion:** 2026-11-16
- **Revision del profesor:** Pendiente

## Objetivo y alcance

Ordenar un archivo de registros [SPEC-INDEX-01](10-registros-indice.md),
sin implementar busqueda ni conectarlo aun al grabador.
El archivo de entrada debe existir, ser regular y no estar siendo modificado ni
abierto para escritura por otra instancia o proceso durante la operacion.
No se implementa bloqueo entre procesos.

## Algoritmo

Cada pasada distribuye en dos archivos temporales bloques alternos de tamano
fijo, comenzando con un registro por bloque. Luego mezcla un bloque de cada
archivo hacia un tercer archivo. Los bloques resultantes quedan ordenados y
se duplica su tamano en cada pasada, hasta cubrir todos los registros.
Un bloque final incompleto se conserva. Al comparar nombres iguales, se toma
primero el registro del bloque izquierdo para conservar su orden relativo.
Se utiliza String.compareTo, coherente con la spec binaria y la igualdad exacta.
Cada registro completo, incluido su offset, viaja junto con su nombre.

Se reutilizan IndexEntry e IndexFileManager; solamente se mantienen en memoria
los registros necesarios para una mezcla y los buffers fijos de un registro.
No se carga ni se ordena el indice completo en arreglos o colecciones.
Un indice vacio o de un solo registro se valida y ya esta ordenado.

## Archivos y errores

Los archivos temporales se crean en una carpeta unica junto al indice, para
que el resultado y el indice esten en el mismo sistema de archivos. Esto evita
depender de que data/temp y una ruta de indice personalizada esten en el
mismo volumen. Solo se eliminan los temporales creados por la operacion.

El indice original no se escribe durante las pasadas. Al terminar y cerrar
todos los archivos se solicita un movimiento atomico para reemplazarlo.
Si el sistema no permite ese movimiento o falla una operacion, se informa el
error mediante DiskSortException con la causa y no se intenta una copia no
atomica como alternativa. Los registros corruptos se reportan con su causa.
No se garantiza recuperacion ante fallos fisicos o cambios concurrentes.
Un error de limpieza tambien se informa; si ya habia otro error, se conserva
como error adicional. Si el error ocurre despues del movimiento, el indice
puede ya estar reemplazado. No se eliminan archivos ajenos.

## Verificacion prevista

Pruebas con archivos temporales: vacio, uno, ordenado, inverso, numero impar,
varias pasadas, nombres repetidos y tildes; verificar conteo, orden, offsets y
estabilidad. Comprobar corrupcion, ruta inexistente, que no se sustituya el
original antes de terminar y limpieza de los archivos temporales.

## Verificacion realizada

`mvn -B -Dtest=BalancedMergeSortTest,IndexFileManagerTest test` ejecuto 18
casos sin fallos, errores ni omisiones. Se comprobaron el orden y los offsets,
la estabilidad de nombres repetidos, varias pasadas, vacio, un registro,
reordenamiento, corrupcion, rutas invalidas y limpieza en estos escenarios.
El reemplazo atomico funciono en el sistema de archivos usado en la prueba;
su disponibilidad en otros sistemas no esta garantizada.

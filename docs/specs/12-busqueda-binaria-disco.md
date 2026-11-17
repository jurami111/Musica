# SPEC-SEARCH-01: Busqueda binaria en recordings.index

- **Historia:** [US-SEARCH-01](../stories/US-SEARCH-01-busqueda-binaria-disco.md)
- **Estado de implementacion:** Implementada
- **Estado de revision:** Approved
- **Aprobado por:** Integrante del equipo que solicito la tarea
- **Fecha de aprobacion:** 2026-11-16
- **Revision del profesor:** Pendiente

## Contrato

`DiskBinarySearch.buscar(Path rutaIndice, String nombre)` devuelve
`Optional<IndexEntry>`. Un resultado vacio significa que no existe una
coincidencia exacta. La sobrecarga `buscar(String nombre)` utiliza
AppConstants.RUTA_INDICE.

El nombre consultado pasa ValidadorNombres.validar. La igualdad y el orden
usan String.compareTo/String.equals, sin recortar espacios, cambiar mayusculas
ni normalizar tildes.

## Acceso en disco

1. Abrir el indice existente en solo lectura con RandomAccessFile.
2. Confirmar que sea un archivo regular cuyo tamano sea multiplo de 256.
3. Definir limites inclusivos `inferior = 0`, `superior = cantidad - 1`.
4. Calcular `medio = inferior + (superior - inferior) / 2`.
5. Calcular `posicion = medio * 256` como long, usar `seek(posicion)` y leer
   exactamente un registro de nombre, long y 148 bytes de relleno.
6. Comparar el nombre y reducir el intervalo. Terminar al encontrar igualdad
   o cuando `inferior > superior`.

La memoria no depende de la cantidad de registros: se mantiene el registro
actual y sus bytes. No se usa un arreglo/lista con todo el indice, ni se ordena
en esta operacion. El indice debe haberse ordenado previamente mediante
[Balanced Merge Sort](11-mezcla-equilibrada.md).

La primera coincidencia encontrada se devuelve cuando hay nombres repetidos.
La unicidad de nombres se mantiene en recordings.list. La operacion de busqueda
no recorre todo el archivo para detectar nombres duplicados ni verificar que
el indice completo este ordenado.

## Errores

- Un nombre no valido produce IllegalArgumentException.
- Un archivo inexistente/no regular o un fallo de lectura produce
  StorageIOException conservando la causa.
- Un tamano truncado, relleno no cero o registro invalido produce
  ArchivoCorruptoException.
- La busqueda es de solo lectura y no altera los bytes del indice.

No se captura Exception de forma general ni se convierte un error en resultado
vacio.

## Verificacion prevista

Probar archivos vacios, un registro, conteos pares e impares; primero, medio y
ultimo; ausencias menores, mayores y entre claves; mayusculas, espacios y
tildes exactos; offsets grandes; relleno y truncamiento corruptos; ruta
inexistente; y que el indice permanezca byte por byte igual. Verificar tambien
el encabezado correspondiente en recordings.music.

## Uso desde el futuro cargador

1. Asegurar que el indice fue ordenado despues de su ultima modificacion.
2. Llamar `buscar(nombre)` o proporcionar la ruta del indice.
3. Si el Optional esta vacio, informar que no se encontro la grabacion.
4. Si existe, utilizar `IndexEntry.obtenerPosicionEncabezado()` con
   `MusicFileManager.leerEncabezado(posicion)`.

La busqueda no modifica el indice, no ordena bajo demanda y no consulta
recordings.list. Un indice vacio se interpreta como una busqueda sin resultados.

## Verificacion realizada

`mvn -B -Dtest=DiskBinarySearchTest,BalancedMergeSortTest,IndexFileManagerTest test`
compilo Java y ejecuto 26 casos sin fallos, errores ni omisiones.
Las pruebas incluyen 8 de busqueda y comprueban que el offset obtenido permite
leer el encabezado correspondiente. No se conecto aun a la interfaz de carga.

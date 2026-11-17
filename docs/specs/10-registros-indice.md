# SPEC-INDEX-01: Registros binarios del indice

- **Historia:** [US-INDEX-01](../stories/US-INDEX-01-registros-indice.md)
- **Estado de implementacion:** Implementada
- **Estado de revision:** Approved
- **Aprobado por:** Integrante del equipo que solicito la tarea
- **Fecha de aprobacion:** 2026-11-16
- **Revision del profesor:** Pendiente

## Formato

Se mantiene el [esquema binario existente](03-esquema-archivos-binarios.md).
Cada registro de recordings.index contiene, en este orden:

| Campo | Tipo | Bytes |
|---|---|---:|
| Nombre | 50 unidades UTF-16 | 100 |
| Posicion absoluta del encabezado en recordings.music | long | 8 |
| Relleno de bytes cero | bytes | 148 |
| Total | | 256 |

El nombre se escribe con NombreBinario y se valida con ValidadorNombres,
como en los archivos existentes. El long se escribe en orden big-endian.
La posicion es no negativa y multiplo de 256; cero es una posicion valida.
El registro numero n comienza en el desplazamiento n * 256 del indice.

## Operaciones

IndexEntry conserva el nombre y la posicion del encabezado. IndexFileManager
se abre con la ruta predeterminada o una ruta proporcionada, sin truncar un
archivo existente. Crear carpetas si no existen, cerrar con try-with-resources.

- obtenerCantidad(): numero de registros en disco, como long.
- agregarRegistro(entrada): agregar un registro al final.
- leerRegistro(numero): recuperar un registro individual, contando desde cero.

Las operaciones no cargan el indice completo; se utiliza un buffer de un solo
registro. Se admite una sola instancia escritora por archivo, sin coordinacion
entre procesos ni recuperacion de caidas.

## Errores

Un archivo cuyo tamano no sea multiplo de 256, un nombre o posicion invalida,
un relleno distinto de cero o un registro truncado producen
ArchivoCorruptoException. Los fallos de apertura, lectura, escritura y cierre
producen StorageIOException conservando la causa. Los argumentos invalidos o
usar un gestor cerrado son errores explicitos de programacion.
Tras un fallo de escritura no se permite seguir escribiendo con esa instancia;
no se promete rollback.

## Limites de esta historia

Agregar al final no mantiene el indice ordenado. Balanced Merge Sort, busqueda
binaria, integracion con RecorderService y validacion de que la posicion apunte
al encabezado correcto con el mismo nombre quedan pendientes. No se debe usar
un indice sin ordenar como si estuviera listo para la busqueda binaria.

## Verificacion prevista

Verificar bytes, tamano y relleno, multiples registros y reapertura, posicion
cero y posiciones grandes, nombres validos e invalidos, registros truncados y
corruptos, limites de lectura y fallos de apertura. Repetir las pruebas de
almacenamiento relacionadas para detectar regresiones.

## Verificacion realizada

`mvn -B -Dtest=IndexFileManagerTest,MusicFileManagerTest,ListFileManagerTest test`
compilo el proyecto y ejecuto 41 casos sin fallos, errores ni omisiones.
La prueba del indice comprueba tambien que una posicion guardada permite leer
un encabezado real; el gestor no verifica automaticamente esa referencia.

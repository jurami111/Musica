# SPEC-LIST-01: recordings.list y rechazo de duplicados

- **Historia:** [US-LIST-01](../stories/US-LIST-01-lista-y-nombres-unicos.md)
- **Estado de implementacion:** Implementada
- **Estado de revision:** Approved
- **Aprobado por:** Usuaria, como integrante del equipo
- **Fecha de aprobacion:** 2026-11-15
- **Revision del profesor:** Pendiente

## Formato aprobado

| Campo, en orden | Tipo | Bytes |
|---|---|---:|
| Nombre, con la misma codificacion que recordings.music | 50 char | 100 |
| Fecha/hora UTC, milisegundos desde 1970-01-01 | long | 8 |
| Total | | 108 |

No hay encabezado global, tipo de registro ni campos adicionales. El nombre se
rellena con caracteres nulos. Se usan DataInputStream y DataOutputStream, en
big-endian. La fecha conserva precision de milisegundos; RecordingMetadata
normaliza los Instant recibidos a esa precision. El formato admite cualquier
fecha representable por un long de milisegundos, sin imponer rangos arbitrarios.
La visualizacion en hora local corresponde a la interfaz futura.

Los 256 bytes del formato de music/index no se aplican a esta lista.

## Orden y acceso

Las entradas se anexan al final en el orden de registro. Un ajuste del reloj
no reordena las grabaciones existentes. No se ordena alfabeticamente.
ListFileManager implementa AutoCloseable, ofrece constructor predeterminado
y otro con Path, crea carpetas necesarias y no trunca archivos existentes.

- obtenerCantidad(): numero de registros, usando long.
- leerRegistro(numero): entrada individual, con numeracion desde cero.
- existeNombre(nombre): busqueda secuencial en disco, sin colecciones.
- registrar(RecordingMetadata): validar unicidad y agregar nombre/fecha.
- registrar(nombre): usar la fecha/hora actual y devolver los metadatos guardados.
- close(): cerrar los recursos; uso recomendado con try-with-resources.

La implementacion mantiene unicamente el registro actual en memoria y utiliza
RandomAccessFile para posicionar las operaciones en disco. Los metodos se
sincronizan; se admite un unico gestor escritor por archivo. No se implementan
bloqueos entre procesos ni recuperacion de caidas.

## Nombres y duplicados

Se reutilizan las reglas de RecordingHeader mediante ValidadorNombres:
nombre no nulo, no vacio ni solo espacios, sin nulos y maximo 50 unidades UTF-16.
No se eliminan espacios ni se convierten mayusculas. La igualdad es String.equals.
El ordenamiento y la busqueda del indice futuro deberan respetar esa igualdad.
La codificacion binaria de nombres tambien se comparte entre music y list.

existeNombre devuelve boolean para el flujo normal del controlador. registrar
vuelve a comprobar en disco y lanza DuplicateNameException antes de escribir si
se viola la unicidad. La interfaz debera validar con una condicion y pedir otro
nombre, no usar esa excepcion para repetir el ciclo.

Esta comprobacion considera los nombres registrados en recordings.list.
No se consulta music ni el indice, que aun no esta implementado. No sustituye
la busqueda binaria en disco que el enunciado exige para cargar una grabacion.

## Errores y consistencia

El tamano total debe ser multiplo de 108. Un registro truncado, un nombre
invalido o un relleno de nombre inconsistente producen ArchivoCorruptoException.
La lectura valida el registro consultado, sin escanear todo el archivo al abrir.
No existe checksum; un cambio de bits que siga siendo un nombre/fecha valido
no puede detectarse con este formato.

Los fallos de lectura, escritura, apertura y cierre se propagan como
StorageIOException con causa. Despues de un fallo de escritura no se permite
seguir agregando con esa instancia; no se promete rollback.
Los indices de lectura invalidos y los argumentos de programacion invalidos
se rechazan explicitamente; no se devuelve null ni una lista vacia ante errores.

Esta historia no cambia RecorderService ni MusicFileManager para registrar
automaticamente grabaciones. El futuro controlador debe comprobar primero el
nombre, completar la grabacion y registrar sus metadatos, definiendo como
resolver un fallo entre archivos. Aqui no se garantiza atomicidad music/list.

## Verificacion prevista

Comprobar bytes reales y relleno, orden de registro aun con nombres/fechas
desordenados, reapertura, duplicado sin escritura, comparacion exacta,
limite y validacion de nombres compartida, registros truncados/corruptos
y errores de disco. Reejecutar tambien MusicFileManagerTest por reutilizar
la validacion y codificacion de sus nombres.

## Uso desde el controlador futuro

Primero comprobar ValidadorNombres.esValido(nombre). Si el nombre es valido,
consultar existeNombre(nombre) y, con una condicion, solicitar otro si ya existe.
Solo despues registrar los metadatos. La proteccion de registrar permanece activa
aunque el llamador omita la consulta; no se deben ocultar sus errores.

Para mostrar la lista, obtenerCantidad() indica cuantos registros leer desde
cero. La interfaz puede consumirlos individualmente sin ordenarlos ni cargarlos
todos en un arreglo.

## Verificacion realizada

`mvn -B -Dtest=ListFileManagerTest,MusicFileManagerTest test` termino correctamente:
29 casos ejecutados, cero fallos, cero errores y ninguno omitido.
Nueve corresponden a lista/duplicados y veinte a recordings.music.
No se ejecutaron pruebas Android ni de sonido, fuera de este alcance.

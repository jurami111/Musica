# SPEC-MUSIC-01: Lectura y escritura binaria

- **Historia:** [US-MUSIC-01](../stories/US-MUSIC-01-lectura-escritura-binaria.md)
- **Estado de implementacion:** Implementada
- **Estado de revision:** Approved
- **Aprobado por:** Usuaria, como integrante del equipo
- **Fecha de aprobacion:** 2026-11-15
- **Revision del profesor:** Pendiente

## Formato

Se conserva el [formato de 256 bytes](03-esquema-archivos-binarios.md).
Encabezados: tipo, 50 char, inicio, final exclusivo y 136 bytes cero.
Comandos: tipo, tiempo, synth, accion, cantidad, 16 double y 104 bytes cero.
Se escriben tambien en cero los parametros no utilizados.
La codificacion es big-endian con DataInputStream/DataOutputStream.

Cada operacion utiliza un buffer de un solo registro. RandomAccessFile permite
posicionarlo en disco y actualizar el encabezado sin desplazar otros registros;
implementa DataInput/DataOutput, pero no hereda de InputStream/OutputStream.
No se cargan colecciones completas de grabaciones.

## Modelos y parametros

RecordingHeader contiene nombre, inicio y final. El nombre no puede estar vacio,
ser solo espacios, contener nulos ni superar 50 unidades UTF-16. Las posiciones
son long no negativos, alineados a 256; final es mayor o igual que inicio.

SynthCommand contiene tiempo en nanosegundos no negativo, identificador de synth,
accion y parametros finitos. El arreglo de parametros pertenece a un comando,
no al conjunto de grabaciones, y se copia para evitar modificaciones externas.

| Synth | Accion | Parametros en orden |
|---|---|---|
| Piano | tocar | nota MIDI, duracionMs, tercera (3 o 4) |
| Bajo | tocar | nota MIDI, duracionMs |
| Organo | iniciar nota | nota MIDI, r1, r2, r3, a1, a2, a3 |
| Organo | terminar nota | nota MIDI |

Las notas son enteros de 0 a 127; duracionMs es un entero positivo representable
como int. Los demas parametros del organo se conservan como numeros finitos.
Los identificadores son los de SynthConstants, no los nodos OSC.
Esto define almacenamiento, no agrega opciones ni cambia el Bridge existente.

## Operaciones de MusicFileManager

- Abrir con la ruta predeterminada o una Path proporcionada. Crear sus carpetas
  si no existen; no truncar un archivo existente.
- reservarEncabezado(): agregar un registro provisional y devolver su posicion.
  Su tipo es TIPO_ENCABEZADO_PENDIENTE, su nombre es nulo y ambos limites apuntan
  al primer comando. Solo puede haber una reserva activa por instancia.
- escribirComando(comando): agregar a esa reserva, devolver su posicion y exigir
  tiempos no decrecientes. Se valida antes de escribir.
- finalizarGrabacion(nombre): completar el encabezado reservado con el nombre,
  inicio y final actual, devolver su posicion y liberar la reserva. Un nombre
  invalido no descarta la reserva. Se admiten grabaciones sin comandos.
- leerEncabezado(posicion) y leerComando(posicion): leer un registro completo
  del tipo solicitado. No se usa null ni EOF como senal de fin.
- obtenerTamano(): informar bytes actuales; close(): cerrar el archivo.

Los metodos se sincronizan para no compartir el cursor entre operaciones.
Se permite una sola instancia escritora por archivo; no hay bloqueo entre
procesos, recuperacion de caidas ni transacciones en esta historia. El modo rwd
solicita escritura sin buffer pendiente de datos del archivo.
Cerrar con una reserva sin finalizar deja un encabezado provisional detectable;
no se inventa un nombre ni se declara una grabacion terminada.

## Validacion y errores

El tamano total debe ser multiplo de 256. Leer exige una posicion alineada que
contenga un registro completo. Un encabezado definitivo debe iniciar justo
despues de si mismo y terminar dentro del archivo. Los tipos desconocidos,
encabezados provisionales al leer como definitivos, nombres mal rellenados,
limites invalidos y parametros inconsistentes producen ArchivoCorruptoException.
Cada lectura valida su registro; no recorre toda la grabacion ni el archivo.

Los argumentos invalidos proporcionados por el programador usan
IllegalArgumentException; invocar escritura sin reserva o despues de cerrar es
un error de uso (IllegalStateException). No son validaciones de interfaz ni se
usan para pedir repetidamente entradas. Las validaciones normales de usuario
pertenecen al controlador futuro.

Los fallos de apertura, lectura, escritura y cierre se propagan como
StorageIOException con causa. Un fallo de escritura puede dejar un registro
parcial: no hay rollback ni se permite continuar escribiendo con esa instancia.
No se usan capturas generales ni resultados de exito ante fallos.

## Pruebas previstas

Comprobar tamano y relleno fisicos, lectura equivalente, limite del nombre y de
los parametros, tiempos largos, actualizacion sin modificar comandos, varias
grabaciones, reapertura, entradas invalidas sin escribir, registros truncados,
tipos/campos/rellenos corruptos y apertura sobre una ruta no valida.

## Uso desde el futuro grabador

1. Abrir MusicFileManager y llamar a reservarEncabezado().
2. Escribir cada SynthCommand con escribirComando(); se devuelve su posicion.
3. Al terminar, validar el nombre en el controlador y llamar a
   finalizarGrabacion(nombre). Esta historia no verifica nombres duplicados.
4. Conservar la posicion del encabezado para la futura entrada del indice.
5. Leer ese encabezado y recorrer sus comandos desde obtenerInicio() mientras la
   posicion sea menor que obtenerFin(), avanzando BYTES_REGISTRO cada vez.
6. Cerrar el gestor con try-with-resources. El llamador maneja o propaga las
   excepciones especificas; no se ocultan errores.

## Verificacion realizada

`mvn -B -Dtest=MusicFileManagerTest test` compilo todas las fuentes Java y
ejecuto 20 casos de almacenamiento sin fallos, errores ni omisiones. Incluye
lectura independiente con DataInputStream, verificacion fisica del relleno,
varias grabaciones, reapertura y corrupcion de campos y registros.
No se modifica el Bridge ni se conecta todavia el editor a este almacenamiento.

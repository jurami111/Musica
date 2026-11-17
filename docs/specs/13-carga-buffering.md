# SPEC-PLAY-01: Carga y buffering de grabaciones

- **Historia:** [US-PLAY-01](../stories/US-PLAY-01-carga-buffering.md)
- **Estado de implementacion:** Implementada
- **Estado de revision:** Approved
- **Aprobado por:** Integrante del equipo que solicito la tarea
- **Fecha de aprobacion:** 2026-11-16
- **Revision del profesor:** Pendiente

## Alcance

Completar la clase vacia PlaybackBufferThread, usando la busqueda y el acceso
a music existentes sin modificar clases implementadas por el companero.
El cargador requiere indice actualizado y ordenado por nombre antes de buscar.
El registro del indice debe apuntar a un encabezado con el mismo nombre.
Un indice ausente es un error, no equivale a una cancion inexistente.
El archivo music debe existir: la carga no crea grabaciones vacias.

## Reproduccion

Cargar obtiene el encabezado, no toca musica y devuelve un hilo preparado.
Una vez iniciado el hilo, se abre music y se valida de nuevo el encabezado;
si cambio desde la carga se informa el error.
Se lee como maximo un comando anticipado, hasta el final exclusivo. Es un
buffer limitado, no una copia de la grabacion. No se permite modificar los
archivos mientras carga o reproduce; esta historia no agrega bloqueos.

El tiempo almacenado es relativo al inicio de grabacion en nanosegundos.
El origen de reproduccion se toma al iniciar el hilo, no al cargar. La espera
para cada comando se calcula respecto a ese origen, descontando el tiempo
empleado leyendo y enviando comandos. Si un comando ya va tarde se envia sin
agregar una espera negativa. Los tiempos iguales se envian en orden.
Si el sistema despierta antes de tiempo se vuelve a esperar lo restante.
Se rechazan tiempos decrecientes como archivo corrupto.

El sistema operativo, la lectura, el envio UDP y SuperCollider pueden introducir
retrasos. No se garantiza precision absoluta ni sonido identico a nivel de
muestra; se respetan los tiempos solicitados tanto como el sistema permita.

El llamador obtiene un resultado asincrono de finalizacion y debe observarlo.
Errores de archivo o reproductor se propagan con su causa. Interrumpir el hilo
interrumpe la espera, termina con error y conserva el estado de interrupcion.
El motor no modifica los dispositivos. [SPEC-APP-01](15-integracion-final.md)
agrega el cierre del receptor para liberar notas de organo tras finalizar,
fallar o interrumpir.

## Limites conocidos

Esta historia implemento el cargador independiente. La actualizacion automatica
de recordings.index y su acceso por consola se integran en
[SPEC-APP-01](15-integracion-final.md).
La integración del receptor se completó en
[SPEC-PLAY-02](14-receptor-comandos-reproduccion.md): el buffer usa
`playCommand` y ReproductorBridge admite terceras menores y parciales
personalizados representables como float OSC.
La interfaz por consola se integra en SPEC-APP-01. La reproduccion con hardware
real queda pendiente.

## Verificacion prevista

Probar carga encontrada/no encontrada, indice y music ausentes o inconsistentes,
play separado de carga, buffer limitado, tiempos iguales y distintos, retraso de
lectura/envio sin acumulacion, tiempos decrecientes, fallo del reproductor,
interrupcion y grabacion vacia. Simular reloj y espera para resultados repetibles.

## Uso desde el futuro controlador

`PlaybackBufferThread.cargar(rutaIndice, rutaMusica, nombre, reproductor)`
devuelve un Optional: vacio si no se encontro el nombre, o un hilo preparado.
Cuando se solicite play, el controlador debe llamar `start()` una sola vez y
observar `obtenerResultado()` para notificar la finalizacion o el error.
Las rutas no deben cambiar mientras se reproduce. Para interrumpir una espera
se puede llamar `interrupt()` y se debe observar el error de interrupcion.

## Verificacion realizada

`mvn -B -Dtest=PlaybackBufferThreadTest,DiskBinarySearchTest,MusicFileManagerTest test`
ejecuto 37 casos sin fallos, errores ni omisiones. Los 9 casos de reproduccion
usan archivos temporales y, para las pruebas de tiempos, un reloj simulado.
No se probo sonido real ni se conecto el motor a una interfaz.

# Musica

Power Band conecta un móvil Android (piano), una cámara que sigue un objeto rojo
(órgano) y un gamepad USB (bajo) con SuperCollider. El editor dispone de una
entrada por consola y otra independiente con ventana Swing.

## Ejecutar

1. Instalar Java 17 o posterior, Maven y SuperCollider. Las dependencias nativas
   configuradas en Maven son para macOS Apple Silicon.
2. Abrir [myband.sc](supercollider/myband.sc) en SuperCollider y evaluar el bloque
   completo. Esperar el mensaje de synths cargados y mantener el servidor abierto.
   Son definiciones propias compatibles con el contrato OSC, no una copia de las
   del profesor ni una promesa de igual timbre. Comenzar con volumen bajo.
3. Conectar un gamepad compatible con SDL y permitir el acceso a la cámara al
   proceso que inicia Java. Conectar Android a la misma red.
4. Desde la raíz:

   ```sh
   mvn -B compile dependency:build-classpath -Dmdep.outputFile=target/dependencias.txt
   java -cp "target/classes:$(cat target/dependencias.txt)" com.powerband.Main
   ```

El servidor móvil escucha en TCP 5000; se cambia agregando el puerto como argumento
al comando Java. OSC usa UDP `127.0.0.1:57110`. Un envío UDP no confirma sonido.
Una fuente ausente se informa y no impide usar las demás.

### Ventana Swing en Windows

Después de preparar SuperCollider y los dispositivos como se indica arriba,
desde la raíz del proyecto en PowerShell:

```powershell
mvn -B compile dependency:build-classpath "-Dmdep.outputFile=target\dependencias.txt"
$dependencias = (Get-Content -Raw 'target\dependencias.txt').Trim()
java -cp "target\classes;$dependencias" com.powerband.ui.MainWindow
```

Opcionalmente se puede agregar el puerto TCP como argumento después de
`MainWindow`. No abrir la consola y la ventana al mismo tiempo sobre los mismos
archivos.

La ventana muestra hasta 200 comandos recientes y avisa si descarta mensajes
del monitor por sobrecarga; esto no descarta comandos de la grabación. Use
**Grabar** y **Parar grabacion** para grabar y asignar un nombre. La lista
cronológica se muestra por páginas: seleccione un nombre y use **Cargar** y
**Reproducir**. Al reproducir se detienen los instrumentos; después use
**Reiniciar instrumentos** y reconecte Android si es necesario.

Si se cancela el nombre, la grabación queda pendiente: use **Nombrar y guardar**
para continuar. Al cerrar con una grabación activa o pendiente se solicita
guardarla; cancelar el diálogo mantiene la ventana abierta. Los avisos de
dispositivos se muestran en la parte inferior y los detalles se registran en la
consola. El inicio de un hilo no confirma que un dispositivo esté conectado.

## Grabar, cargar y reproducir

```text
grabar
... tocar instrumentos ...
finalizar
Mi ensayo
listar
cargar Mi ensayo
play
```

`finalizar` detiene la grabación antes de pedir el nombre. Si es inválido o
duplicado, se solicita otro sin perder lo grabado. El nombre distingue
mayúsculas y espacios; admite hasta 50 unidades UTF-16. Cada guardado actualiza
el índice con Balanced Merge Sort en disco. `listar` lee la lista cronológica;
`cargar` busca por nombre mediante búsqueda binaria en disco y no toca sonido
hasta `play`. Para repetir, volver a cargar.

`parar` interrumpe el play. Antes del play se detienen los tres lectores para no
mezclar sonido en vivo. Después pueden reiniciarse con `camara`, `gamepad` y
`movil`; Android deberá reconectarse. También existen `parar camara`,
`parar gamepad` y `parar movil`. `salir` cierra recursos; si se está grabando,
solicita un nombre antes de salir.

Se guardan `recordings.music`, `recordings.index` y `recordings.list` en `data/`.
No modificar estos archivos desde otro proceso mientras se usa la aplicación.
No hay transacción entre archivos: un fallo durante el guardado bloquea nuevas
operaciones de catálogo en esa sesión e informa la etapa. Revisar los archivos
antes de reiniciar; no se implementa reparación automática. Los datos antiguos
creados sin índice no se migran automáticamente.
Si se cierra la entrada sin proporcionar nombre, queda un encabezado pendiente
no publicado en el catálogo, con advertencia explícita.

## Verificación

Las pruebas Java se ejecutan con `mvn -B test`. Las pruebas con reloj simulado,
archivos temporales y UDP local no prueban sonido ni dispositivos reales.
Android sigue pendiente de compilación con su SDK/Gradle y prueba en un teléfono.
SuperCollider no está instalado en el entorno de implementación: la carga y
audición de estas definiciones también requieren validación manual.

Prueba física pendiente: iniciar los tres dispositivos, grabarlos simultáneamente,
finalizar y nombrar, reiniciar Java, listar, cargar y reproducir. Comprobar notas,
orden e intervalos, rechazo de nombres repetidos y ausencia de órgano sostenido
al usar `parar`. La reproducción conserva comandos e intervalos, no las mismas
muestras de ruido del sintetizador.

## Diseño y especificaciones

Consultar la [arquitectura](docs/specs/01-arquitectura-general.md) y la
[integración final aprobada](docs/specs/15-integracion-final.md).

El instrumento Android y su servidor TCP se describen en la
[spec de instrumento Android](docs/specs/06-instrumento-android-sockets.md),
incluidas las instrucciones de ejecución y las verificaciones pendientes.

Los controles para cámara, gamepad, móvil y grabación se explican en la
[spec de instrumentos y bus](docs/specs/09-instrumentos-bus-tiempo-real.md).
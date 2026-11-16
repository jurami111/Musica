# SPEC-INST-02: Instrumentos y bus en tiempo real

- **Historia relacionada:** [US-INST-02: Cámara, gamepad y grabación simultánea](../stories/US-INST-02-instrumentos-bus-grabacion.md)
- **Estado de implementación:** Implementada; prueba física pendiente
- **Estado de revisión:** Approved
- **Aprobado por:** julian, en representación del equipo
- **Fecha de aprobación:** 2026-11-16
- **Revisión del profesor:** Pendiente

## Objetivo y alcance

Hacer que el móvil, una cámara web y un gamepad USB produzcan comandos
musicales simultáneamente, notificarlos al editor y grabarlos mediante los
contratos ya aprobados. Se conservan el
[móvil](06-instrumento-android-sockets.md), el
[Bridge](02-contrato-bridge-osc.md) y el
[contrato de integración](07-contrato-integracion-musical.md).

Incluye una entrada por consola para arrancar y detener las fuentes y
controlar la grabación. No incluye micrófono, edición gráfica, reproducción
de grabaciones, selección de sintetizador ni detección de manos o rostros.
La cámara se procesa localmente; no se guardan ni transmiten imágenes.

## Mapeos musicales propuestos

- **Móvil:** mantener los gestos y el piano existentes: tercera mayor y
  duración de 300 ms. No cambiar el protocolo TCP de Android.
- **Cámara:** usar órgano y detectar un objeto rojo por sus píxeles. Reducir
  cada cuadro a 80 × 60; un píxel es rojo si `R >= 100`, `R >= 1.5 * G` y
  `R >= 1.5 * B`. Si hay al menos 24 píxeles rojos, el centro horizontal de
  esos píxeles determina una de ocho notas MIDI `60, 62, 64, 65, 67, 69, 71,
  72`, en ocho franjas iguales de izquierda a derecha. Procesar como máximo
  10 cuadros por segundo.
- Al aparecer el objeto se inicia la nota; al cambiar de franja se termina la
  anterior y se inicia la nueva. Al desaparecer el objeto o detener la cámara
  se termina la nota activa. Para evitar cambios por un único cuadro ruidoso,
  exigir dos cuadros consecutivos con la misma franja o ausencia antes de
  aplicar un cambio. Usar los parciales predeterminados del órgano:
  proporciones `1, 2, 3` y amplitudes `1, 0.5, 0.25`.
- **Gamepad:** las cuatro posiciones de los botones principales
  inferior, derecha, izquierda y superior tocan respectivamente las notas
  MIDI `36, 38, 40, 43` con bajo y duración `300 ms`. Producir una nota
  únicamente al pasar de suelto a pulsado; mantener pulsado no la repite.
  Los cuatro botones pueden tocarse simultáneamente.
- Los umbrales de cámara son una propuesta inicial, no una garantía de
  detección bajo cualquier iluminación. No se guardan cuadros ni audio.

## Dispositivos y ejecución

- Detectar y abrir la cámara predeterminada y un gamepad USB compatible.
  Verificar que la biblioteca seleccionada sea compatible con Java 17 y
  macOS, incluyendo sus componentes nativos y licencia; documentar
  instalación y limitaciones. La API física se separa de la lógica de
  mapeo para probarla con datos simulados.
- Una fuente ausente o desconectada debe identificarse claramente sin
  impedir que las otras funcionen; no se anuncia que está activa. No se
  presupone que cualquier gamepad exponga los mismos identificadores: el
  adaptador debe reconocer las cuatro posiciones o informar que el mando
  no es compatible. Sin reconexión automática en esta historia; indicar cómo
  reiniciar la fuente.
- Permitir iniciar y detener los tres instrumentos y la grabación desde
  consola. El servidor TCP y las capturas funcionan fuera del hilo de
  consola. El usuario debe autorizar el acceso a la cámara en el sistema
  operativo. Liberar cámara, gamepad, sockets, hilos y cliente OSC al salir.

## Bus y editor

- Los tres productores publican eventos usando `SynthCommand` y el contrato
  `CommandObserver`; el editor se suscribe como observador. El bus mantiene
  la reproducción en vivo mediante el Bridge existente, sin duplicar la
  lógica OSC ni enviar dos veces la misma nota.
- Centralizar la publicación y serializarla entre productores: cada evento
  se entrega al editor en el mismo orden en que entra al bus. Mientras se
  graba, el bus asigna el tiempo transcurrido desde el inicio usando reloj
  monotónico compartido; los tiempos escritos son no negativos y no
  decrecientes. La inicialización, la publicación y la finalización de la
  grabación se coordinan para no escribir comandos fuera de sus límites.
- El editor recibe eventos incluso cuando no se graba; fuera de una
  grabación se usa un tiempo relativo a la sesión del bus, no una fecha.
  Los eventos de una grabación usan el origen de esa grabación, conforme al
  contrato de almacenamiento. No se requiere mostrar cada evento en pantalla.
- Un fallo de envío OSC o del observador se comunica explícitamente, con
  causa. Un fallo de grabación detiene esa grabación y se informa; no se
  descartan comandos silenciosamente ni se anuncia un resultado correcto.
  Un envío UDP exitoso no confirma que se haya reproducido sonido.

## Grabación

- Al iniciar, solicitar nombre, validar con las reglas existentes y comprobar
  que no esté en `recordings.list`; rechazar duplicados por condición, sin
  usar excepciones para repetir la entrada. Reservar un encabezado en
  `recordings.music` y establecer el origen monotónico solo al iniciar.
- El editor escribe cada comando recibido durante la grabación mediante
  `MusicFileManager`, sin cargar toda la interpretación en memoria. Al
  finalizar, cerrar el encabezado con el nombre y registrar metadatos
  mediante `ListFileManager`; no crear ni prometer un índice o reproducción.
- Si una nota de órgano ya suena al iniciar, escribir al tiempo cero su
  comando de inicio con sus parámetros reales. Si sigue activa al finalizar,
  escribir su comando de terminación al tiempo de finalización, sin apagar
  el sonido en vivo solo por detener la grabación. Al detener la cámara o
  cerrar la aplicación, terminar la nota realmente activa y notificarla si
  corresponde.
- Los dos archivos no admiten una transacción conjunta. Si falla la escritura
  o el registro de metadatos, informar qué paso falló y que podría quedar un
  encabezado provisional o una grabación sin entrada en la lista. No afirmar
  que se guardó ni intentar borrar datos anteriores. La recuperación de
  fallos entre archivos queda fuera de esta historia.

## Verificación y criterios de aceptación

1. Probar con imágenes sintéticas las franjas, detección y dos cuadros de
   estabilidad; probar pulsaciones y liberación de los cuatro botones con
   eventos simulados.
2. Probar fuentes concurrentes y un observador de prueba: todos los comandos
   válidos llegan una vez, con tiempos de grabación ordenados y sin duplicar
   el envío OSC. Verificar inicio y fin con órgano activo.
3. Crear una grabación temporal, leer sus comandos y metadatos existentes,
   y comprobar nombre único, orden, parámetros, fallos de almacenamiento y
   cierre de recursos. Probar que una fuente ausente no detiene las demás.
4. Ejecutar las pruebas Java relacionadas y registrar el resultado. Una
   prueba física de cámara, gamepad, móvil o sonido se informará únicamente
   si se ejecuta; la app Android sigue teniendo su propia verificación
   pendiente.

## Verificación realizada

`mvn -B -Dtest=BibliotecasNativasTest,InstrumentSocketServerTest,BusInstrumentosTest,DetectoresInstrumentosTest,BridgeTest,MusicFileManagerTest,ListFileManagerTest test`
compiló Java y ejecutó 53 pruebas automatizadas sin fallos. Incluyen el
recorrido de las tres fuentes simuladas hasta la grabación, publicación
concurrente, límites de grabación del órgano y carga nativa sin abrir hardware.
No se probó ningún dispositivo físico ni sonido con SuperCollider.

## Ejecución en macOS Apple Silicon

Se usan OpenCV/JavaCPP `4.14.0-1.5.14` y Jamepad `2.30.0.0` desde Maven.
El proyecto incluye los binarios nativos de OpenCV y OpenBLAS para macOS
ARM64; otras plataformas requieren sus propios binarios y no se verificaron.
Los artefactos incluyen componentes y licencias de terceros; antes de
redistribuirlos se deben revisar los avisos incluidos en sus paquetes.

Desde la raíz del proyecto:

```sh
mvn -B compile dependency:build-classpath -Dmdep.outputFile=target/dependencias.txt
java -cp "target/classes:$(cat target/dependencias.txt)" com.powerband.instrumentos.ConsolaInstrumentos
```

La aplicación inicia los tres lectores; cualquier fuente no disponible se
notifica sin detener las demás. Conceder permiso de cámara a la aplicación
que ejecuta Java. Conectar un gamepad compatible con el mapeo de SDL y el
móvil al servidor TCP del equipo; el puerto predeterminado es 5000 y se
puede cambiar añadiendo un argumento numérico al comando Java.

En consola usar `grabar NOMBRE`, `finalizar` y `salir`. Los comandos
`parar camara`, `parar gamepad` y `parar movil` detienen cada fuente;
`camara`, `gamepad` y `movil` las inician nuevamente. Usar `salir` para
cerrar ordenadamente y finalizar una grabación activa. Para escuchar sonido,
SuperCollider debe estar ejecutándose con los tres synths cargados: el
archivo `myband.sc` del repositorio sigue sin contener esas definiciones.
La aplicación Android y los gestos aún necesitan verificación física.

## Revisión

Aprobada por el equipo. La revisión del profesor es independiente y permanece
pendiente.

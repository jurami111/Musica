# SPEC-INST-01: Instrumento Android y protocolo de sensores

- **Historia relacionada:** [US-INST-01: Instrumento Android y servidor de sockets](../stories/US-INST-01-instrumento-android.md)
- **Estado de implementación:** Implementación parcial; verificación Android pendiente
- **Estado de revisión:** Approved
- **Aprobado por:** julian, en representación del equipo
- **Fecha de aprobación:** 2026-11-15
- **Revisión del profesor:** Pendiente

## Objetivo y alcance

Conectar un móvil Android a PowerBand por TCP y transformar los movimientos
del teléfono en notas de piano usando el
[Bridge existente](02-contrato-bridge-osc.md). Se admitirá un teléfono a la
vez. No se incluyen grabación, elección de synth, conexiones simultáneas,
descubrimiento automático ni ejecución de SuperCollider en Android.

## Aplicación Android

- Crear una aplicación Android nativa en Java, con una pantalla sencilla en
  orientación vertical. La carpeta Android actual no contiene un proyecto
  compilable, por lo que habrá que añadir su configuración de compilación.
- Permitir escribir la dirección IP o nombre del servidor y el puerto
  (por defecto, `5000`), conectarse y desconectarse. Mostrar instrucciones
  breves de los gestos, nota seleccionada si se puede conocer localmente y
  estado o error de conexión sin bloquear la pantalla.
- Leer acelerómetro y giroscopio con `SensorManager`. Si falta alguno, avisar
  y no iniciar el envío. Mientras la pantalla esté activa y conectada, enviar
  como máximo una muestra cada 50 ms, usando las lecturas más recientes de
  ambos sensores. Esperar al menos una lectura de cada uno antes de enviar.
- La red se ejecuta fuera del hilo de interfaz. Al desconectar, salir de la
  pantalla o destruirla, detener la lectura y cerrar la conexión. Un fallo
  de red debe mostrarse y dejar la aplicación desconectada; el usuario puede
  volver a conectarse manualmente.
- La pantalla no muestra la nota seleccionada: el protocolo solo envía
  muestras del móvil al servidor y no devuelve el estado de los gestos.

## Servidor Java

- `InstrumentSocketServer` escucha en un hilo separado, recibe un cliente a
  la vez y permite detener la escucha y cerrar la conexión activa de forma
  ordenada. Tras una desconexión vuelve a esperar otro cliente. Se usa el
  puerto `5000` por defecto y se permite configurarlo.
- Como la aplicación de escritorio todavía no tiene una ventana funcional,
  proporcionar una forma sencilla de iniciar y detener el servidor por
  consola. El servidor muestra su estado y los errores sin simular éxito.
- El procesamiento interpreta las muestras y llama a
  `MobileSensorInstrument`, configurado con `LeadSynthCommand` y el cliente
  OSC existente. El servidor no implementa por separado la comunicación
  con SuperCollider.

## Protocolo TCP propuesto

- TCP es un flujo: cada muestra es una línea ASCII terminada en `\n`, con
  seis números decimales separados por comas, en este orden:
  `ax,ay,az,gx,gy,gz`. Ejemplo: `0.0,0.0,9.81,0.0,0.0,0.0`.
- `ax`, `ay`, `az` son aceleraciones en m/s², incluida la gravedad;
  `gx`, `gy`, `gz` son velocidades angulares en rad/s. Se usa el punto
  decimal independientemente del idioma del dispositivo.
- El servidor procesa únicamente líneas completas, con exactamente seis
  números finitos y un máximo de 256 bytes antes del salto de línea. Ante
  una línea inválida o demasiado larga, informa el error y cierra esa
  conexión; vuelve a aceptar otro cliente. Una desconexión a mitad de línea
  no produce una nota.
- El protocolo no lleva autenticación ni cifrado: se utiliza solo en una red
  local de confianza. La conexión TCP del móvil no sustituye al protocolo
  OSC/UDP entre Java y SuperCollider.

## Gestos propuestos

Son reglas iniciales sencillas, no un estándar musical. Se deben validar con
un teléfono real y podrán ajustarse en una historia posterior.

- Al conectar, la nota seleccionada es MIDI `60` (do central). El giro
  alrededor del eje Z cambia la nota un semitono: `gz >= 1.5` aumenta y
  `gz <= -1.5` disminuye. La nota queda limitada al rango `48`–`72`. Se
  requiere volver a `|gz| < 0.5` antes de aceptar otro giro.
- La intensidad del movimiento es
  `|sqrt(ax² + ay² + az²) - 9.80665|`. Al alcanzar `4.0 m/s²`, se toca
  la nota seleccionada con duración de `300 ms`. Para volver a tocar debe
  bajar de `2.0 m/s²` y transcurrir al menos `400 ms` desde la nota anterior.
  Los intervalos se miden con un reloj monotónico del servidor.
- Para una misma muestra, se procesa primero el giro y después el movimiento.
  Al cambiar de cliente se reinician la nota y el estado de los gestos.
  El piano termina sus notas automáticamente según el contrato Bridge.

## Verificación

1. Probar lectura de líneas TCP fragmentadas o agrupadas, formato inválido,
   límites de longitud, desconexión, cierre y reconexión con sockets locales.
2. Probar con muestras sintéticas los umbrales, el rearme, la espera entre
   notas, los límites de notas y la delegación al Bridge, sin SuperCollider.
3. Compilar la aplicación Android y el proyecto Java con sus herramientas
   disponibles; ejecutar las pruebas automatizadas que se agreguen.
4. Registrar por separado una prueba física del móvil y del sonido real solo
   si se realiza con sensores y SuperCollider disponibles.

## Uso y verificación realizada

1. Compilar Java con `mvn -B compile` y arrancar desde la raíz con
   `java -cp target/classes com.powerband.network.InstrumentSocketServer`.
   Para otro puerto, añadirlo como argumento. Detener con Ctrl+C.
2. Abrir `android-instrument` en Android Studio con JDK 17 y SDK Android 35,
   o compilar con Gradle 8.9 usando `gradle -p android-instrument
   :app:assembleDebug`. No se incluye Gradle Wrapper.
3. En una red local de confianza, indicar en el teléfono la dirección de la
   computadora y el puerto. Para oír el piano se necesita SuperCollider
   ejecutándose con `classicPiano` cargado; las definiciones proporcionadas
   por el usuario aún no están incorporadas al archivo del repositorio.

`mvn -B -Dtest=InstrumentSocketServerTest,BridgeTest test` compiló Java y
ejecutó 13 pruebas automatizadas sin fallos, incluido el recorrido TCP a OSC.
`xmllint --noout` validó la sintaxis XML del manifiesto Android. La app no se
compiló porque faltan las herramientas Android y el usuario decidió no
instalarlas. Tampoco se verificaron sensores físicos ni sonido real.

## Revisión

Aprobada por el equipo. La revisión del profesor es independiente y permanece
pendiente.

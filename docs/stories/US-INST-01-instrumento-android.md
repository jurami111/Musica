# US-INST-01: Instrumento Android y servidor de sockets

- **Estado de implementación:** Implementación parcial; verificación Android pendiente
- **Estado de revisión:** Approved
- **Aprobado por:** julian, en representación del equipo
- **Fecha de aprobación:** 2026-11-15
- **Revisión del profesor:** Pendiente
- **Spec relacionada:** [Instrumento Android y protocolo de sensores](../specs/06-instrumento-android-sockets.md)

## Historia

Como integrante de la banda, quiero usar el acelerómetro y el giroscopio de
un teléfono Android para tocar el piano mediante la aplicación de escritorio,
sin conectar el teléfono directamente con SuperCollider.

## Alcance

Crear una aplicación Android sencilla que envíe lecturas de ambos sensores y
un servidor TCP en un hilo de Java que las reciba y las traduzca a notas
mediante el Bridge existente. Se admite un móvil a la vez y se usa piano.

## Criterios de aceptación

1. La aplicación permite indicar la dirección y el puerto del servidor,
   conectarse y desconectarse, y muestra el estado de conexión y los errores.
2. Mientras está conectada, transmite lecturas del acelerómetro y giroscopio
   mediante el protocolo de la spec; informa si falta alguno de los sensores.
3. El servidor recibe las lecturas fuera del hilo de interfaz, procesa un móvil
   a la vez y puede detenerse sin dejar sockets ni hilos activos.
4. Los gestos acordados producen notas de piano a través de
   `MobileSensorInstrument` y `LeadSynthCommand`, sin duplicar la lógica OSC.
5. Se controlan los datos inválidos y las desconexiones; los recursos se
   liberan y los fallos se comunican explícitamente.
6. Hay pruebas automatizadas del protocolo, los gestos y la conexión local sin
   depender de sensores físicos ni de SuperCollider.
7. Se registra por separado la compilación, las pruebas automatizadas y, si
   se realiza, la prueba con un móvil y SuperCollider reales.

## Revisión

Historia y spec aprobadas por el equipo. La revisión del profesor se registra
por separado y permanece pendiente.

## Verificación realizada

- `mvn -B -Dtest=InstrumentSocketServerTest,BridgeTest test`: el proyecto
  Java compiló y se ejecutaron 13 pruebas automatizadas sin fallos, incluida
  la conversión de TCP a OSC mediante sockets locales.
- El manifiesto Android se comprobó como XML con `xmllint --noout`.
- No se compiló la aplicación Android: faltan Gradle, JDK compatible y el SDK
  Android, y el usuario decidió no instalarlos. No se realizó una prueba con
  móvil o SuperCollider reales. Por ello, la historia todavía no cumple todos
  los criterios de aceptación.

# US-INST-02: Cámara, gamepad y grabación simultánea

- **Estado de implementación:** Implementada; prueba física pendiente
- **Estado de revisión:** Approved
- **Aprobado por:** julian, en representación del equipo
- **Fecha de aprobación:** 2026-11-16
- **Revisión del profesor:** Pendiente
- **Spec relacionada:** [Instrumentos y bus en tiempo real](../specs/09-instrumentos-bus-tiempo-real.md)

## Historia

Como integrante de la banda, quiero tocar simultáneamente con el móvil, la
cámara y un gamepad, y grabar sus comandos musicales desde el editor, para
conservar una interpretación conjunta.

## Alcance

Agregar captura de cámara con un objeto rojo y de un gamepad USB, conectar
ambos instrumentos y el móvil al bus de notificaciones, y permitir iniciar y
finalizar una grabación con nombre desde consola. Usar los archivos y contratos
existentes; no agregar captura de micrófono ni una interfaz gráfica.

## Criterios de aceptación

1. La cámara transforma la posición de un objeto rojo en notas de órgano y
   termina la nota cuando el objeto desaparece o se detiene la captura.
2. Cuatro botones del gamepad permiten tocar notas de bajo, una por pulsación.
3. Móvil, cámara y gamepad pueden estar activos simultáneamente y sus
   comandos llegan al editor por un bus compartido sin alterar el mapeo del
   móvil existente.
4. Se inicia y finaliza desde consola una grabación con nombre válido y
   único; los comandos de los tres instrumentos se escriben en el formato
   existente, con tiempos relativos ordenados y notas de órgano equilibradas.
5. Se notifican errores de dispositivos, conexión y almacenamiento; detener
   la aplicación libera recursos. Una grabación incompleta o con fallo no se
   anuncia como guardada correctamente.
6. Las pruebas automatizadas cubren mapeos, concurrencia, grabación, fallos y
   cierre con fuentes simuladas, sin necesitar hardware ni SuperCollider.
7. Se registran por separado compilación, pruebas automatizadas y pruebas
   físicas cuando se puedan realizar.

## Revisión

Historia y spec aprobadas por el equipo. La revisión del profesor se registra
por separado y permanece pendiente.

## Verificación realizada

`mvn -B -Dtest=BibliotecasNativasTest,InstrumentSocketServerTest,BusInstrumentosTest,DetectoresInstrumentosTest,BridgeTest,MusicFileManagerTest,ListFileManagerTest test`
compiló el proyecto y ejecutó 53 pruebas automatizadas sin fallos. Se probó
el recorrido de los tres instrumentos con fuentes simuladas, la grabación y
la carga de bibliotecas nativas, sin abrir dispositivos físicos.
No se probaron la cámara, el mando, el móvil ni el sonido real.

# US-PLAY-02: Receptor de comandos de reproducción

- **Estado de implementación:** Implementada
- **Estado de revisión:** Approved
- **Aprobado por:** julian, en representación del equipo
- **Fecha de aprobación:** 2026-11-16
- **Revisión del profesor:** Pendiente
- **Spec relacionada:** [Receptor de comandos del buffer](../specs/14-receptor-comandos-reproduccion.md)

## Historia

Como integrante de la banda, quiero ejecutar los comandos que entrega el
buffer en SuperCollider, para escuchar las grabaciones conservando los
parámetros almacenados de piano, bajo y órgano.

## Alcance

Integrar `playCommand` con el receptor existente y completar el envío de los
parámetros musicales mediante el Bridge y OSC, sin cambiar el formato binario
ni implementar una interfaz gráfica.

## Criterios de aceptación

1. El buffer entrega sus comandos mediante `playCommand`, sin duplicar envíos
   ni alterar la planificación temporal existente.
2. Se conservan las llamadas existentes a `reproducirComando`.
3. Piano admite tercera mayor o menor; bajo conserva nota y duración; órgano
   conserva los seis parámetros de parciales y permite terminar la nota.
4. Los fallos de envío se propagan al resultado asíncrono del buffer.
5. Se verifica con archivos temporales y un receptor UDP local el recorrido
   desde una grabación hasta los mensajes OSC, sin requerir SuperCollider.

## Revisión

Historia y spec aprobadas por el equipo. La revisión del profesor
permanece pendiente y se registra por separado.

## Verificación realizada

`mvn -B -Dtest=ReproduccionOscTest,PlaybackBufferThreadTest,BridgeTest,BusInstrumentosTest,InstrumentSocketServerTest test`
compiló el proyecto y ejecutó 31 pruebas automatizadas sin fallos. Se comprobó
el recorrido archivo-buffer-OSC por UDP local, no sonido real en SuperCollider.

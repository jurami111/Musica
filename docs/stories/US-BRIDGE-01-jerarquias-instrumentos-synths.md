# US-BRIDGE-01: Jerarquías Bridge de instrumentos y synths

- **Estado de implementación:** Implementada
- **Estado de revisión:** Approved
- **Aprobado por:** Equipo de desarrollo
- **Revisión del profesor:** Aprobado
- **Spec relacionada:** [Contrato Bridge y OSC](../specs/02-contrato-bridge-osc.md)

## Historia

Como integrante del equipo, quiero conectar los instrumentos con los
sintetizadores mediante el patrón Bridge, para poder combinar cada tipo de
instrumento con un synth sin que dependa de una clase concreta de synth.

## Alcance

Definir la abstracción `Instrument` y sus tres subclases existentes, la
interfaz `SynthImplementor` y los tres comandos concretos existentes para
invocar los synths de SuperCollider.

## Criterios de aceptación

1. `Instrument` utiliza `SynthImplementor` y no depende de comandos concretos.
2. `AudioMicInstrument`, `MobileSensorInstrument` y `WebcamInstrument` pueden
   recibir cualquier implementación de `SynthImplementor`.
3. `BassSynthCommand`, `LeadSynthCommand` y `PadSynthCommand` implementan el
   contrato común y envían los mensajes necesarios para el synth que les
   corresponda.
4. El destino y los nombres OSC se toman de `SynthConstants`; los mensajes
   respetan el contrato de los synths proporcionados por el profesor.
5. Se puede verificar la delegación y el envío de mensajes sin depender de
   dispositivos físicos ni de una instancia real de SuperCollider.
6. El proyecto compila y las pruebas de comportamiento agregadas pasan.

## Preparación

El usuario proporcionó el cliente y las definiciones de los synths. La spec
actualizada fue aprobada por el equipo.

## Verificación realizada

`mvn -B -Dtest=BridgeTest test` compiló el proyecto y ejecutó seis pruebas
automatizadas correctamente. Se verificaron los mensajes UDP locales, sin
ejecutar SuperCollider ni probar reproducción de sonido.

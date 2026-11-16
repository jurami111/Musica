# US-ARQ-01: Contrato de integración musical

- **Estado de implementación:** Implementada
- **Estado de revisión:** Approved
- **Aprobado por:** julian, en representación del equipo
- **Fecha de aprobación:** 2026-11-16
- **Revisión del profesor:** Pendiente
- **Spec relacionada:** [Contrato de integración musical](../specs/07-contrato-integracion-musical.md)

## Historia

Como integrante del equipo, quiero un contrato común para representar,
observar y ejecutar comandos musicales, para integrar los instrumentos, la
grabación y la reproducción sin acoplar sus implementaciones.

## Alcance

Definir la clase existente `SynthCommand` y las interfaces `CommandObserver` y
`AudioEnginePlayer`. Esta historia no implementa el grabador, el reproductor
ni su conexión con el Bridge.

## Criterios de aceptación

1. `SynthCommand` conserva su contrato de datos y su compatibilidad con el
   formato binario aprobado.
2. `CommandObserver` recibe un comando musical y permite comunicar errores
   de procesamiento al llamador.
3. `AudioEnginePlayer` recibe un comando completo y permite comunicar fallos
   de reproducción al llamador.
4. Los contratos documentan el significado del tiempo, la responsabilidad
   de programar la reproducción y las limitaciones actuales del Bridge.
5. El proyecto compila y se ejecutan las pruebas relacionadas sin fallos.

## Revisión

Historia y spec aprobadas por el equipo. La revisión del profesor se registra
por separado y permanece pendiente.

## Verificación realizada

`mvn -B -Dtest=MusicFileManagerTest,BridgeTest test` compiló el proyecto y
ejecutó 26 pruebas existentes de almacenamiento y Bridge sin fallos. Las
interfaces no incluyen implementaciones de reproducción ni observación.
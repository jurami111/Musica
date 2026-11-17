# SPEC-ARQ-01: Contrato de integración musical

- **Historia relacionada:** [US-ARQ-01: Contrato de integración musical](../stories/US-ARQ-01-contrato-integracion.md)
- **Estado de implementación:** Implementada
- **Estado de revisión:** Approved
- **Aprobado por:** julian, en representación del equipo
- **Fecha de aprobación:** 2026-11-16
- **Revisión del profesor:** Pendiente

## Objetivo y alcance

Definir contratos sencillos entre los módulos que producen, observan y
reproducen comandos musicales. Incluye la documentación del modelo existente
y las firmas de dos interfaces. No incluye implementar registro de
observadores, captura de instrumentos, grabación, planificación temporal,
reproducción ni conexión con SuperCollider.

## SynthCommand

Se conserva la clase y la API existentes: `SynthCommand(long tiempo,
int sintetizador, int accion, double... parametros)` y los métodos
`obtenerTiempo`, `obtenerSintetizador`, `obtenerAccion` y `obtenerParametros`.
El tiempo es un desplazamiento no negativo, en nanosegundos, relativo al
inicio de la grabación. El arreglo se copia al construir y al consultarlo.

Los identificadores, acciones, cantidades y orden de parámetros son los del
[contrato de almacenamiento](07-lectura-escritura-music.md):

| Synth | Acción | Parámetros |
|---|---|---|
| Piano | tocar | nota MIDI, duración en ms, tercera |
| Bajo | tocar | nota MIDI, duración en ms |
| Órgano | iniciar nota | nota MIDI, r1, r2, r3, a1, a2, a3 |
| Órgano | terminar nota | nota MIDI |

Se conservan las validaciones existentes y las constantes compartidas. El
modelo es un dato inmutable, no envía mensajes ni ejecuta sonidos.

## CommandObserver

Se propone una interfaz con un único método:

```java
void observarComando(SynthCommand comando) throws PowerBandException;
```

El productor futuro entregará un comando no nulo al observador. El observador
podrá procesarlo o almacenarlo; los errores del proyecto, incluidos los de
almacenamiento, se propagan al llamador sin fingir éxito. Esta interfaz no
define cómo se suscriben observadores, ni crea hilos o colas.

## AudioEnginePlayer

Se propone una interfaz con un único método:

```java
void reproducirComando(SynthCommand comando) throws IOException;
```

El llamador entrega un comando no nulo y decide cuándo invocarlo; el
reproductor futuro ejecuta el comando inmediatamente, sin esperar por el
campo de tiempo. La planificación y el orden de los comandos pertenecen al
controlador de reproducción futuro. Un fallo de envío o de comunicación se
propaga como `IOException`; un envío UDP exitoso no confirma reproducción.

El contrato recibe el comando completo para no perder la tercera del piano ni
los parciales del órgano. La integración posterior
[SPEC-PLAY-02](14-receptor-comandos-reproduccion.md) agrega el alias compatible
`playCommand` y la reproducción de terceras menores y parciales personalizados.
Los valores que no pueden convertirse a float OSC se rechazan explícitamente,
sin descartar parámetros silenciosamente.

## Criterios de aceptación y verificación

1. Las dos interfaces exponen las firmas anteriores sin implementar lógica.
2. `SynthCommand` mantiene los datos, validaciones y copias existentes y
   continúa siendo compatible con los usos de almacenamiento.
3. Las pruebas existentes de almacenamiento y Bridge pasan; cualquier prueba
   nueva se limita al comportamiento modificado por esta historia.
4. Se registra por separado el resultado de compilación y el de las pruebas.

## Verificación realizada

`mvn -B -Dtest=MusicFileManagerTest,BridgeTest test` compiló el proyecto y
ejecutó 26 pruebas existentes sin fallos: 20 de almacenamiento y 6 de Bridge.
No se implementó ni probó un reproductor o un observador concreto.

## Revisión

Aprobada por el equipo. La revisión del profesor es independiente y permanece
pendiente.
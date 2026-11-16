# SPEC-BRIDGE-01: Contrato Bridge y OSC

- **Historia relacionada:** [US-BRIDGE-01: Jerarquías Bridge de instrumentos y synths](../stories/US-BRIDGE-01-jerarquias-instrumentos-synths.md)
- **Estado de implementación:** Implementada
- **Estado de revisión:** Approved
- **Aprobado por:** julian, en representación del equipo
- **Fecha de aprobación:** 2026-11-15
- **Revisión del profesor:** Pendiente

## Objetivo y alcance

Proponer la implementación más simple de las dos jerarquías del patrón
Bridge para que los instrumentos invoquen synths de SuperCollider sin
depender de comandos concretos.

Incluye la delegación desde instrumentos, los tres comandos concretos y el
envío de mensajes OSC necesario para invocar los synths. No incluye capturar
audio, cámara o sensores, gestionar dispositivos, grabar o reproducir
archivos, ni construir una interfaz gráfica.

## Diseño propuesto

- `SynthImplementor` define `iniciarNota(int nota, int duracionMs)` y
  `terminarNota(int nota)`. Las notas MIDI válidas van de 0 a 127 y la duración
  debe ser positiva. Para piano y bajo, iniciar toca una nota de duración
  definida y terminar no envía mensajes porque terminan automáticamente.
  Para órgano, iniciar mantiene la nota hasta terminarla y la duración no se
  utiliza. Terminar una nota de órgano que no está activa no envía mensajes.
- `Instrument` conserva una referencia a `SynthImplementor`, recibida en el
  constructor, y delega en ella las operaciones musicales.
- `AudioMicInstrument`, `MobileSensorInstrument` y `WebcamInstrument` reciben
  esa dependencia sin elegir ni importar un comando concreto. No se agrega
  lógica de captura que aún no está definida.
- `BassSynthCommand`, `LeadSynthCommand` y `PadSynthCommand` implementan
  `SynthImplementor`. Cada uno prepara el mensaje correspondiente y utiliza
  un mecanismo de envío OSC compartido y sustituible en las pruebas.
- Se conservan los nombres de clases existentes. Los nuevos nombres de
  métodos y variables se escriben en español, salvo los exigidos por el
  protocolo o herramientas.

## Contrato OSC

La implementación utilizará la dirección, puerto, rutas y nombres de
`SynthConstants`: `127.0.0.1`, `57110`, `/s_new`, `/n_set`, `classicPiano`,
`stringBass` y `organ70s`. Los identificadores binarios de los synths no son
identificadores de nodos de SuperCollider.

Asignación confirmada por el usuario:

| Comando existente | Synth |
|---|---|
| `BassSynthCommand` | `stringBass` |
| `LeadSynthCommand` | `classicPiano` |
| `PadSynthCommand` | `organ70s` |

El usuario proporcionó en la conversación el cliente `BandClient` y las
definiciones de `myband`. Los mensajes usan el siguiente contrato:

- `/s_new` recibe, en orden, el nombre del synth, el identificador de nodo,
  la acción de inserción `0`, el grupo `1` y los pares de control indicados
  abajo. Los identificadores de nodo comienzan en 10000 y se asignan sin
  repetirse entre los comandos que comparten el emisor.
- Piano: `note` (entero MIDI), `durMs` (entero) y `third` (entero `4`, acorde
  mayor). Bajo: `note` y `durMs`, ambos enteros.
- Órgano: `freq` (float, calculado como `440 * 2^((nota - 69) / 12)`),
  `r1`, `r2`, `r3` (floats `1`, `2`, `3`) y `a1`, `a2`, `a3` (floats `1`,
  `0.5`, `0.25`).
- Para terminar el órgano se envía `/n_set` con el identificador del nodo,
  `gate` y el entero `0`. Se conserva la relación nota-nodo hasta que el
  envío de terminación tenga éxito; al reiniciar una nota activa se termina
  el nodo anterior antes de crear otro.
- OSC se envía por UDP. Las cadenas ASCII se terminan en cero y se rellenan
  a múltiplos de cuatro bytes; los enteros y floats usan cuatro bytes en
  orden big-endian y los tipos se indican en la cadena de etiquetas OSC.

Esta historia usa el acorde mayor y los parciales predeterminados del cliente;
configurar acordes menores o parciales personalizados queda fuera de alcance.
El archivo de SuperCollider del repositorio todavía no contiene las
definiciones proporcionadas, por lo que una conexión real requiere cargarlas;
no se presentará como verificada sin una prueba real. Se mantendrá la
compatibilidad con el
[esquema binario](03-esquema-archivos-binarios.md). El contrato de parámetros
para almacenamiento se acordó en [SPEC-MUSIC-01](07-lectura-escritura-music.md);
su integración con grabación y reproducción no forma parte de esta historia.

## Errores

Un fallo de envío debe comunicarse explícitamente al llamador, conservando
su causa cuando corresponda. No se usará una excepción para el flujo normal
ni se informará éxito si el envío falla.

## Uso y ciclo de vida

Se comparte una instancia de `BandClient` entre los comandos dirigidos al
mismo servidor para asignar identificadores de nodo distintos. El código que
inicia una nota de órgano debe terminarla antes de cerrar el cliente: `close`
cierra el socket, pero no libera automáticamente los nodos. Un envío UDP
exitoso no confirma que SuperCollider haya recibido o reproducido el mensaje.

```java
try (BandClient cliente = new BandClient()) {
    Instrument instrumento = new WebcamInstrument(new PadSynthCommand(cliente));
    instrumento.iniciarNota(60, 1000);
    instrumento.terminarNota(60);
}
```

## Criterios de aceptación y verificación

1. Cada instrumento acepta cualquier `SynthImplementor` y delega sin depender
   de los tres comandos concretos.
2. Cada comando implementa el contrato común y envía el mensaje acordado
   para su synth, usando las constantes existentes.
3. Las pruebas comprueban la delegación con implementaciones de prueba, el
   mensaje de cada comando con un emisor sustituible y la propagación de
   fallos de envío; no requieren hardware ni SuperCollider.
4. La compilación y las pruebas automatizadas se registran por separado.
   Una prueba real con SuperCollider solo se informará como realizada si se
   ejecuta con las definiciones correspondientes.

## Verificación realizada

`mvn -B -Dtest=BridgeTest test` compiló el proyecto y ejecutó seis pruebas
automatizadas sin fallos, incluidas las comprobaciones de mensajes UDP locales
y errores de envío. No se probó la reproducción con SuperCollider.

## Revisión

La historia registra aprobación del equipo y del profesor. El equipo aprobó la
spec actualizada; la revisión de la spec por el profesor se registra por
separado y permanece pendiente.

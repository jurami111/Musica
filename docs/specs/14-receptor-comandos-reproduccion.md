# SPEC-PLAY-02: Receptor de comandos del buffer

- **Historia relacionada:** [US-PLAY-02: Receptor de comandos de reproducción](../stories/US-PLAY-02-receptor-comandos.md)
- **Estado de implementación:** Implementada
- **Estado de revisión:** Approved
- **Aprobado por:** julian, en representación del equipo
- **Fecha de aprobación:** 2026-11-16
- **Revisión del profesor:** Pendiente

## Objetivo y alcance

Completar el receptor de los comandos leídos por
[PlaybackBufferThread](13-carga-buffering.md) para enviarlos a SuperCollider
mediante el Bridge existente. Reutilizar `ReproductorBridge` y `BandClient`,
sin agregar otro codificador OSC.

No incluye interfaz gráfica, actualización del índice, reproducción simultánea
con instrumentos en vivo ni cambios del formato de almacenamiento. Tampoco
incluía el cierre automático de notas de órgano tras interrupciones;
[SPEC-APP-01](15-integracion-final.md) agrega ese cierre al receptor y al buffer.

## Contrato de entrada

- Conservar `AudioEnginePlayer.reproducirComando(SynthCommand)`.
- Añadir `playCommand(SynthCommand)` como método predeterminado que delega en
  `reproducirComando`. El nombre en inglés se conserva por el requisito de
  integración. La interfaz continúa teniendo un solo método abstracto.
- El buffer llama a `playCommand` una sola vez por comando. El receptor
  ejecuta inmediatamente: no vuelve a esperar por su campo de tiempo.
- Conservar el acceso secuencial y el resultado asíncrono del buffer.

## Traducción a OSC

Se conservan los nombres, destino y rutas definidos en `SynthConstants` y el
[contrato binario de parámetros](07-lectura-escritura-music.md):

| Comando | Controles enviados |
|---|---|
| Piano, tocar | `note`, `durMs`, `third` (3 o 4) |
| Bajo, tocar | `note`, `durMs` |
| Órgano, iniciar | `freq` calculada desde MIDI, `r1`, `r2`, `r3`, `a1`, `a2`, `a3` |
| Órgano, terminar | Nodo activo asociado a la nota, `gate` igual a 0 |

Agregar operaciones de parámetros completos a las implementaciones del
Bridge que las necesiten, conservando las operaciones actuales y sus valores
predeterminados para no cambiar los instrumentos en vivo. El receptor no
descarta ni sustituye los parámetros almacenados.

Los parámetros del órgano se almacenan como `double`, pero OSC usa `float`
según el cliente proporcionado. Se permite el redondeo propio de esa
conversión; si un valor finito se desborda o un valor no nulo se convierte en
cero, rechazarlo explícitamente antes de enviar o liberar nodos.

Mantener la asociación nota-nodo del órgano y el comportamiento existente de
terminar una nota previa antes de reiniciarla. Una terminación de una nota
inactiva no envía mensajes. Usar una instancia de receptor para la sesión de
reproducción, sin compartir el estado del órgano con instrumentos en vivo.

## Errores

No ocultar errores de envío o conversión. Los fallos técnicos conservan su
causa y llegan al resultado asíncrono del buffer. Un envío UDP exitoso no
confirma recepción ni sonido en SuperCollider.

## Criterios de aceptación y verificación

1. Probar acordes mayores y menores, bajo, parciales personalizados y
   terminación del órgano, manteniendo el comportamiento predeterminado.
2. Verificar bytes OSC recibidos por UDP local desde comandos de una grabación
   temporal procesada por el buffer.
3. Probar fallos de envío y conversiones no representables, sin envíos
   parciales ni pérdida silenciosa de parámetros.
4. Ejecutar las pruebas relacionadas de buffer, Bridge e instrumentos;
   registrar compilación y pruebas por separado.
5. Documentar cómo proporcionar el receptor al cargador. La prueba de sonido
   real exige SuperCollider con los synths cargados y solo se registrará si
   se realiza.

## Integración implementada

`ReproductorBridge` recibe los tres comandos concretos porque las operaciones
de tercera y parciales pertenecen a piano y órgano respectivamente; no se
añaden operaciones de instrumentos específicos a `SynthImplementor`.
La composición existente de instrumentos sigue compilando sin cambios.

El llamador mantiene abierto `BandClient` hasta que termina el hilo:

```java
try (BandClient cliente = new BandClient()) {
    AudioEnginePlayer reproductor = new ReproductorBridge(
            new LeadSynthCommand(cliente), new BassSynthCommand(cliente),
            new PadSynthCommand(cliente));
    var carga = PlaybackBufferThread.cargar(rutaIndice, rutaMusica, nombre, reproductor);
    if (carga.isPresent()) {
        var hilo = carga.get();
        hilo.start();
        hilo.obtenerResultado().toCompletableFuture().get();
    }
}
```

El índice debe estar actualizado y ordenado. El llamador maneja la ausencia
del nombre y los errores; el ejemplo no implementa interfaz de usuario ni
gestión de fuentes físicas. El buffer ahora solicita el cierre automático
del receptor, conforme a SPEC-APP-01. SuperCollider debe estar
ejecutándose con los tres synths cargados.

## Verificación realizada

`mvn -B -Dtest=ReproduccionOscTest,PlaybackBufferThreadTest,BridgeTest,BusInstrumentosTest,InstrumentSocketServerTest test`
compiló Java y ejecutó 31 pruebas automatizadas sin fallos. Incluyen mensajes
OSC leídos por UDP local desde una grabación temporal, errores de envío y
rechazo de conversiones inválidas antes de liberar nodos. No se verificó
sonido real en SuperCollider.

## Revisión

Aprobada por el equipo. La revisión del profesor es independiente
y permanece pendiente.

# SPEC-ARQ-00: Arquitectura general de Power Band

- **Estado de implementación:** Integrada; validación física pendiente
- **Estado de revisión:** Pendiente para este resumen descriptivo; cambios
  funcionales revisados en [SPEC-APP-01](15-integracion-final.md).
- **Aprobación:** Julian aprobó SPEC-APP-01 el 2026-11-16; no constituye una
  aprobación independiente de este resumen.
- **Revisión del profesor:** Pendiente
- **Historia relacionada:** [US-APP-01](../stories/US-APP-01-flujo-completo.md)

## Alcance

Aplicación Java con editor por consola y tres instrumentos físicos.
Android es un proceso independiente que transmite seis componentes de sensores
por TCP. La cámara y el gamepad se leen en hilos locales.

## Flujo y responsabilidades

1. `Main` delega en `ConsolaInstrumentos`: crea recursos, inicia lectores y
   atiende comandos de usuario.
2. Cada adaptador transforma muestras en notas mediante una subclase de
   `Instrument`, la abstracción del Bridge. Esta conoce `SynthImplementor`,
   no el sintetizador concreto.
3. `BusInstrumentos` serializa los comandos concurrentes y sus tiempos,
   ejecuta sonido en vivo y notifica al editor a través de `CommandObserver`.
   Su adaptador `SynthImplementor` permite que los instrumentos no dependan
   de la grabación ni de OSC.
4. `ReproductorBridge` despacha al comando concreto de piano, bajo u órgano;
   las implementaciones usan `BandClient` para codificar y enviar OSC.
5. `MusicEditorController` observa eventos y los entrega a `RecorderService`.
   El servicio escribe cada comando inmediatamente en music; al guardar
   completa el encabezado, anexa y ordena index, y publica los metadatos en list.
6. `SesionEditor` coordina grabación/carga/play y evita cambiar archivos
   durante la reproducción. Enumera list registro a registro. La búsqueda
   usa `DiskBinarySearch`; `BalancedMergeSort` ordena solo sobre archivos.
7. `PlaybackBufferThread` valida el encabezado y lee un comando anticipado,
   esperando contra un origen monotónico establecido al iniciar play.
   Notifica el resultado y cierra las notas del receptor aun ante interrupción.

El receptor vivo y el de grabaciones tienen mapas de notas separados. Comparten
un cliente OSC para asignar IDs únicos. Las fuentes se detienen antes del play.
Los archivos de música e índice conservan registros binarios de 256 bytes.
List conserva metadatos de 108 bytes en orden de grabación.

## Reglas y límites

Las constantes de aplicación, almacenamiento, instrumentos y synths viven en
la biblioteca `common/constants`. Android tiene su biblioteca independiente.
Los fallos técnicos conservan su causa y se muestran; no se utilizan
excepciones para repetir nombres inválidos. No existe transacción conjunta
ni recuperación automática entre archivos.

No se requiere GUI. El teclado controla el editor, pero no es un instrumento.
UDP no garantiza recepción ni disponibilidad del servidor de audio.
Las validaciones físicas, compilación Android, GitHub y revisión del profesor
se informan por separado de las pruebas automatizadas.

## Criterios de aceptación

Los de [US-APP-01](../stories/US-APP-01-flujo-completo.md): flujo completo por
consola, índice ordenado en disco, límites de sesión, cierre de notas y pruebas
reproducibles sin anunciar como realizadas las verificaciones físicas.
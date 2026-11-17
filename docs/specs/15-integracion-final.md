# SPEC-APP-01: Integración final por consola

- **Historia:** [US-APP-01](../stories/US-APP-01-flujo-completo.md)
- **Estado de implementación:** Implementada; validación física pendiente
- **Estado de revisión:** Approved
- **Aprobado por:** Julian, 2026-11-16
- **Revisión del profesor:** Pendiente

## Alcance y reglas

Reutilizar los contratos aprobados y mantener los formatos binarios. La
consola, no una GUI, será el editor accesible. `Main` delega en su entrada.
No se ejecutan compras, commits o pushes ni se dan por hechas pruebas físicas.

## Grabación e indexación

`grabar` inicia sin nombre. `finalizar` detiene la escritura de nuevos eventos,
equilibra notas sostenidas y solicita un nombre. La grabación queda pendiente
hasta proporcionar un nombre válido y único, sin capturar eventos adicionales.
La validación de usuario se hace por condiciones, no excepciones para repetir.
Conservar las APIs usadas por pruebas existentes como compatibilidad.

Al guardar: completar el encabezado, anexar nombre y posición al índice, cerrar
el índice, ejecutar Balanced Merge Sort y registrar nombre/fecha en list.
La fecha corresponde al inicio de grabación. Mantener el acceso a registros
en disco; no cargar el catálogo completo en memoria.

No hay transacción entre archivos. Si falla cualquier etapa se bloquean
nuevas grabaciones y cargas en esa sesión y se informa la etapa y posible
inconsistencia. No anunciar éxito ni borrar registros previos.

## Cargar y reproducir

`listar` muestra nombre y fecha leyendo list registro por registro, en orden
de grabación. `cargar NOMBRE` prepara un PlaybackBufferThread mediante búsqueda
binaria en index. `play` inicia el buffer ya cargado y observa su resultado;
`parar` interrumpe y espera su salida. Para volver a tocar se vuelve a cargar.
No permitir grabar ni iniciar otro play mientras reproduce, ni cargar mientras
haya una grabación activa o pendiente de nombre.

Al ejecutar play se detienen los lectores de instrumentos para no mezclar
sonido en vivo. Después podrán iniciarse de nuevo con los comandos existentes.
El receptor de reproducción tiene estado de órgano separado del receptor vivo
y comparte solo el cliente de envío OSC para no repetir IDs de nodos.

Agregar un cierre de reproducción predeterminado sin efecto a AudioEnginePlayer;
el buffer lo invoca también ante fallo o interrupción, conservando el error
original y agregando errores de cierre. ReproductorBridge libera sus notas
de órgano sin cerrar el cliente OSC compartido.

## Constantes y synths

Extraer a la biblioteca de constantes las duraciones, notas, umbrales, límites,
puertos, escalas y valores de protocolo restantes de los componentes tocados.
No cambiar sus valores ni mapeos. Las dimensiones y escalas compartidas se
consultan sin exponer arreglos mutables.

Incorporar definiciones compatibles con classicPiano, stringBass y organ70s
basadas en el contrato proporcionado por el usuario, con instrucciones
para iniciar servidor y cargar synths. Se incluyen definiciones propias,
no una copia de las del profesor ni una promesa de igual timbre.
No afirmar reproducción idéntica a
nivel de muestras: los synths incluyen ruido; preservar comandos e intervalos.

## Criterios de aceptación

1. Grabar sin nombre, detener y corregir nombres inválidos o duplicados sin
   escribir más eventos en la grabación detenida.
2. Guardar completa music, ordena index en disco y publica list cronológicamente;
   los offsets permiten recuperar los comandos después de reabrir los archivos.
3. Listar, cargar y play son operaciones separadas y accesibles por consola.
4. No modificar grabaciones ni empezar otro play durante la reproducción.
5. Liberar notas de órgano en todos los cierres, preservando los errores técnicos.
6. Documentar arranque y límites de verificación, sin atribuir pruebas físicas
   ni aprobaciones del profesor que no se hayan realizado.

## Verificación y límites

Probar grabación sin nombre, rechazo y corrección de nombres, dos grabaciones
fuera de orden alfabético, índice ordenado con offsets correctos, listado,
carga y play, errores entre archivos y liberación del órgano. Reejecutar
pruebas relacionadas. Documentar ejecución y prueba manual con tres dispositivos.

No están disponibles aquí Gradle/SDK Android ni SuperCollider. Su instalación
y las pruebas físicas siguen pendientes; la negativa previa a instalar Android
se respeta. La publicación y defensa ante el profesor corresponden al equipo.

## Verificacion realizada

El 2026-11-16:

```sh
mvn -B -Dtest=SesionEditorTest,BusInstrumentosTest,PlaybackBufferThreadTest,ReproduccionOscTest,BridgeTest,DetectoresInstrumentosTest,InstrumentSocketServerTest,MusicFileManagerTest,IndexFileManagerTest,ListFileManagerTest,BalancedMergeSortTest,DiskBinarySearchTest test
```

- Compilación del proyecto Java y de sus pruebas correcta con release 17.
- 98 pruebas automatizadas relacionadas, sin fallos, errores ni omisiones.
  No se presenta como una ejecución nueva de la suite completa: no incluye
  BibliotecasNativasTest.
- Nuevos casos: detener antes de nombrar sin capturar eventos posteriores;
  reintentar nombres; ordenar dos grabaciones y conservar su cronología;
  reabrir los archivos y reproducir por búsqueda binaria; bloquear operaciones
  incompatibles; comunicar fallo del índice; liberar órgano y reintentar cierres;
  preservar errores de cierre y envío/interrupción.
- Las pruebas existentes verifican formato fijo, algoritmos de disco,
  concurrencia, mapeos y mensajes OSC por UDP local.
- `git diff --check` no detectó errores de espacio.
- `java -cp target/classes com.powerband.Main puerto-invalido` mostró el uso
  esperado sin abrir fuentes. No se ejecutó el flujo de consola con hardware.

No se compilaron los archivos Android modificados: faltan Gradle/SDK.
No se ejecutaron sclang/scsynth ni se escuchó audio: no están instalados.
Las definiciones SuperCollider requieren evaluación manual. Las pruebas físicas
siguen pendientes según el [procedimiento del README](../../README.md#verificación).
No se hicieron commits ni publicación en GitHub. La revisión del profesor
permanece pendiente.

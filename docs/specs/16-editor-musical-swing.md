# SPEC-UI-01: Editor musical en Swing

- **Historia:** [US-UI-01](../stories/US-UI-01-editor-musical.md)
- **Estado de implementación:** Implementada; validación visual y física pendiente
- **Estado de revisión:** Approved
- **Aprobado por:** El equipo, confirmada por Joselyn el 2026-11-16
- **Revisión del profesor:** Pendiente

## Alcance

Agregar una entrada Swing independiente que utilice el sistema existente.
La consola y su entrada continúan disponibles y no se modifican las clases
implementadas por el compañero. Swing es parte de Java; no se propone agregar
dependencias. Las clases vacías de `ui` son candidatas para la vista.

Esta entrada es adicional a la consola descrita en la
[spec de integración](15-integracion-final.md); no cambia sus reglas ni los
formatos y algoritmos de almacenamiento.

## Monitor y grabación

Componer un observador para conservar la entrega de comandos al editor actual
y mostrar los comandos en vivo. El monitor presenta los 200 más recientes;
el límite también debe impedir que se acumule sin control el trabajo pendiente
para la interfaz. Si se descartan mensajes visuales por sobrecarga, indicarlo.
Los límites del monitor no deben eliminar ni modificar comandos grabados.

La ventana ofrece controles para iniciar y detener la grabación. Al detenerla,
solicita un nombre y conserva la grabación pendiente mientras se corrija un
nombre inválido o duplicado. Se reutilizan las validaciones y el guardado
existentes; no se usan excepciones para repetir una entrada inválida.

## Lista y reproducción

Presentar nombre y fecha en el orden cronológico de `recordings.list`. Para
mantener acotada la memoria, mostrar páginas de registros en vez de cargar todo
el catálogo; no cargar el índice en memoria ni cambiar la búsqueda binaria o
el ordenamiento externo. La selección permite cargar una grabación y después
iniciar o detener su reproducción. Para reproducirla otra vez se vuelve a cargar,
como en la sesión existente.

No permitir operaciones incompatibles con grabación activa, nombre pendiente
o reproducción. Antes de reproducir, detener cámara, gamepad y servidor móvil
y liberar las notas vivas, igual que en la consola. Ofrecer controles para
reiniciar las fuentes después; no iniciarlas durante la reproducción.

## Fuentes, errores y cierre

Iniciar las fuentes sin impedir que se utilicen las demás si alguna falla.
La cámara y el gamepad realizan su conexión en hilos propios y notifican sus
fallos mediante registros: conectar esos avisos a la vista sin modificar sus
clases ni presentar el inicio del hilo como confirmación de conexión. El
servidor móvil puede informar fallos al iniciarse. El envío OSC por UDP no
confirma que SuperCollider esté disponible.

Mostrar los errores de almacenamiento, fuentes, reproducción y cierre y
conservar su detalle para diagnóstico. Un fallo de guardado puede dejar los
archivos inconsistentes: no anunciar éxito y respetar el bloqueo de la sesión
existente. No atribuir éxito a una operación fallida.

Al solicitar el cierre con grabación activa, detenerla y pedir un nombre;
si ya está pendiente, pedirlo directamente. No cerrar normalmente hasta
guardarla; permitir permanecer en la ventana para corregir el nombre. Si
ocurre un fallo que impida guardarla, informar la situación y no afirmar
que se preservó. Detener reproducción y fuentes y cerrar los recursos al
salir, informando cualquier error de cierre.

## Concurrencia

Actualizar componentes Swing exclusivamente en el hilo de eventos. Ejecutar
fuera de ese hilo las operaciones de archivos, el inicio y cierre de fuentes,
la espera de reproducción y el cierre de recursos. Coordinar las operaciones
y sus estados para evitar acciones simultáneas incompatibles y cambios de
interfaz fuera del hilo de eventos.

## Criterios de aceptación y verificación

Se aplican los criterios de la [historia](../stories/US-UI-01-editor-musical.md).
Crear pruebas enfocadas con archivos temporales y colaboradores simulados
para los estados de grabación, nombres inválidos y duplicados, carga,
reproducción, límites del monitor, errores y cierre, sin exigir hardware ni
una pantalla real cuando sea posible. Verificar compilación y registrar los
comandos y resultados reales una vez realizada la implementación.

La disponibilidad de bibliotecas nativas en Windows, la conexión de los tres
dispositivos, SuperCollider y el audio requieren validación aparte. No se
presentarán las pruebas simuladas como pruebas físicas.

## Estado de revisión

Joselyn confirmó la aprobación del equipo. La revisión del profesor permanece
pendiente.

## Verificación realizada

El 2026-11-16 se ejecutó:

```powershell
mvn -B '-Dtest=ControlEditorTest,ColaMonitorTest,RecordingsListPanelTest,SesionEditorTest,BusInstrumentosTest,PlaybackBufferThreadTest' test
```

Resultado: 29 pruebas sin fallos, errores ni omisiones. Incluyen nueve pruebas
de componentes y lógica de la ventana: límites y descarte del monitor,
paginación y selección, corrección de nombres, detención de fuentes antes de
reproducir, fallo al detenerlas y preparación del cierre con grabación activa,
pendiente o fallida. Las otras 20 cubren la sesión, el bus y el buffer existentes.
Las pruebas usan archivos temporales y colaboradores simulados; las del bus
también usan UDP local.

No se abrió ni comprobó visualmente la ventana completa, ni se probó con los
tres dispositivos o audio de SuperCollider. La disponibilidad de bibliotecas
nativas para Windows y la prueba física siguen pendientes.

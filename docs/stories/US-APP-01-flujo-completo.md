# US-APP-01: Flujo completo de Power Band

- **Estado de implementación:** Implementada; validación física pendiente
- **Estado de revisión:** Approved
- **Aprobado por:** Julian, 2026-11-16
- **Revisión del profesor:** Pendiente
- **Spec relacionada:** [Integración final por consola](../specs/15-integracion-final.md)

## Objetivo y alcance

Completar la aplicación por consola reutilizando instrumentos, archivos y
algoritmos existentes: grabar, detener, asignar nombre, indexar, listar,
cargar y reproducir. Centralizar constantes restantes y documentar el arranque.
Incorporar synths compatibles y liberar notas sostenidas al terminar el play.
No incluye interfaz gráfica, compra de hardware, commits ni publicación.

## Criterios de aceptación

1. `Main` inicia la aplicación y sus tres fuentes; las fuentes ausentes se
   informan sin bloquear las demás.
2. Al detener se deja de capturar para esa grabación, se solicita un nombre
   válido y único y se permite corregirlo sin perder lo ya grabado.
3. Al guardar se completa music, se agrega y ordena index con Balanced Merge
   Sort y se registra list en orden cronológico.
4. La consola permite listar, cargar por nombre y ejecutar play posteriormente.
5. Se controlan grabación y reproducción para evitar modificar archivos durante
   el play; errores se muestran sin anunciar guardado o reproducción exitosos.
6. El play libera notas de órgano al finalizar, fallar o interrumpirse.
7. El proyecto compila y se prueban los flujos completos con archivos temporales
   y emisores simulados. Las validaciones físicas se registran aparte.

## Resultado y verificaciones

Implementados nombre al finalizar, índice automático, listado cronológico,
carga/play por consola, cierre de notas, entrada Main, constantes y documentación.
Se añadieron definiciones propias de synths compatibles con el contrato OSC,
sin afirmar que sean las originales del profesor.

Compilación Java con release 17 correcta. Se ejecutaron 98 pruebas automatizadas
relacionadas sin fallos, errores ni omisiones; detalle y comando en la
[spec](../specs/15-integracion-final.md#verificacion-realizada).
También se comprobó el rechazo de un argumento de puerto inválido sin abrir
dispositivos.

Pendientes: compilar y probar Android con SDK/Gradle, evaluar los synths en
SuperCollider, probar los tres dispositivos simultáneos y escuchar una grabación
tras reiniciar la aplicación. No se hicieron commits ni push. La revisión del
profesor sigue pendiente.

# US-UI-01: Vista del editor musical

- **Estado de implementación:** Implementada; validación visual y física pendiente
- **Estado de revisión:** Approved
- **Aprobado por:** El equipo, confirmada por Joselyn el 2026-11-16
- **Revisión del profesor:** Pendiente
- **Spec relacionada:** [Editor musical en Swing](../specs/16-editor-musical-swing.md)

## Objetivo y alcance

Ofrecer una ventana Swing separada para monitorear comandos en vivo, grabar,
asignar un nombre y seleccionar grabaciones para cargarlas y reproducirlas.
Reutilizar la sesión y los instrumentos existentes sin modificar las clases
implementadas por el compañero ni reemplazar la entrada por consola.

## Criterios de aceptación

1. La ventana muestra los comandos en vivo y conserva como máximo los 200 más
   recientes en el monitor, sin alterar los comandos grabados.
2. Permite iniciar y detener una grabación; al detener solicita un nombre válido
   y único y permite corregirlo sin perder los comandos capturados.
3. Muestra las grabaciones en orden cronológico y permite seleccionar, cargar,
   reproducir y detener una grabación.
4. Antes de reproducir detiene las fuentes en vivo y ofrece reiniciarlas después.
   Una fuente no disponible no impide usar las demás y se informa el problema.
5. Si se intenta cerrar con una grabación activa o pendiente de nombre, se
   finaliza y guarda antes de salir; se puede continuar en la ventana para
   corregir el nombre.
6. La ventana permanece receptiva durante operaciones de archivos, fuentes y
   reproducción; los errores se muestran sin anunciar un éxito inexistente.
7. Se comprueba el comportamiento con pruebas automatizadas sin depender de
   dispositivos físicos; las pruebas físicas se registran por separado.

## Revisión y verificaciones

Joselyn confirmó la aprobación del equipo. No se ha atribuido aprobación al
profesor.

La ventana se implementó como entrada separada, sin modificar las clases
implementadas por el compañero. Se ejecutaron 29 pruebas relacionadas sin
fallos; los detalles están en la [spec](../specs/16-editor-musical-swing.md).
La apertura visual de la ventana y las pruebas con dispositivos y audio real
quedan pendientes.

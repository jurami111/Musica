# Instrucciones de trabajo para Power Band

## Desarrollo guiado por especificaciones

Antes de cambiar código por una tarea funcional:

1. Crear o actualizar una historia en `docs/stories/` con su objetivo y criterios
   de aceptación.
2. Crear o actualizar la spec relacionada en `docs/specs/`, incluyendo alcance,
   reglas y decisiones necesarias para implementar la historia.
3. Dejar la revisión como `Pendiente` hasta que el equipo revise y acepte la
   historia y la spec. Entonces usar `Approved` y registrar quién aprobó.
   Indicar por separado si la revisión del profesor sigue pendiente. Nunca
   presentar la aprobación del equipo como aprobación del profesor.
4. No comenzar la implementación hasta que la historia y la spec hayan sido
   revisadas y aprobadas.
5. Después de implementar, actualizar el estado de implementación y anotar las
   verificaciones realizadas. No presentar una compilación como si fueran
   pruebas automatizadas.

Cada historia y spec debe indicar al menos su identificador, estado, alcance y
criterios de aceptación. Mantener enlaces entre ambas. Para una tarea nueva,
explicar al usuario la historia y la spec propuestas antes de pedir aprobación.
No inventar aprobaciones ni decisiones del profesor.

## Estilo del proyecto

- Mantener el código sencillo, legible y con responsabilidades claras.
- Usar nombres nuevos de clases, métodos y variables en español, excepto los
  nombres ya existentes o los que el profesor o una herramienta exijan conservar.
- No usar excepciones para controlar el flujo normal del programa.
- Preservar el trabajo existente y limitar cada cambio a la tarea aprobada.

# US-EXC-01: Excepciones personalizadas

- **Estado de implementación:** Implementada
- **Estado de revisión:** Approved
- **Aprobado por:** Equipo de trabajo
- **Fecha de aprobación:** 2026-11-14
- **Revisión del profesor:** Pendiente
- **Spec relacionada:** [Excepciones personalizadas](../specs/05-excepciones-personalizadas.md)

## Historia

Como integrante del equipo, quiero identificar los errores de almacenamiento,
archivos corruptos, nombres duplicados y conexión mediante excepciones
personalizadas, para informar los fallos con claridad sin mezclar esos casos.

## Criterios de aceptación

1. Existe una excepción base del proyecto que conserva un mensaje y puede
   conservar la causa original.
2. Hay tipos específicos para errores de I/O, archivos corruptos, ordenamiento
   en disco, nombres duplicados y conexión de dispositivos.
3. Los nombres existentes de las clases se conservan; la excepción para
   archivos corruptos se llama `ArchivoCorruptoException`.
4. Los nombres duplicados se comprueban con una condición para manejar la
   solicitud normal de otro nombre; no se usan excepciones para repetir ese
   flujo.
5. El proyecto compila con las excepciones personalizadas.

## Verificación realizada

`mvn -B test` terminó correctamente y compiló el proyecto. El proyecto todavía
no contiene pruebas automatizadas.

# US-INDEX-01: Registros del indice

- **Estado de implementacion:** Implementada
- **Estado de revision:** Approved
- **Aprobado por:** Integrante del equipo que solicito la tarea
- **Fecha de aprobacion:** 2026-11-16
- **Revision del profesor:** Pendiente
- **Spec:** [SPEC-INDEX-01](../specs/10-registros-indice.md)

## Historia

Como integrante del equipo, quiero escribir y leer registros de indice de
longitud fija para asociar cada nombre con la posicion de su encabezado en
recordings.music, sin cargar el indice completo en memoria.

## Criterios de aceptacion

1. Cada registro ocupa exactamente 256 bytes: nombre, posicion y relleno.
2. Se reutilizan las reglas y la codificacion de nombres existentes.
3. Las posiciones son long no negativos y multiplos de 256.
4. Se pueden agregar registros y recuperarlos por numero, aun tras cerrar
   y reabrir el archivo.
5. Se reportan datos corruptos y fallos de disco sin ocultar el error.
6. Las pruebas verifican los bytes reales, el relleno, la lectura y errores.

No incluye ordenamiento externo, busqueda binaria, actualizacion automatica
del indice ni verificacion de la referencia contra recordings.music.

## Verificacion

`mvn -B -Dtest=IndexFileManagerTest,MusicFileManagerTest,ListFileManagerTest test`
compilo el proyecto y ejecuto 41 casos: 12 del indice, 20 de music y 9 de list,
sin fallos, errores ni omisiones. Se usaron archivos temporales.

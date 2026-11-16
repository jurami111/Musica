# US-LIST-01: Lista cronologica y nombres unicos

- **Estado de implementacion:** Implementada
- **Estado de revision:** Approved
- **Aprobado por:** Usuaria, como integrante del equipo
- **Fecha de aprobacion:** 2026-11-15
- **Revision del profesor:** Pendiente
- **Spec:** [SPEC-LIST-01](../specs/08-lista-y-nombres-unicos.md)

## Historia

Como usuario, quiero consultar las grabaciones en orden de registro y recibir
un rechazo si intento registrar un nombre existente, para identificar y
recuperar mis grabaciones sin nombres ambiguos.

## Alcance y criterios de aceptacion

1. recordings.list contiene solo nombre y fecha/hora, en registros de 108 bytes.
2. Cada entrada se agrega al final, sin ordenar ni cargar la lista en memoria.
3. La lectura permite contar registros y recuperarlos individualmente en orden.
4. existeNombre permite comprobar duplicados mediante una condicion. registrar
   repite la comprobacion antes de escribir y protege la unicidad con
   DuplicateNameException, sin usar excepciones para el ciclo de la interfaz.
5. Se conserva la comparacion exacta, incluyendo mayusculas y espacios.
6. La reapertura no pierde datos; los duplicados no modifican el archivo.
7. Los fallos de disco conservan su causa y los registros truncados o invalidos
   se reportan como ArchivoCorruptoException.
8. La validacion y codificacion de nombres se comparten con recordings.music.

No incluye indice, busqueda binaria, interfaz, ni coordinacion transaccional
con recordings.music. La aprobacion se recibio antes de implementar.

## Verificacion

`mvn -B -Dtest=ListFileManagerTest,MusicFileManagerTest test` compilo el proyecto
y ejecuto 29 casos sin fallos, errores ni omisiones: 9 de lista/duplicados y
20 de regresion de recordings.music. Solo se usaron archivos temporales.

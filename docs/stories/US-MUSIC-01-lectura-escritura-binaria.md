# US-MUSIC-01: Lectura y escritura de recordings.music

- **Estado de implementacion:** Implementada
- **Estado de revision:** Approved
- **Aprobado por:** Usuaria, como integrante del equipo
- **Fecha de aprobacion:** 2026-11-15
- **Revision del profesor:** Pendiente
- **Spec:** [SPEC-MUSIC-01](../specs/07-lectura-escritura-music.md)

## Historia

Como integrante del equipo, quiero guardar y recuperar encabezados y comandos
musicales con registros binarios de 256 bytes para conservar sus datos y acceder
a ellos por posicion sin cargar todas las grabaciones en memoria.

## Alcance y criterios de aceptacion

1. Los modelos conservan los nombres RecordingHeader y SynthCommand y encapsulan
   los datos, con nombres nuevos en espanol.
2. Cada encabezado y comando ocupa exactamente 256 bytes en disco.
3. Reservar un encabezado permite escribir comandos antes de conocer el nombre;
   finalizarlo no mueve ni modifica sus comandos.
4. Se recuperan por posicion los campos originales, incluidos nombres con tildes,
   tiempos y parametros de piano, bajo y organo.
5. Se rechazan registros truncados, campos invalidos, posiciones inconsistentes
   y rellenos distintos de cero con ArchivoCorruptoException.
6. Los fallos de disco conservan su causa en StorageIOException.
7. Las pruebas utilizan archivos temporales y verifican los bytes reales.

No incluye indice, lista, unicidad de nombres, grabacion en vivo ni reproduccion.
La aprobacion se recibio antes de implementar esta historia.

## Verificacion

`mvn -B -Dtest=MusicFileManagerTest test` compilo el proyecto y ejecuto
20 casos de prueba: cero fallos, cero errores y ninguno omitido.
Las pruebas usaron archivos temporales, no las grabaciones del usuario.
No se ejecutaron pruebas de Android ni de sonido: no forman parte del alcance.

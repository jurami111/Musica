# US-PLAY-01: Carga y buffering de grabaciones

- **Estado de implementacion:** Implementada
- **Estado de revision:** Approved
- **Aprobado por:** Integrante del equipo que solicito la tarea
- **Fecha de aprobacion:** 2026-11-16
- **Revision del profesor:** Pendiente
- **Spec:** [SPEC-PLAY-01](../specs/13-carga-buffering.md)

## Historia

Como usuario quiero cargar una grabacion por nombre y reproducir sus comandos
con sus tiempos relativos, sin cargar la grabacion completa en memoria.

## Criterios de aceptacion

1. Cargar busca en el indice previamente ordenado, lee el encabezado y no inicia
   la reproduccion; un nombre ausente se informa como resultado vacio.
2. Play lee progresivamente hasta el final exclusivo usando un buffer acotado.
3. Los comandos se envian en orden tomando como origen el inicio de play, no
   el momento de carga; el tiempo de trabajo no se suma a cada intervalo.
4. Se informa la finalizacion o error de reproduccion al llamador.
5. Se rechazan errores de lectura, tiempos decrecientes y referencias erroneas.
6. Las pruebas utilizan archivos temporales, reloj y reproductor simulados.

No incluye modificar clases implementadas por el companero, conectar la interfaz,
ni actualizar el indice al grabar.

## Verificacion

`mvn -B -Dtest=PlaybackBufferThreadTest,DiskBinarySearchTest,MusicFileManagerTest test`
ejecuto 37 casos sin fallos: 9 de reproduccion, 8 de busqueda y 20 de music.
No se probaron dispositivos fisicos ni SuperCollider.

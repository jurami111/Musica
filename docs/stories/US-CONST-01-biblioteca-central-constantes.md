# US-CONST-01: Biblioteca central de constantes

- **Estado de implementación:** Implementada
- **Estado de revisión:** Approved
- **Aprobado por:** Equipo de trabajo
- **Fecha de aprobación:** 2026-11-14
- **Revisión del profesor:** Pendiente
- **Spec relacionada:** [Esquema de archivos binarios](../specs/03-esquema-archivos-binarios.md)

## Historia

Como integrante del equipo, quiero centralizar las rutas, los nombres de
archivos, los tamaños binarios y los valores de comunicación con SuperCollider,
para que el proyecto use una sola definición de cada valor.

## Criterios de aceptación

1. Las clases existentes `AppConstants`, `BinaryConstants` y `SynthConstants`
   conservan sus nombres.
2. Las rutas y nombres de los archivos de grabaciones están centralizados.
3. Los tamaños, límites y códigos del formato binario están centralizados.
4. La dirección, el puerto OSC y los nombres de los sintetizadores se definen
   en una biblioteca central.
5. Cada registro binario de `recordings.music` y `recordings.index` tiene un
   tamaño de 256 bytes, incluyendo su relleno.
6. El proyecto compila con las constantes implementadas.

## Verificación realizada

`mvn -B test` terminó correctamente y compiló el proyecto. El proyecto todavía
no contiene pruebas automatizadas.

# Esquema de archivos binarios

- **Historia relacionada:** [US-CONST-01: Biblioteca central de constantes](../stories/US-CONST-01-biblioteca-central-constantes.md)
- **Estado de revisión:** Approved
- **Aprobado por:** Equipo de trabajo
- **Fecha de aprobación:** 2026-11-14
- **Revisión del profesor:** Pendiente

## Constantes compartidas

- `AppConstants`: nombres de archivos y rutas relativas a la carpeta desde la que
  se ejecuta Java. Ejecutar desde la raiz del proyecto para utilizar `data`.
- `BinaryConstants`: tamanos, rellenos y codigos de registros y acciones.
- `SynthConstants`: destino OSC, nombres e identificadores de los sintetizadores
  y limites de notas MIDI.

Las clases conservan sus nombres existentes. Las constantes nuevas se nombran
en espanol. No se crean archivos ni directorios al consultar estas constantes.
El puerto OSC 57110 y los nombres de synths provienen del cliente del profesor.
Los identificadores binarios son propios del proyecto, no identificadores de
nodos de SuperCollider.

## Reglas comunes

Cada registro de `recordings.music` y `recordings.index` ocupa 256 bytes.
Este es el tamano de un registro, no del archivo completo. El espacio restante
se completa con bytes de valor cero. `recordings.list` queda fuera de esta regla.

Los enteros se escribiran con `writeInt` (4 bytes), las posiciones y tiempos con
`writeLong` (8 bytes) y los parametros con `writeDouble` (8 bytes), en el orden
indicado abajo. La lectura debera usar las operaciones correspondientes de
`DataInput`. Las operaciones de `DataOutput` utilizan orden big-endian.
`ObjectOutputStream` y `writeUTF` no se utilizaran para estos registros porque
su formato no garantiza los tamanos definidos aqui.

El nombre dispone de 50 unidades UTF-16, escritas mediante 50 operaciones
`writeChar` de 2 bytes cada una, sin BOM: 100 bytes en total. Los nombres cortos
se completaran con caracteres nulos. Los nombres vacios, con caracteres nulos
o mayores de 50 unidades se rechazaran mediante validacion, sin truncarlos.
Algunos simbolos, como ciertos emojis, ocupan dos unidades UTF-16.
Los nombres deben ser unicos; la comprobacion en disco corresponde al modulo
de almacenamiento y aun no esta implementada.

## Encabezado en recordings.music

| Campo, en orden | Tipo | Bytes |
|---|---|---:|
| Tipo de registro: encabezado (1) | int | 4 |
| Nombre | 50 char | 100 |
| Inicio de los comandos | long | 8 |
| Final exclusivo de los comandos | long | 8 |
| Relleno | bytes en cero | 136 |
| Total | | 256 |

Las posiciones son desplazamientos absolutos en bytes dentro de
`recordings.music`. El inicio apunta inmediatamente despues del encabezado.
El final apunta inmediatamente despues del ultimo comando.
Una grabacion sin comandos tiene inicio y final iguales.
Al implementar la grabacion se debera reservar el encabezado y completarlo
al finalizar, cuando se conozcan el nombre y la posicion final.

## Comando en recordings.music

| Campo, en orden | Tipo | Bytes |
|---|---|---:|
| Tipo de registro: comando (2) | int | 4 |
| Tiempo relativo al inicio, en nanosegundos | long | 8 |
| Identificador del sintetizador | int | 4 |
| Accion | int | 4 |
| Cantidad de parametros utilizados | int | 4 |
| Parametros: 16 posiciones | 16 double | 128 |
| Relleno | bytes en cero | 104 |
| Total | | 256 |

Las posiciones de parametros no utilizadas tambien se escribiran en cero.
La cantidad de parametros no puede ser negativa ni superar 16.
El tiempo se medira como tiempo transcurrido, no como fecha del sistema.

Identificadores: piano = 1, bajo = 2, organo = 3.
Acciones: tocar = 1, iniciar nota = 2, terminar nota = 3.
Los nombres OSC se conservan exactamente: `classicPiano`, `stringBass` y
`organ70s`.

Antes de implementar los comandos se debera acordar el significado y orden
de sus parametros para cada sintetizador y accion, incluyendo como asociar
el inicio y final de una nota del organo. Esta biblioteca reserva el espacio;
todavia no implementa ese contrato ni la reproduccion.

## Entrada en recordings.index

| Campo, en orden | Tipo | Bytes |
|---|---|---:|
| Nombre | 50 char | 100 |
| Posicion absoluta del encabezado en recordings.music | long | 8 |
| Relleno | bytes en cero | 148 |
| Total | | 256 |

El indice no tiene un encabezado adicional. El registro numero n, contando
desde cero, comienza en n * 256 bytes. Los calculos de posiciones utilizaran
`long`. El ordenamiento externo y la busqueda binaria deberan utilizar el mismo
criterio de comparacion de nombres, pendiente de definir antes de implementarlos.

## Alcance actual

Solo se implementa la biblioteca central de constantes. La escritura, lectura,
validacion de entradas, ordenamiento, busqueda y conexion OSC quedan pendientes.
El proyecto actual contiene clases vacias, por lo que aun no hay operaciones
con valores quemados que reemplazar. Los modulos futuros deberan reutilizar
estas constantes en lugar de repetir sus valores.

No se fija un maximo arbitrario de canciones o duracion de grabacion.
El puerto de un servidor para instrumentos externos se acordara cuando se
defina su comunicacion.
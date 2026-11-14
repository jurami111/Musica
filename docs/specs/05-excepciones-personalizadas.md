# Excepciones personalizadas

- **Historia relacionada:** [US-EXC-01: Excepciones personalizadas](../stories/US-EXC-01-excepciones-personalizadas.md)
- **Estado de implementación:** Implementada
- **Estado de revisión:** Approved
- **Aprobado por:** Equipo de trabajo
- **Fecha de aprobación:** 2026-11-14
- **Revisión del profesor:** Pendiente

## Objetivo

Definir tipos de excepción propios para los errores relevantes del proyecto,
conservar la causa técnica cuando corresponda y evitar usar excepciones para
controlar el flujo normal de entrada del usuario.

## Jerarquía

Todas las excepciones específicas heredan de `PowerBandException`, que a su vez
hereda de `Exception`:

```text
PowerBandException
├── StorageIOException
├── ArchivoCorruptoException
├── DiskSortException
├── DuplicateNameException
└── DeviceConnectionException
```

Los nombres existentes se mantienen. `ArchivoCorruptoException` identifica un
archivo que se pudo leer, pero no cumple el formato esperado.

## Contrato de las excepciones

- `PowerBandException` ofrece constructores con mensaje y con mensaje más causa.
- `StorageIOException` identifica fallos reales de lectura o escritura.
- `ArchivoCorruptoException` identifica datos binarios inválidos o inconsistentes.
- `DiskSortException` identifica fallos del ordenamiento externo.
- `DuplicateNameException` identifica el rechazo de un nombre repetido en una
  operación de almacenamiento.
- `DeviceConnectionException` identifica fallos de conexión con un dispositivo
  o con SuperCollider.
- Las excepciones que pueden envolver un fallo técnico conservan la causa
  original usando el constructor de mensaje y causa.
- Los duplicados se detectan primero mediante una condición para que la interfaz
  pueda solicitar otro nombre sin usar una excepción como mecanismo de repetición.

## Alcance

Esta spec documenta la jerarquía y los constructores ya implementados. La
integración de estas excepciones en los módulos de almacenamiento, ordenamiento
y red queda fuera de esta historia y deberá especificarse con sus respectivas
historias antes de implementarse.

## Verificación registrada

La compilación Maven terminó correctamente con `mvn -B test`. No había pruebas
automatizadas en el proyecto, por lo que este resultado confirma compilación,
no el comportamiento de operaciones de archivos o red.

## Revisión

Pendiente de revisión por el profesor o la persona responsable del proyecto.
No registrar una aprobación ni una fecha hasta recibir confirmación explícita.

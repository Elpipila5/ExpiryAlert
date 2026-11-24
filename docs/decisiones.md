# Decisiones y cambios pendientes

## DEC00 - Organización del repositorio

Fecha: 8 de octubre de 2026. Estado: **aplicado a la preparación documental**.

El usuario pidió preparar todo lo necesario para que los compañeros incorporen sus avances. Se crean documentación, estructura y tareas sin elegir un framework ni declarar implementadas funciones que aún no están en el repositorio.

El plan de trabajo organiza el backlog y el calendario. La conciliación con la Actividad 1 queda abierta en DEC01.

## DEC01 - Conciliar los documentos

Estado: **Pendiente de revisión con equipo/docente**.

- Actividad 1 excluye lectura de caducidad por fotografía; el plan exige F03 de OCR.
- Actividad 1 incluye código de barras; el plan no lo enumera.
- Actividad 1 incluye búsqueda/filtros; confirmar si se mantienen dentro de PB04.
- El plan añade consumo, verificación, bajo stock, desayuno y recetas.
- Confirmar variantes de los nombres del equipo y asignar responsabilidades iniciales.

Registrar el acuerdo, quién participó, fecha, historias afectadas y efecto en los hitos. Hasta entonces, los issues del plan son la organización de trabajo propuesta; no representan una aprobación nueva del alcance.

## DEC02 - Tecnología y reglas del proyecto existente

Estado: **Documentado a partir del código 1.4.0**, vinculado a T01. Ver arquitectura y modelo de datos implementados; la revisión del equipo de la base 1.3.0 fue comunicada el 24/11/2026.

Confirmar framework, SDK, plataformas, proveedor de autenticación, almacenamiento/nube, permisos y dependencias. Validar modelo lógico, unidades, redondeo, ventana de caducidad, periodicidad, bajo stock, tratamiento de productos que vencen hoy y selección alimentaria para recetas.

## Plantilla de cambio

| Campo | Contenido por registrar |
| --- | --- |
| ID y fecha | Identificador único y fecha de decisión. |
| Problema | Qué necesidad o contradicción se resuelve. |
| Alternativas | Opciones viables consideradas. |
| Acuerdo | Decisión y participantes que la validaron. |
| Impacto | Historias, fechas, datos, permisos y pruebas afectados. |
| Seguimiento | Issue/PR y evidencia de ejecución. |

## DEC03 - Fotografías opcionales por producto

Fecha: 24/11/2026. Solicitado por José Enrique al pedir completar el cronograma y buscar imágenes por nombre/marca. Se mantiene la personalización previa.

Se guarda una miniatura JPEG en el documento privado de Firestore, con fuente y página, sin requerir Storage. Se ofrecen sugerencias Open Food Facts, búsqueda Google Imágenes, galería y enlace directo. La API de Google está cerrada a nuevos clientes: el botón abre una búsqueda y no simula extracción automática. El usuario confirma la coincidencia antes de guardar. Límite JPEG 90 000 bytes/Base64 120 000 caracteres; compatibilidad con productos sin foto. Las reglas ampliadas se prueban con dos cuentas en emulador y necesitan publicación en el proyecto real.

No se infiere cantidad restante a partir de la imagen. Las recetas conservan el catálogo local y excluyen categorías no alimentarias reconocidas.

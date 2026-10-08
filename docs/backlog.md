# Product Backlog

Historias tomadas del plan de trabajo. La prioridad y los criterios se conservan; las tareas detalladas en GitHub sirven para descomponerlas. **Estado inicial: Pendiente de verificar para todas.** Los avances que ya existan deben demostrarse antes de cambiar este estado.

| ID | Historia | Prioridad | Función | Sprint | Criterio de aceptación |
| --- | --- | --- | --- | --- | --- |
| PB01 | Como usuario quiero crear una cuenta e iniciar sesión para conservar mi inventario. | Alta | F01 | S1; consolidación S5 | Autenticación funcional, sesión y error controlado ante credenciales inválidas. |
| PB02 | Como usuario quiero registrar manualmente un producto para controlar caducidad y cantidad. | Alta | F02 | S2 | Registro persistente con fecha, cantidad y unidad; edición y eliminación. |
| PB03 | Como usuario quiero fotografiar un producto para detectar fecha y cantidad del empaque. | Alta | F03 | S3 | OCR devuelve candidatos; el usuario confirma/corrige y luego guarda. |
| PB04 | Como usuario quiero ver el inventario clasificado para identificar qué requiere atención. | Alta | F04 | S2; estados S4 | Lista con vigentes, próximos a caducar, caducados y cantidad estimada. |
| PB05 | Como usuario quiero una alerta antes de la caducidad y confirmar si aún tengo el producto. | Alta | F05 | S4 | Notificación programada; respuesta actualiza estado o elimina/archiva. |
| PB06 | Como usuario quiero registrar cuánto consumí para estimar cuánto queda. | Alta | F06 | S6 | Descuento correcto por unidad; rechazo de valores imposibles. |
| PB07 | Como usuario quiero corregir periódicamente el inventario estimado. | Alta | F07 | S6 | Confirmación periódica y ajuste manual disponibles. |
| PB08 | Como usuario quiero saber cuándo un producto está por terminarse para comprar más. | Media | F08 | S7 | Umbral configurable o predeterminado; aviso y opción de lista de compras. |
| PB09 | Como usuario quiero indicar mi horario de desayuno para recibir una pregunta oportuna. | Media | F09 | S8 | Hora configurable y permisos opcionales; funciona sin acceso a alarmas del sistema. |
| PB10 | Como usuario quiero recetas relacionadas con mis productos para aprovecharlos. | Media | F10 | S7 | Consulta externa o enlaces a YouTube; prioriza productos próximos a caducar. |
| PB11 | Como equipo queremos pruebas, seguridad y registro de errores para una entrega estable. | Alta | Transversal | S1-S9 | Flujos críticos probados, permisos revisados, errores controlados y compilación distribuible. |

## Cómo mantenerlo

Cada historia tiene un issue real en [GitHub Issues](https://github.com/Elpipila5/ExpiryAlert/issues). El índice con enlaces está en [issues](issues.md). Allí se acuerdan responsable, estado, subtareas, revisión y evidencia.

El cierre del issue exige la [Definition of Done](equipo.md#definition-of-done). La cantidad de commits no demuestra por sí sola que una historia funciona. Tampoco se cuenta una historia como terminada solo por pasar su fecha planeada.

## Trabajo de preparación

- **T01:** incorporar y ejecutar el proyecto existente, confirmar tecnología y documentar instalación.
- **DEC01:** conciliar OCR, código de barras, búsqueda/filtros y variantes de los nombres del equipo entre documentos.

Estos elementos no sustituyen PB01-PB11. No se han asignado usuarios de GitHub a las historias porque aún no se han verificado los perfiles de los compañeros.

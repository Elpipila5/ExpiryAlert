# ExpiryAlert

Aplicación para la detección de productos que ya van a expirar, ya expiraron o están a punto de terminarse.

Permite controlar la caducidad y las cantidades de un inventario personal. El proyecto pertenece a **Desarrollo Móvil Integral**, grupo **IDGS10**, Universidad Tecnológica de Montemorelos, septiembre-noviembre de 2026.

**Estado del repositorio:** base de colaboración y planificación. El código que el equipo ya tiene debe incorporarse mediante un pull request. Las funciones del backlog permanecen pendientes de verificar; esta documentación no acredita que estén implementadas.

## Empieza aquí

1. Consulta las [tareas abiertas](https://github.com/Elpipila5/ExpiryAlert/issues) y toma una tarea pequeña.
2. Sigue la [guía para subir avances](CONTRIBUTING.md).
3. Incorpora el proyecto existente dentro de [`app/`](app/README.md), conservando su estructura y dependencias.
4. Abre un [pull request](https://github.com/Elpipila5/ExpiryAlert/pulls) hacia `main` y pide revisión a otro integrante.
5. Registra las pruebas y evidencias antes de marcar la tarea como terminada.

**Acceso del equipo:** el propietario debe añadir los usuarios de GitHub de sus compañeros en **Settings > Collaborators** y ellos deben aceptar la invitación. Si alguien aún no tiene permiso de escritura, puede trabajar desde un fork y abrir un pull request. Consulta [los pasos](CONTRIBUTING.md#1-acceso-y-copia-del-repositorio).

## Qué debe desarrollar la aplicación

El [plan de trabajo](docs/referencias/README.md#plan-de-trabajo) se usa como referencia operativa para organizar este repositorio. Incluye autenticación, registro manual, OCR con confirmación, inventario, avisos de caducidad, consumo, verificación periódica, bajo stock, lista de compras, horario de desayuno y recomendaciones externas.

La [Actividad 1](docs/referencias/README.md#actividad-1) tiene diferencias respecto al plan: excluye OCR e incluye código de barras y búsqueda/filtros. La conciliación está registrada en [alcance](docs/alcance.md) y [decisiones](docs/decisiones.md); el equipo debe validarla en una revisión. Los identificadores F01-F10 de este repositorio corresponden al **plan**, no a la numeración de la Actividad 1.

## Organización

| Ruta | Contenido |
| --- | --- |
| [`app/`](app/README.md) | Proyecto móvil existente y futuros avances de código. |
| [`docs/`](docs/README.md) | Alcance, backlog, calendario, arquitectura propuesta y modelo de datos. |
| [`docs/pruebas/`](docs/pruebas/README.md) | Casos funcionales y resultados por registrar. |
| [`docs/evidencias/`](docs/evidencias/README.md) | Capturas, demos y registros de cada sprint. |
| [`docs/referencias/`](docs/referencias/README.md) | Identificación de los documentos fuente y las secciones utilizadas. |
| [`tests/`](tests/README.md) | Guía para ubicar pruebas automatizadas según la tecnología real. |
| [`assets/`](assets/README.md) | Recursos compartidos de diseño. |
| [`.github/`](.github/PULL_REQUEST_TEMPLATE.md) | Plantillas de tareas, errores y pull requests. |

## Equipo y entregas

- José Enrique Guajardo Lugo.
- Martin Abelardo Gutiérrez Alcocer.
- Carlos Emanuel Calderón Franco.

Docente: **Ing. Mario Alberto Chacón Muñoz**. Las responsabilidades de producto, técnica y calidad están pendientes de asignación; los tres integrantes desarrollan y revisan código. Las variantes de nombres en los originales están anotadas en [equipo](docs/equipo.md).

| Hito | Fecha | Resultado esperado según el plan |
| --- | --- | --- |
| Sprint 5 | 26-30 de octubre de 2026 | Núcleo F01-F05 integrado y al menos aproximadamente 50 % demostrable. |
| Sprint 8 | 16-20 de noviembre de 2026 | Versión candidata, prácticamente todo el alcance implementado. |
| Presentación final | 24 y 25 de noviembre de 2026 | Aplicación distribuible, código, pruebas y documentación. |
| Cierre | 26 y 27 de noviembre de 2026 | Cierre y retroalimentación. |

Consulta los [nueve sprints](docs/sprints.md), la [Definition of Done](docs/equipo.md#definition-of-done) y la [instalación](docs/instalacion.md). Los comandos de ejecución se documentarán al incorporar el código y confirmar la tecnología que ya utiliza el equipo.

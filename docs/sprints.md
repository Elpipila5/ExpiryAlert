# Nueve sprints y calendario

Periodo general del plan: **22 de septiembre a 27 de noviembre de 2026**. La tabla de sprints comienza el 28 de septiembre. Los días 22-27 quedan como preparación previa; no se inventa un décimo sprint.

| Sprint | Periodo 2026 | Sprint Goal | Backlog/funciones | Incremento y evidencia |
| --- | --- | --- | --- | --- |
| S1 | 28 septiembre-2 octubre | Establecer una base técnica estable. | PB01 + arquitectura | Proyecto compilable, navegación y autenticación inicial; commits, capturas y demo. |
| S2 | 5-9 octubre | Lograr el primer flujo real de datos. | PB02, PB04 | CRUD manual e inventario persistente; prueba registrar, consultar y editar. |
| S3 | 12-16 octubre | Reducir captura manual mediante fotografía. | PB03 | Cámara, OCR y confirmación de fecha/cantidad; casos con distintos empaques. |
| S4 | 19-23 octubre | Convertir fechas en alertas accionables. | PB05; completar estados | Notificación y confirmación de existencia; fechas simuladas y capturas. |
| S5 | 26-30 octubre | Consolidar al menos aproximadamente 50 % demostrable. | PB01-PB05 + PB11 | Flujo completo integrado, nube/autenticación, build, pruebas y aportes de los tres integrantes. |
| S6 | 2-6 noviembre | Estimar inventario restante de forma útil. | PB06, PB07 | Consumo, saldo y ajustes; pruebas numéricas y casos límite. |
| S7 | 9-13 noviembre | Ayudar a consumir y reponer productos. | PB08, PB10 | Bajo stock, lista de compras y recetas/enlaces; evidencia externa y manejo de fallos. |
| S8 | 16-20 noviembre | Completar y estabilizar la versión candidata. | PB09 + PB11 | Rutina configurable, permisos y regresión; build candidato e incidencias. |
| S9 | 23-27 noviembre | Liberar una versión final estable. | Correcciones y cierre | Versión distribuible, documentación, pruebas y presentación el 24-25. |

Los días **26 y 27 de noviembre** se reservan para cierre/retroalimentación; no amplían la entrega del 24-25.

## Hito de S5

- Navegación, registro e inicio de sesión.
- Registro manual y consulta del inventario.
- OCR con confirmación/corrección.
- Clasificación por caducidad, avisos y pregunta sobre existencia.
- Integración de datos/nube, seguridad inicial y pruebas de los flujos implementados.
- Build ejecutable, commits verificables de los tres integrantes y evidencia del alcance funcional.

## Hito de S8

Cantidades, correcciones, bajo stock, lista de compras, recetas y horario de desayuno integrados. Regresión, permisos y defectos críticos revisados antes de liberar.

## Registro de cada sprint

Copiar [la plantilla de sprint](evidencias/plantilla-sprint.md) a `docs/evidencias/sprint-XX/registro.md`. Registrar historias comprometidas, tareas con responsables, estados, build, review, retrospectiva y resultados reales. El calendario es una planificación; no acredita avances realizados.

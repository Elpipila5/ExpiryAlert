# Pruebas y defectos

La [matriz](matriz.md) prepara escenarios manuales; sus resultados se registran al ejecutarlos. Las pruebas automatizadas tienen informes separados y no aprueban por sí solas los flujos completos del teléfono.

- [Diseño y personalización 1.3.0](diseno-personalizacion.md): compilación, pruebas locales y comprobación del rediseño.
- [Cantidades, compras, desayuno y recetas](v21-sprints6-9.md): implementación y matriz funcional.
- [Cierre de la versión candidata](cierre-sprint9.md): estado actual y verificaciones de entrega pendientes.
- [Resultados automatizados y APK](https://github.com/Elpipila5/ExpiryAlert/actions).

## Registro de ejecución

Copiar una tabla por build dentro del sprint correspondiente:

| Caso | Commit/build | Dispositivo/SO | Fecha y ejecutor | Resultado real | Estado | Evidencia/defecto |
| --- | --- | --- | --- | --- | --- | --- |
| Por completar | Por completar | Por completar | Por completar | Por completar | No ejecutado | Por completar |

Estados: **No ejecutado**, **Aprobado**, **Fallido** o **Bloqueado**. Si falla, abrir un issue con la plantilla de error, pasos de reproducción y resultado esperado. Volver a comprobar el caso después de corregirlo.

La regresión incluye sesión, CRUD, OCR, caducidad, consumo, avisos, permisos y recetas. Para cálculos y fechas, usar datos controlados y comprobar los límites; para permisos y notificaciones, registrar el dispositivo/plataforma utilizados.

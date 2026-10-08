# Pruebas y defectos

La [matriz](matriz.md) prepara escenarios; **ninguno se da por ejecutado**. Añadir resultados al probar el código, identificando commit/build, dispositivo, fecha, responsable y evidencia.

## Registro de ejecución

Copiar una tabla por build dentro del sprint correspondiente:

| Caso | Commit/build | Dispositivo/SO | Fecha y ejecutor | Resultado real | Estado | Evidencia/defecto |
| --- | --- | --- | --- | --- | --- | --- |
| Por completar | Por completar | Por completar | Por completar | Por completar | No ejecutado | Por completar |

Estados: **No ejecutado**, **Aprobado**, **Fallido** o **Bloqueado**. Si falla, abrir un issue con la plantilla de error, pasos de reproducción y resultado esperado. Volver a comprobar el caso después de corregirlo.

La regresión incluye sesión, CRUD, OCR, caducidad, consumo, avisos, permisos y recetas. Para cálculos y fechas, usar datos controlados y comprobar los límites; para permisos y notificaciones, registrar el dispositivo/plataforma utilizados.

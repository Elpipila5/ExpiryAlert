# Registro del Sprint 05 — ExpiryAlert

**Periodo:** 26–30 de octubre de 2026.  
**Sprint Goal (según `docs/sprints.md`):** consolidar una demostración funcional de aproximadamente 50 % del alcance, integrando F01–F05 y seguridad inicial.  
**Rama probada:** `feature/sprint5-integracion-seguridad-pruebas`; **commit de código verificado en el dispositivo por el usuario:** `5517e5b` (28/10/2026).  
**PR:** [#18 — Integración, seguridad y pruebas](https://github.com/Elpipila5/ExpiryAlert/pull/18).  
**Dispositivo/versión Android:** no informados; pendientes de registrar.  
**Evidencias gráficas y APK:** pendientes de adjuntar por el equipo.

## Sprint Backlog

| Historia / área | Trabajo integrado | Estado del avance reportado | PR/evidencia |
| --- | --- | --- | --- |
| F01, autenticación | Ingreso y cierre de sesión; inventario de cuenta | Funcionamiento de inicio/cierre informado como aprobado | PR #18; faltan capturas |
| F02, inventario | Registro, edición, eliminación y listado activo | Aprobado por prueba del usuario; error de IDs corregido | `5517e5b`; falta captura |
| F03, fotografía / OCR | Escaneo y extracción de etiquetas | Funcionalidad probada y reportada aprobada | PR #18; falta captura |
| Código de barras (mejora del registro) | Escaneo y autocompletado | Reportado aprobado | PR #18; falta captura |
| F04, estado de caducidad | Clasificación, fecha pendiente y vista | Flujo integrado; cobertura de fechas límite aún pendiente | PR #18 |
| F05, alertas y confirmación | Alerta automática con app abierta; archivar desde notificación | Alerta y archivo reportados aprobados | PR #18; falta captura |
| F05, conservar producto | Opción «Sí, todavía lo tengo» | **Aprobado en la experiencia del teléfono (reportado por el usuario)**: tras responder y girar la pantalla ya no reaparece el diálogo | Confirmar en Firestore `ultimaConfirmacion` y adjuntar captura |
| PB11, seguridad Firestore | Reglas por `usuarioId` preparadas | **Pendiente de validar y publicar en Firebase** | `firestore.rules`; `docs/seguridad-firestore.md` |
| PB11, calidad y evidencia | Matriz y correcciones de defectos | En proceso | `docs/pruebas/sprint5-integracion.md` |

## Avance y defectos corregidos

| Fecha | Realizado y validación informada | Bloqueo/acuerdo |
| --- | --- | --- |
| 28/10/2026 | Compilación/login, registro/edición, escaneo por código y OCR reportados aprobados | Fotografías y salida de build por registrar |
| 28/10/2026 | Notificación llega tras guardar producto próximo sin pulsar botón manual | No implica que se haya probado WorkManager con la aplicación cerrada |
| 28/10/2026 | El usuario reportó bug: archivados visibles y borrado fallaba por falta de ID | Corregido en `InventarioProductos.productoVisible` |
| 28/10/2026 | Tras instalar commit `5517e5b`, usuario confirmó: archivados antiguos ocultos, archivar desde notificación funciona y Eliminar no falla por ID | Se conserva historial archivado en Firestore; no se borra automáticamente |

## Verificación de corrección de rotación (28/10/2026)

Después de reportar que al girar el teléfono reaparecía el diálogo con **ambas respuestas**, se incorporó un control de notificaciones atendidas y una corrección en el procesamiento del Intent de Android, con versión de aplicación `1.0.1-sprint5`. El usuario comunicó posteriormente: **«listo, ya funciona bien»**, en respuesta a la prueba de ambas opciones.

**Resultado reportado:** diálogo de confirmación ya no reaparece indebidamente al rotar. Esta validación es del comportamiento visible en el teléfono. **No se ha comprobado aún en esta conversación el valor concreto de `ultimaConfirmacion` dentro de Firestore, ni la publicación de las reglas de seguridad**. Captura, versión exacta del APK instalado y modelo de teléfono están pendientes de adjuntar.

## Pruebas funcionales reportadas

**Importante:** «Aprobado (informado)» significa ejecución comunicada por el usuario, no captura auditada ni prueba instrumentada automática.

| Comprobación | Resultado informado | Estado | Evidencia a adjuntar |
| --- | --- | --- | --- |
| Compila y permite iniciar sesión | Funciona | Aprobado (informado) | Captura de compilación y pantalla |
| Registrar y editar productos | Funciona | Aprobado (informado) | Antes y después |
| Código de barras y OCR | Funciona | Aprobado (informado) | Foto/reconocimiento sin datos privados |
| Notificación de producto próximo | Llegó sin presionar botón de revisión | Aprobado (informado) | Captura de notificación |
| Cierre de sesión y notificaciones privadas | Desaparecen | Aprobado (informado) | Antes/después |
| Archivar desde notificación | Desaparece del inventario activo | Aprobado (informado) | Diálogo y cambio de `activo=false` |
| Productos archivados antes de corregir | Dejan de mostrarse | Aprobado (informado) | Captura inventario actualizado |
| Eliminar producto activo | No aparece error de ID; se elimina | Aprobado (informado) | Captura antes/después |
| Conservar producto desde notificación | El usuario confirmó que las respuestas funcionan tras corregir la rotación | Aprobado (informado, funcionamiento visual); verificación de `ultimaConfirmacion` pendiente | Captura de producto visible y campo `ultimaConfirmacion` |
| Separación visual de inventarios entre cuentas | El usuario confirmó «cada quien tiene sus propios productos» | Aprobado (informado, en interfaz) | Capturas de inventarios con cuentas de prueba A/B, sin datos personales |
| Denegación de acceso cruzado mediante reglas publicadas | No confirmado con simulador ni reglas de servidor | Pendiente | Pruebas de acceso permitido y denegado en Firebase Rules Playground |
| Notificación periódica con app totalmente cerrada | No confirmado | Pendiente (opcional en demo; documentar comportamiento) | Evidencia de WorkManager, sin garantía de horario exacto |

## Sprint Review

**Fecha:** 28/10/2026 (revisión en curso).  
**Incremento probado:** app Android compilable con acceso de cuenta, inventario CRUD, escaneo de productos, OCR, aviso al guardar, archivo y eliminación.  
**Demostración completa:** respuesta «Sí, todavía lo tengo» reportada funcionando; quedan pendientes evidencia visual y verificación de `ultimaConfirmacion`, además de seguridad del aislamiento entre cuentas con reglas realmente publicadas.  
**Retroalimentación del equipo/docente:** no suministrada.  
**Build distribuible o enlace a APK:** pendiente.  
**Aportes de los tres integrantes:** no comprobados aún; registrar sus commits/revisiones, sin inferir identidades.

## Retrospectiva

- **Mantener:** validar cada flujo en un teléfono y registrar problemas concretos de interfaz y datos.
- **Corregir:** agregar pruebas de transformación de documentos y de estados de inventario antes de integrar.
- **Próximo sprint:** F06 consumo/saldo y F07 verificación y ajustes, **a partir del 2/11/2026** según cronograma.
- **Mejora pospuesta:** reconocimiento más cómodo de la fecha del envase, solicitado por el usuario, sin bloquear esta entrega.

## Cierre

No marcar Sprint 5 como «Terminado» hasta comprobar en Firebase la escritura de `ultimaConfirmacion` y las reglas publicadas, ejecutar pruebas de seguridad y adjuntar evidencias/build. Consultar la [Definition of Done](../../equipo.md#definition-of-done).

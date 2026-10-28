# Sprint 5 — Integración y demostración (26–30 octubre 2026)

**Estado:** código preparado, **pruebas reales por ejecutar y registrar**. La meta del cronograma es un flujo funcional de F01–F05 que demuestre al menos aproximadamente 50 % del alcance; no se declara cumplimiento por el mero hecho de tener código.

## Resultado comunicado durante la revisión del 28/10/2026

El probador confirmó inicialmente cuatro comprobaciones aprobadas y dos pendientes. En la prueba posterior confirmó **cinco comprobaciones aprobadas y una pendiente**, trabajando en la rama `feature/sprint5-integracion-seguridad-pruebas`. El modelo/version Android y evidencias visuales aún no se han informado. Estas observaciones **no reemplazan** la matriz completa de aceptación que aparece más abajo.

| Comprobación efectivamente informada | Estado informado | Evidencia pendiente |
| --- | --- | --- |
| Compilación de aplicación e inicio de sesión | Aprobado | Captura del build y sesión |
| Registrar y editar productos | Aprobado | Captura antes/después |
| Escaneo de códigos y OCR | Aprobado | Captura del reconocimiento |
| Aviso automático al guardar un producto con fecha próxima | **Aprobado (informado por el usuario)** | La notificación llegó sin pulsar «Revisar caducidades ahora»; falta captura |
| Archivar desde una notificación («Ya no lo tengo») | **Aprobado (informado por el usuario)** | El producto desaparece del inventario; falta captura de Firestore |
| Cierre de sesión oculta notificaciones privadas | Aprobado | Captura antes/después |

### Corrección del diagnóstico (28/10/2026)

**Resultado posterior comunicado:** tras actualizar la rama y ejecutar la aplicación, el usuario confirmó que **la notificación llega sin presionar el botón de revisión**. Se da por aprobada esa comprobación comunicada, pero **NO** la confirmación ni el archivado, todavía sin prueba reportada. Tampoco se ha demostrado en esta conversación una alerta entregada de manera autónoma por WorkManager con la app completamente cerrada.

Tras informar el usuario que solo aparecía **«Las alertas pueden tardar unos momentos»** y no llegaba ningún aviso, se reemplazó la comprobación manual a través de WorkManager por una consulta **inmediata** de Firestore desde la app abierta. La revisión periódica con WorkManager **se mantiene**, pero es independiente.

**Procedimiento actualizado (tras hacer Git Fetch/Pull y compilar esta rama):**

1. Iniciar sesión y registrar **un producto nuevo** con nombre distintivo, cantidad mayor a cero y fecha **29/10/2026** (fecha de mañana para la prueba del 28 de octubre).
2. Guardarlo: el código solicita una comprobación inmediata del inventario con Firestore. Verificar que haya conexión.
3. Si no aparece aviso, ir al inventario y pulsar **🔔 Revisar caducidades ahora**.
4. **Leer el texto del resultado debajo del botón**. Ya no aparece solamente un mensaje genérico:
   - `Se enviaron N alertas`: abrir la bandeja y tocar una; si no se ve, revisar ajustes del teléfono, modo No molestar y categoría/canal de notificaciones.
   - `No se pueden mostrar alertas`: activar permiso de notificaciones para ExpiryAlert y canal «Alertas de caducidad».
   - `Ninguno requiere aviso`: revisar la fecha real de `fechaCaducidad`, `cantidad > 0`, que `activo != false` y que corresponde a la cuenta actual.
   - `Ya se notificaron hoy` o `confirmaste hoy`: la app evita duplicados; crear un producto **distinto** para repetir la prueba, no reinstalar ni borrar datos personales.
   - `No se pudo consultar Firestore`: revisar internet y reglas publicadas en Firebase. Guardar captura de todo el error, especialmente `PERMISSION_DENIED`.
5. Si llega una notificación, tocarla para abrir **¿Todavía tienes este producto?**. Verificar ambas respuestas con **dos productos diferentes** y sus cambios en Firestore.
6. Registrar resultado real y evidencia antes de marcar los casos pendientes como aprobados.

No se afirma que WorkManager ejecute a una hora exacta: Android puede retrasar sus tareas. No se consideran finalizados los casos hasta observar que las notificaciones se muestran y la confirmación/archivado persiste.

### Prueba final pendiente: confirmar o archivar desde una notificación

> **Importante:** `ProgramadorAlertas.revisarAhora()` solicita la tarea a WorkManager; no garantiza ejecutarla al instante. Un producto ya notificado hoy tampoco generará un segundo aviso el mismo día. Usar **productos nuevos** para cada intento.

**Caso A — alerta real del producto recién creado (ya observada por el usuario; conservar como regresión)**

1. Confirmar sesión iniciada, conectividad y notificaciones habilitadas en Android; revisar también el canal **Alertas de caducidad**.
2. Crear un producto manual de prueba con nombre distintivo, cantidad mayor a 0 y caducidad mañana (ejemplo para 28/10/2026: **29/10/2026**). No dejar fecha vacía.
3. Guardarlo y confirmar que aparece en el inventario. Con la corrección de Sprint 5, el código solicita una revisión directa de Firestore con la app abierta; WorkManager mantiene la revisión periódica independiente.
4. No es necesario pulsar **🔔 Revisar caducidades ahora** si la notificación ya llegó automáticamente. El botón permanece para diagnósticos y demostraciones.
5. Dar oportunidad al sistema de ejecutar la tarea. Si aparece **Alerta de caducidad**, registrar captura y aprobar el caso; no exigir que el aviso sea inmediato.

**Caso B — verificar las dos respuestas (usar dos productos distintos)**

1. Para un producto de prueba nuevo, tocar la **notificación** (no esperar botones en el panel del teléfono). ExpiryAlert debe abrir un cuadro **¿Todavía tienes este producto?**.
2. Elegir **Sí, todavía lo tengo**. El documento debe mantener `activo` y escribir `ultimaConfirmacion` en Firestore; el producto sigue visible.
3. Con otro producto nuevo y su propia notificación, elegir **Ya no lo tengo**. El documento queda con `activo=false`, `cantidad=0`, se oculta del inventario y se cancela su notificación.
4. Capturar panel de notificaciones, diálogo y cambios de inventario (sin exponer UID/correo). No repetir con el mismo producto: la deduplicación diaria puede impedir nuevas alertas.

**Si la notificación no aparece:** comprobar que fecha no está en blanco, que el producto pertenece a la cuenta actual, que no está archivado ni con cantidad cero; verificar el canal y permiso de notificaciones. En Logcat filtrar por `ExpiryAlertCaducidad`, `WorkManager` y `PERMISSION_DENIED`. Si el trabajo aparece en cola o con restricción de red, no confundir demora de WorkManager con error de caducidad. Capturar el error completo.

**Si se abre la app pero no el diálogo:** registrar si estaba abierta o cerrada, la versión Android, si hubo cambio de usuario y los mensajes de Logcat. No marcar S5-08 ni S5-09 aprobados hasta observar la escritura real en Firestore y el cambio en inventario.

### Corrección del inventario archivado y del ID de producto (28/10/2026)

El usuario informó que al pulsar **«Ya no lo tengo»** los artículos seguían visibles y, al seleccionar **Eliminar**, aparecía **«No se encontró el ID del producto»**.

**Causa verificada en `MainActivity.kt`:** la transformación `mapNotNull` seguía devolviendo `datos` aunque `activo == false`. Al mismo tiempo, añadía `id` solo a los activos. Por tanto, los productos archivados se mostraban **sin ID**. Se corrigió utilizando `InventarioProductos.productoVisible(documento.id, documento.data)`, que devuelve `null` para archivados y asigna el ID real a todos los productos visibles. Se incluyen pruebas unitarias en `InventarioProductosTest.kt`.

**Validación en Android Studio (ejecutada y aprobada por el usuario en el commit `5517e5b`; conservar como regresión):**

1. Hacer Git Pull en la rama `feature/sprint5-integracion-seguridad-pruebas`, Sync Gradle y Run; ejecutar `InventarioProductosTest`.
2. Revisar si un artículo archivado antes de la corrección **ya no aparece en la lista**. Debe permanecer en Firestore con `activo=false` como historial (no se ha borrado de la base).
3. Crear **dos productos nuevos** con fecha de mañana. En la notificación del primero pulsar **«Sí, todavía lo tengo»**; debe permanecer en la lista y registrar `ultimaConfirmacion`.
4. En la notificación del segundo pulsar **«Ya no lo tengo»**; debe desaparecer de la lista, con `activo=false` y `cantidad=0` en Firestore.
5. Crear un **tercer producto activo**, pulsar **Eliminar** y confirmar. Debe borrarse el documento de Firestore sin mostrar **«No se encontró el ID del producto»**.
6. Verificar que los documentos de otro usuario no aparecen al cambiar de cuenta.

**Resultado posterior comunicado:** el usuario instaló la corrección `5517e5b` y aprobó las tres pruebas: los archivados anteriores desaparecen, «Ya no lo tengo» retira el producto y «Eliminar» funciona con ID real. La evidencia fotográfica sigue pendiente. 

### Validaciones comunicadas después de corregir el error de ID

El usuario confirmó expresamente las siguientes pruebas con el commit `5517e5b` de esta rama:

- **Aprobado:** productos archivados antes de la corrección ya no se muestran.
- **Aprobado:** «Ya no lo tengo» desde la notificación retira el producto del inventario.
- **Aprobado:** «Eliminar» un producto activo ya no da el error «No se encontró el ID».
- **Todavía no confirmado:** «Sí, todavía lo tengo» mantiene el producto y escribe `ultimaConfirmacion`.
- **Todavía no confirmado:** reglas del propietario publicadas y probadas en Firebase con dos cuentas.

El documento de evidencia del sprint está en [`docs/evidencias/sprint-05/registro.md`](../evidencias/sprint-05/registro.md). Los resultados se basan en el reporte del usuario; conservar capturas, modelo/version Android y APK sigue siendo necesario para documentar la entrega.

## Orden de demostración sugerido (flujo extremo a extremo)

1. Abrir ExpiryAlert e iniciar sesión con una **cuenta de prueba A**. Mostrar la lista de productos vinculada al usuario.
2. Registrar un producto manualmente (por ejemplo `Leche demo`, `Lácteos`, `1 L`) con fecha de mañana; verificar que aparece en inventario.
3. Pulsar **Revisar caducidades ahora** si el trabajo automático aún no se ejecuta. Abrir la notificación, elegir **Sí, todavía lo tengo** y comprobar que se guarda la confirmación.
4. Registrar un producto mediante **código de barras**, revisar datos sugeridos, opcionalmente usar **OCR de fecha impresa**, confirmar y guardar. Si el código no está en el catálogo o la fecha no se reconoce, usar el flujo de corrección manual.
5. Editar nombre/fecha/cantidad del producto y comprobar que se actualiza en tiempo real; mostrar que se ha solicitado revisión de caducidad tras guardarlo.
6. Abrir una notificación de otro producto y elegir **Ya no lo tengo**; debe desaparecer del inventario activo.
7. Eliminar un producto y verificar que desaparece; no debe quedar su alerta visible.
8. Cerrar sesión, iniciar sesión con una **cuenta de prueba B** y comprobar que no puede ver ni cambiar productos de A.
9. Ejecutar pruebas con permiso de notificaciones denegado: el inventario y guardado deben seguir funcionando.
10. Mostrar el resultado de las pruebas unitarias y la matriz de seguridad de Firestore **solo después de activarla y comprobarla en Firebase**.

## Casos a registrar

| ID | Comportamiento | Resultado esperado | Estado |
| --- | --- | --- | --- |
| S5-01 | Registro / login / sesión persistente | Flujo estable | No ejecutado |
| S5-02 | Producto manual con caducidad mañana | Guardado en Firestore y listado | No ejecutado |
| S5-03 | Edición de fecha y cantidad | Actualización visible y comprobación de alertas solicitada | No ejecutado |
| S5-04 | Eliminar producto | Sale del inventario; no se presenta el error de ID | **Aprobado (informado); captura pendiente** |
| S5-05 | Escanear código EAN/UPC | Autocompleta datos disponibles o permite corregir | **Aprobado (informado); captura pendiente** |
| S5-06 | OCR de fecha | Sugiere una fecha con confirmación obligatoria | **Aprobado (informado); captura pendiente** |
| S5-07 | Fecha pendiente | Guarda sin inventar fecha y no avisa hasta completar | No ejecutado |
| S5-08 | Notificación, respuesta «todavía lo tengo» | Marca `ultimaConfirmacion` | No ejecutado |
| S5-09 | Notificación, respuesta «ya no lo tengo» | Archiva y no aparece en productos activos | **Aprobado (informado); captura pendiente** |
| S5-10 | Sin permiso de notificaciones | CRUD sigue funcionando | No ejecutado |
| S5-11 | Dos cuentas diferentes y reglas publicadas | No hay acceso cruzado a productos | No ejecutado |
| S5-12 | Cierre de sesión con avisos visibles | No quedan avisos privados de la cuenta en el panel | **Aprobado (informado); captura pendiente** |
| S5-13 | Red desactivada durante consulta externa | Error controlado y registro manual disponible | No ejecutado |
| S5-14 | APK / Gradle | Compila e instala en dispositivo de prueba | **Aprobado (informado); APK/captura pendiente** |

## Registro de evidencias

Rellenar por cada caso: **commit/build**, teléfono/emulador y versión Android, fecha de ejecución, responsable, resultado observado, estado (aprobado/fallido/bloqueado) y captura o enlace a video. No marcar «Aprobado» sin ejecutar.

Usar los casos de [registro inteligente](registro-inteligente-codigos.md), [OCR](sprint5-ocr.md) y [seguridad Firestore](../seguridad-firestore.md) como guías complementarias.

## Limitaciones técnicas que deben explicarse en la exposición

- WorkManager realiza comprobaciones **aproximadas**, no alarmas exactas; para demostrar inmediatamente se usa el botón manual.
- Los EAN/UPC convencionales no contienen necesariamente la caducidad del envase. Puede quedar **pendiente** y no se emitirán alertas hasta conocerla.
- Las sugerencias de OCR deben confirmarse o corregirse.
- Las reglas de Firestore se vuelven efectivas **solo al desplegarlas al proyecto Firebase**.
- F06 y F07 (consumo y verificación periódica) comienzan la semana del 2–6 de noviembre.

## Entrega para el Sprint Review

Conservar build APK de depuración o distribuible, captura de la matriz cumplimentada, notificación funcional, captura OCR/registro, Firestore (sin datos personales), y commits de los tres integrantes. El cronograma pide una demostración ejecutable, no solamente documentación.

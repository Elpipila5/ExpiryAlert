# Integración de recetas y funcionalidades del cronograma en v21

Versión Android: `1.2.0-v21`, versionCode 5. Base: rama `v21`, commit `1fd11f4`.

Esta versión conserva el perfil y el diseño minimalista de v21, integra las recetas de main con la lista de compras y añade los flujos funcionales de los sprints 6, 7 y 8. La aceptación en un teléfono y la revisión del equipo continúan siendo necesarias para cerrar el sprint 9.

## Funciones implementadas

| Función del plan | Implementación y acceso |
| --- | --- |
| F06 Consumo y cantidades | Botones **Consumir** y **Ajustar cantidad** en cada producto. Consumo desde el saldo actual mediante transacciones Firestore, sin saldos negativos. Conversión kg/g y L/ml, con seis decimales de precisión. |
| F07 Verificación periódica | **Revisar cantidades** en la despensa. Confirmar saldo o corregirlo. Intervalo de 7 días por defecto, configurable entre 1 y 90 días. Recordatorio diario si hay revisiones pendientes. |
| F08 Bajo stock y compras | Umbral de 20% de la cantidad inicial por defecto, configurable de 1 a 100%. Incluye agotados; excluye archivados. Aviso, botón de reposición y lista con alta, edición, marcado y eliminación. |
| F09 Desayuno | **Mi desayuno** y horario configurable desde **Perfil → Inventario y horario de desayuno**. Recordatorio aproximado mediante WorkManager. Pregunta qué se consumió y permite registrar varios productos de forma explícita. |
| F10 Recetas | Las 28 recetas locales de main, con las tarjetas y colores de v21. Completas, parciales e ideas sin inventario; faltantes, copia, incorporación a compras, pasos y búsqueda en YouTube. |

Los productos continúan en Firestore con la consulta por `usuarioId`. Las compras y preferencias se guardan localmente, separadas por UID, para funcionar sin un servicio nuevo. Se conservan al cerrar sesión y se recuperan al volver a la misma cuenta en el mismo dispositivo; no se sincronizan entre dispositivos ni sobreviven a desinstalar o borrar datos.

No se añadieron campos ni colecciones que requieran ampliar las reglas actuales de Firestore: se actualizan `cantidad`, `cantidadInicial` y `ultimaConfirmacion`. Esto no confirma cuáles reglas están publicadas en el proyecto Firebase. Un error de permisos debe verificarse en la consola con la cuenta de prueba.

## Reglas de cantidades

Un consumo se descuenta del saldo leído en una transacción, no de una copia antigua mostrada en pantalla. Se rechazan valores negativos, cero para consumo, cantidades no finitas, consumos superiores al saldo y conversiones entre unidades incompatibles. Se permiten fracciones de pieza. Las unidades personalizadas solo admiten consumo en esa misma unidad.

El ajuste admite cero y valores hasta la cantidad inicial del lote. Un producto nuevo representa una compra o lote nuevo, con su propia caducidad. Editar nombre, categoría, marca o fecha no sobrescribe cantidades cambiadas por otro cliente. Para productos antiguos cuya cantidad inicial no exista, la primera operación adopta el saldo actual como base.

La cantidad cero se conserva visible como agotada para facilitar la reposición y se excluye de recetas y desayuno. Archivar continúa ocultando el producto. Abrir una receta, marcar una compra o abrir un aviso no descuenta cantidades automáticamente.

`ultimaConfirmacion` sirve tanto para la confirmación de existencia/cantidad desde un aviso como para las revisiones y ajustes. El aviso de caducidad ahora muestra la cantidad estimada antes de confirmar.

## Notificaciones y sesión

La revisión diaria ya existente ahora comprueba también stock y cantidades pendientes. Los avisos se deduplican por cuenta, tipo y día. Al tocar un aviso se abre Compras, Revisar cantidades o Mi desayuno, según corresponda. El cierre de sesión cancela los trabajos y oculta las notificaciones de esa cuenta.

El desayuno está desactivado inicialmente. Se programa al guardar un horario y se restaura al volver a iniciar sesión. WorkManager no garantiza la hora exacta bajo ahorro de batería o Doze; no se solicita acceso a alarmas exactas. Sin permiso de notificaciones, las funciones siguen disponibles desde la aplicación.

## Verificación automatizada

La compilación Android y las pruebas unitarias se ejecutaron con `:app:testDebugUnitTest :app:assembleDebug`. Resultado: **compilación correcta, APK de depuración generado y 62 pruebas aprobadas, cero fallos**. Incluyen 29 casos nuevos de cantidades, unidades, bajo stock y horario, además de recetas, fechas, inventario, OCR y códigos de barras. Se utilizó Kotlin 2.2.10 y JUnit 4.13.2 sobre los archivos de producción.

El APK generado corresponde a `com.example.expiryalert`, versión `1.2.0-v21`, versionCode 5 y minSdk 24. La generación del APK no confirma la aceptación funcional en un teléfono.

La verificación final también ejecutó `:app:lintDebug`, con cero errores, y `:app:assembleDebugAndroidTest`, que compiló correctamente el APK de pruebas. Se incorporaron cuatro pruebas Android de persistencia, separación de cuentas, deduplicación de compras y preferencias. Su ejecución requiere un emulador o dispositivo y sigue pendiente; no se marcan como aprobadas por el hecho de compilar.

En Android Studio:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
.\gradlew.bat :app:assembleDebugAndroidTest
.\gradlew.bat :app:connectedDebugAndroidTest
```

## Matriz de aceptación en dispositivo

| Caso | Pasos y resultado esperado | Estado |
| --- | --- | --- |
| V21-01 | Instalar y comprobar versión 1.2.0-v21; perfil, inicio y tarjetas conservan el diseño de v21. | Pendiente |
| V21-02 | Sin inventario: recetas muestra ideas con todos los ingredientes por comprar. | Pendiente |
| V21-03 | Solo tortillas: quesadillas con queso faltante. Añadir queso: quesadillas completas. | Pendiente |
| V21-04 | Agregar faltantes de una receta a Compras dos veces: no duplicar pendientes. Copiar faltantes y abrir YouTube. | Pendiente |
| V21-05 | Leche 1 L: consumir 250 ml y comprobar saldo 0.75 L en inventario y recetas. | Pendiente |
| V21-06 | Rechazar consumo cero, negativo, superior al saldo o gramos para un producto en litros. | Pendiente |
| V21-07 | Agotar el saldo: mostrar Agotado; excluirlo de recetas y desayuno; ofrecer compras. | Pendiente |
| V21-08 | Corregir cantidad; el consumo siguiente parte de la cantidad corregida. Ajustar a cero y rechazar un ajuste mayor que la inicial. | Pendiente |
| V21-09 | Dos clientes consumen el mismo producto: saldo coherente, sin cantidades negativas ni pérdida de una actualización. | Pendiente |
| V21-10 | Configurar umbral 50%, dejar 40% y comprobar aviso y sugerencia de reposición. Cambiar a 20% y comprobar el criterio. | Pendiente |
| V21-11 | Crear, editar, marcar, quitar y reabrir Compras; reiniciar la aplicación y comprobar persistencia. | Pendiente |
| V21-12 | Revisar cantidades vencidas según intervalo; confirmar o ajustar y comprobar que dejan de estar pendientes. | Pendiente |
| V21-13 | Activar desayuno a una hora futura; tocar el aviso y registrar consumo. Desactivar horario: cancelar trabajo. | Pendiente |
| V21-14 | Denegar notificaciones: inventario, recetas, compras y registro manual de desayuno siguen disponibles. | Pendiente |
| V21-15 | Cerrar A y entrar B: inventario, compras, preferencias y avisos permanecen separados. Volver A recupera su lista. | Pendiente |
| V21-16 | Sin conexión: compras funcionan localmente y las transacciones muestran error sin simular un guardado exitoso. | Pendiente |
| V21-17 | Girar el teléfono en recetas, compras y nuevas pantallas. Girar tras atender un aviso no repite su pregunta. | Pendiente |
| V21-18 | Regresión: registro, login, CRUD, OCR, código de barras, archivo y alertas de caducidad. | Pendiente |

## Cierre del sprint 9

Antes de declarar la entrega final: completar la matriz en un dispositivo real, guardar capturas o video, ejecutar las pruebas instrumentadas, revisar las reglas publicadas y obtener la revisión de otro integrante. Generar el APK/AAB de distribución con las claves del equipo; no subir claves privadas al repositorio ni atribuir revisiones o pruebas que no se realizaron.

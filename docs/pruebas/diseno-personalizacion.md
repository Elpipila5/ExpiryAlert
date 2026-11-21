# Diseño y personalización — 1.3.0

Versión Android: `1.3.0`, versionCode 6. Continúa la integración de los sprints 6 a 8 incluida en main.

## Cambios

- Inicio con identidad verde y menta, bienvenida, accesos con iconos y tarjetas de producto.
- «Nuevo producto» ocupa todo el ancho y crece si el texto necesita más altura.
- El resumen del perfil usa dos columnas en un teléfono normal. Las etiquetas pueden ocupar varias líneas; con fuentes grandes se reduce el número de columnas.
- El botón de luna/sol está disponible en inicio, despensa y cabeceras. El perfil también incluye un interruptor. La elección se guarda por dispositivo; antes de elegir se sigue el tema del sistema.
- El selector de fotos de Android permite agregar o cambiar la imagen. Se copia a almacenamiento privado, se orienta y recorta al centro, con límite de 768 × 768 píxeles. El avatar de inicio y del perfil se actualiza.
- Las fotos se separan por UID. Quitar una no modifica la de otra cuenta. La copia sigue disponible aunque se borre el original de la galería.
- La foto es local: no se sincroniza entre dispositivos y se elimina al borrar datos o desinstalar la aplicación.
- Se mantiene la edición de nombre con Firebase Auth y el resumen usa el inventario activo en tiempo real.

## Verificación

[Android CI completó la compilación y validación](https://github.com/Elpipila5/ExpiryAlert/actions/runs/44871523149) del código `ab785db4fd4e17fb1c9b9acc422634e94674d6a2`:

- 72 pruebas locales aprobadas, sin fallos: 62 de la lógica existente y 10 nuevas de diseño y personalización.
- `:app:assembleDebug`: APK de aplicación generado.
- `:app:assembleDebugAndroidTest`: APK de pruebas instrumentadas generado.
- `:app:lintDebug`: completado sin errores.
- Informes, capturas renderizadas y APK conservados como artefactos de esa ejecución.

Se revisaron capturas locales del inicio claro, oscuro y del perfil. Los 10 casos nuevos comprueban: botón a 320 dp y fuente 1.6, resumen a 320 dp, resumen oscuro con fuente 1.5, cambio efectivo del fondo y persistencia del tema, inicio y producto en ambos temas, perfil y resumen, persistencia de foto tras borrar su origen, separación de cuentas y eliminación aislada.

Las comprobaciones de texto miden el ancho de cada línea dibujada y rechazan elipsis y recorte de altura. Se evita confundir el ancho reservado de un párrafo centrado con su texto visible.

Comandos:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
.\gradlew.bat :app:connectedDebugAndroidTest
```

Las capturas locales se generan en `app/build/reports/diseno`. CI usa JDK 25 para Gradle y JDK 21 para las pruebas locales. Robolectric usa API 27 con gráficos nativos.

### Ejecución nativa posterior — 21/11/2026

La [ejecución en Android 15](https://github.com/Elpipila5/ExpiryAlert/actions/runs/44928823005) del código `6a5a97045e465a94c9f41a00e663606bc2bb59ba` aprobó **18 pruebas instrumentadas**, con cero fallos, errores o casos omitidos. Incluye los 10 casos compartidos de diseño/fotos, cuatro de compras/preferencias, dos de avisos atendidos, el contexto de aplicación y una foto de prueba entregada mediante `content://`.

La prueba de URI de contenido ejecuta `ImageDecoder` en API 35 y confirma que la copia guardada permanece después de borrar el original. La selección interactiva en la galería del usuario sigue pendiente de comprobación en su teléfono.

Se revisaron capturas nativas del inicio oscuro y del perfil. El informe incluye 11 imágenes. El cambio de tema se mide en el centro del contenido y espera el fotograma actualizado; las esquinas pueden estar cubiertas por las barras del sistema. Las capturas adicionales se guardan mediante `additionalTestOutputDir` y Gradle las recupera antes de desinstalar los APK de prueba, en `app/build/outputs/connected_android_test_additional_output/`.

Estos resultados corresponden a un emulador temporal y no acreditan la aceptación en el teléfono ni la configuración real de Firebase. El estado completo está en [la lista de cierre](cierre-sprint9.md).

## Comprobar en el teléfono

| Caso | Resultado esperado |
| --- | --- |
| Abrir la despensa y el perfil | Botón completo; etiquetas de resumen legibles sin puntos suspensivos. |
| Aumentar la fuente del teléfono | Filas reorganizadas y botones con altura suficiente. |
| Pulsar luna/sol y reiniciar | Tema elegido conservado; texto, campos y diálogos legibles. |
| Agregar una foto, volver al inicio y reiniciar | Misma imagen en ambos avatares. |
| Cambiar y quitar la foto | Se actualiza la imagen; al quitarla vuelve la inicial. |
| Cancelar el selector | La foto anterior permanece. |
| Entrar con otra cuenta | No aparece la foto de la cuenta anterior. |
| Crear producto, consumir, ajustar, ver recetas y compras | Se conservan los flujos de la versión anterior. |

La aceptación en teléfono, las reglas publicadas de Firestore y la matriz funcional del cronograma siguen pendientes de validación del equipo.

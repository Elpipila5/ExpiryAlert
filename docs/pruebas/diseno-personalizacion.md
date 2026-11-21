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

Las capturas locales se generan en `app/build/reports/diseno`. CI usa JDK 25 para Gradle y JDK 21 para las pruebas. Robolectric usa API 27 con gráficos nativos. Las pruebas instrumentadas comparten los casos de prueba y usan la API del dispositivo; se compilaron, pero su ejecución sigue pendiente.

La ruta ImageDecoder de API 28+ y la interacción con el selector del sistema necesitan la comprobación en un teléfono. Los resultados locales y de CI no se presentan como aceptación en dispositivo.

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

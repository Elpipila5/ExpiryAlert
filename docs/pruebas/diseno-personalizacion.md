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

La versión inicial del rediseño compiló, generó APK de aplicación y APK de pruebas, pasó las 62 pruebas de lógica y terminó lint sin errores. Se revisaron capturas renderizadas del inicio claro, oscuro y del perfil.

Se añadieron 10 casos compartidos entre pruebas locales Robolectric y pruebas Android: botón a 320 dp y escala de fuente 1.6, resumen a 320 dp, resumen oscuro con escala 1.5, cambio efectivo del fondo y persistencia del tema, inicio y producto en ambos temas, perfil y resumen, persistencia de foto tras borrar su origen, separación de cuentas y eliminación aislada. La suite completa contiene 72 casos locales.

En la última ejecución local completada pasaron los tres casos de fotos, el cambio de tema y los casos de resumen. Tres comprobaciones de texto del botón detectaron el ancho reservado del párrafo centrado; se corrigió la aserción para medir el ancho de cada línea dibujada, sin permitir elipsis ni recorte de altura. La ejecución final se interrumpió al perderse el entorno. El resultado del código publicado debe comprobarse en la ejecución de Android CI y no se deduce de la compilación previa.

Comandos:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
.\gradlew.bat :app:connectedDebugAndroidTest
```

Las capturas de las pruebas locales se generan en `app/build/reports/diseno`. Robolectric usa API 27 con gráficos nativos; las pruebas instrumentadas ejercitan las mismas reglas sobre la API del teléfono. La ruta ImageDecoder de API 28+ y la interacción con el selector del sistema requieren la comprobación en dispositivo.

El emulador de este entorno arrancó, pero rechazó la instalación por `INSTALL_FAILED_VERIFICATION_FAILURE` (espera agotada del verificador de integridad). Por ello las pruebas instrumentadas no se consideran ejecutadas.

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

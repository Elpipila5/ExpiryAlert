# Pruebas: registro inteligente por código de barras y etiqueta

**Alcance:** flujo alternativo al OCR de ExpiryAlert, sin romper el registro manual ni las alertas.  
**Estado:** implementación propuesta en rama; pendiente de compilar/probar en Android Studio.

## Procedimiento de prueba (preferentemente un teléfono con Google Play Services)

1. Cambiar a `feature/registro-inteligente-codigo-barras` y sincronizar Gradle.
2. Entrar a ExpiryAlert → Agregar producto → **Escanear código**.
3. Enfocar el EAN/UPC de un aceite (por ejemplo), y esperar la consulta de Open Food Facts.
4. Verificar si se rellenan nombre, marca, categoría, cantidad y unidad; **no inventar caducidad**.
5. Si el producto está disponible pero no tiene fecha, elegir **Guardar** con fecha vacía.
6. Verificar en inventario el indicador **Caducidad pendiente** y que no aparezca alerta de ese artículo.
7. Editar producto, añadir una fecha real del envase (o fotografiar etiqueta antes de guardar el producto) y verificar que los avisos siguen funcionando.
8. Alternar **Leer etiqueta y caducidad (opcional)**, confirmar la fecha impresa y guardarla.
9. Repetir sin conexión / con código inexistente. Debe quedar disponible el registro manual y no bloquearse la aplicación.
10. Repetir con código manual y un dispositivo sin Google Play Services, que puede no tener lector automático.

## Evidencias recomendadas
- Pantalla del lector de código y formulario autocompletado (sin datos personales).
- Registro guardado sin fecha y tarjeta de inventario "Caducidad pendiente".
- Edición posterior con fecha real, seguida de una alerta.
- Prueba con el código del aceite que falló en la versión anterior.
- Resultado de `ConsultaProductoCodigoTest` y captura del build.

## Reglas importantes
- Un EAN/UPC comercial identifica una referencia de producto; **no incluye normalmente la caducidad del lote específico**.
- Open Food Facts es una base colaborativa: la coincidencia, idioma y calidad de los campos dependen de sus registros. No todos los productos tienen datos.
- El lector funciona mediante Google Play Services; necesita estar disponible en el dispositivo. El reconocimiento de códigos es local, pero buscar fichas del producto sí requiere internet.
- Solo se consulta el catálogo al escanear o pulsar "Buscar"; los límites públicos de lecturas de producto son de 15 solicitudes/minuto/IP según documentación.
- Los productos con caducidad pendiente **no reciben avisos por caducidad** hasta que agreguen la fecha.
- La app atribuye datos de Open Food Facts (licencia ODbL); validar las obligaciones de reutilización si se publica o distribuye ampliamente.
- La cantidad retornada se refiere a la presentación del producto, no a cuánto ha consumido el usuario.

# Manual de usuario de ExpiryAlert

## Cuenta y despensa

Crear una cuenta o iniciar sesión. Cada cuenta consulta sus propios productos. Mi despensa muestra cantidades y estados de caducidad; los archivados se ocultan del inventario activo.

## Registrar un producto

Pulsar **Nuevo producto**. Escribir nombre, categoría, marca opcional, cantidad inicial, unidad y fecha. La fecha puede quedar pendiente. El código EAN/UPC ayuda a completar datos del catálogo; no identifica la fecha de una unidad específica. La cámara o galería permiten leer fecha y cantidad impresas por OCR; revisar los datos detectados antes de guardar.

### Foto opcional del producto

La foto del producto es independiente de la imagen de perfil y del OCR.

1. Escribir nombre y marca y pulsar **Buscar foto automática**, o escanear un código con imagen en el catálogo.
2. Comprobar la vista previa y pulsar **Usar esta foto**. La app busca coincidencias de nombre y marca en Open Food Facts; un código identifica la presentación del catálogo.
3. Si no aparece una coincidencia, pulsar **Buscar en Google Imágenes**. La búsqueda contiene nombre y marca. Descargar la imagen elegida y volver a **Elegir foto del dispositivo**; también se puede copiar su enlace HTTPS directo y usar **Cargar vista previa**.
4. Pulsar **Guardar en inventario**. La miniatura queda asociada al producto en Firestore y se recupera con esa cuenta. El enlace de origen del catálogo se conserva con su atribución.

En **Editar datos** se puede cambiar o quitar la foto; guardar confirma el cambio. La imagen se prepara antes del guardado: no es necesario conservar el archivo original ni un permiso permanente de galería. Si no se encuentra foto o falla Internet, se puede registrar el producto sin imagen.

## Consumo y revisión

**Consumir** descuenta una cantidad positiva del saldo vigente. Se pueden convertir kg/g y L/ml, pero no masa en volumen. No se admiten saldos negativos ni consumos mayores a lo disponible. **Ajustar cantidad** corrige el saldo; una reposición se registra como otro producto/lote. **Revisar cantidades** permite confirmar productos pendientes de verificación.

## Alertas

Conceder notificaciones cuando Android las solicite. Los avisos de caducidad permiten indicar si aún se conserva el producto o archivarlo. Una respuesta atendida no vuelve a aparecer por girar el teléfono. Editar o eliminar un producto actualiza/cancela sus avisos. WorkManager puede retrasar recordatorios por batería y conectividad.

## Compras y desayuno

Agregar a compras desde un producto con bajo stock, manualmente o desde ingredientes faltantes de una receta. Se puede editar la lista y marcar compras. En Mi perfil → Inventario y recordatorios se configuran umbral de stock, intervalo de revisión y desayuno. Mi desayuno permite registrar explícitamente lo consumido.

## Recetas

El catálogo local de 28 recetas distingue preparables, recetas con faltantes e ideas sin coincidencias. Excluye vencidos, archivados, agotados y categorías no alimentarias reconocidas. Prioriza ingredientes próximos a vencer y advierte fechas pendientes. Cada receta muestra pasos, compras faltantes y búsqueda de preparación en YouTube. No descuenta productos automáticamente al consultar una receta.

## Personalización y conservación de datos

El botón luna/sol cambia el tema y recuerda la elección. Mi perfil permite modificar nombre y foto personal. Productos y fotos de producto se guardan en Firestore; perfil fotográfico, compras y preferencias se conservan por cuenta en el dispositivo. El tema es una preferencia del dispositivo. Cerrar sesión conserva esos datos privados para la próxima entrada de la misma cuenta.

# Firebase propio de ExpiryAlert

Estado observado el 24/11/2026. El solicitante aclaró que el proyecto recién creado es su primer proyecto de Firebase. El identificador `expiryalert-e9b37` procedía del archivo incluido previamente en el código; no se ha accedido a ese proyecto ni se ha migrado su contenido.

## Proyecto configurado

| Dato | Valor verificado en Firebase Console |
| --- | --- |
| Nombre | ExpiryAlert |
| ID | `expiryalert-49b5e` |
| Número | `124630660413` |
| Plan | Spark, sin cambio a Blaze |
| App Android | ExpiryAlert Android |
| Paquete | `com.example.expiryalert` |
| ID de la app | `1:124630660413:android:c8f7341e1858d69e540154` |
| Authentication | Correo electrónico/contraseña habilitado |
| Firestore | Base `(default)`, edición Standard, ubicación `nam5` |

La base se creó en modo de producción, inicialmente con acceso de clientes denegado. Después se publicaron las reglas de [`firestore.rules`](../firestore.rules). No se habilitaron acceso anónimo, reglas públicas, Storage ni servicios de pago.

## Evidencia de las reglas

En la zona de pruebas de Firebase Console se utilizó una creación simulada en `/productos/prueba-configuracion`, con `usuarioId=prueba-propietario`, nombre y categoría válidos, cantidad `1` y fecha `30/11/2026`.

| Identidad simulada | Resultado observado |
| --- | --- |
| Sin autenticación | Escritura rechazada |
| UID `prueba-propietario` | Escritura autorizada |
| UID `prueba-otra-cuenta`, conservando el propietario del documento | Escritura rechazada |

Son identidades ficticias del simulador; no se crearon cuentas ni productos reales para estas pruebas. El editor coincidía exactamente con el texto del repositorio antes de publicar. Después de publicar, se recargó la consola y se comprobó la revisión activa y las funciones `propietario` y `fotoValida`. SHA-256 del archivo publicado: `2ba79f1e2efc2e61914367ccc96a03538a3722d2862f4d3813671ef0e9fd1e3c`.

[Reglas publicadas del proyecto](https://console.firebase.google.com/project/expiryalert-49b5e/firestore/databases/-default-/security/rules).

La suite automatizada de 14 casos en emulador complementa estas tres simulaciones. Ni el simulador ni la suite sustituyen la prueba de registro y persistencia desde el APK instalado.

## Cliente Android conectado en 1.4.1

El solicitante proporcionó el archivo oficial `google-services.json`. Se verificaron proyecto, número, ID de app y paquete frente a los datos de la consola y se copió sin modificaciones a `app/google-services.json`. La versión Android pasa a **1.4.1, código 8**. El plugin de Google Services ya está configurado en Gradle, sin complementos adicionales de Android Studio.

`.firebaserc` y Android seleccionan ahora el mismo proyecto. El empaquetado comprueba que los recursos `project_id` y `google_app_id` generados por Gradle coinciden con el archivo cliente y registra ambos en `VERSION_Y_PRUEBAS.json`, sin incluir claves API en esos metadatos. El workflow del commit entregado acredita el resultado de compilación y pruebas.

El archivo cliente contiene identificadores usados por el SDK Android; no es una cuenta de servicio ni una credencial administrativa. Authentication y las reglas publicadas protegen los productos.

## Aceptación pendiente en el teléfono

1. Actualizar `main` o `v21`, sincronizar Gradle y ejecutar Run desde Android Studio con la misma firma anterior. Verificar **1.4.1** al final de Mi perfil.
2. Registrar una cuenta dentro de la app: Authentication del proyecto nuevo comienza vacío. No se migraron automáticamente cuentas ni inventario del proyecto anterior. Las preferencias locales ligadas al UID anterior no pasan automáticamente a la cuenta nueva.
3. Guardar, cambiar y quitar la foto de un producto; cerrar sesión y volver a entrar. Comprobar aislamiento con otra cuenta y registrar el resultado en la matriz de pruebas.

Estas pruebas de aceptación no se declaran realizadas por compilar el APK o pasar el emulador.

## Confirmación posterior y versión 1.4.2

El usuario confirmó que el registro de su cuenta aparece en Authentication y que encontró la vista de datos de Firestore. Se conserva el archivo oficial de configuración y el proyecto `expiryalert-49b5e` en 1.4.2; no hace falta crear otra cuenta para actualizar desde 1.4.1. Los flujos automatizados de alta, foto, edición, eliminación y permisos se ejecutan con un backend demo local y se documentan en [correcciones 1.4.2](pruebas/correcciones-142.md). Esa prueba aislada no certifica operaciones con la cuenta real.

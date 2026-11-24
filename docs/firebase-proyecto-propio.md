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

## Pendiente antes de conectar y entregar el APK

1. Incorporar el `google-services.json` generado para esta app en `app/google-services.json`. La descarga se completó en la consola, pero no se pudo recuperar su contenido en el entorno de trabajo. **El archivo de la app aún apunta al proyecto anterior: la transición no está terminada.** No se inventan ni reutilizan las claves de otro proyecto.
2. Verificar el ID de proyecto y el paquete del archivo, incrementar la versión Android, ejecutar las comprobaciones y compilar con la nueva configuración. Hasta entonces no instalar los artefactos de este borrador como versión conectada al nuevo proyecto.
3. Actualizar desde Android Studio conservando los datos locales. Registrar una cuenta dentro de la app, porque Authentication del proyecto nuevo comienza vacío. No se ha migrado automáticamente el inventario del proyecto anterior.
4. Guardar, cambiar y quitar la foto de un producto; cerrar sesión y volver a entrar. Comprobar aislamiento con otra cuenta y registrar el resultado en la matriz de pruebas.

`.firebaserc` selecciona el nuevo proyecto para futuros comandos de Firebase. **No sustituye `google-services.json` ni cambia por sí solo el proyecto usado por Android.**

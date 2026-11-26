# Entrega de ExpiryAlert 1.4.1

Proyecto de Desarrollo Móvil Integral, IDGS10, Universidad Tecnológica de Montemorelos. Docente: Ing. Mario Alberto Chacón Muñoz. Periodo septiembre–noviembre de 2026; presentación el 24 y 25 de noviembre.

**Firebase propio conectado:** 1.4.1 incorpora el archivo oficial de `expiryalert-49b5e`, que ya tiene Authentication y reglas publicadas. **Registrar una cuenta dentro de la app:** no se migraron cuentas ni inventario del proyecto anterior. [Configuración y aceptación pendiente](../firebase-proyecto-propio.md).

## Contenido preparado

- `ExpiryAlert.apk`: aplicación ejecutable de depuración.
- `codigo/`: código y configuración versionados; incluye manuales, reglas y pruebas.
- `VERSION_Y_PRUEBAS.json`: versión, proyecto/app Firebase verificados en los recursos de Gradle, commit de fuente/build, resultado de pruebas locales y SHA-256 del APK.
- `evidencias/`: resultados locales, lint y capturas disponibles en el trabajo de compilación.
- `SHA256SUMS.txt`: integridad de los archivos del paquete.

El workflow **Android CI** adjunta además `pruebas-android-15` y `pruebas-reglas-firestore`. Verificar que sus cuatro trabajos terminaron correctamente. Los artefactos se conservan 90 días; descargar el paquete para la entrega.

## Actualizar sin perder los datos

En el proyecto de Android Studio, actualizar `main` y ejecutar `Actualizar-ExpiryAlert.cmd`, o sincronizar Gradle y pulsar Run. El script comprueba la versión instalada. **Versión esperada: 1.4.1, código 8.**

Para instalar el APK de CI sobre otra instalación es necesaria la misma firma. Si Android rechaza una actualización por firma, compilar desde Android Studio con la clave que se utilizó antes. Desinstalar elimina fotos de perfil, compras y preferencias locales; evitarlo para actualizar.

## Firebase del proyecto propio

Las fotos nuevas se guardan en el campo `fotoProducto` del documento privado del producto. El proyecto nuevo `expiryalert-49b5e` ya tiene Firestore Standard y acceso por correo/contraseña. Las reglas ampliadas se publicaron el 24/11/2026 y se comprobaron tres casos en el simulador. Android 1.4.1 ya incorpora el archivo oficial de configuración; **queda por registrar la prueba de cuenta y persistencia desde el teléfono.**

Para una publicación posterior:

1. Abrir [las reglas del proyecto expiryalert-49b5e](https://console.firebase.google.com/project/expiryalert-49b5e/firestore/databases/-default-/security/rules).
2. Conservar una copia del texto actualmente publicado. Si existen otras colecciones con reglas propias, conservarlas y combinar el bloque de `productos`.
3. Incorporar [firestore.rules](../../firestore.rules), que ya incluye validación de imagen y compatibilidad de documentos antiguos.
4. Publicar y comprobar desde el teléfono un producto con foto con la cuenta propietaria. Las pruebas de dos cuentas en emulador están en `seguridad/reglas.test.mjs`.

Si el proyecto se dedica exclusivamente a esta app y ya se dispone de acceso administrativo, también se puede usar:

```bash
npx --prefix seguridad firebase login
npx --prefix seguridad firebase deploy --only firestore:rules --project expiryalert-49b5e --config firebase.json
```

La publicación indicada se verificó en Firebase Console; las pruebas de emulador por sí solas no demuestran despliegue. El proyecto nuevo empieza sin cuentas ni inventario. No se ha migrado contenido del proyecto anterior.

## Material de consulta

- [Manual de usuario](manual-usuario.md)
- [Reporte técnico y resultados](reporte-tecnico.md)
- [Presentación editable](Presentacion_ExpiryAlert.pptx)
- [Guion de demostración](presentacion.md)
- [Cierre del Sprint 9](../pruebas/cierre-sprint9.md)
- [Modelo implementado](../modelo-datos.md)

# Entrega de ExpiryAlert 1.4.0

Proyecto de Desarrollo Móvil Integral, IDGS10, Universidad Tecnológica de Montemorelos. Docente: Ing. Mario Alberto Chacón Muñoz. Periodo septiembre–noviembre de 2026; presentación el 24 y 25 de noviembre.

## Contenido preparado

- `ExpiryAlert.apk`: aplicación ejecutable de depuración.
- `codigo/`: código y configuración versionados; incluye manuales, reglas y pruebas.
- `VERSION_Y_PRUEBAS.json`: versión, commit de fuente/build, resultado de pruebas locales y SHA-256 del APK.
- `evidencias/`: resultados locales, lint y capturas disponibles en el trabajo de compilación.
- `SHA256SUMS.txt`: integridad de los archivos del paquete.

El workflow **Android CI** adjunta además `pruebas-android-15` y `pruebas-reglas-firestore`. Verificar que sus cuatro trabajos terminaron correctamente. Los artefactos se conservan 90 días; descargar el paquete para la entrega.

## Actualizar sin perder los datos

En el proyecto de Android Studio, actualizar `main` y ejecutar `Actualizar-ExpiryAlert.cmd`, o sincronizar Gradle y pulsar Run. El script comprueba la versión instalada. **Versión esperada: 1.4.0, código 7.**

Para instalar el APK de CI sobre otra instalación es necesaria la misma firma. Si Android rechaza una actualización por firma, compilar desde Android Studio con la clave que se utilizó antes. Desinstalar elimina fotos de perfil, compras y preferencias locales; evitarlo para actualizar.

## Activar el guardado de fotos en Firebase

Las fotos nuevas se guardan en el campo `fotoProducto` del documento privado del producto. Las reglas anteriores que restringen campos pueden rechazarlo hasta publicar la ampliación.

1. Abrir [las reglas del proyecto expiryalert-e9b37](https://console.firebase.google.com/project/expiryalert-e9b37/firestore/rules).
2. Conservar una copia del texto actualmente publicado. Si existen otras colecciones con reglas propias, conservarlas y combinar el bloque de `productos`.
3. Incorporar [firestore.rules](../../firestore.rules), que ya incluye validación de imagen y compatibilidad de documentos antiguos.
4. Publicar y comprobar desde el teléfono un producto con foto con la cuenta propietaria. Las pruebas de dos cuentas en emulador están en `seguridad/reglas.test.mjs`.

Si el proyecto se dedica exclusivamente a esta app y ya se dispone de acceso administrativo, también se puede usar:

```bash
npx --prefix seguridad firebase login
npx --prefix seguridad firebase deploy --only firestore:rules --project expiryalert-e9b37 --config firebase.json
```

El entorno de desarrollo de esta entrega no dispone de una sesión administrativa de Firebase. La existencia del archivo y las pruebas de emulador no demuestran publicación en producción.

## Material de consulta

- [Manual de usuario](manual-usuario.md)
- [Reporte técnico y resultados](reporte-tecnico.md)
- [Presentación editable](Presentacion_ExpiryAlert.pptx)
- [Guion de demostración](presentacion.md)
- [Cierre del Sprint 9](../pruebas/cierre-sprint9.md)
- [Modelo implementado](../modelo-datos.md)

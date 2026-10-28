# Seguridad de Firebase — entrega Sprint 5

## Estado y alcance

El archivo [`firestore.rules`](../firestore.rules) contiene una **propuesta de reglas de producción**, basada en los campos actualmente guardados por ExpiryAlert. **GitHub no publica las reglas en Firebase de forma automática.** El cambio no asegura los datos hasta que alguien autorizado lo pruebe y lo publique.

Las reglas protegen la colección raíz `productos` y aplican:
- Solo una persona autenticada puede consultar, crear, modificar o eliminar sus productos.
- La consulta del inventario debe usar `.whereEqualTo("usuarioId", usuario.uid)`.
- En creación, `usuarioId` debe ser exactamente el UID autenticado y la cantidad inicial debe ser positiva.
- En modificación, impiden cambiar de propietario o falsificar `fechaCreacion`, aunque la aplicación envíe `usuarioId` sin cambios.
- La fecha puede quedar vacía para `caducidadPendiente`; las cantidades pueden ser decimales y llegar a cero al archivar.
- Se rechazan campos inesperados por defecto; **esto requiere actualizar y probar las reglas al agregar F06–F10**.
- Las reglas no reemplazan validación de cliente, no verifican la fecha real del calendario ni la calidad del OCR.

## Pasos antes de publicar

1. En Firebase Console, identificar el **proyecto Firebase utilizado por la app** y abrir Firestore Database → Rules.
2. **Guardar una copia de las reglas actuales.** Si el mismo proyecto contiene otras colecciones o aplicaciones, combinar cuidadosamente: **no reemplazar sus reglas a ciegas**.
3. Comparar los documentos de `productos` reales con los campos admitidos por `firestore.rules`. Si existen campos adicionales, adaptar el esquema de manera explícita.
4. Usar el simulador de reglas de Firebase/Firestore o el emulador local y verificar la matriz siguiente con cuentas de prueba.
5. Tras validar, copiar/combinar las reglas del archivo, pulsar **Publish**, esperar su propagación y repetir las pruebas desde un teléfono con dos cuentas.
6. Ante errores `PERMISSION_DENIED`, revisar campos y reglas antes de revertir. No cambiar temporalmente a `allow read, write: if true`.

## Casos de seguridad

| Prueba | Resultado esperado |
| --- | --- |
| Cuenta A crea un producto con `usuarioId=A` | Permitido |
| Cuenta A consulta sus documentos filtrando `usuarioId=A` | Permitido |
| Usuario sin sesión intenta leer un producto | Denegado |
| Cuenta B intenta leer el ID de un producto de A | Denegado |
| Cuenta B intenta modificar o eliminar el producto de A | Denegado |
| Cuenta A intenta crear un producto poniendo `usuarioId=B` | Denegado |
| Cuenta A cambia el propietario de un producto de A | Denegado |
| Cuenta A actualiza `ultimaConfirmacion` de su producto | Permitido |
| Cuenta A archiva el producto con `activo=false, cantidad=0` | Permitido |
| Cuenta A guarda una fecha vacía con `caducidadPendiente=true` | Permitido |
| Cuenta A cambia fecha y cantidad propias en el formulario | Permitido |
| Consulta sin filtro por `usuarioId` | Denegada para lista |

**Evidencia obligatoria:** capturas de resultado del simulador, estado real en Android, nombres de casos y commit del APK. No copiar correos completos ni UIDs de personas a las capturas públicas.

## Copias de seguridad Android

Se desactiva respaldo automático de la app para que preferencias locales, tokens y datos temporales no migren como copias sin control. Firebase continúa manteniendo los datos persistidos en Firestore; un usuario autenticado recupera su inventario desde ese servicio. Se han definido también exclusiones de transferencia para las versiones que aplican `data-extraction-rules`.

## Limitaciones

- **No hay forma de verificar desde este repositorio qué reglas están actualmente desplegadas en Firebase.**
- El User-Agent de Open Food Facts y la configuración cliente de Firebase no deben tratarse como credenciales de servidor; nunca subir una cuenta de servicio, claves privadas, contraseñas o tokens de CI al repositorio.
- En futuras semanas, al incorporar movimientos de consumo, preferencias o lista de compras, será necesario ampliar reglas, consultas y pruebas de acceso para esas colecciones.

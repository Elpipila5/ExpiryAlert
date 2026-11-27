# Correcciones y validación de ExpiryAlert 1.4.2

26 de noviembre de 2026. Sprint 9. Versión 1.4.2, código 9; mismo diseño y Firebase `expiryalert-49b5e` de 1.4.1.

## Defectos y solución

| Problema comprobado en el código | Corrección | Verificación |
| --- | --- | --- |
| Dos ID diferentes con el mismo hash reemplazaban su notificación y compartían PendingIntent. | Etiqueta y URI con UID/ID completos; cancelación compatible con avisos anteriores y comprobación diaria sincronizada. | Regresión con `Aa`/`BB` en API 27 y Android 15, cancelación por producto/cuenta y deduplicación. |
| `SimpleDateFormat.parse` aceptaba fechas seguidas de texto o con formato incompleto. | Coincidencia completa `dd/MM/yyyy`, calendario y formato estables independientemente del idioma. | Casos inválidos, día actual, cambio de horario y calendario tailandés. |
| El OCR reconocía mg, pero el consumo no podía convertirlos a gramos. | Conversión kg/g/mg dentro de la misma familia de masa. | Descuento de 500 mg sobre 1 g; conserva validación de unidades incompatibles y límites. |
| Desayuno admitía productos de higiene que las recetas excluían. | `ProductosConsumo` comparte filtros de categoría, nombre, cantidad, fecha y archivo. | Alimentos vigentes/sin fecha con advertencia; exclusión de higiene, vencidos, archivados y agotados. |
| El guardado perdía su estado al recrear la actividad mientras Firestore respondía. | ViewModel conserva operación y resultado; bloquea doble envío, permite reintentar errores e ignora respuestas de otra sesión. | Pruebas de estado y alta real con red suspendida, foto y recreación de MainActivity. |
| El formulario validaba con un conversor y guardaba con otro. | Se guarda una sola cantidad validada. | Alta desde la interfaz con `2,5`, recuperada como número 2.5 en Firestore local. La escritura manual ya normalizaba la coma antes de esta corrección. |
| Se emitía una notificación de prueba al abrir o girar la aplicación. | El permiso concedido inicia una revisión real; no se envían pruebas automáticas en cada apertura. | Inicio real, cambio de tema y navegación desde una notificación. |
| La despensa no distinguía caché y servidor ni ofrecía reintentar una lectura fallida. | Usa la misma carga con metadatos que las otras pantallas; los avisos se basan en datos confirmados por servidor. | Regresión Android y consulta de revisión con `Source.SERVER`. |

Las notificaciones de inventario también abren la actividad existente cuando está disponible, conservando formularios y navegación.

## Reproducción antes y después

El commit `efb0338` añadió regresiones antes de modificar la implementación. La [ejecución inicial](https://github.com/Elpipila5/ExpiryAlert/actions/runs/45925282091) registró **91 pruebas locales con 4 fallos** y **30 Android 15 con 2 fallos**, sin omisiones. Los fallos reproducen miligramos, fechas y colisión de avisos: se esperaba conservar dos notificaciones y quedaba una. Las 14 pruebas de reglas y el diagnóstico Windows aprobaron.

La [validación final de 1.4.2](https://github.com/Elpipila5/ExpiryAlert/actions/runs/45927237633) aprobó **101 pruebas locales, 37 en Android 15 y 14 de reglas**, sin fallos, errores ni omisiones. Pasaron los cuatro trabajos: compilación/lint, Android, reglas y diagnóstico Windows. Los siete flujos de Firebase local se ejecutaron completos. Lint registró cero errores.

Código probado: `63d5a68f4c53d235ce8d8796eb5f777a5aa6fd11`, integrado mediante [PR #30](https://github.com/Elpipila5/ExpiryAlert/pull/30). Se verificó que el árbol integrado coincide con el probado y se sincronizó `v21` con `main`.

El [paquete de entrega](https://github.com/Elpipila5/ExpiryAlert/actions/runs/45927237633/artifacts/15167630492) contiene 210 archivos cuya integridad se comprobó, versión 1.4.2/código 9 y recursos Firebase del proyecto real. SHA-256 del APK: `a157a9f6f3f531aa8102e71235af44feb582edd6f352a226a0e1040725c84c34`. Los DEX del APK de aplicación no contienen la configuración del backend demo. Los resultados se registraron después de concluir la ejecución.

## Flujos Android con Firebase local

`FlujosFirebaseInstrumentedTest` arranca MainActivity y utiliza Authentication y Firestore del proyecto ficticio `demo-expiryalert-pruebas`:

1. Registrar un producto con cantidad decimal y foto desde el selector de imágenes, suspender la red durante el envío, recrear la actividad y comprobar que solo hay un documento. Borrar el archivo original, recuperar la foto guardada, editar y eliminar el producto.
2. Consumir 0.25 kg de 1000 g, ajustar a 180 g, rechazar un consumo mayor, mostrar bajo stock y conservar la lista de compras tras recrear la actividad.
3. Mostrar recetas con inventario vacío, tortillas y tortillas con queso; enviar los faltantes a compras.
4. Excluir cosméticos/higiene del desayuno; comprobar un único trabajo programado, ejecutar el trabajador de recordatorio y cancelarlo.
5. Abrir el producto desde una notificación, archivarlo en Firestore y comprobar que no reaparezca el diálogo al recrear la actividad.
6. Crear dos cuentas locales; rechazar lectura y actualización ajenas mediante el SDK Android y conservar los datos de la propietaria.
7. Ejecutar la revisión real de caducidades contra el servidor local: avisar solo productos activos con saldo, omitir confirmaciones de hoy, deduplicar y detener los avisos al cerrar sesión.

El selector recibe una imagen controlada mediante Espresso Intents; la lectura, compresión, persistencia y recuperación se ejecutan en el código real. La ejecución directa del trabajador comprueba generación/cancelación, sin simular el transcurso de horas ni afirmar entrega en Doze.

El runner solo activa ese backend con `expiry.emuladores=true`, cambia las opciones Firebase exclusivamente en el proceso de prueba y se verifica el ID demo antes de crear cuentas. Sin esa opción los siete casos de integración se omiten, sin escribir en producción. CI siempre proporciona la opción, exige los siete casos y debe finalizar sin omisiones. Las cuentas/documentos temporales se eliminan después de cada caso.

Con Android emulador iniciado, JDK/SDK del proyecto y Node 22 o posterior:

```bash
npm --prefix seguridad ci --no-audit --no-fund
seguridad/node_modules/.bin/firebase emulators:exec --only firestore,auth --project demo-expiryalert-pruebas --config firebase.json "bash .github/scripts/pruebas-android.sh"
```

El permiso HTTP de debug se limita a los destinos locales del emulador. Los servicios reales y las fotos web siguen usando HTTPS. No se cambian `google-services.json`, las reglas publicadas ni el esquema de productos.

## Límites de la aceptación

El registro en Authentication fue confirmado por el usuario. La comprobación automatizada local no certifica un guardado/foto con su cuenta real, ni notificaciones bajo ahorro de batería en el Xiaomi. Estos casos requieren observar el dispositivo y servicio reales; no constituyen funciones nuevas pendientes de programación. Las participaciones quedan fuera de este cierre técnico por indicación del solicitante.

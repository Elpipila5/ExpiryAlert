# Pruebas de registro por fotografía (Sprint 5)

**Estado:** casos diseñados; requieren ejecución en Android Studio y dispositivo/emulador.

## Preparación
1. Cambiar a la rama `feature/ocr-registro-fotografia-sprint5`, sincronizar Gradle y ejecutar.
2. Iniciar sesión y abrir «Agregar producto».
3. Preparar fotografías legibles de etiquetas con fecha y contenido neto; ocultar información personal.

## Casos mínimos
- **OCR-01:** seleccionar imagen de galería con «CAD 29/10/2026» y «500 g». Debe proponer 29/10/2026, 500, g; no guardar hasta usar datos y pulsar «Guardar».
- **OCR-02:** tomar fotografía con cámara; debe reconocer texto desde archivo temporal con FileProvider.
- **OCR-03:** imagen con fecha de fabricación y caducidad; debe preferir línea «CAD», «VENCE», «EXP».
- **OCR-04:** texto ilegible o etiqueta sin fecha; permite corregir manualmente y evita guardar datos inválidos.
- **OCR-05:** cancelar en confirmación; no modifica el formulario ni guarda productos.
- **OCR-06:** confirmar y guardar; Firestore recibe `fechaCaducidad` (dd/MM/yyyy), `cantidad` numérica, `cantidadInicial`, `unidad` y `usuarioId`.
- **OCR-07:** registrar 1.5 L; editar y comprobar que se mantiene el valor decimal.
- **OCR-08:** producto manual antiguo sin `unidad`; debe seguir mostrándose con «pzas» predeterminado.
- **OCR-09:** registro con fecha próxima a caducar; «Revisar caducidades ahora» debe seguir notificándolo.
- **OCR-10:** cancelar cámara o denegar selección; no debe cerrarse la aplicación ni guardar información.

## Evidencias para el docente
- Captura de la fotografía elegida (si no contiene datos personales).
- Pantalla de confirmación con datos OCR corregibles.
- Tarjeta de producto guardado y documento en Firestore (sin UID/correo real visible).
- Notificación correspondiente al producto.
- Build y pruebas unitarias `ExtractorDatosOCRTest` ejecutadas.

## Decisiones y límites
- ML Kit reconoce texto localmente; no estima cuánto producto queda en el empaque.
- Los datos del OCR se usan como **sugerencia** y exigen confirmación del usuario.
- La fotografía se conserva temporalmente en la caché local para su análisis; no se sube a Firebase Storage.
- Detección automática centrada en fechas numéricas; fechas no reconocidas pueden corregirse en el formulario.
- El consumo de cantidades, verificación periódica y alertas de bajo stock pertenecen a sprints posteriores.

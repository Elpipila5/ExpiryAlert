# Versión candidata y cierre del Sprint 9

Estado al 21/11/2026: **versión candidata 1.3.0, versionCode 6**. La preparación técnica no sustituye la aceptación del equipo ni la prueba en el teléfono. El Sprint 9 del plan abarca del 23 al 27 de noviembre, con presentación el 24 y 25.

## Evidencia disponible

- El [rediseño integrado](https://github.com/Elpipila5/ExpiryAlert/pull/24) conserva las funciones de inventario, recetas, consumo, reposición y desayuno.
- La [validación automatizada](https://github.com/Elpipila5/ExpiryAlert/actions/runs/44928823005) del código `6a5a97045e465a94c9f41a00e663606bc2bb59ba` aprobó las **72 pruebas locales y las 18 pruebas instrumentadas en Android 15 (API 35)**, sin fallos, errores ni casos omitidos. También generó ambos APK y terminó lint sin errores.
- Las pruebas Android cubren los 10 casos compartidos de diseño/fotos, cuatro de compras y preferencias por cuenta, dos de avisos atendidos, una foto desde una URI de contenido y una comprobación del contexto de la app. Los informes incluyen **11 capturas nativas**, además de las capturas locales.
- La foto desde `content://` se decodificó y guardó correctamente en API 35, incluso tras eliminar el original. Esto comprueba el protocolo y el guardado, pero no automatiza la elección en la interfaz del selector del sistema.
- Las comprobaciones funcionales de autenticación, CRUD, OCR, códigos y respuestas a avisos fueron informadas por el usuario en una versión anterior. Su registro histórico está en [Sprint 5](sprint5-integracion.md); la regresión completa de 1.3.0 continúa pendiente en el teléfono.

## Estado de las funciones del plan

| Función | Implementación actual | Verificación y límite |
| --- | --- | --- |
| F01 Cuenta y sesión | Firebase Auth, registro, ingreso y cierre de sesión. | Funcionamiento informado en versión anterior; volver a probar la sesión en 1.3.0. |
| F02 Registro de productos | Alta, edición, eliminación, cantidad inicial y unidad. | Compilación y validaciones; pendiente de regresión con Firestore real. |
| F03 Fotografía y OCR | Lectura de fecha/cantidad con confirmación y corrección. | Lógica automatizada; luz, cámara y empaques requieren teléfono. |
| F04 Inventario y estado | Inventario por usuario, clasificación y saldos. | Pruebas de fechas y visibilidad; resumen adaptable. |
| F05 Caducidad | Avisos, confirmación y archivo; estado atendido persistente. | Dos casos de estado atendido aprobados en Android 15; la entrega con app cerrada requiere comprobar WorkManager y permisos. |
| F06 Consumo | Transacciones, saldo restante y conversiones kg/g y L/ml. | Cálculos y límites automatizados; falta comprobar concurrencia y permisos con dos clientes reales. |
| F07 Verificación y ajuste | Revisión periódica configurable, confirmación y corrección de saldo. | Lógica automatizada; falta demostración completa con Firestore y avisos. |
| F08 Bajo stock y compras | Umbral configurable, reposición, lista y marcado de compras. | Lógica automatizada; casos de compras y separación por cuenta aprobados en Android 15. |
| F09 Desayuno | Horario configurable y registro explícito de productos consumidos. | Cálculo de horario y persistencia; la llegada aproximada del recordatorio requiere prueba real. |
| F10 Recetas | 28 recetas locales, faltantes, compras, pasos y YouTube. | Motor automatizado; demostrar sin inventario, solo tortillas y tortillas con queso. |
| PB11 Calidad | Compilación, 72 casos locales, 18 casos Android y lint aprobados en CI. | Pendientes reglas publicadas, aceptación, evidencias finales y firma de distribución del equipo. |

Las fotos de perfil, compras y preferencias se conservan por cuenta **en este dispositivo**. No se sincronizan entre teléfonos. El tema es una preferencia del dispositivo. La cantidad restante se estima a partir de consumos y ajustes; una foto no la mide.

## Pendientes para declarar la entrega final

1. **Teléfono:** actualizar desde main, verificar versión 1.3.0 y completar las matrices de [cantidades y recetas](v21-sprints6-9.md) y [personalización](diseno-personalizacion.md). Incluir selección, cambio y eliminación de foto, cancelación del selector, reinicio, rotación y cambio de cuenta.
2. **Firebase:** comprobar las reglas realmente publicadas, compatibilidad de campos y acceso del propietario frente a otra cuenta. Seguir [seguridad](../seguridad-firestore.md). No está confirmado desde este repositorio.
3. **Recordatorios:** comprobar caducidad, bajo stock, revisión y desayuno con permisos concedidos/denegados y ahorro de batería. WorkManager puede retrasar las entregas.
4. **Distribución y evidencias:** generar APK/AAB con la clave del equipo, registrar commit, teléfono y Android utilizados, capturas o video y revisión del otro integrante. No se ha creado ni publicado una clave privada de distribución.

La [instalación actual](../instalacion.md) explica cómo actualizar conservando los datos locales. Las pruebas en emulador y CI se registran como tales; no acreditan pruebas físicas ni cambios publicados en Firebase.

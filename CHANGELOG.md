# Registro de cambios

## 2026-11-21 - Actualización visible en el teléfono

- Sincronización de v21 con main al detectar que conservaba el diseño anterior.
- Actualizador Windows que verifica carpeta, commit, APK y versión instalada y abre la aplicación, conservando datos.
- Prueba de arranque de MainActivity y cambio de tema: 19 casos Android y 72 locales aprobados.
- Sintaxis y diagnóstico del actualizador comprobados en Windows; falta confirmar la instalación en el teléfono del usuario.

## 2026-11-21 - Validación de 1.3.0

- CI ejecuta las pruebas instrumentadas en un emulador de Android 15 con API 35.
- 18 casos Android y 72 locales aprobados, sin fallos; APK generados y lint sin errores.
- Prueba de foto mediante URI de contenido y recuperación automática de 11 capturas nativas.
- Guías de instalación actualizadas a main y lista de cierre con pendientes de teléfono, Firebase y distribución.

## 2026-11-20 - 1.3.0

- Diseño verde y menta con nueva bienvenida, iconos y tarjetas de producto.
- Botón «Nuevo producto» a ancho completo y con altura adaptable al texto.
- Resumen en dos columnas en el perfil; se reorganiza según ancho y tamaño de letra.
- Modo claro/oscuro para todas las pantallas, persistente en este dispositivo.
- Selección, cambio y eliminación de foto de perfil, con archivos separados por cuenta.
- Colores de formularios, diálogos y selectores de fecha/hora adaptados al tema.
- Diez pruebas nuevas de diseño y personalización, compartidas entre Robolectric y Android.
- Android CI: 72 pruebas aprobadas, ambos APK generados y lint sin errores; aceptación en teléfono pendiente.
- Validación y pasos para el teléfono en `docs/pruebas/diseno-personalizacion.md`.

## 2026-11-19 - 1.2.0-v21

- Recetas de main integradas al diseño de v21 y a la lista de compras.
- Registro de consumo con transacciones y conversión entre unidades compatibles.
- Corrección de saldos, verificación periódica y umbral configurable de bajo stock.
- Compras persistentes por cuenta en el dispositivo y recordatorio configurable de desayuno.
- Navegación desde avisos y cancelación de trabajos al cerrar sesión.
- 62 pruebas unitarias aprobadas; matriz de aceptación en dispositivo en `docs/pruebas/v21-sprints6-9.md`.

## 2026-10-08 - Preparación del repositorio

- Documentación de alcance, historias, calendario y acuerdos pendientes.
- Guía de colaboración, plantillas de issues y pull requests.
- Estructura para incorporar el proyecto móvil existente, pruebas y evidencias.
- Referencias a los documentos fuente compartidos con el equipo.

Esta entrada registra la preparación documental; no representa una versión ejecutable de la app. Las entregas futuras deben indicar funcionalidades verificadas, correcciones, build y evidencias.

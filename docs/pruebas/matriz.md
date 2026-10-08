# Matriz inicial de aceptación

Estado de **todos los casos: No ejecutado**. Los resultados esperados ampliados a partir del plan y las reglas de modelo son propuestas a validar con el equipo.

| Caso | Historia | Escenario | Resultado esperado |
| --- | --- | --- | --- |
| CP01 | PB01 | Crear cuenta e iniciar sesión válida. | Accede al inventario correspondiente y conserva sesión según diseño. |
| CP02 | PB01 | Credenciales inválidas y cierre de sesión. | Error controlado; al cerrar sesión no queda accesible el inventario privado. |
| CP03 | PB01, PB11 | Dos usuarios con productos diferentes. | Un usuario no puede consultar o modificar productos del otro. |
| CP04 | PB02 | Alta manual completa y reinicio de la app. | El producto persiste con fecha, categoría, cantidad y unidad correctas. |
| CP05 | PB02 | Campos vacíos, fecha inválida o cantidad negativa. | Se explica el error y no se guarda un producto inválido. |
| CP06 | PB02 | Editar producto y eliminarlo. | La consulta refleja el cambio; los avisos asociados se actualizan/cancelan. |
| CP07 | PB03 | Foto con fecha y cantidad legibles. | Devuelve candidatos y exige confirmación antes de guardar. |
| CP08 | PB03 | Texto ambiguo, foto borrosa o cámara denegada. | Permite corregir/reintentar o registrar manualmente sin bloquear el flujo. |
| CP09 | PB04 | Fechas antes de hoy, hoy, límite de ventana y fuera de ventana. | Clasifica según la regla acordada y muestra el vencimiento de hoy. |
| CP10 | PB04 | Inventario vacío y cambio de día. | Estado vacío claro; recalcula fechas al actualizar el inventario. |
| CP11 | PB05 | Aviso previo y respuesta «todavía lo tengo». | Aviso real en dispositivo; respuesta actualiza la verificación. |
| CP12 | PB05 | Respuesta «consumido/desechado», edición o eliminación. | Retira/archiva según diseño y no conserva avisos obsoletos. |
| CP13 | PB05, PB11 | Permiso de notificaciones denegado. | La app conserva el inventario y explica cómo activar avisos. |
| CP14 | PB06 | Consumir 250 g de un saldo de 1000 g. | Quedan 750 g y un movimiento identificable. |
| CP15 | PB06 | Consumo cero, negativo, no numérico o mayor al saldo. | Rechaza la entrada sin cambiar saldo ni duplicar movimientos. |
| CP16 | PB06, PB07 | Corregir saldo a 600 g y consumir 100 g. | El saldo posterior es 500 g; conserva trazabilidad de la corrección. |
| CP17 | PB06 | Unidades distintas y redondeo. | Usa únicamente conversiones aprobadas; no mezcla g con ml sin una regla válida. |
| CP18 | PB07 | Confirmación periódica y ajuste manual. | Actualiza fecha de verificación y cantidad coherente. |
| CP19 | PB08 | Alcanzar el umbral y añadir a compras. | Detecta bajo stock y guarda el item sin duplicaciones accidentales. |
| CP20 | PB09 | Configurar desayuno sin acceso a alarmas del sistema. | La interacción usa la hora elegida y funciona sin ese acceso. |
| CP21 | PB10 | Inventario con alimentos próximos, caducados y cosméticos. | Recomienda según los filtros alimentarios acordados y prioriza próximos disponibles. |
| CP22 | PB10 | Error de red, cuota o respuesta vacía externa. | Error controlado y alternativa de búsqueda/enlace cuando corresponda. |
| CP23 | PB11 | Red interrumpida durante persistencia/sincronización. | Comportamiento documentado; no muestra éxito falso ni duplica operaciones al reintentar. |
| CP24 | PB11 | Compilar e instalar la versión candidata. | Build reproducible y flujos críticos probados en el dispositivo objetivo. |

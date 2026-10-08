# Modelo de datos propuesto

Este es un **modelo lógico de partida**, no una migración ni un esquema ya implementado. Se adapta al almacenamiento que utiliza el proyecto real. Los campos adicionales y reglas propuestas se validan en DEC02.

## Entidades

| Entidad | Campos sugeridos | Relación y propósito |
| --- | --- | --- |
| Usuario | `id`, `nombre`, `correo`, `creado_en` | Propietario del inventario; credenciales gestionadas por la solución de autenticación. |
| Producto | `id`, `usuario_id`, `nombre`, `categoria`, `fecha_caducidad`, `cantidad_inicial`, `cantidad_actual`, `unidad`, `foto_ref`, `situacion`, `verificado_en`, `creado_en`, `actualizado_en` | Cada alta representa un lote con su propia fecha; dos empaques pueden caducar en días distintos. |
| Movimiento | `id`, `producto_id`, `tipo`, `cantidad`, `saldo_anterior`, `saldo_nuevo`, `registrado_en` | Consumo o corrección de cantidad; conserva trazabilidad. |
| Preferencias | `usuario_id`, `dias_aviso`, `intervalo_verificacion`, `umbral_stock`, `hora_desayuno`, `zona_horaria` | Configuración por usuario, con valores predeterminados por confirmar. |
| ItemCompra | `id`, `usuario_id`, `producto_origen_id`, `nombre`, `cantidad_sugerida`, `unidad`, `comprado`, `creado_en` | Elementos de reposición y estado de lista. |
| Recordatorio | `id`, `usuario_id`, `producto_id`, `tipo`, `programado_para`, `estado` | Si la implementación requiere persistir avisos o identificadores de la plataforma. |

`foto_ref` puede ser una referencia local o a almacenamiento de imágenes según la arquitectura. `producto_id` en un recordatorio de desayuno puede ser opcional. Los textos OCR son candidatos temporales; no crean un producto sin confirmación.

## Cantidades

La cantidad actual es la base vigente de la estimación. Al iniciar, coincide con la inicial. Un consumo positivo actualiza:

```text
nuevo saldo = saldo actual - consumo validado
```

Rechazar un consumo nulo, negativo, no numérico o mayor que el saldo disponible. Una corrección manual establece el nuevo saldo confirmado y registra el movimiento; los consumos siguientes parten de ese saldo. No seguir restando desde la cantidad inicial después de una corrección.

Propuesta: una corrección de un mismo lote admite `0 <= saldo <= cantidad_inicial`. Una reposición se registra como otro lote; cualquier otro comportamiento requiere acuerdo documentado. No mezclar gramos con mililitros sin una conversión válida; convertir solo unidades compatibles y con una regla definida. Confirmar representación numérica, redondeo y tolerancias antes de implementar.

## Fechas y estados

Guardar caducidad como fecha de calendario, sin convertirla accidentalmente a otro día por zona horaria. Propuesta pendiente de aprobación:

- `caducado`: fecha de caducidad anterior a hoy.
- `proximo`: fecha desde hoy hasta `hoy + dias_aviso`, incluidos ambos límites.
- `vigente`: fecha posterior a esa ventana.

Un producto que vence hoy se muestra de forma explícita como «vence hoy». El equipo confirma si debe clasificarse de otra manera según el criterio acordado. Los documentos no definen un valor de `dias_aviso`.

Separar el estado calculado por fecha de la situación física: `activo`, `consumido`, `desechado` o `archivado`. Recalcular el estado con la fecha actual; no guardar un estado que se vuelva obsoleto al pasar los días.

## Reglas de integración

- Validar nombre, categoría, fecha, cantidad y unidad antes de persistir.
- Mantener movimientos y saldo consistentes; evitar duplicar un consumo al reintentar una operación.
- Cada operación debe pertenecer al usuario autenticado. Probar aislamiento entre dos cuentas.
- Al editar fecha o retirar producto, actualizar/cancelar sus notificaciones.
- Confirmar cómo se guardan fotos, se elimina un producto y se conserva o elimina su historial.

Estas reglas orientan la implementación; los casos de aceptación del plan están en [backlog](backlog.md).

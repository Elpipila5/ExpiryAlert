# Alcance y trazabilidad

## Referencia de trabajo

Se organiza el repositorio a partir del [plan de trabajo](referencias/README.md#plan-de-trabajo), que detalla cantidades, OCR y nueve sprints. Es una decisión de organización para esta preparación; no acredita una aprobación del docente ni resuelve automáticamente las diferencias con la [Actividad 1](referencias/README.md#actividad-1).

Los códigos F de las siguientes tablas corresponden al plan. Todas las funciones están **pendientes de verificar en el repositorio** hasta integrar y revisar el código existente.

| ID | Funcionalidad | Prioridad | Dependencias | Resultado requerido |
| --- | --- | --- | --- | --- |
| F01 | Registro e inicio de sesión | Alta | Ninguna | Cuenta, sesión segura y manejo de credenciales inválidas. |
| F02 | Registro manual | Alta | F01 | Nombre, categoría, caducidad, cantidad inicial, unidad y foto opcional; edición y eliminación. |
| F03 | Fotografía y OCR | Alta | F02 | Texto candidato de fecha y cantidad; confirmación/corrección antes de guardar. |
| F04 | Inventario y estado | Alta | F02 | Vigentes, próximos a caducar y caducados, con cantidad estimada restante. |
| F05 | Alertas de caducidad | Alta | F04 | Aviso anticipado, atención a productos caducados y pregunta sobre si aún se conservan. |
| F06 | Cantidades y consumo | Alta | F02, F04 | Registrar cantidad usada y recalcular saldo sin valores imposibles. |
| F07 | Verificación periódica | Alta | F04, F06 | Confirmar existencia/cantidad y permitir ajustes. |
| F08 | Bajo stock y lista de compras | Media | F06 | Umbral configurable o predeterminado, aviso y opción de reposición. |
| F09 | Rutina de desayuno | Media | F01 | Hora elegida por el usuario y pregunta contextual, con permisos opcionales. |
| F10 | Recetas y contenido externo | Media | F04, F06 | Priorizar productos disponibles/próximos a caducar; enlaces o consulta externa. |

Autenticación, almacenamiento, nube, validaciones, permisos, seguridad, errores y pruebas son transversales. El proveedor y el framework se confirman al revisar el proyecto existente.

## Diferencias entre documentos

| Elemento de Actividad 1 | Referencia en el plan | Tratamiento en esta preparación |
| --- | --- | --- |
| F01: autenticación | F01 | Se conserva. |
| F02: registrar | F02 | Se amplía con cantidad y unidad. |
| F03: lista y F04: estado | F04 | Se agrupan en inventario. |
| F05: notificaciones | F05 | Se añade confirmación de existencia. |
| F06: editar/eliminar | F02 | Forma parte del CRUD manual. |
| F07: búsqueda/filtros | Sin historia explícita | Se registra para confirmar si forma parte de PB04 o de una ampliación. |
| F08: código de barras | Sin historia explícita | Se mantiene como decisión pendiente de alcance. |
| Exclusión de lectura de fechas por fotografía | F03 requiere OCR | Contradicción registrada en DEC01 para revisión con equipo/docente. |
| Cantidades, rutinas y recetas no detalladas | F06-F10 | Se documentan según el plan, sujetas a conciliación del alcance. |

No dar por equivalentes los mismos números F de ambos documentos. No borrar requisitos anteriores sin dejar acuerdo y motivo en la revisión.

## Límites funcionales

La foto sirve para leer texto impreso del empaque. La cantidad restante es una **estimación basada en la cantidad inicial, los consumos y las correcciones**; la app no la mide mirando la fotografía.

La rutina de desayuno debe funcionar con una hora configurada por el usuario. El acceso a alarmas del sistema es opcional y no puede bloquear el flujo principal.

La compra automática, conexión directa con inventarios/precios de supermercados y gestión empresarial están excluidas en la Actividad 1 y no se incorporan al backlog de esta preparación.

El uso de recetas debe limitarse a productos alimentarios disponibles; no recomendar productos caducados. Esta validación es una propuesta técnica para PB10 que el equipo debe confirmar.

## Cambios de alcance

Registrar cada cambio en [decisiones](decisiones.md), explicar su efecto en tiempo/dependencias y discutirlo en Sprint Review antes de comprometer una historia nueva. La semana 9 se reserva para estabilización y entrega.

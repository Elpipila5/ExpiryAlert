# Arquitectura propuesta

**Propuesta pendiente de validar con el código existente.** Los documentos no fijan Flutter, Kotlin, proveedor de nube ni base de datos. Confirmarlos al integrar T01 y registrarlos en [decisiones](decisiones.md).

## Separación de responsabilidades

| Área | Responsabilidad | Módulos sugeridos |
| --- | --- | --- |
| Presentación | Pantallas, formularios, navegación y estados de carga/error. | Sesión, inventario, registro manual/OCR, consumo, compras, ajustes y recetas. |
| Dominio | Reglas independientes de cámara, red y almacenamiento. | Caducidad, validación de cantidades, consumo, correcciones y selección de recomendaciones. |
| Datos y servicios | Persistencia y comunicación con capacidades externas. | Repositorios, autenticación, nube/local, OCR, notificaciones y recetas/enlaces. |

Adaptar esta separación a la estructura actual del equipo; no exige renombrar o reescribir todo el proyecto.

## Flujos principales

- Manual: completar datos → validar → guardar → consultar inventario.
- OCR: solicitar cámara → tomar foto → extraer candidatos → confirmar/corregir → validar → guardar.
- Consumo: elegir producto → introducir cantidad en su unidad → validar saldo → registrar movimiento → actualizar inventario/avisos.
- Caducidad: evaluar fecha local → programar aviso → preguntar existencia → guardar respuesta y actualizar avisos.
- Recetas: seleccionar alimentos disponibles → priorizar próximos a caducar → consultar servicio o abrir búsqueda externa → mostrar errores controlados.

## Decisiones técnicas antes de integrar

Documentar plataforma objetivo, versiones del SDK, método de autenticación, almacenamiento local/nube y reglas de sincronización, permisos y dependencia de OCR/notificaciones. Registrar cómo se aíslan inventarios por usuario y cómo se cancelan avisos al editar o retirar un producto.

Las reglas de negocio deben poder verificarse con fecha y datos de prueba controlados. Las operaciones de red deben tener estados de error y recuperación. No presentar persistencia local como sincronización en nube si esta última todavía no está implementada.

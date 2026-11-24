# Modelo de datos implementado

Estado: versión 1.4.0. Este documento describe el código actual; no presenta colecciones hipotéticas como implementadas.

## Autenticación y productos

Firebase Authentication gestiona UID, correo, credenciales y nombre de perfil. La contraseña no se almacena en documentos propios. Cada documento de la colección Firestore `productos` representa un lote con su ID y fecha. El ID se obtiene de `document.id`, no de `document.data`.

| Campo | Tipo y uso |
| --- | --- |
| `usuarioId` | UID propietario, obligatorio e inmutable. |
| `nombre`, `categoria` | Texto obligatorio; hasta 150 y 100 caracteres. |
| `marca`, `codigoBarras` | Texto opcional; hasta 150 y 32 caracteres. |
| `cantidadInicial`, `cantidad` | Cantidad inicial y saldo vigente. Saldo numérico entre 0 y 1 000 000. |
| `unidad` | Hasta 30 caracteres; conversiones compatibles kg/g y L/ml. |
| `fechaCaducidad` | Texto `dd/MM/yyyy` o vacío. El cliente valida calendario; las reglas validan formato. |
| `caducidadPendiente` | Booleano para la fecha vacía. |
| `fechaCreacion`, `ultimaConfirmacion` | Timestamp de alta y última confirmación/cambio de cantidad. |
| `correoUsuario` | Correo asociado; no se usa como autorización. |
| `activo` | False al archivar; se oculta del inventario activo. |
| `fotoProducto` | Mapa opcional con `contenido` JPEG en Base64, `fuente` y `pagina` de origen. |

La foto conserva proporción y orientación, con lado máximo de 640 px y JPEG de hasta 90 000 bytes. Base64 ocupa como máximo 120 000 caracteres, por debajo del límite de documento de Firestore. Se guarda dentro del documento privado: no requiere bucket de Storage ni enlaces de galería caducables. Los productos antiguos sin imagen siguen siendo válidos. Quitar la foto elimina el campo; borrar el documento elimina también la imagen.

La fuente vale `galeria`, `web` u `openfoodfacts`. En este último caso se conserva enlace al producto y atribución CC BY-SA. La imagen del catálogo no determina caducidad ni cantidad restante.

## Cantidades y estados

`RepositorioCantidades` utiliza transacciones sobre el saldo del servidor, comprueba propietario/estado y controla reintentos. Un consumo positivo no puede exceder el saldo; una corrección puede llegar a cero y no superar la cantidad inicial. Una reposición se registra como otro lote. No se crea una colección independiente de movimientos históricos.

Los estados se calculan por fecha local: antes de hoy es vencido; desde hoy hasta siete días es próximo; después es seguro. La fecha vacía queda pendiente y la inválida no se considera segura. La etiqueta temporal no se persiste como un estado que pueda quedar obsoleto.

## Persistencia local

- `PreferenciasInventario`: compras, stock, revisión y desayuno por UID. Valores iniciales: stock 20 %, revisión siete días, desayuno desactivado.
- `FotosPerfil`: JPEG privado por UID, independiente de `fotoProducto`.
- `ControlApariencia`: tema oscuro/claro del dispositivo.
- `AvisosAtendidos` y preferencias de avisos: respuestas atendidas y control de duplicados por usuario/tipo/día.

Compras, preferencias y fotos de perfil no se sincronizan entre dispositivos. Productos y sus imágenes se recuperan desde Firestore cuando las reglas 1.4.0 están publicadas.

## Seguridad

Las consultas usan `whereEqualTo("usuarioId", uid)`. [firestore.rules](../firestore.rules) rechaza otra cuenta, falta de sesión, transferencia de propietario, campos inesperados e imágenes excesivas. La suite `seguridad/` comprueba identidades separadas en emulador; publicar en producción es una operación distinta.

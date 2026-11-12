# Sprint 7 — PB10 / F10: recetas según productos reales del inventario

**Semana:** 9–13 de noviembre de 2026.  
**Rama para probar:** `feature/recetas-inventario-desde-cero`.  
**Versión Android:** `1.1.1-recetas` (versionCode 4).

**Esta implementación reemplaza el sistema de recetas anterior.** Los resultados se calculan localmente a partir de los productos de Firestore; no necesita IA, OpenAI, backend Python ni claves externas. El backend anterior se conserva en el repositorio solo como una opción para otro desarrollo, pero no participa en esta pantalla.

## Comportamiento exigido

- **Receta completa:** si todos los ingredientes aparecen en el inventario con cantidad > 0, activos y no vencidos, mostrar «✅ Puedes preparar estas recetas».
- **Receta incompleta:** si tiene al menos un ingrediente y falta algún otro, mostrar el nombre de la receta, «✅ Tienes» y «🛒 Te falta comprar», más botón para copiar faltantes.
- **Sin inventario o sin coincidencias:** mostrar exactamente «No hay ninguna receta que puedas hacer con los productos que tienes actualmente.» y a continuación **recetas para las que necesitas comprar ingredientes**, con sus listas completas. No debe quedarse la pantalla en blanco.
- **Productos vencidos:** no contar como ingredientes utilizables ni recomendar su consumo.
- **Productos archivados / cantidad 0:** no contar.
- **Fecha vacía:** permite sugerencia con advertencia explícita «Comprueba la fecha», sin tratarla automáticamente como producto seguro.
- **Vista de recetas:** botón «Ver cómo prepararla» y enlace a resultados de búsqueda de YouTube.
- **Usuario:** la consulta Firestore usa `whereEqualTo("usuarioId", uid)` y solo los documentos de la cuenta autenticada.

## Pruebas en Android Studio

| ID | Datos de prueba | Resultado esperado |
| --- | --- | --- |
| RC01 | Cuenta nueva sin ningún producto | Aviso «No hay ninguna receta que puedas hacer…» y varias recetas con listas de compra completas |
| RC02 | Solo `Tortillas de maíz` (cantidad 1) | «Quesadillas de queso» en incompletas: Tienes tortilla; falta queso |
| RC03 | `Tortillas de maíz` + `Queso manchego`, ambas con cantidad > 0 | «Quesadillas de queso» en completas; ningún ingrediente faltante |
| RC04 | `Huevo` únicamente | «Huevos cocidos» en completas; otras como «Huevos con tomate» aparecen con faltantes |
| RC05 | `Leche entera` con fecha vacía | Recetas con leche muestran «Comprueba la fecha» y el ingrediente que falte |
| RC06 | Tomate con fecha de ayer | No cuenta como disponible; no se consume ni cambia automáticamente |
| RC07 | Tortillas con `activo=false` | No cuenta para ninguna receta |
| RC08 | Queso con cantidad = 0 | No cuenta como ingrediente |
| RC09 | Cambiar cantidad o eliminar producto mientras está abierta la pantalla | Listas de recetas se actualizan con el listener Firestore |
| RC10 | Pulsar «📋 Copiar lo que me falta» | Portapapeles contiene únicamente los nombres faltantes de esa receta |
| RC11 | Pulsar «▶ Buscar preparación en YouTube» | Abre una búsqueda HTTPS de la receta |
| RC12 | Cerrar sesión A y entrar B | B solo usa su propio inventario |
| RC13 | Desactivar internet y no tener datos Firestore cacheados | Error de carga visible y catálogo de ideas de compras; no afirmar que se conoce inventario |
| RC14 | Ejecutar pruebas `RecetasLocalesTest` | Casos completos, parciales, vacíos, sinónimos y fechas sin fallos |
| RC15 | Regresar a la pantalla principal | Inventario, alertas, login y CRUD existentes siguen funcionando |

## Pasos rápidos para mostrar al docente

1. Registrar en la cuenta de prueba solo `Tortillas de maíz` y entrar a Recetas: mostrar la quesadilla y el queso faltante.
2. Registrar `Queso manchego` y volver: ahora debe aparecer entre las que sí se pueden preparar.
3. Archivarlas o usar una cuenta vacía: comprobar que siempre hay ideas de recetas, pero ninguna marcada como preparable.
4. Abrir «Ver cómo prepararla» y copiar los faltantes.
5. Tomar capturas de los tres estados, del build y de las pruebas unitarias.

## Limitaciones explicadas

El sistema usa un catálogo local de 28 recetas y sinónimos comunes. Detecta presencia por nombre y cantidad positiva, pero **no comprueba que haya suficiente cantidad para todas las porciones**. Para alimentos sin fecha, se requiere verificar seguridad de consumo. No se descuenta automáticamente nada del inventario por consultar una receta. Los enlaces YouTube abren búsquedas, no vídeos verificados individualmente.

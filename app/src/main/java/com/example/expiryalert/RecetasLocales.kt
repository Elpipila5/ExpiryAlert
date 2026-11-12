package com.example.expiryalert

import java.text.Normalizer
import java.util.Calendar
import java.util.Locale

/**
 * Catálogo local de recetas: nunca requiere servidor de IA ni datos externos.
 * Detecta ingredientes presentes y propone qué comprar si falta alguno.
 */
data class RecetaLocal(
    val nombre: String,
    val tiempoMinutos: Int,
    val ingredientes: List<String>,
    val disponibles: List<String>,
    val faltantes: List<String>,
    val proximosACaducar: List<String>,
    val sinFechaVerificada: List<String>,
    val pasos: List<String>
) {
    val puedePrepararse: Boolean get() = faltantes.isEmpty()
    val porcentajeDisponible: Int get() =
        if (ingredientes.isEmpty()) 0 else (disponibles.size * 100) / ingredientes.size
}

data class ResultadoRecetas(
    val preparables: List<RecetaLocal>,
    val requierenCompras: List<RecetaLocal>,
    val otrasIdeas: List<RecetaLocal>,
    val productosAptos: Int
) {
    val todas: List<RecetaLocal>
        get() = preparables + requierenCompras + otrasIdeas
}

object RecetasLocales {

    private data class Plantilla(
        val nombre: String,
        val minutos: Int,
        val ingredientes: List<String>,
        val pasos: List<String>
    )

    private data class Producto(
        val nombre: String,
        val nombreNormalizado: String,
        val diasRestantes: Long?
    )

    // Sinónimos de los nombres habituales del inventario, incluidas marcas y plurales.
    // Las coincidencias respetan límites de palabras: "pan" NO coincide con "panela".
    private val sinonimos: Map<String, List<String>> = mapOf(
        "huevo" to listOf("huevo", "huevos"),
        "tomate" to listOf("tomate", "tomates", "jitomate", "jitomates"),
        "aceite" to listOf("aceite"),
        "arroz" to listOf("arroz"),
        "zanahoria" to listOf("zanahoria", "zanahorias"),
        "cebolla" to listOf("cebolla", "cebollas"),
        "tortilla" to listOf("tortilla", "tortillas"),
        "queso" to listOf("queso", "quesos"),
        "lechuga" to listOf("lechuga"),
        "pepino" to listOf("pepino", "pepinos"),
        "pan" to listOf("pan", "bolillo", "bolillos", "tostada", "tostadas"),
        "pasta" to listOf("pasta", "espagueti", "spaghetti", "fideos", "fideo", "macarrones"),
        "papa" to listOf("papa", "papas", "patata", "patatas"),
        "avena" to listOf("avena"),
        "plátano" to listOf("platano", "platanos", "banana", "bananas", "banano"),
        "leche" to listOf("leche"),
        "atún" to listOf("atun"),
        "frijoles" to listOf("frijol", "frijoles"),
        "limón" to listOf("limon", "limones", "lima", "limas"),
        "aguacate" to listOf("aguacate", "aguacates", "palta"),
        "jamón" to listOf("jamon"),
        "mayonesa" to listOf("mayonesa"),
        "yogur" to listOf("yogur", "yogurt", "yoghurt"),
        "manzana" to listOf("manzana", "manzanas"),
        "pollo" to listOf("pollo", "pechuga de pollo"),
        "carne" to listOf("carne", "res", "bistec"),
        "chile" to listOf("chile", "chiles", "jalapeno", "serrano"),
        "maíz" to listOf("maiz", "elote", "elotes"),
        "crema" to listOf("crema"),
        "harina" to listOf("harina"),
        "azúcar" to listOf("azucar")
    )

    // Cada receta tiene ingredientes REQUERIDOS. Nada se presupone disponible.
    // El agua utilizada en la cocción se considera un recurso básico y no un
    // producto que se descuente del inventario.
    private val catalogo = listOf(
        Plantilla("Huevos cocidos", 12, listOf("huevo"),
            listOf("Coloca los huevos en agua suficiente.", "Hierve hasta cocción completa y enfría.", "Pela y sirve.")),
        Plantilla("Quesadillas de queso", 10, listOf("tortilla", "queso"),
            listOf("Coloca queso sobre las tortillas.", "Dobla y calienta hasta fundir el queso.", "Sirve caliente.")),
        Plantilla("Huevos a la mexicana", 15, listOf("huevo", "tomate", "cebolla", "chile", "aceite"),
            listOf("Lava y corta tomate, cebolla y chile.", "Sofríe las verduras con aceite.", "Añade los huevos y cocínalos por completo.")),
        Plantilla("Huevos con tomate", 15, listOf("huevo", "tomate", "aceite"),
            listOf("Lava y corta el tomate.", "Saltea el tomate con aceite.", "Añade los huevos y cocina completamente.")),
        Plantilla("Huevos con jamón", 12, listOf("huevo", "jamón", "aceite"),
            listOf("Corta el jamón.", "Calienta aceite y agrega el jamón.", "Incorpora los huevos y cocina por completo.")),
        Plantilla("Arroz con verduras", 30, listOf("arroz", "zanahoria", "cebolla", "aceite"),
            listOf("Lava y corta las verduras.", "Cuece el arroz hasta que esté bien hecho.", "Saltea las verduras con aceite y combina.")),
        Plantilla("Arroz con huevo", 25, listOf("arroz", "huevo", "aceite"),
            listOf("Cuece el arroz.", "Prepara el huevo completamente cocido con un poco de aceite.", "Mezcla y sirve caliente.")),
        Plantilla("Pasta con tomate", 25, listOf("pasta", "tomate", "cebolla", "aceite"),
            listOf("Cuece la pasta en agua.", "Sofríe cebolla y tomate con aceite.", "Mezcla la salsa con la pasta caliente.")),
        Plantilla("Papas doradas", 25, listOf("papa", "aceite"),
            listOf("Lava y corta las papas.", "Cocínalas hasta que estén suaves.", "Dóralas con un poco de aceite.")),
        Plantilla("Papas con queso", 30, listOf("papa", "queso"),
            listOf("Lava y cocina las papas hasta ablandar.", "Corta y cubre con queso.", "Calienta hasta fundir el queso.")),
        Plantilla("Ensalada de tomate y pepino", 10, listOf("tomate", "pepino", "limón"),
            listOf("Lava bien tomate y pepino.", "Corta en cubos.", "Agrega jugo de limón y mezcla.")),
        Plantilla("Ensalada de lechuga", 10, listOf("lechuga", "tomate", "limón"),
            listOf("Lava y desinfecta las verduras.", "Corta tomate y lechuga.", "Adereza con limón.")),
        Plantilla("Guacamole sencillo", 10, listOf("aguacate", "tomate", "cebolla", "limón"),
            listOf("Lava y corta tomate y cebolla.", "Tritura el aguacate.", "Mezcla con limón y verduras.")),
        Plantilla("Pan con aguacate", 8, listOf("pan", "aguacate"),
            listOf("Tuesta ligeramente el pan.", "Pela y machaca el aguacate.", "Úntalo sobre el pan.")),
        Plantilla("Sándwich de queso", 8, listOf("pan", "queso"),
            listOf("Coloca queso entre dos rebanadas de pan.", "Sirve frío o calienta hasta fundir el queso.")),
        Plantilla("Sándwich de jamón y queso", 10, listOf("pan", "jamón", "queso"),
            listOf("Coloca jamón y queso sobre el pan.", "Cierra el sándwich y sirve o calienta.")),
        Plantilla("Sándwich de atún", 12, listOf("pan", "atún", "mayonesa"),
            listOf("Escurre el atún.", "Mézclalo con mayonesa bien conservada.", "Rellena el pan y sirve.")),
        Plantilla("Ensalada de atún", 12, listOf("atún", "lechuga", "tomate", "limón"),
            listOf("Lava y desinfecta la lechuga y el tomate.", "Escurre el atún.", "Mezcla y añade limón.")),
        Plantilla("Licuado de plátano", 5, listOf("plátano", "leche"),
            listOf("Verifica que la leche esté en buen estado.", "Pela el plátano.", "Licúa ambos y sirve de inmediato.")),
        Plantilla("Avena con plátano", 12, listOf("avena", "plátano", "leche"),
            listOf("Calienta la leche.", "Cuece la avena hasta ablandarla.", "Agrega el plátano en rodajas.")),
        Plantilla("Yogur con plátano", 5, listOf("yogur", "plátano"),
            listOf("Comprueba la refrigeración del yogur.", "Corta el plátano.", "Combina ambos ingredientes.")),
        Plantilla("Yogur con manzana", 5, listOf("yogur", "manzana"),
            listOf("Lava y corta la manzana.", "Combina con yogur bien conservado.")),
        Plantilla("Frijoles con queso", 12, listOf("frijoles", "queso"),
            listOf("Calienta completamente los frijoles cocidos.", "Agrega queso y sirve caliente.")),
        Plantilla("Tacos de frijoles", 12, listOf("frijoles", "tortilla"),
            listOf("Calienta completamente los frijoles cocidos.", "Calienta las tortillas.", "Forma los tacos y sirve.")),
        Plantilla("Tacos de pollo", 20, listOf("pollo", "tortilla", "tomate"),
            listOf("Cocina el pollo a temperatura segura y desmenúzalo.", "Lava y pica el tomate.", "Calienta tortillas y arma los tacos.")),
        Plantilla("Pollo con arroz", 35, listOf("pollo", "arroz", "cebolla", "aceite"),
            listOf("Cocina el pollo completamente.", "Cuece el arroz con cebolla.", "Combina con un poco de aceite y sirve caliente.")),
        Plantilla("Pasta con queso", 20, listOf("pasta", "queso"),
            listOf("Cuece la pasta hasta que esté lista.", "Escurre y añade queso.", "Calienta hasta fundir y sirve.")),
        Plantilla("Ensalada de manzana y plátano", 8, listOf("manzana", "plátano", "yogur"),
            listOf("Lava la manzana y corta ambas frutas.", "Mezcla con yogur bien refrigerado.", "Sirve de inmediato."))
    )

    private fun normalizar(texto: String): String {
        val sinAcentos = Normalizer.normalize(
            texto.lowercase(Locale.ROOT), Normalizer.Form.NFD
        ).replace(Regex("""\p{M}+"""), "")
        return sinAcentos.replace(Regex("""[^a-z0-9]+"""), " ").trim()
    }

    private fun tieneIngrediente(nombreProducto: String, ingrediente: String): Boolean {
        val texto = " " + nombreProducto + " "
        val alternativas = sinonimos[ingrediente] ?: listOf(ingrediente)
        return alternativas.any { alternativa ->
            texto.contains(" " + normalizar(alternativa) + " ")
        }
    }

    private fun aptos(productos: List<Map<String, Any>>, hoy: Calendar): List<Producto> =
        productos.mapNotNull { dato ->
            if (dato["activo"] == false) return@mapNotNull null

            val cantidad = when (val valor = dato["cantidad"]) {
                is Number -> valor.toDouble()
                is String -> valor.replace(',', '.').toDoubleOrNull()
                else -> null
            } ?: return@mapNotNull null
            if (cantidad <= 0 || !cantidad.isFinite()) return@mapNotNull null

            val nombre = dato["nombre"]?.toString()?.trim().orEmpty()
            if (nombre.isBlank()) return@mapNotNull null

            val fecha = dato["fechaCaducidad"]?.toString()?.trim().orEmpty()
            val dias = if (fecha.isBlank()) {
                null
            } else {
                FechasCaducidad.diasRestantes(fecha, hoy) ?: return@mapNotNull null
            }
            if (dias != null && dias < 0) return@mapNotNull null

            Producto(nombre, normalizar(nombre), dias)
        }

    /**
     * Ofrece TODAS las recetas del catálogo, incluso con inventario vacío.
     * - preparables: 100 % de ingredientes disponibles.
     * - requierenCompras: algunos presentes, otros faltantes.
     * - otrasIdeas: 0 coincidencias, habría que comprar todo.
     */
    fun analizar(
        productos: List<Map<String, Any>>,
        hoy: Calendar = Calendar.getInstance()
    ): ResultadoRecetas {
        val disponibles = aptos(productos, hoy)
        val propuestas = catalogo.map { plantilla ->
            val presentes = mutableListOf<String>()
            val faltantes = mutableListOf<String>()
            val proximos = mutableListOf<String>()
            val sinFecha = mutableListOf<String>()

            for (ingrediente in plantilla.ingredientes) {
                val candidato = disponibles
                    .filter { tieneIngrediente(it.nombreNormalizado, ingrediente) }
                    .minByOrNull { it.diasRestantes ?: Long.MAX_VALUE }

                if (candidato == null) {
                    faltantes += ingrediente
                } else {
                    presentes += ingrediente
                    if (candidato.diasRestantes == null) sinFecha += candidato.nombre
                    if (candidato.diasRestantes != null && candidato.diasRestantes <= 7) {
                        proximos += candidato.nombre
                    }
                }
            }
            RecetaLocal(
                nombre = plantilla.nombre,
                tiempoMinutos = plantilla.minutos,
                ingredientes = plantilla.ingredientes,
                disponibles = presentes,
                faltantes = faltantes,
                proximosACaducar = proximos.distinct(),
                sinFechaVerificada = sinFecha.distinct(),
                pasos = plantilla.pasos
            )
        }

        val completables = propuestas.filter { it.puedePrepararse }
            .sortedWith(compareByDescending<RecetaLocal> { it.proximosACaducar.size }
                .thenBy { it.tiempoMinutos })
        val conFaltantes = propuestas.filter {
            it.disponibles.isNotEmpty() && it.faltantes.isNotEmpty()
        }.sortedWith(compareBy<RecetaLocal> { it.faltantes.size }
            .thenByDescending { it.proximosACaducar.size }
            .thenByDescending { it.disponibles.size }
            .thenBy { it.tiempoMinutos })
        val paraComprar = propuestas.filter { it.disponibles.isEmpty() }
            .sortedWith(compareBy<RecetaLocal> { it.faltantes.size }
                .thenBy { it.tiempoMinutos })

        return ResultadoRecetas(completables, conFaltantes, paraComprar, disponibles.size)
    }

    // Compatibilidad para las llamadas antiguas; nunca devuelve vacío solo
    // porque no haya productos, pues también ofrece recetas para comprar.
    fun recomendar(productos: List<Map<String, Any>>): List<RecetaLocal> =
        analizar(productos).todas
}

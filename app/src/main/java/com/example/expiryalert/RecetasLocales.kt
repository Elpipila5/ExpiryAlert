package com.example.expiryalert

import java.text.Normalizer

/**
 * Sugerencias locales para F10: funcionan sin un servidor IA desplegado.
 * Solo usan artículos activos y con cantidad positiva; nunca productos vencidos.
 * Las fechas pendientes se señalan expresamente para su verificación.
 */
data class RecetaLocal(
    val nombre: String,
    val tiempoMinutos: Int,
    val disponibles: List<String>,
    val faltantes: List<String>,
    val proximosACaducar: List<String>,
    val sinFechaVerificada: List<String>,
    val pasos: List<String>
)

object RecetasLocales {
    private data class Ingrediente(val nombre: String, val palabras: List<String>)
    private data class Plantilla(
        val nombre: String,
        val tiempo: Int,
        val ingredientes: List<String>,
        val pasos: List<String>
    )
    private data class Producto(val nombre: String, val dias: Long?)

    private val vocabulario = mapOf(
        "huevo" to listOf("huevo"),
        "tomate" to listOf("tomate", "jitomate"),
        "aceite" to listOf("aceite"),
        "arroz" to listOf("arroz"),
        "zanahoria" to listOf("zanahoria"),
        "cebolla" to listOf("cebolla"),
        "tortilla" to listOf("tortilla"),
        "queso" to listOf("queso"),
        "lechuga" to listOf("lechuga"),
        "pepino" to listOf("pepino"),
        "pan" to listOf("pan ", "pan de", "bolillo"),
        "pasta" to listOf("pasta", "espagueti", "spaghetti", "fideo"),
        "papa" to listOf("papa", "patata"),
        "avena" to listOf("avena"),
        "platano" to listOf("platano", "banana"),
        "leche" to listOf("leche"),
        "atun" to listOf("atun"),
        "frijol" to listOf("frijol"),
        "limon" to listOf("limon")
    )

    private val plantillas = listOf(
        Plantilla("Huevos con tomate", 15, listOf("huevo", "tomate", "aceite"),
            listOf("Lava y corta el tomate.", "Cocina el tomate con un poco de aceite.",
                "Añade los huevos y cocínalos completamente.")),
        Plantilla("Arroz con verduras", 30, listOf("arroz", "zanahoria", "cebolla", "aceite"),
            listOf("Lava y corta las verduras.", "Cuece el arroz hasta que esté bien hecho.",
                "Saltea las verduras y mezcla con el arroz.")),
        Plantilla("Quesadillas", 12, listOf("tortilla", "queso"),
            listOf("Coloca queso sobre una tortilla.", "Calienta hasta que el queso se derrita.",
                "Sirve caliente.")),
        Plantilla("Ensalada fresca", 12, listOf("lechuga", "tomate", "pepino", "limon"),
            listOf("Lava muy bien las verduras.", "Córtalas en porciones pequeñas.",
                "Mezcla y adereza al gusto.")),
        Plantilla("Sándwich de queso y tomate", 10, listOf("pan", "queso", "tomate"),
            listOf("Lava y corta el tomate.", "Arma el sándwich con pan y queso.",
                "Calienta si lo prefieres.")),
        Plantilla("Pasta con tomate", 25, listOf("pasta", "tomate", "cebolla", "aceite"),
            listOf("Cuece la pasta según sus instrucciones.", "Cocina tomate y cebolla con aceite.",
                "Mezcla la pasta con la salsa bien caliente.")),
        Plantilla("Papas salteadas", 25, listOf("papa", "cebolla", "aceite"),
            listOf("Lava y corta las papas.", "Cocínalas hasta ablandar completamente.",
                "Saltea con cebolla hasta dorar.")),
        Plantilla("Avena con plátano", 10, listOf("avena", "platano", "leche"),
            listOf("Calienta la leche adecuadamente.", "Cuece la avena hasta obtener textura suave.",
                "Sirve con plátano limpio y cortado.")),
        Plantilla("Licuado de plátano", 5, listOf("platano", "leche"),
            listOf("Comprueba que la leche esté apta y fría.", "Pela el plátano.",
                "Licua ambos ingredientes y sirve de inmediato.")),
        Plantilla("Frijoles con tortilla", 15, listOf("frijol", "tortilla", "queso"),
            listOf("Calienta completamente los frijoles cocidos.",
                "Calienta las tortillas.", "Sirve con queso si está disponible."))
    )

    private fun normalizar(texto: String): String = Normalizer
        .normalize(texto.lowercase(), Normalizer.Form.NFD)
        .replace(Regex("""\p{Mn}+"""), "")
        .trim()

    fun recomendar(productos: List<Map<String, Any>>): List<RecetaLocal> {
        val aptos = productos.mapNotNull { dato ->
            if (dato["activo"] == false) return@mapNotNull null
            val cantidad = (dato["cantidad"] as? Number)?.toDouble() ?: return@mapNotNull null
            if (cantidad <= 0.0 || !cantidad.isFinite()) return@mapNotNull null
            val nombre = dato["nombre"]?.toString()?.trim().orEmpty()
            if (nombre.isBlank()) return@mapNotNull null
            val fecha = dato["fechaCaducidad"]?.toString().orEmpty()
            val dias = if (fecha.isBlank()) null
                       else FechasCaducidad.diasRestantes(fecha) ?: return@mapNotNull null
            if (dias != null && dias < 0) return@mapNotNull null
            Producto(nombre, dias)
        }
        if (aptos.isEmpty()) return emptyList()

        return plantillas.mapNotNull { plantilla ->
            val disponibles = mutableListOf<String>()
            val faltantes = mutableListOf<String>()
            val proximos = mutableListOf<String>()
            val sinFecha = mutableListOf<String>()

            for (ingrediente in plantilla.ingredientes) {
                val sinonimos = vocabulario[ingrediente] ?: listOf(ingrediente)
                val hallado = aptos
                    .filter { producto ->
                        val texto = normalizar(producto.nombre)
                        sinonimos.any { palabra -> texto.contains(normalizar(palabra)) }
                    }
                    .minByOrNull { it.dias ?: 999999L }

                if (hallado == null) {
                    faltantes += ingrediente
                } else {
                    disponibles += ingrediente
                    if (hallado.dias == null) sinFecha += hallado.nombre
                    if (hallado.dias != null && hallado.dias <= 7) proximos += hallado.nombre
                }
            }
            if (disponibles.isEmpty()) null else RecetaLocal(
                nombre = plantilla.nombre,
                tiempoMinutos = plantilla.tiempo,
                disponibles = disponibles,
                faltantes = faltantes,
                proximosACaducar = proximos.distinct(),
                sinFechaVerificada = sinFecha.distinct(),
                pasos = plantilla.pasos
            )
        }.sortedWith(compareByDescending<RecetaLocal> { it.proximosACaducar.size }
            .thenByDescending { it.disponibles.size }
            .thenBy { it.faltantes.size })
            .take(6)
    }
}

package com.example.expiryalert

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

data class EstadoGuardadoProducto(
    val clave: String = "",
    val guardando: Boolean = false,
    val completado: Boolean = false,
    val error: String? = null
)

/** Conserva la operación en curso al girar el teléfono, sin retener la actividad. */
class GuardadoProductoViewModel : ViewModel() {
    var estado by mutableStateOf(EstadoGuardadoProducto())
        private set
    private var generacion = 0L

    fun guardar(clave: String, accion: ((Exception?) -> Unit) -> Unit) {
        if (estado.guardando || (estado.clave == clave && estado.completado)) return
        val turno = ++generacion
        estado = EstadoGuardadoProducto(clave, guardando = true)
        val terminado: (Exception?) -> Unit = { fallo ->
            if (turno == generacion && estado.guardando) {
                estado = EstadoGuardadoProducto(clave, completado = fallo == null,
                    error = fallo?.localizedMessage ?: if (fallo != null) "No se pudo guardar. Revisa tu conexión." else null)
            }
        }
        try { accion(terminado) } catch (fallo: Exception) { terminado(fallo) }
    }

    fun consumirResultado(clave: String) {
        if (estado.clave == clave && !estado.guardando) estado = EstadoGuardadoProducto()
    }

    fun invalidar() {
        generacion++
        estado = EstadoGuardadoProducto()
    }

    override fun onCleared() { invalidar() }
}

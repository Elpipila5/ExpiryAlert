package com.example.expiryalert

import android.os.Handler
import android.os.Looper
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Cliente de la Etapa 2 del compañero, integrado sin exponer la clave de IA.
 * Requiere que RECETAS_API_BASE_URL apunte a un backend HTTPS desplegado.
 */
object RecetasIACliente {
    private val urlBase: String
        get() = BuildConfig.RECETAS_API_BASE_URL.trim().trimEnd('/')

    val estaConfigurado: Boolean
        get() = urlBase.startsWith("https://") && urlBase.length > "https://".length

    data class RecetaIA(
        val nombre: String,
        val tiempoMinutos: Int,
        val porciones: Int,
        val ingredientes: List<String>,
        val faltantes: List<String>,
        val pasos: List<String>,
        val advertencia: String
    )

    fun solicitar(onResultado: (Result<List<RecetaIA>>) -> Unit) {
        val main = Handler(Looper.getMainLooper())
        fun entregar(resultado: Result<List<RecetaIA>>) {
            main.post { onResultado(resultado) }
        }

        if (!estaConfigurado) {
            entregar(Result.failure(IllegalStateException(
                "La generación con IA aún no está configurada. " +
                    "Las recetas sugeridas y los enlaces de YouTube sí están disponibles."
            )))
            return
        }

        val usuario = FirebaseAuth.getInstance().currentUser
        if (usuario == null) {
            entregar(Result.failure(IllegalStateException("Inicia sesión primero.")))
            return
        }

        usuario.getIdToken(false)
            .addOnSuccessListener { token ->
                val jwt = token.token
                if (jwt.isNullOrBlank()) {
                    entregar(Result.failure(IllegalStateException("No se obtuvo el token de sesión.")))
                    return@addOnSuccessListener
                }

                Thread {
                    var conexion: HttpURLConnection? = null
                    try {
                        val url = URL("$urlBase/recetas/ia")
                        require(url.protocol == "https") {
                            "La conexión al backend requiere HTTPS."
                        }
                        conexion = (url.openConnection() as HttpURLConnection).apply {
                            requestMethod = "POST"
                            connectTimeout = 12000
                            readTimeout = 45000
                            doOutput = true
                            setRequestProperty("Authorization", "Bearer $jwt")
                            setRequestProperty("Accept", "application/json")
                            setRequestProperty("Content-Type", "application/json")
                        }
                        conexion.outputStream.use {
                            it.write("{}".toByteArray(Charsets.UTF_8))
                        }

                        val estado = conexion.responseCode
                        if (estado !in 200..299) {
                            val detalle = try {
                                val respuesta = conexion.errorStream?.bufferedReader()?.use {
                                    it.readText().take(1500)
                                }.orEmpty()
                                JSONObject(respuesta).optString("detail").take(180)
                            } catch (_: Exception) { "" }
                            throw IllegalStateException(
                                "El servidor respondió HTTP $estado" +
                                    (if (detalle.isBlank()) "." else ": $detalle")
                            )
                        }

                        val texto = conexion.inputStream.bufferedReader().use {
                            it.readText().take(128000)
                        }
                        val arreglo = JSONObject(texto).getJSONArray("recetas")
                        val recetas = (0 until arreglo.length()).map { indice ->
                            val dato = arreglo.getJSONObject(indice)
                            fun lista(campo: String): List<String> {
                                val array = dato.optJSONArray(campo) ?: return emptyList()
                                return (0 until array.length()).map { array.optString(it) }
                            }
                            RecetaIA(
                                nombre = dato.getString("nombre"),
                                tiempoMinutos = dato.optInt("tiempoMinutos"),
                                porciones = dato.optInt("porciones"),
                                ingredientes = lista("ingredientes"),
                                faltantes = lista("faltantes"),
                                pasos = lista("pasos"),
                                advertencia = dato.optString("advertencia")
                            )
                        }
                        if (recetas.isEmpty()) {
                            throw IllegalStateException("El servidor no devolvió recetas.")
                        }
                        entregar(Result.success(recetas))
                    } catch (e: Exception) {
                        entregar(Result.failure(e))
                    } finally {
                        conexion?.disconnect()
                    }
                }.start()
            }
            .addOnFailureListener {
                entregar(Result.failure(IllegalStateException(
                    "No se pudo verificar la sesión de Firebase: ${it.localizedMessage}"
                )))
            }
    }
}

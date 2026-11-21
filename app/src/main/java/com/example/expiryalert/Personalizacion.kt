package com.example.expiryalert

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.util.AtomicFile
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.expiryalert.ui.theme.ExpiryAlertTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import kotlin.math.min
import kotlin.math.roundToInt

class ControlApariencia(context: Context, oscuroSistema: Boolean, nombrePreferencias: String = "apariencia_expiry") {
    private val preferencias = context.applicationContext.getSharedPreferences(nombrePreferencias, Context.MODE_PRIVATE)
    var oscuro by mutableStateOf(preferencias.getBoolean("modo_oscuro", oscuroSistema))
        private set
    var revisionFotos by mutableLongStateOf(0L)
        private set

    fun establecerOscuro(valor: Boolean) {
        oscuro = valor
        preferencias.edit().putBoolean("modo_oscuro", valor).apply()
    }
    fun seguirSistema(valor: Boolean) {
        if (!preferencias.contains("modo_oscuro")) oscuro = valor
    }
    fun fotoActualizada() { revisionFotos++ }
}

val LocalApariencia = staticCompositionLocalOf<ControlApariencia?> { null }

@Composable
fun ExpiryAlertPersonalizado(contenido: @Composable () -> Unit) {
    val contexto = LocalContext.current
    val sistemaOscuro = isSystemInDarkTheme()
    val control = remember(contexto.applicationContext) { ControlApariencia(contexto, sistemaOscuro) }
    val oscuro = control.oscuro
    val vista = LocalView.current
    val actividad = LocalActivity.current
    LaunchedEffect(sistemaOscuro) { control.seguirSistema(sistemaOscuro) }
    SideEffect {
        actividad?.window?.let { ventana ->
            WindowCompat.getInsetsController(ventana, vista).apply {
                isAppearanceLightStatusBars = !oscuro
                isAppearanceLightNavigationBars = !oscuro
            }
        }
    }
    CompositionLocalProvider(LocalApariencia provides control) {
        ExpiryAlertTheme(darkTheme = oscuro, content = contenido)
    }
}

@Composable
fun BotonModoOscuro() {
    val control = LocalApariencia.current ?: return
    FilledTonalIconButton(onClick = { control.establecerOscuro(!control.oscuro) },
        modifier = Modifier.size(44.dp), shape = CircleShape,
        colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = SurfaceCard, contentColor = TextPrimary)) {
        Icon(if (control.oscuro) IconosExpiry.Sol else IconosExpiry.Luna,
            contentDescription = if (control.oscuro) "Activar modo claro" else "Activar modo oscuro",
            modifier = Modifier.size(21.dp))
    }
}

/** La foto se copia al almacenamiento privado; no depende de permisos permanentes de la galería. */
object FotosPerfil {
    private const val TAMANO = 768

    internal fun archivo(context: Context, uid: String): File {
        require(uid.isNotBlank()) { "Inicia sesión para personalizar tu perfil." }
        val clave = MessageDigest.getInstance("SHA-256").digest(uid.toByteArray(Charsets.UTF_8))
            .joinToString("") { (it.toInt() and 255).toString(16).padStart(2, '0') }
        val carpeta = File(context.applicationContext.filesDir, "fotos_perfil")
        return File(carpeta, "$clave.jpg")
    }

    fun existe(context: Context, uid: String): Boolean = uid.isNotBlank() &&
        (archivo(context, uid).isFile || File(archivo(context, uid).path + ".bak").isFile)

    fun leer(context: Context, uid: String): Bitmap? {
        if (!existe(context, uid)) return null
        return try { AtomicFile(archivo(context, uid)).openRead().use { BitmapFactory.decodeStream(it) } }
        catch (_: Exception) { null }
    }

    @Synchronized
    fun guardar(context: Context, uid: String, uri: Uri) {
        require(uid.isNotBlank()) { "Inicia sesión para personalizar tu perfil." }
        require(uri.scheme in listOf("content", "file")) { "Selecciona una imagen de tu dispositivo." }
        val original = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val fuente = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(fuente) { decoder, info, _ ->
                val mayor = maxOf(info.size.width, info.size.height)
                require(mayor > 0) { "La imagen está vacía." }
                val escala = min(1.0, (TAMANO * 2.0) / mayor)
                decoder.setTargetSize((info.size.width * escala).roundToInt().coerceAtLeast(1),
                    (info.size.height * escala).roundToInt().coerceAtLeast(1))
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else leerCompatible(context, uri)
        val lado = min(original.width, original.height)
        val recorte = Bitmap.createBitmap(original, (original.width - lado) / 2, (original.height - lado) / 2, lado, lado)
        val medida = min(lado, TAMANO)
        val escalada = Bitmap.createScaledBitmap(recorte, medida, medida, true)
        val opaca = Bitmap.createBitmap(medida, medida, Bitmap.Config.ARGB_8888)
        Canvas(opaca).apply { drawColor(android.graphics.Color.WHITE); drawBitmap(escalada, 0f, 0f, null) }
        val destino = archivo(context, uid)
        check(destino.parentFile!!.isDirectory || destino.parentFile!!.mkdirs()) { "No se pudo preparar la carpeta de fotos." }
        val atomico = AtomicFile(destino)
        val salida = atomico.startWrite()
        try {
            check(opaca.compress(Bitmap.CompressFormat.JPEG, 88, salida)) { "No se pudo guardar la imagen." }
            atomico.finishWrite(salida)
        } catch (fallo: Exception) {
            atomico.failWrite(salida)
            throw fallo
        } finally {
            setOf(original, recorte, escalada, opaca).forEach { it.recycle() }
        }
    }

    private fun leerCompatible(context: Context, uri: Uri): Bitmap {
        val opciones = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opciones) }
        require(opciones.outWidth > 0 && opciones.outHeight > 0) { "No se pudo abrir esta imagen." }
        opciones.inJustDecodeBounds = false
        opciones.inSampleSize = 1
        while (maxOf(opciones.outWidth, opciones.outHeight) / opciones.inSampleSize > TAMANO * 2) opciones.inSampleSize *= 2
        val foto = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opciones) }
            ?: error("No se pudo abrir esta imagen.")
        val orientacion = try {
            context.contentResolver.openInputStream(uri)?.use { ExifInterface(it).getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (_: Exception) { ExifInterface.ORIENTATION_NORMAL }
        val matriz = Matrix().apply {
            when (orientacion) {
                ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> { postRotate(90f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_TRANSVERSE -> { postRotate(270f); postScale(-1f, 1f) }
            }
        }
        return if (matriz.isIdentity) foto else Bitmap.createBitmap(foto, 0, 0, foto.width, foto.height, matriz, true)
            .also { if (it !== foto) foto.recycle() }
    }

    @Synchronized
    fun quitar(context: Context, uid: String) { AtomicFile(archivo(context, uid)).delete() }
}

@Composable
fun AvatarPerfil(uid: String, nombre: String, tamano: Dp = 48.dp, onClick: (() -> Unit)? = null,
    descripcionClick: String = "Abrir mi perfil") {
    val contexto = LocalContext.current
    val revision = LocalApariencia.current?.revisionFotos ?: 0L
    val foto = key(uid) {
        produceState<ImageBitmap?>(null, revision) {
            value = withContext(Dispatchers.IO) { FotosPerfil.leer(contexto, uid)?.asImageBitmap() }
        }.value
    }
    val inicial = nombre.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "U"
    val modificador = Modifier.size(tamano).clip(CircleShape)
        .background(Brush.linearGradient(listOf(StatusSafeBg, SurfaceMuted)))
        .border(2.dp, SurfaceCard, CircleShape)
        .then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick)
            .semantics { contentDescription = descripcionClick })
    Box(modificador, contentAlignment = Alignment.Center) {
        if (foto != null) Image(foto!!, "Foto de perfil", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Text(inicial, style = if (tamano >= 80.dp) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold, color = AccentSage)
    }
}

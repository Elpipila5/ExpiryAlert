package com.example.expiryalert

import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expiryalert.ui.theme.ExpiryAlertTheme
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

typealias ProductoMapa = Map<String, Any>

private fun mapaVacio(): ProductoMapa = emptyMap()
private fun listaProductosVacia(): List<ProductoMapa> = emptyList()

// ----------------------------------------------------
// PALETA MINIMALISTA Y ELEGANTE
// ----------------------------------------------------
internal val BgPrimary = Color(0xFFFAFAFA)       // Blanco hueso muy sutil
internal val SurfaceCard = Color(0xFFFFFFFF)     // Blanco puro
internal val SurfaceMuted = Color(0xFFF4F4F5)    // Gris tenue para inputs y píldoras
internal val BorderSubtle = Color(0xFFE4E4E7)    // Borde fino elegante
internal val TextPrimary = Color(0xFF18181B)     // Negro carbón suave
internal val TextSecondary = Color(0xFF71717A)   // Gris neutro para subtítulos
internal val AccentDark = Color(0xFF18181B)      // Botón principal minimalista
internal val AccentSage = Color(0xFF0F766E)      // Verde salvia profundo (Recetas / Seguro)

internal val StatusSafe = Color(0xFF15803D)
internal val StatusSafeBg = Color(0xFFF0FDF4)
internal val StatusWarn = Color(0xFFB45309)
internal val StatusWarnBg = Color(0xFFFFFBEB)
internal val StatusDanger = Color(0xFFB91C1C)
internal val StatusDangerBg = Color(0xFFFEF2F2)

class MainActivity : ComponentActivity() {

  companion object {
    private const val ESTADO_AVISO_UID = "expiry_aviso_uid"
    private const val ESTADO_AVISO_ID = "expiry_aviso_id"
    private const val ESTADO_DESTINO = "expiry_destino"
    private const val ESTADO_DESTINO_UID = "expiry_destino_uid"
  }

  private var avisoUid by mutableStateOf("")
  private var avisoProductoId by mutableStateOf("")
  private var destinoPantalla by mutableStateOf("")
  private var destinoUid by mutableStateOf("")

  private fun atenderDestino() {
    destinoPantalla = ""
    destinoUid = ""
  }

  private fun limpiarExtrasAviso(origen: Intent?) {
    origen?.removeExtra(NotificationHelper.EXTRA_USUARIO_ID)
    origen?.removeExtra(NotificationHelper.EXTRA_PRODUCTO_ID)
    origen?.removeExtra(AvisosInventario.EXTRA_DESTINO)
    origen?.removeExtra(AvisosInventario.EXTRA_UID)
  }

  private fun leerAviso(origen: Intent?) {
    val destino = origen?.getStringExtra(AvisosInventario.EXTRA_DESTINO).orEmpty()
    if (destino in listOf("compras", "revision", "desayuno")) {
      destinoPantalla = destino
      destinoUid = origen?.getStringExtra(AvisosInventario.EXTRA_UID).orEmpty()
    }
    val uid = origen?.getStringExtra(NotificationHelper.EXTRA_USUARIO_ID).orEmpty()
    val id = origen?.getStringExtra(NotificationHelper.EXTRA_PRODUCTO_ID).orEmpty()
    if (uid.isNotBlank() && id.isNotBlank() && !AvisosAtendidos.fueAtendido(this, uid, id)) {
      avisoUid = uid
      avisoProductoId = id
    }
    limpiarExtrasAviso(origen)
  }

  private fun atenderAviso() {
    avisoUid = ""
    avisoProductoId = ""
    limpiarExtrasAviso(intent)
  }

  override fun onSaveInstanceState(outState: Bundle) {
    if (avisoUid.isNotBlank() && avisoProductoId.isNotBlank()) {
      outState.putString(ESTADO_AVISO_UID, avisoUid)
      outState.putString(ESTADO_AVISO_ID, avisoProductoId)
    }
    outState.putString(ESTADO_DESTINO, destinoPantalla)
    outState.putString(ESTADO_DESTINO_UID, destinoUid)
    super.onSaveInstanceState(outState)
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    leerAviso(intent)
  }

  private val solicitarPermisoNotificaciones =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
      if (concedido) {
        Toast.makeText(this, "Notificaciones activadas", Toast.LENGTH_SHORT).show()
        NotificationHelper.mostrarNotificacionPrueba(this)
      } else {
        Toast.makeText(this, "Permiso de notificaciones no concedido", Toast.LENGTH_SHORT).show()
      }
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    NotificationHelper.crearCanalNotificaciones(this)
    pedirPermisoNotificaciones()

    if (savedInstanceState == null) {
      leerAviso(intent)
    } else {
      destinoPantalla = savedInstanceState.getString(ESTADO_DESTINO).orEmpty()
      destinoUid = savedInstanceState.getString(ESTADO_DESTINO_UID).orEmpty()
      val uid = savedInstanceState.getString(ESTADO_AVISO_UID).orEmpty()
      val productoId = savedInstanceState.getString(ESTADO_AVISO_ID).orEmpty()
      if (uid.isNotBlank() && productoId.isNotBlank() && !AvisosAtendidos.fueAtendido(this, uid, productoId)) {
        avisoUid = uid
        avisoProductoId = productoId
      }
      limpiarExtrasAviso(intent)
    }

    FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
      ProgramadorAlertas.activar(this, uid)
    }

    setContent {
      ExpiryAlertTheme {
        ExpiryAlertApp(
          destinoUid = destinoUid,
          destinoPantalla = destinoPantalla,
          destinoAtendido = { atenderDestino() },
          avisoUid = avisoUid,
          avisoProductoId = avisoProductoId,
          avisoAtendido = { atenderAviso() },
          activarAlertas = { uid -> ProgramadorAlertas.activar(this, uid) },
          cerrarAlertas = { uid -> ProgramadorAlertas.desactivar(this, uid) }
        )
      }
    }
  }

  private fun pedirPermisoNotificaciones() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
        solicitarPermisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
      } else {
        NotificationHelper.mostrarNotificacionPrueba(this)
      }
    } else {
      NotificationHelper.mostrarNotificacionPrueba(this)
    }
  }
}

// ----------------------------------------------------
// COMPONENTES REUTILIZABLES MINIMALISTAS
// ----------------------------------------------------

@Composable
fun CampoTextoMinimalista(
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  placeholder: String = "",
  readOnly: Boolean = false,
  esPassword: Boolean = false,
  keyboardType: KeyboardType = KeyboardType.Text
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Text(
      text = label,
      fontSize = 13.sp,
      fontWeight = FontWeight.Medium,
      color = TextSecondary,
      modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
    )
    OutlinedTextField(
      value = value,
      onValueChange = onValueChange,
      placeholder = if (placeholder.isNotBlank()) {
        { Text(placeholder, color = Color(0xFFA1A1AA), fontSize = 15.sp) }
      } else null,
      readOnly = readOnly,
      singleLine = true,
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(12.dp),
      visualTransformation = if (esPassword) PasswordVisualTransformation() else VisualTransformation.None,
      keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
      colors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = SurfaceCard,
        unfocusedContainerColor = SurfaceMuted,
        focusedBorderColor = TextPrimary,
        unfocusedBorderColor = BorderSubtle,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary,
        cursorColor = TextPrimary
      )
    )
  }
}

@Composable
fun CabeceraNavegacion(titulo: String, onRegresar: () -> Unit) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(bottom = 24.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Surface(
      onClick = onRegresar,
      shape = RoundedCornerShape(10.dp),
      color = SurfaceCard,
      border = BorderStroke(1.dp, BorderSubtle)
    ) {
      Text(
        text = "← Volver",
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = TextPrimary,
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
      )
    }
    Text(
      text = titulo,
      fontSize = 17.sp,
      fontWeight = FontWeight.SemiBold,
      color = TextPrimary
    )
    Spacer(modifier = Modifier.width(76.dp))
  }
}

@Composable
fun ExpiryAlertApp(
  destinoUid: String,
  destinoPantalla: String,
  destinoAtendido: () -> Unit,
  avisoUid: String,
  avisoProductoId: String,
  avisoAtendido: () -> Unit,
  activarAlertas: (String) -> Unit,
  cerrarAlertas: (String) -> Unit
) {
  var pantalla by rememberSaveable {
    mutableStateOf(if (FirebaseAuth.getInstance().currentUser != null) "principal" else "inicio")
  }
  var productoEditar by remember { mutableStateOf(mapaVacio()) }
  val contextoApp = LocalContext.current

  val accionCerrarSesion: () -> Unit = {
    FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
      cerrarAlertas(uid)
      NotificationHelper.cancelarAlertasDeUsuario(contextoApp, uid)
    }
    FirebaseAuth.getInstance().signOut()
    productoEditar = mapaVacio()
    avisoAtendido()
    destinoAtendido()
    pantalla = "inicio"
  }

  LaunchedEffect(destinoUid, destinoPantalla) {
    if (destinoPantalla.isNotBlank()) {
      if (FirebaseAuth.getInstance().currentUser?.uid == destinoUid &&
        destinoPantalla in listOf("compras", "revision", "desayuno")) pantalla = destinoPantalla
      destinoAtendido()
    }
  }

  BackHandler(enabled = pantalla !in listOf("inicio", "principal")) {
    pantalla = if (FirebaseAuth.getInstance().currentUser != null) "principal" else "inicio"
  }

  LaunchedEffect(pantalla) {
    if (pantalla == "principal") {
      FirebaseAuth.getInstance().currentUser?.uid?.let(activarAlertas)
    }
  }

  if (avisoUid.isNotBlank() && avisoProductoId.isNotBlank()) {
    if (FirebaseAuth.getInstance().currentUser?.uid != avisoUid ||
      AvisosAtendidos.fueAtendido(contextoApp, avisoUid, avisoProductoId)
    ) {
      LaunchedEffect(avisoUid, avisoProductoId) { avisoAtendido() }
    } else {
      DialogoConfirmacionCaducidad(avisoUid, avisoProductoId, avisoAtendido)
    }
  }

  Surface(modifier = Modifier.fillMaxSize(), color = BgPrimary) {
    when (pantalla) {
      "inicio" -> PantallaInicio(
        irLogin = { pantalla = "login" },
        irRegistro = { pantalla = "registro" }
      )
      "login" -> PantallaLogin(
        regresar = { pantalla = "inicio" },
        irRegistro = { pantalla = "registro" },
        loginExitoso = { pantalla = "principal" }
      )
      "registro" -> PantallaRegistro(
        regresar = { pantalla = "inicio" },
        irLogin = { pantalla = "login" },
        registroExitoso = { pantalla = "principal" }
      )
      "principal" -> PantallaPrincipal(
        agregarProducto = { pantalla = "agregarProducto" },
        verRecetas = { pantalla = "recetas" },
        irPerfil = { pantalla = "perfil" },
        irCompras = { pantalla = "compras" },
        irRevision = { pantalla = "revision" },
        irDesayuno = { pantalla = "desayuno" },
        editarProducto = { prod ->
          productoEditar = prod
          pantalla = "editarProducto"
        },
        cerrarSesion = accionCerrarSesion
      )
      "perfil" -> PantallaPerfil(
        regresar = { pantalla = "principal" },
        irAjustes = { pantalla = "ajustes" },
        cerrarSesion = accionCerrarSesion
      )
      "recetas" -> RecetasScreen(
        onVolver = { pantalla = "principal" },
        onCompras = { pantalla = "compras" }
      )
      "compras" -> ComprasScreen(onVolver = { pantalla = "principal" })
      "revision" -> RevisionInventarioScreen(onVolver = { pantalla = "principal" })
      "ajustes" -> AjustesInventarioScreen(onVolver = { pantalla = "perfil" })
      "desayuno" -> DesayunoScreen(
        onVolver = { pantalla = "principal" },
        onRecetas = { pantalla = "recetas" },
        onAjustes = { pantalla = "ajustes" }
      )
      "agregarProducto" -> PantallaAgregarProducto(
        regresar = { pantalla = "principal" },
        productoGuardado = { pantalla = "principal" }
      )
      "editarProducto" -> {
        if (productoEditar.isNotEmpty()) {
          PantallaEditarProducto(
            producto = productoEditar,
            regresar = {
              productoEditar = mapaVacio()
              pantalla = "principal"
            },
            productoActualizado = {
              productoEditar = mapaVacio()
              pantalla = "principal"
            }
          )
        } else {
          pantalla = "principal"
        }
      }
    }
  }
}

// ----------------------------------------------------
// PANTALLA DE INICIO
// ----------------------------------------------------

@Composable
fun PantallaInicio(irLogin: () -> Unit, irRegistro: () -> Unit) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(BgPrimary)
      .padding(horizontal = 28.dp)
      .padding(top = 90.dp, bottom = 36.dp),
    horizontalAlignment = Alignment.Start
  ) {
    Surface(
      shape = RoundedCornerShape(14.dp),
      color = AccentDark,
      modifier = Modifier.size(48.dp)
    ) {
      Box(contentAlignment = Alignment.Center) {
        Text(
          text = "EA",
          color = Color.White,
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.sp
        )
      }
    }

    Spacer(modifier = Modifier.height(28.dp))

    Text(
      text = "ExpiryAlert",
      fontSize = 36.sp,
      fontWeight = FontWeight.Bold,
      color = TextPrimary,
      letterSpacing = (-1).sp
    )

    Spacer(modifier = Modifier.height(10.dp))

    Text(
      text = "Gestión inteligente de inventario y control de caducidades sin distracciones.",
      fontSize = 16.sp,
      color = TextSecondary,
      lineHeight = 24.sp
    )

    Spacer(modifier = Modifier.height(36.dp))

    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      color = SurfaceCard,
      border = BorderStroke(1.dp, BorderSubtle)
    ) {
      Column(
        modifier = Modifier.padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        ItemCaracteristica("Escaneo rápido", "Códigos de barras y lectura óptica de fechas.")
        HorizontalDivider(color = SurfaceMuted)
        ItemCaracteristica("Alertas oportunas", "Notificaciones antes de que tus productos venzan.")
        HorizontalDivider(color = SurfaceMuted)
        ItemCaracteristica("Cocina sin desperdicio", "Sugerencias de recetas basadas en tu despensa.")
      }
    }

    Spacer(modifier = Modifier.weight(1f))

    Button(
      onClick = irLogin,
      modifier = Modifier
        .fillMaxWidth()
        .height(52.dp),
      shape = RoundedCornerShape(12.dp),
      colors = ButtonDefaults.buttonColors(containerColor = AccentDark, contentColor = Color.White)
    ) {
      Text(text = "Iniciar sesión", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedButton(
      onClick = irRegistro,
      modifier = Modifier
        .fillMaxWidth()
        .height(52.dp),
      shape = RoundedCornerShape(12.dp),
      border = BorderStroke(1.dp, BorderSubtle),
      colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
    ) {
      Text(text = "Crear una cuenta", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
  }
}

@Composable
private fun ItemCaracteristica(titulo: String, descripcion: String) {
  Row(verticalAlignment = Alignment.Top) {
    Box(
      modifier = Modifier
        .padding(top = 6.dp)
        .size(6.dp)
        .clip(CircleShape)
        .background(AccentSage)
    )
    Spacer(modifier = Modifier.width(12.dp))
    Column {
      Text(text = titulo, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
      Text(text = descripcion, fontSize = 13.sp, color = TextSecondary)
    }
  }
}

// ----------------------------------------------------
// INICIAR SESIÓN
// ----------------------------------------------------

@Composable
fun PantallaLogin(regresar: () -> Unit, irRegistro: () -> Unit, loginExitoso: () -> Unit) {
  var correo by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var cargando by remember { mutableStateOf(false) }
  val context = LocalContext.current
  val auth = FirebaseAuth.getInstance()

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(BgPrimary)
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 24.dp)
      .padding(top = 56.dp, bottom = 30.dp)
  ) {
    CabeceraNavegacion(titulo = "Acceso", onRegresar = regresar)

    Text(
      text = "Bienvenido de nuevo",
      fontSize = 28.sp,
      fontWeight = FontWeight.Bold,
      color = TextPrimary,
      letterSpacing = (-0.5).sp
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = "Ingresa tus credenciales para acceder a tu inventario.",
      fontSize = 15.sp,
      color = TextSecondary
    )

    Spacer(modifier = Modifier.height(28.dp))

    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      color = SurfaceCard,
      border = BorderStroke(1.dp, BorderSubtle)
    ) {
      Column(modifier = Modifier.padding(22.dp)) {
        CampoTextoMinimalista(
          value = correo,
          onValueChange = { correo = it },
          label = "Correo electrónico",
          placeholder = "nombre@correo.com",
          keyboardType = KeyboardType.Email
        )
        Spacer(modifier = Modifier.height(16.dp))
        CampoTextoMinimalista(
          value = password,
          onValueChange = { password = it },
          label = "Contraseña",
          placeholder = "••••••••",
          esPassword = true,
          keyboardType = KeyboardType.Password
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
          onClick = {
            if (correo.isBlank() || password.isBlank()) {
              Toast.makeText(context, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            } else {
              cargando = true
              auth.signInWithEmailAndPassword(correo.trim(), password).addOnCompleteListener { task ->
                cargando = false
                if (task.isSuccessful) {
                  loginExitoso()
                } else {
                  Toast.makeText(context, "Error: ${task.exception?.localizedMessage}", Toast.LENGTH_LONG).show()
                }
              }
            }
          },
          enabled = !cargando,
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
          colors = ButtonDefaults.buttonColors(containerColor = AccentDark, contentColor = Color.White),
          shape = RoundedCornerShape(12.dp)
        ) {
          if (cargando) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
          } else {
            Text(text = "Continuar", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(text = "¿No tienes cuenta?", fontSize = 14.sp, color = TextSecondary)
      TextButton(onClick = irRegistro) {
        Text(text = "Regístrate", fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
      }
    }
  }
}

// ----------------------------------------------------
// CREAR CUENTA
// ----------------------------------------------------

@Composable
fun PantallaRegistro(regresar: () -> Unit, irLogin: () -> Unit, registroExitoso: () -> Unit) {
  var nombre by remember { mutableStateOf("") }
  var correo by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var confirmarPassword by remember { mutableStateOf("") }
  var cargando by remember { mutableStateOf(false) }
  val context = LocalContext.current
  val auth = FirebaseAuth.getInstance()

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(BgPrimary)
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 24.dp)
      .padding(top = 56.dp, bottom = 30.dp)
  ) {
    CabeceraNavegacion(titulo = "Registro", onRegresar = regresar)

    Text(
      text = "Crea tu cuenta",
      fontSize = 28.sp,
      fontWeight = FontWeight.Bold,
      color = TextPrimary,
      letterSpacing = (-0.5).sp
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = "Configura tu perfil en segundos para empezar.",
      fontSize = 15.sp,
      color = TextSecondary
    )

    Spacer(modifier = Modifier.height(24.dp))

    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      color = SurfaceCard,
      border = BorderStroke(1.dp, BorderSubtle)
    ) {
      Column(modifier = Modifier.padding(22.dp)) {
        CampoTextoMinimalista(
          value = nombre,
          onValueChange = { nombre = it },
          label = "Nombre o alias",
          placeholder = "Ej. Martín"
        )
        Spacer(modifier = Modifier.height(14.dp))
        CampoTextoMinimalista(
          value = correo,
          onValueChange = { correo = it },
          label = "Correo electrónico",
          placeholder = "nombre@correo.com",
          keyboardType = KeyboardType.Email
        )
        Spacer(modifier = Modifier.height(14.dp))
        CampoTextoMinimalista(
          value = password,
          onValueChange = { password = it },
          label = "Contraseña",
          placeholder = "Mínimo 6 caracteres",
          esPassword = true,
          keyboardType = KeyboardType.Password
        )
        Spacer(modifier = Modifier.height(14.dp))
        CampoTextoMinimalista(
          value = confirmarPassword,
          onValueChange = { confirmarPassword = it },
          label = "Confirmar contraseña",
          placeholder = "Repite tu contraseña",
          esPassword = true,
          keyboardType = KeyboardType.Password
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
          onClick = {
            when {
              nombre.isBlank() || correo.isBlank() || password.isBlank() || confirmarPassword.isBlank() ->
                Toast.makeText(context, "Completa todos los campos", Toast.LENGTH_SHORT).show()
              password != confirmarPassword ->
                Toast.makeText(context, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
              password.length < 6 ->
                Toast.makeText(context, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
              else -> {
                cargando = true
                auth.createUserWithEmailAndPassword(correo.trim(), password).addOnCompleteListener { task ->
                  if (task.isSuccessful) {
                    val request = UserProfileChangeRequest.Builder().setDisplayName(nombre.trim()).build()
                    auth.currentUser?.updateProfile(request)?.addOnCompleteListener {
                      cargando = false
                      registroExitoso()
                    } ?: run {
                      cargando = false
                      registroExitoso()
                    }
                  } else {
                    cargando = false
                    Toast.makeText(context, "Error: ${task.exception?.localizedMessage}", Toast.LENGTH_LONG).show()
                  }
                }
              }
            }
          },
          enabled = !cargando,
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
          colors = ButtonDefaults.buttonColors(containerColor = AccentDark, contentColor = Color.White),
          shape = RoundedCornerShape(12.dp)
        ) {
          if (cargando) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
          } else {
            Text(text = "Crear cuenta", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(18.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(text = "¿Ya tienes cuenta?", fontSize = 14.sp, color = TextSecondary)
      TextButton(onClick = irLogin) {
        Text(text = "Inicia sesión", fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
      }
    }
  }
}

// ----------------------------------------------------
// PANTALLA DE PERFIL DE USUARIO
// ----------------------------------------------------

@Composable
fun PantallaPerfil(regresar: () -> Unit, irAjustes: () -> Unit, cerrarSesion: () -> Unit) {
  val context = LocalContext.current
  val usuario = FirebaseAuth.getInstance().currentUser
  val db = FirebaseFirestore.getInstance()

  var nombreEditable by remember { mutableStateOf(usuario?.displayName.orEmpty()) }
  var guardandoNombre by remember { mutableStateOf(false) }
  var editandoNombre by remember { mutableStateOf(false) }

  var totalProductos by remember { mutableIntStateOf(0) }
  var seguros by remember { mutableIntStateOf(0) }
  var porVencer by remember { mutableIntStateOf(0) }
  var vencidos by remember { mutableIntStateOf(0) }

  LaunchedEffect(usuario?.uid) {
    usuario?.uid?.let { uid ->
      db.collection("productos").whereEqualTo("usuarioId", uid).get().addOnSuccessListener { snapshot ->
        val lista = snapshot.documents.mapNotNull { doc ->
          InventarioProductos.productoVisible(doc.id, doc.data)
        }
        totalProductos = lista.size
        seguros = lista.count { calcularEstadoCaducidad(it["fechaCaducidad"]?.toString().orEmpty()) == "Seguro" }
        porVencer = lista.count { calcularEstadoCaducidad(it["fechaCaducidad"]?.toString().orEmpty()) == "Próximo a caducar" }
        vencidos = lista.count { calcularEstadoCaducidad(it["fechaCaducidad"]?.toString().orEmpty()) == "Vencido" }
      }
    }
  }

  val inicial = remember(nombreEditable, usuario?.email) {
    val base = nombreEditable.ifBlank { usuario?.email ?: "U" }
    base.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "U"
  }

  val fechaCreacionCuenta = remember(usuario) {
    usuario?.metadata?.creationTimestamp?.let { ts ->
      if (ts > 0) SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(ts) else null
    } ?: "Reciente"
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(BgPrimary)
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 24.dp)
      .padding(top = 56.dp, bottom = 36.dp)
  ) {
    CabeceraNavegacion(titulo = "Perfil", onRegresar = regresar)

    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      color = SurfaceCard,
      border = BorderStroke(1.dp, BorderSubtle)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Box(
          modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(AccentDark),
          contentAlignment = Alignment.Center
        ) {
          Text(text = inicial, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!editandoNombre) {
          Text(
            text = nombreEditable.ifBlank { "Usuario" },
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(text = usuario?.email ?: "Sin correo asociado", fontSize = 14.sp, color = TextSecondary)
          Spacer(modifier = Modifier.height(16.dp))
          OutlinedButton(
            onClick = { editandoNombre = true },
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, BorderSubtle),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
          ) {
            Text("Editar nombre", fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
          }
        } else {
          CampoTextoMinimalista(
            value = nombreEditable,
            onValueChange = { nombreEditable = it },
            label = "Nombre para mostrar"
          )
          Spacer(modifier = Modifier.height(14.dp))
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
              onClick = {
                nombreEditable = usuario?.displayName.orEmpty()
                editandoNombre = false
              },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, BorderSubtle)
            ) {
              Text("Cancelar", color = TextSecondary)
            }
            Button(
              onClick = {
                if (nombreEditable.isBlank()) {
                  Toast.makeText(context, "Escribe un nombre válido", Toast.LENGTH_SHORT).show()
                } else {
                  guardandoNombre = true
                  val req = UserProfileChangeRequest.Builder().setDisplayName(nombreEditable.trim()).build()
                  usuario?.updateProfile(req)?.addOnCompleteListener { task ->
                    guardandoNombre = false
                    if (task.isSuccessful) {
                      editandoNombre = false
                      Toast.makeText(context, "Nombre actualizado", Toast.LENGTH_SHORT).show()
                    } else {
                      Toast.makeText(context, "No se pudo actualizar", Toast.LENGTH_SHORT).show()
                    }
                  }
                }
              },
              enabled = !guardandoNombre,
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(containerColor = AccentDark)
            ) {
              Text(if (guardandoNombre) "Guardando..." else "Guardar", color = Color.White)
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(18.dp))

    Text(
      text = "RESUMEN DE DESPENSA",
      fontSize = 12.sp,
      fontWeight = FontWeight.SemiBold,
      color = TextSecondary,
      letterSpacing = 0.8.sp,
      modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      EstadisticaMiniCard(Modifier.weight(1f), "Activos", totalProductos.toString(), TextPrimary)
      EstadisticaMiniCard(Modifier.weight(1f), "En orden", seguros.toString(), StatusSafe)
      EstadisticaMiniCard(Modifier.weight(1f), "Por vencer", porVencer.toString(), StatusWarn)
      EstadisticaMiniCard(Modifier.weight(1f), "Vencidos", vencidos.toString(), StatusDanger)
    }

    Spacer(modifier = Modifier.height(22.dp))

    Text(
      text = "PREFERENCIAS Y CUENTA",
      fontSize = 12.sp,
      fontWeight = FontWeight.SemiBold,
      color = TextSecondary,
      letterSpacing = 0.8.sp,
      modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )

    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      color = SurfaceCard,
      border = BorderStroke(1.dp, BorderSubtle)
    ) {
      Column(modifier = Modifier.padding(20.dp)) {
        OutlinedButton(onClick = irAjustes, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
          Text("Inventario y horario de desayuno", color = TextPrimary)
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = SurfaceMuted)
        FilaDetallePerfil("Miembro desde", fechaCreacionCuenta)
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = SurfaceMuted)
        FilaDetallePerfil("ID de cuenta", "${usuario?.uid?.take(10) ?: "N/A"}…")
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = SurfaceMuted)
        OutlinedButton(
          onClick = {
            NotificationHelper.mostrarNotificacionPrueba(context)
            Toast.makeText(context, "Notificación enviada", Toast.LENGTH_SHORT).show()
          },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.dp, BorderSubtle)
        ) {
          Text("Enviar notificación de prueba", color = TextPrimary, fontSize = 13.sp)
        }
      }
    }

    Spacer(modifier = Modifier.height(24.dp))

    OutlinedButton(
      onClick = cerrarSesion,
      modifier = Modifier
        .fillMaxWidth()
        .height(50.dp),
      shape = RoundedCornerShape(12.dp),
      border = BorderStroke(1.dp, Color(0xFFFECACA)),
      colors = ButtonDefaults.outlinedButtonColors(
        containerColor = StatusDangerBg,
        contentColor = StatusDanger
      )
    ) {
      Text(text = "Cerrar sesión", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
  }
}

@Composable
private fun FilaDetallePerfil(etiqueta: String, valor: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(text = etiqueta, fontSize = 14.sp, color = TextSecondary)
    Text(text = valor, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
  }
}

@Composable
fun EstadisticaMiniCard(
  modifier: Modifier = Modifier,
  titulo: String,
  valor: String,
  colorIndicador: Color
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(14.dp),
    color = SurfaceCard,
    border = BorderStroke(1.dp, BorderSubtle)
  ) {
    Column(
      modifier = Modifier.padding(vertical = 14.dp, horizontal = 10.dp),
      horizontalAlignment = Alignment.Start
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(7.dp)
            .clip(CircleShape)
            .background(colorIndicador)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = titulo,
          fontSize = 11.sp,
          fontWeight = FontWeight.Medium,
          color = TextSecondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = valor,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        color = TextPrimary
      )
    }
  }
}

// ----------------------------------------------------
// PANTALLA PRINCIPAL
// ----------------------------------------------------

@Composable
fun PantallaPrincipal(
  agregarProducto: () -> Unit,
  verRecetas: () -> Unit,
  irPerfil: () -> Unit,
  irCompras: () -> Unit,
  irRevision: () -> Unit,
  irDesayuno: () -> Unit,
  editarProducto: (ProductoMapa) -> Unit,
  cerrarSesion: () -> Unit
) {
  val usuario = FirebaseAuth.getInstance().currentUser
  val db = FirebaseFirestore.getInstance()
  val context = LocalContext.current

  var productos by remember { mutableStateOf(listaProductosVacia()) }
  var cargando by remember { mutableStateOf(true) }
  var errorCarga by remember { mutableStateOf("") }
  var productoEliminar by remember { mutableStateOf(mapaVacio()) }
  var revisandoCaducidades by remember { mutableStateOf(false) }
  var resultadoRevision by remember { mutableStateOf("") }
  var productoAccion by rememberSaveable(usuario?.uid) { mutableStateOf("") }
  var accionCantidad by rememberSaveable(usuario?.uid) { mutableStateOf(OperacionCantidad.CONSUMIR.name) }
  val ajustes = PreferenciasInventario.ajustes(context, usuario?.uid.orEmpty())
  val porReponer = productos.count { ControlCantidades.bajoStock(it, ajustes.porcentajeStock) }
  val porRevisar = productos.count { revisionPendiente(it, ajustes) }

  productos.firstOrNull { it["id"] == productoAccion }?.let { producto ->
    DialogoCantidad(producto, OperacionCantidad.valueOf(accionCantidad), { productoAccion = "" }) {
      productoAccion = ""
      Toast.makeText(context, "Cantidad actualizada", Toast.LENGTH_SHORT).show()
    }
  }

  LaunchedEffect(productos, usuario?.uid, cargando, errorCarga) {
    if (usuario != null && !cargando && errorCarga.isBlank()) AvisosInventario.comprobar(context, usuario.uid, productos)
  }

  val nombreMostrado = remember(usuario?.displayName, usuario?.email) {
    val display = usuario?.displayName
    if (!display.isNullOrBlank()) display
    else usuario?.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() } ?: "Usuario"
  }

  val inicialAvatar = remember(nombreMostrado) {
    nombreMostrado.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "U"
  }

  val conteoSeguros = remember(productos) {
    productos.count { calcularEstadoCaducidad(it["fechaCaducidad"]?.toString().orEmpty()) == "Seguro" }
  }
  val conteoPorVencer = remember(productos) {
    productos.count { calcularEstadoCaducidad(it["fechaCaducidad"]?.toString().orEmpty()) == "Próximo a caducar" }
  }
  val conteoVencidos = remember(productos) {
    productos.count { calcularEstadoCaducidad(it["fechaCaducidad"]?.toString().orEmpty()) == "Vencido" }
  }

  DisposableEffect(usuario?.uid) {
    if (usuario == null) {
      cargando = false
      errorCarga = "No hay una sesión iniciada."
      onDispose { }
    } else {
      val listener = db.collection("productos")
        .whereEqualTo("usuarioId", usuario.uid)
        .addSnapshotListener { snapshot, error ->
          if (error != null) {
            cargando = false
            errorCarga = error.localizedMessage ?: "Error desconocido"
            return@addSnapshotListener
          }
          if (snapshot != null) {
            productos = snapshot.documents.mapNotNull { doc ->
              InventarioProductos.productoVisible(doc.id, doc.data)
            }
            cargando = false
            errorCarga = ""
          }
        }
      onDispose { listener.remove() }
    }
  }

  if (productoEliminar.isNotEmpty()) {
    AlertDialog(
      onDismissRequest = { productoEliminar = mapaVacio() },
      containerColor = SurfaceCard,
      title = { Text(text = "Eliminar producto", fontWeight = FontWeight.Bold, color = TextPrimary) },
      text = {
        Text(
          text = "¿Confirmas que deseas eliminar \"${productoEliminar["nombre"]}\" de tu inventario?",
          color = TextSecondary
        )
      },
      confirmButton = {
        Button(
          onClick = {
            val id = productoEliminar["id"]?.toString().orEmpty()
            if (id.isBlank()) {
              Toast.makeText(context, "No se encontró el ID del producto", Toast.LENGTH_SHORT).show()
              productoEliminar = mapaVacio()
            } else {
              db.collection("productos").document(id).delete()
                .addOnSuccessListener {
                  FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
                    NotificationHelper.cancelarAlerta(context, uid, id)
                  }
                  Toast.makeText(context, "Producto eliminado", Toast.LENGTH_SHORT).show()
                  productoEliminar = mapaVacio()
                }
                .addOnFailureListener { err ->
                  Toast.makeText(context, "Error: ${err.localizedMessage}", Toast.LENGTH_LONG).show()
                  productoEliminar = mapaVacio()
                }
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = StatusDanger, contentColor = Color.White),
          shape = RoundedCornerShape(10.dp)
        ) {
          Text(text = "Eliminar", fontWeight = FontWeight.SemiBold)
        }
      },
      dismissButton = {
        TextButton(onClick = { productoEliminar = mapaVacio() }) {
          Text(text = "Cancelar", color = TextSecondary)
        }
      }
    )
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(BgPrimary)
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 20.dp)
      .padding(top = 56.dp, bottom = 32.dp)
  ) {
    // Cabecera minimalista
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "Hola, $nombreMostrado",
          fontSize = 13.sp,
          fontWeight = FontWeight.Medium,
          color = TextSecondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = "Mi Despensa",
          fontSize = 28.sp,
          fontWeight = FontWeight.Bold,
          color = TextPrimary,
          letterSpacing = (-0.5).sp
        )
      }
      Surface(
        modifier = Modifier
          .size(44.dp)
          .clip(CircleShape)
          .clickable { irPerfil() },
        shape = CircleShape,
        color = SurfaceCard,
        border = BorderStroke(1.dp, BorderSubtle)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Text(
            text = inicialAvatar,
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(22.dp))

    // Tarjetas de estado limpias
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      EstadisticaMiniCard(Modifier.weight(1f), "En orden", conteoSeguros.toString(), StatusSafe)
      EstadisticaMiniCard(Modifier.weight(1f), "Por vencer", conteoPorVencer.toString(), StatusWarn)
      EstadisticaMiniCard(Modifier.weight(1f), "Vencidos", conteoVencidos.toString(), StatusDanger)
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Acciones principales
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      Button(
        onClick = agregarProducto,
        modifier = Modifier
          .weight(1f)
          .height(48.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AccentDark, contentColor = Color.White),
        shape = RoundedCornerShape(12.dp)
      ) {
        Text(text = "+ Nuevo producto", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
      }
      OutlinedButton(
        onClick = verRecetas,
        modifier = Modifier
          .weight(1f)
          .height(48.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderSubtle),
        colors = ButtonDefaults.outlinedButtonColors(
          containerColor = SurfaceCard,
          contentColor = AccentSage
        )
      ) {
        Text(text = "Ver recetas", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
      }
    }

    Spacer(modifier = Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      OutlinedButton(onClick = irCompras, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
        Text("Mis compras ($porReponer)", color = TextPrimary, fontSize = 13.sp)
      }
      OutlinedButton(onClick = irDesayuno, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
        Text("Mi desayuno", color = TextPrimary, fontSize = 13.sp)
      }
    }
    TextButton(onClick = irRevision) {
      Text(if (porRevisar > 0) "Revisar cantidades · $porRevisar pendientes" else "Revisar cantidades de mi despensa", color = AccentSage)
    }
    Spacer(modifier = Modifier.height(18.dp))

    // Encabezado de lista
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "INVENTARIO (${productos.size})",
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextSecondary,
        letterSpacing = 0.8.sp
      )
      Text(
        text = if (revisandoCaducidades) "Sincronizando…" else "Verificar alertas",
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = AccentSage,
        modifier = Modifier.clickable(enabled = !revisandoCaducidades) {
          usuario?.uid?.let { uid ->
            revisandoCaducidades = true
            resultadoRevision = ""
            RevisionManualCaducidad.revisar(context, uid) { mensaje ->
              revisandoCaducidades = false
              resultadoRevision = mensaje
            }
          }
        }
      )
    }

    if (revisandoCaducidades) {
      Spacer(modifier = Modifier.height(8.dp))
      LinearProgressIndicator(
        modifier = Modifier.fillMaxWidth(),
        color = AccentDark,
        trackColor = BorderSubtle
      )
    }
    if (resultadoRevision.isNotBlank()) {
      Spacer(modifier = Modifier.height(8.dp))
      Text(text = resultadoRevision, color = TextSecondary, fontSize = 13.sp)
    }

    Spacer(modifier = Modifier.height(12.dp))

    when {
      cargando -> {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
          contentAlignment = Alignment.Center
        ) {
          CircularProgressIndicator(color = TextPrimary, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
        }
      }
      errorCarga.isNotBlank() -> {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          color = SurfaceCard,
          border = BorderStroke(1.dp, BorderSubtle)
        ) {
          Column(modifier = Modifier.padding(22.dp)) {
            Text(text = "Error al cargar el inventario", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = errorCarga, color = TextSecondary, fontSize = 14.sp)
          }
        }
      }
      productos.isEmpty() -> {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          color = SurfaceCard,
          border = BorderStroke(1.dp, BorderSubtle)
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 40.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = "Sin productos registrados",
              fontSize = 17.sp,
              fontWeight = FontWeight.SemiBold,
              color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Agrega tu primer artículo mediante código de barras o registro manual.",
              fontSize = 14.sp,
              color = TextSecondary,
              textAlign = TextAlign.Center
            )
          }
        }
      }
      else -> {
        productos.forEach { prod ->
          TarjetaProducto(
            producto = prod,
            editar = { editarProducto(prod) },
            eliminar = { productoEliminar = prod },
            bajoStock = ControlCantidades.bajoStock(prod, ajustes.porcentajeStock),
            consumir = { accionCantidad = OperacionCantidad.CONSUMIR.name; productoAccion = prod["id"].toString() },
            ajustar = { accionCantidad = OperacionCantidad.CORREGIR.name; productoAccion = prod["id"].toString() },
            agregarCompra = {
              try {
                val uid = usuario?.uid ?: error("Inicia sesión para guardar compras.")
                val nuevos = PreferenciasInventario.agregarCompras(context, uid, listOf(ItemCompra(
                  nombre = prod["nombre"].toString(),
                  cantidad = ControlCantidades.numero(prod["cantidadInicial"])?.takeIf { it > 0 } ?: 1.0,
                  unidad = prod["unidad"]?.toString().orEmpty().ifBlank { "pzas" }
                )))
                Toast.makeText(context, if (nuevos == 0) "Ya está en tu lista" else "Agregado a compras", Toast.LENGTH_SHORT).show()
              } catch (e: Exception) { Toast.makeText(context, e.localizedMessage, Toast.LENGTH_LONG).show() }
            }
          )
          Spacer(modifier = Modifier.height(10.dp))
        }
      }
    }
  }
}

// ----------------------------------------------------
// TARJETA DEL PRODUCTO MINIMALISTA
// ----------------------------------------------------

@Composable
fun TarjetaProducto(
  producto: ProductoMapa,
  editar: () -> Unit,
  eliminar: () -> Unit,
  bajoStock: Boolean,
  consumir: () -> Unit,
  ajustar: () -> Unit,
  agregarCompra: () -> Unit
) {
  val cantidad = ControlCantidades.numero(producto["cantidad"]) ?: 0.0
  val inicial = ControlCantidades.numero(producto["cantidadInicial"])?.takeIf { it > 0 }
  val unidad = producto["unidad"]?.toString() ?: "pzas"
  val fecha = producto["fechaCaducidad"]?.toString().orEmpty()
  val estado = calcularEstadoCaducidad(fecha)
  val color = when (estado) {
    "Vencido" -> StatusDanger
    "Próximo a caducar" -> StatusWarn
    "Seguro" -> StatusSafe
    else -> TextSecondary
  }
  PanelInventario {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
      Column(Modifier.weight(1f).padding(end = 12.dp)) {
        Text(producto["nombre"]?.toString() ?: "Sin nombre", fontSize = 17.sp,
          fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Text(listOfNotNull(producto["categoria"]?.toString(), producto["marca"]?.toString()?.takeIf { it.isNotBlank() })
          .joinToString(" · "), fontSize = 13.sp, color = TextSecondary)
      }
      val fondo = when (estado) {
        "Vencido" -> StatusDangerBg
        "Próximo a caducar" -> StatusWarnBg
        "Seguro" -> StatusSafeBg
        else -> SurfaceMuted
      }
      Row(Modifier.clip(RoundedCornerShape(99.dp)).background(fondo).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(estado, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
      }
    }
    HorizontalDivider(color = SurfaceMuted)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      Column(Modifier.weight(1f)) {
        Text("CANTIDAD RESTANTE", fontSize = 10.sp, color = TextSecondary)
        Text("${ControlCantidades.texto(cantidad)} $unidad", color = TextPrimary, fontWeight = FontWeight.SemiBold)
        if (inicial != null) Text("Inicial: ${ControlCantidades.texto(inicial)} $unidad", fontSize = 12.sp, color = TextSecondary)
      }
      Column(Modifier.weight(1f)) {
        Text("VENCIMIENTO", fontSize = 10.sp, color = TextSecondary)
        Text(fecha.ifBlank { "Pendiente" }, color = TextPrimary)
      }
    }
    if (bajoStock) Text(if (cantidad == 0.0) "Agotado · considera reponerlo" else "Bajo stock · considera reponerlo",
      color = StatusWarn, fontWeight = FontWeight.Medium)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      OutlinedButton(onClick = consumir, enabled = cantidad > 0.0, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
        Text("Consumir", color = if (cantidad > 0) TextPrimary else TextSecondary)
      }
      OutlinedButton(onClick = ajustar, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
        Text("Ajustar cantidad", color = TextPrimary)
      }
    }
    if (bajoStock) OutlinedButton(onClick = agregarCompra, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
      Text("Agregar a compras", color = AccentSage)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      TextButton(onClick = editar) { Text("Editar datos", color = TextPrimary) }
      TextButton(onClick = eliminar) { Text("Eliminar", color = StatusDanger) }
    }
  }
}

fun calcularEstadoCaducidad(fechaCaducidad: String): String {
  if (fechaCaducidad.isBlank()) return "Caducidad pendiente"
  val dias = FechasCaducidad.diasRestantes(fechaCaducidad) ?: return "Fecha inválida"
  return when {
    dias < 0 -> "Vencido"
    dias <= 7 -> "Próximo a caducar"
    else -> "Seguro"
  }
}

// ----------------------------------------------------
// AGREGAR PRODUCTO
// ----------------------------------------------------

@Composable
fun PantallaAgregarProducto(regresar: () -> Unit, productoGuardado: () -> Unit) {
  var nombre by remember { mutableStateOf("") }
  var categoria by remember { mutableStateOf("Alimentos") }
  var marca by remember { mutableStateOf("") }
  var codigoBarras by remember { mutableStateOf("") }
  var cantidad by remember { mutableStateOf("1") }
  var unidad by remember { mutableStateOf("pzas") }
  var fechaCaducidad by remember { mutableStateOf("") }
  var cargando by remember { mutableStateOf(false) }

  val context = LocalContext.current
  val auth = FirebaseAuth.getInstance()
  val db = FirebaseFirestore.getInstance()
  val calendario = Calendar.getInstance()

  val selectorFecha = DatePickerDialog(
    context,
    { _, year, month, day ->
      val fechaSeleccionada = Calendar.getInstance()
      fechaSeleccionada.set(year, month, day)
      fechaCaducidad = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(fechaSeleccionada.time)
    },
    calendario.get(Calendar.YEAR),
    calendario.get(Calendar.MONTH),
    calendario.get(Calendar.DAY_OF_MONTH)
  )

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(BgPrimary)
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 24.dp)
      .padding(top = 56.dp, bottom = 40.dp)
  ) {
    CabeceraNavegacion(titulo = "Nuevo producto", onRegresar = regresar)

    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      color = SurfaceCard,
      border = BorderStroke(1.dp, BorderSubtle)
    ) {
      Column(modifier = Modifier.padding(18.dp)) {
        Text(
          text = "CAPTURA ASISTIDA",
          fontSize = 11.sp,
          fontWeight = FontWeight.SemiBold,
          color = TextSecondary,
          letterSpacing = 0.8.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        EscanerCodigoProducto { codigo, datos ->
          codigoBarras = codigo
          if (datos != null) {
            if (datos.nombre.isNotBlank()) nombre = datos.nombre
            marca = datos.marca
            categoria = datos.categoria.ifBlank { "Alimentos" }
            if (datos.cantidad.isNotBlank()) {
              cantidad = datos.cantidad
              unidad = datos.unidad.ifBlank { "pzas" }
            }
          }
        }
        Spacer(modifier = Modifier.height(8.dp))
        CapturaOCRProducto { fecha, cantidadDetectada, unidadDetectada ->
          if (fecha.isNotBlank()) fechaCaducidad = fecha
          if (cantidadDetectada.isNotBlank()) {
            cantidad = cantidadDetectada
            unidad = unidadDetectada.ifBlank { unidad }
          }
          Toast.makeText(context, "Datos aplicados. Revisa y guarda.", Toast.LENGTH_LONG).show()
        }
      }
    }

    Spacer(modifier = Modifier.height(18.dp))

    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      color = SurfaceCard,
      border = BorderStroke(1.dp, BorderSubtle)
    ) {
      Column(modifier = Modifier.padding(20.dp)) {
        CampoTextoMinimalista(
          value = nombre,
          onValueChange = { nombre = it },
          label = "Nombre del producto",
          placeholder = "Ej. Leche entera"
        )
        Spacer(modifier = Modifier.height(14.dp))
        CampoTextoMinimalista(
          value = categoria,
          onValueChange = { categoria = it },
          label = "Categoría",
          placeholder = "Ej. Lácteos"
        )
        Spacer(modifier = Modifier.height(14.dp))
        CampoTextoMinimalista(
          value = marca,
          onValueChange = { marca = it },
          label = "Marca (opcional)",
          placeholder = "Ej. Alpura"
        )
        Spacer(modifier = Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          Box(modifier = Modifier.weight(1f)) {
            CampoTextoMinimalista(
              value = cantidad,
              onValueChange = { nuevoValor ->
                val limpio = nuevoValor.replace(',', '.')
                if (limpio.matches(Regex("""\d*(?:\.\d{0,3})?"""))) cantidad = limpio
              },
              label = "Cantidad",
              keyboardType = KeyboardType.Decimal
            )
          }
          Box(modifier = Modifier.weight(1f)) {
            CampoTextoMinimalista(
              value = unidad,
              onValueChange = { unidad = it },
              label = "Unidad",
              placeholder = "pzas, g, ml"
            )
          }
        }
        Spacer(modifier = Modifier.height(14.dp))
        CampoTextoMinimalista(
          value = fechaCaducidad,
          onValueChange = {},
          readOnly = true,
          label = "Fecha de caducidad",
          placeholder = "Sin fecha seleccionada"
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedButton(
            onClick = { selectorFecha.show() },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, BorderSubtle)
          ) {
            Text(text = "Elegir fecha", color = TextPrimary, fontSize = 13.sp)
          }
          if (fechaCaducidad.isNotBlank()) {
            TextButton(onClick = { fechaCaducidad = "" }) {
              Text("Limpiar", color = TextSecondary, fontSize = 13.sp)
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(22.dp))

    Button(
      onClick = {
        val usuario = auth.currentUser
        when {
          usuario == null -> Toast.makeText(context, "No hay una sesión iniciada", Toast.LENGTH_SHORT).show()
          nombre.isBlank() || nombre.trim().length > 150 -> Toast.makeText(context, "Escribe un nombre de hasta 150 caracteres", Toast.LENGTH_SHORT).show()
          marca.trim().length > 150 -> Toast.makeText(context, "La marca admite hasta 150 caracteres", Toast.LENGTH_SHORT).show()
          categoria.isBlank() || categoria.trim().length > 100 -> Toast.makeText(context, "Escribe una categoría de hasta 100 caracteres", Toast.LENGTH_SHORT).show()
          cantidad.isBlank() -> Toast.makeText(context, "Escribe la cantidad", Toast.LENGTH_SHORT).show()
          ControlCantidades.numero(cantidad)?.let { it > 0.0 } != true ->
            Toast.makeText(context, "La cantidad debe ser mayor que 0 y hasta 1000000", Toast.LENGTH_SHORT).show()
          unidad.isBlank() || unidad.trim().length > 30 -> Toast.makeText(context, "Escribe una unidad de hasta 30 caracteres", Toast.LENGTH_SHORT).show()
          fechaCaducidad.isNotBlank() && FechasCaducidad.diasRestantes(fechaCaducidad) == null ->
            Toast.makeText(context, "Fecha inválida.", Toast.LENGTH_SHORT).show()
          else -> {
            cargando = true
            val producto = hashMapOf(
              "nombre" to nombre.trim(),
              "categoria" to categoria.trim(),
              "marca" to marca.trim(),
              "codigoBarras" to codigoBarras,
              "cantidad" to cantidad.toDouble(),
              "cantidadInicial" to cantidad.toDouble(),
              "unidad" to unidad.trim(),
              "fechaCaducidad" to fechaCaducidad,
              "caducidadPendiente" to fechaCaducidad.isBlank(),
              "usuarioId" to usuario.uid,
              "correoUsuario" to (usuario.email ?: ""),
              "fechaCreacion" to Timestamp.now(),
              "ultimaConfirmacion" to Timestamp.now(),
              "activo" to true
            )
            db.collection("productos").add(producto)
              .addOnSuccessListener {
                if (fechaCaducidad.isNotBlank()) {
                  RevisionManualCaducidad.revisar(context, usuario.uid)
                }
                cargando = false
                Toast.makeText(context, "Producto guardado", Toast.LENGTH_SHORT).show()
                productoGuardado()
              }
              .addOnFailureListener { err ->
                cargando = false
                Toast.makeText(context, "Error: ${err.localizedMessage}", Toast.LENGTH_LONG).show()
              }
          }
        }
      },
      enabled = !cargando,
      modifier = Modifier
        .fillMaxWidth()
        .height(52.dp),
      colors = ButtonDefaults.buttonColors(containerColor = AccentDark, contentColor = Color.White),
      shape = RoundedCornerShape(12.dp)
    ) {
      if (cargando) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
      } else {
        Text(text = "Guardar en inventario", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
      }
    }
  }
}

// ----------------------------------------------------
// EDITAR PRODUCTO
// ----------------------------------------------------

@Composable
fun PantallaEditarProducto(
  producto: ProductoMapa,
  regresar: () -> Unit,
  productoActualizado: () -> Unit
) {
  val context = LocalContext.current
  val db = FirebaseFirestore.getInstance()
  val usuario = FirebaseAuth.getInstance().currentUser
  val idProducto = producto["id"]?.toString().orEmpty()

  var nombre by remember(producto) { mutableStateOf(producto["nombre"]?.toString().orEmpty()) }
  var categoria by remember(producto) { mutableStateOf(producto["categoria"]?.toString().orEmpty()) }
  var cantidad by remember(producto) { mutableStateOf(producto["cantidad"]?.toString().orEmpty()) }
  var unidad by remember(producto) { mutableStateOf(producto["unidad"]?.toString() ?: "pzas") }
  var marca by remember(producto) { mutableStateOf(producto["marca"]?.toString().orEmpty()) }
  var codigoBarras by remember(producto) { mutableStateOf(producto["codigoBarras"]?.toString().orEmpty()) }
  var fechaCaducidad by remember(producto) { mutableStateOf(producto["fechaCaducidad"]?.toString().orEmpty()) }
  var cargando by remember { mutableStateOf(false) }

  val calendario = Calendar.getInstance()
  try {
    val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    formato.isLenient = false
    formato.parse(fechaCaducidad)?.let { calendario.time = it }
  } catch (_: Exception) {
  }

  val selectorFecha = DatePickerDialog(
    context,
    { _, year, month, day ->
      val fechaSeleccionada = Calendar.getInstance()
      fechaSeleccionada.set(year, month, day)
      fechaCaducidad = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(fechaSeleccionada.time)
    },
    calendario.get(Calendar.YEAR),
    calendario.get(Calendar.MONTH),
    calendario.get(Calendar.DAY_OF_MONTH)
  )

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(BgPrimary)
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 24.dp)
      .padding(top = 56.dp, bottom = 40.dp)
  ) {
    CabeceraNavegacion(titulo = "Editar producto", onRegresar = regresar)

    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      color = SurfaceCard,
      border = BorderStroke(1.dp, BorderSubtle)
    ) {
      Column(modifier = Modifier.padding(20.dp)) {
        CampoTextoMinimalista(
          value = nombre,
          onValueChange = { nombre = it },
          label = "Nombre del producto"
        )
        Spacer(modifier = Modifier.height(14.dp))
        CampoTextoMinimalista(
          value = categoria,
          onValueChange = { categoria = it },
          label = "Categoría"
        )
        Spacer(modifier = Modifier.height(14.dp))
        CampoTextoMinimalista(
          value = marca,
          onValueChange = { marca = it },
          label = "Marca (opcional)"
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text("Cantidad restante: $cantidad $unidad", color = TextPrimary, fontWeight = FontWeight.Medium)
        Text("Usa «Consumir» o «Ajustar cantidad» en tu despensa para cambiar el saldo. Para otra unidad o una reposición, registra un nuevo producto.",
          color = TextSecondary, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(14.dp))
        CampoTextoMinimalista(
          value = fechaCaducidad,
          onValueChange = {},
          readOnly = true,
          label = "Fecha de caducidad",
          placeholder = "Pendiente"
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedButton(
            onClick = { selectorFecha.show() },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, BorderSubtle)
          ) {
            Text(text = "Cambiar fecha", color = TextPrimary, fontSize = 13.sp)
          }
          if (fechaCaducidad.isNotBlank()) {
            TextButton(onClick = { fechaCaducidad = "" }) {
              Text("Dejar pendiente", color = TextSecondary, fontSize = 13.sp)
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(22.dp))

    Button(
      onClick = {
        when {
          usuario == null -> Toast.makeText(context, "No hay una sesión iniciada", Toast.LENGTH_SHORT).show()
          idProducto.isBlank() -> Toast.makeText(context, "No se encontró el producto", Toast.LENGTH_SHORT).show()
          nombre.isBlank() || nombre.trim().length > 150 -> Toast.makeText(context, "Escribe un nombre de hasta 150 caracteres", Toast.LENGTH_SHORT).show()
          marca.trim().length > 150 -> Toast.makeText(context, "La marca admite hasta 150 caracteres", Toast.LENGTH_SHORT).show()
          categoria.isBlank() || categoria.trim().length > 100 -> Toast.makeText(context, "Escribe una categoría de hasta 100 caracteres", Toast.LENGTH_SHORT).show()
          unidad.isBlank() || unidad.trim().length > 30 -> Toast.makeText(context, "Escribe una unidad de hasta 30 caracteres", Toast.LENGTH_SHORT).show()
          fechaCaducidad.isNotBlank() && FechasCaducidad.diasRestantes(fechaCaducidad) == null ->
            Toast.makeText(context, "Fecha inválida.", Toast.LENGTH_SHORT).show()
          else -> {
            cargando = true
            val cambios = mapOf(
              "nombre" to nombre.trim(),
              "categoria" to categoria.trim(),
              "marca" to marca.trim(),
              "codigoBarras" to codigoBarras,
              "fechaCaducidad" to fechaCaducidad,
              "caducidadPendiente" to fechaCaducidad.isBlank(),
              "usuarioId" to usuario.uid,
              "correoUsuario" to (usuario.email ?: "")
            )
            db.collection("productos").document(idProducto).update(cambios)
              .addOnSuccessListener {
                NotificationHelper.cancelarAlerta(context, usuario.uid, idProducto)
                if (fechaCaducidad.isNotBlank()) {
                  RevisionManualCaducidad.revisar(context, usuario.uid)
                }
                cargando = false
                Toast.makeText(context, "Cambios guardados", Toast.LENGTH_SHORT).show()
                productoActualizado()
              }
              .addOnFailureListener { err ->
                cargando = false
                Toast.makeText(context, "Error: ${err.localizedMessage}", Toast.LENGTH_LONG).show()
              }
          }
        }
      },
      enabled = !cargando,
      modifier = Modifier
        .fillMaxWidth()
        .height(52.dp),
      colors = ButtonDefaults.buttonColors(containerColor = AccentDark, contentColor = Color.White),
      shape = RoundedCornerShape(12.dp)
    ) {
      if (cargando) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
      } else {
        Text(text = "Guardar cambios", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
      }
    }
  }
}

// ----------------------------------------------------
// CONFIRMACIÓN DESDE NOTIFICACIÓN
// ----------------------------------------------------

@Composable
fun DialogoConfirmacionCaducidad(
  uid: String,
  productoId: String,
  cerrar: () -> Unit
) {
  val context = LocalContext.current
  val db = remember { FirebaseFirestore.getInstance() }
  var producto by remember(uid, productoId) { mutableStateOf(mapaVacio()) }
  var error by remember(uid, productoId) { mutableStateOf("") }
  var cargando by remember(uid, productoId) { mutableStateOf(true) }
  var guardando by remember(uid, productoId) { mutableStateOf(false) }
  var intentoLectura by remember(uid, productoId) { mutableIntStateOf(0) }

  LaunchedEffect(uid, productoId, intentoLectura) {
    db.collection("productos").document(productoId).get()
      .addOnSuccessListener { doc ->
        val datos = doc.data
        if (FirebaseAuth.getInstance().currentUser?.uid != uid ||
          !doc.exists() ||
          datos == null ||
          datos["usuarioId"] != uid ||
          datos["activo"] == false
        ) {
          error = "El producto ya no está disponible en tu inventario."
        } else {
          val confirmacionHoy = doc.getTimestamp("ultimaConfirmacion")
            ?.toDate()?.let { FechasCaducidad.esHoy(it) } == true
          if (confirmacionHoy) {
            AvisosAtendidos.marcarAtendido(context, uid, productoId)
            cerrar()
            return@addOnSuccessListener
          }
          producto = datos
          error = ""
        }
        cargando = false
      }
      .addOnFailureListener { excepcion ->
        error = if (
          excepcion is FirebaseFirestoreException &&
          excepcion.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
        ) {
          "Firestore rechazó el acceso al producto (PERMISSION_DENIED)."
        } else {
          "No fue posible consultar el producto: ${excepcion.localizedMessage}"
        }
        cargando = false
      }
  }

  fun confirmar(conservaProducto: Boolean) {
    if (guardando || FirebaseAuth.getInstance().currentUser?.uid != uid) return
    guardando = true
    val cambios = if (conservaProducto) {
      mapOf("ultimaConfirmacion" to Timestamp.now())
    } else {
      mapOf(
        "ultimaConfirmacion" to Timestamp.now(),
        "activo" to false,
        "cantidad" to 0
      )
    }
    db.collection("productos").document(productoId).update(cambios)
      .addOnSuccessListener {
        AvisosAtendidos.marcarAtendido(context, uid, productoId)
        NotificationHelper.cancelarAlerta(context, uid, productoId)
        Toast.makeText(
          context,
          if (conservaProducto) "Producto confirmado" else "Producto archivado",
          Toast.LENGTH_SHORT
        ).show()
        cerrar()
      }
      .addOnFailureListener {
        guardando = false
        error = "Error al guardar: ${it.localizedMessage}"
      }
  }

  AlertDialog(
    onDismissRequest = { if (!guardando) cerrar() },
    containerColor = SurfaceCard,
    title = { Text("¿Todavía tienes este producto?", fontWeight = FontWeight.Bold, color = TextPrimary) },
    text = {
      Column {
        if (cargando) {
          CircularProgressIndicator(color = TextPrimary, strokeWidth = 2.dp)
        } else if (producto.isNotEmpty()) {
          Text(
            text = "${producto["nombre"] ?: "Producto"} · Vence: ${producto["fechaCaducidad"] ?: "Sin fecha"}",
            fontWeight = FontWeight.Medium,
            color = TextPrimary
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Cantidad estimada: ${producto["cantidad"] ?: "0"} ${producto["unidad"] ?: "pzas"}. Al conservarlo confirmas esta cantidad. También puedes corregirla desde tu despensa.",
            color = TextSecondary,
            fontSize = 14.sp
          )
        }
        if (error.isNotBlank()) Text(error, color = StatusDanger, fontSize = 13.sp)
      }
    },
    confirmButton = {
      TextButton(
        enabled = !cargando && !guardando && producto.isNotEmpty(),
        onClick = { confirmar(true) }
      ) { Text("Sí, lo conservo", color = TextPrimary, fontWeight = FontWeight.SemiBold) }
    },
    dismissButton = {
      Column {
        if (error.isNotBlank()) {
          TextButton(
            enabled = !cargando && !guardando,
            onClick = {
              producto = mapaVacio()
              error = ""
              cargando = true
              intentoLectura++
            }
          ) { Text("Reintentar", color = TextPrimary) }
        }
        TextButton(
          enabled = !cargando && !guardando && producto.isNotEmpty(),
          onClick = { confirmar(false) }
        ) { Text("Ya no lo tengo", color = StatusDanger) }
        TextButton(enabled = !guardando, onClick = cerrar) {
          Text("Más tarde", color = TextSecondary)
        }
      }
    }
  )
}

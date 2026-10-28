package com.example.expiryalert

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts

import android.app.DatePickerDialog
import android.os.Bundle
import android.content.Intent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expiryalert.ui.theme.ExpiryAlertTheme
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private val avisoPendiente = mutableStateOf<Pair<String, String>?>(null)

    private fun leerAviso(intent: Intent?) {
        val uid = intent?.getStringExtra(NotificationHelper.EXTRA_USUARIO_ID)
        val id = intent?.getStringExtra(NotificationHelper.EXTRA_PRODUCTO_ID)
        if (!uid.isNullOrBlank() && !id.isNullOrBlank()) {
            avisoPendiente.value = uid to id
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        leerAviso(intent)
    }

    private val solicitarPermisoNotificaciones =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { concedido ->

            if (concedido) {
                Toast.makeText(
                    this,
                    "Notificaciones activadas",
                    Toast.LENGTH_SHORT
                ).show()

                NotificationHelper.mostrarNotificacionPrueba(this)
            } else {
                Toast.makeText(
                    this,
                    "Permiso de notificaciones no concedido",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        NotificationHelper.crearCanalNotificaciones(this)
        pedirPermisoNotificaciones()
        leerAviso(intent)
        FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
            ProgramadorAlertas.activar(this, uid)
        }

        setContent {
            ExpiryAlertTheme {
                ExpiryAlertApp(
                    aviso = avisoPendiente.value,
                    avisoAtendido = { avisoPendiente.value = null },
                    activarAlertas = { uid -> ProgramadorAlertas.activar(this, uid) },
                    cerrarAlertas = { uid -> ProgramadorAlertas.desactivar(this, uid) }
                )
            }
        }
    }

    private fun pedirPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                solicitarPermisoNotificaciones.launch(
                    Manifest.permission.POST_NOTIFICATIONS
                )
            } else {
                NotificationHelper.mostrarNotificacionPrueba(this)
            }
        } else {
            NotificationHelper.mostrarNotificacionPrueba(this)
        }
    }
}

@Composable
fun ExpiryAlertApp(
    aviso: Pair<String, String>?,
    avisoAtendido: () -> Unit,
    activarAlertas: (String) -> Unit,
    cerrarAlertas: (String) -> Unit
) {

    var pantalla by remember {
        mutableStateOf(
            if (FirebaseAuth.getInstance().currentUser != null) {
                "principal"
            } else {
                "inicio"
            }
        )
    }

    var productoEditar by remember {
        mutableStateOf<Map<String, Any>?>(null)
    }
    val contextoApp = LocalContext.current

    LaunchedEffect(pantalla) {
        if (pantalla == "principal") {
            FirebaseAuth.getInstance().currentUser?.uid?.let(activarAlertas)
        }
    }

    aviso?.let { (uid, id) ->
        if (FirebaseAuth.getInstance().currentUser?.uid == uid) {
            DialogoConfirmacionCaducidad(uid, id, avisoAtendido)
        } else {
            LaunchedEffect(uid, id) { avisoAtendido() }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF7F5FA)
    ) {

        when (pantalla) {

            "inicio" -> PantallaInicio(
                irLogin = {
                    pantalla = "login"
                },
                irRegistro = {
                    pantalla = "registro"
                }
            )

            "login" -> PantallaLogin(
                regresar = {
                    pantalla = "inicio"
                },
                irRegistro = {
                    pantalla = "registro"
                },
                loginExitoso = {
                    pantalla = "principal"
                }
            )

            "registro" -> PantallaRegistro(
                regresar = {
                    pantalla = "inicio"
                },
                irLogin = {
                    pantalla = "login"
                },
                registroExitoso = {
                    pantalla = "principal"
                }
            )

            "principal" -> PantallaPrincipal(
                agregarProducto = {
                    pantalla = "agregarProducto"
                },
                editarProducto = { producto ->
                    productoEditar = producto
                    pantalla = "editarProducto"
                },
                cerrarSesion = {
                    FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
                        cerrarAlertas(uid)
                        NotificationHelper.cancelarAlertasDeUsuario(contextoApp, uid)
                    }
                    FirebaseAuth.getInstance().signOut()
                    productoEditar = null
                    avisoAtendido()
                    pantalla = "inicio"
                }
            )

            "agregarProducto" -> PantallaAgregarProducto(
                regresar = {
                    pantalla = "principal"
                },
                productoGuardado = {
                    pantalla = "principal"
                }
            )

            "editarProducto" -> {

                val producto = productoEditar

                if (producto != null) {

                    PantallaEditarProducto(
                        producto = producto,
                        regresar = {
                            productoEditar = null
                            pantalla = "principal"
                        },
                        productoActualizado = {
                            productoEditar = null
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
fun PantallaInicio(
    irLogin: () -> Unit,
    irRegistro: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F5FA))
            .padding(horizontal = 24.dp)
            .padding(top = 90.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "ExpiryAlert",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF6750A4)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Controla las fechas de caducidad\nde tus productos",
            fontSize = 18.sp,
            color = Color.DarkGray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(50.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Color.White,
                    RoundedCornerShape(20.dp)
                )
                .padding(24.dp)
        ) {

            Column {

                Text(
                    text = "Bienvenido",
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Registra tus productos y recibe alertas antes de que caduquen.",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = irLogin,
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF6750A4),
                contentColor = Color.White
            )
        ) {

            Text(
                text = "Iniciar sesión",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(15.dp))

        Button(
            onClick = irRegistro,
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF9A82DB),
                contentColor = Color.White
            )
        ) {

            Text(
                text = "Crear cuenta",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(35.dp))

        Text(
            text = "Nunca olvides una fecha de caducidad",
            fontSize = 14.sp,
            color = Color.Gray
        )
    }
}

// ----------------------------------------------------
// INICIAR SESIÓN
// ----------------------------------------------------

@Composable
fun PantallaLogin(
    regresar: () -> Unit,
    irRegistro: () -> Unit,
    loginExitoso: () -> Unit
) {

    var correo by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F5FA))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 70.dp, bottom = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "ExpiryAlert",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF6750A4)
        )

        Spacer(modifier = Modifier.height(35.dp))

        Text(
            text = "Iniciar sesión",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Ingresa a tu cuenta para continuar",
            fontSize = 16.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(35.dp))

        OutlinedTextField(
            value = correo,
            onValueChange = { correo = it },
            label = {
                Text("Correo electrónico")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email
            )
        )

        Spacer(modifier = Modifier.height(15.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = {
                Text("Contraseña")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            )
        )

        Spacer(modifier = Modifier.height(30.dp))

        Button(
            onClick = {

                if (correo.isBlank() || password.isBlank()) {

                    Toast.makeText(
                        context,
                        "Completa todos los campos",
                        Toast.LENGTH_SHORT
                    ).show()

                } else {

                    cargando = true

                    auth.signInWithEmailAndPassword(
                        correo.trim(),
                        password
                    ).addOnCompleteListener { task ->

                        cargando = false

                        if (task.isSuccessful) {

                            Toast.makeText(
                                context,
                                "Inicio de sesión correcto",
                                Toast.LENGTH_SHORT
                            ).show()

                            loginExitoso()

                        } else {

                            Toast.makeText(
                                context,
                                "No se pudo iniciar sesión: ${task.exception?.localizedMessage}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            },
            enabled = !cargando,
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF6750A4),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(15.dp)
        ) {

            if (cargando) {

                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )

            } else {

                Text(
                    text = "Entrar",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(15.dp))

        TextButton(
            onClick = irRegistro
        ) {

            Text(
                text = "¿No tienes cuenta? Crear cuenta",
                color = Color(0xFF6750A4)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        TextButton(
            onClick = regresar
        ) {

            Text(
                text = "← Volver",
                color = Color.Gray
            )
        }
    }
}

// ----------------------------------------------------
// CREAR CUENTA
// ----------------------------------------------------

@Composable
fun PantallaRegistro(
    regresar: () -> Unit,
    irLogin: () -> Unit,
    registroExitoso: () -> Unit
) {

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
            .background(Color(0xFFF7F5FA))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 55.dp, bottom = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "ExpiryAlert",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF6750A4)
        )

        Spacer(modifier = Modifier.height(25.dp))

        Text(
            text = "Crear cuenta",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Crea tu cuenta para comenzar",
            color = Color.Gray,
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(25.dp))

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = correo,
            onValueChange = { correo = it },
            label = { Text("Correo electrónico") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = confirmarPassword,
            onValueChange = { confirmarPassword = it },
            label = { Text("Confirmar contraseña") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            )
        )

        Spacer(modifier = Modifier.height(25.dp))

        Button(
            onClick = {

                when {

                    nombre.isBlank() ||
                            correo.isBlank() ||
                            password.isBlank() ||
                            confirmarPassword.isBlank() -> {

                        Toast.makeText(
                            context,
                            "Completa todos los campos",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    password != confirmarPassword -> {

                        Toast.makeText(
                            context,
                            "Las contraseñas no coinciden",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    password.length < 6 -> {

                        Toast.makeText(
                            context,
                            "La contraseña debe tener al menos 6 caracteres",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    else -> {

                        cargando = true

                        auth.createUserWithEmailAndPassword(
                            correo.trim(),
                            password
                        ).addOnCompleteListener { task ->

                            cargando = false

                            if (task.isSuccessful) {

                                Toast.makeText(
                                    context,
                                    "Cuenta creada correctamente",
                                    Toast.LENGTH_SHORT
                                ).show()

                                registroExitoso()

                            } else {

                                Toast.makeText(
                                    context,
                                    "Error al crear cuenta: ${task.exception?.localizedMessage}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }
            },
            enabled = !cargando,
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF6750A4),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(15.dp)
        ) {

            if (cargando) {

                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )

            } else {

                Text(
                    text = "Registrarme",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        TextButton(
            onClick = irLogin
        ) {
            Text(
                text = "¿Ya tienes cuenta? Iniciar sesión",
                color = Color(0xFF6750A4)
            )
        }

        TextButton(
            onClick = regresar
        ) {
            Text(
                text = "← Volver",
                color = Color.Gray
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
    editarProducto: (Map<String, Any>) -> Unit,
    cerrarSesion: () -> Unit
) {

    val usuario = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    var productos by remember {
        mutableStateOf<List<Map<String, Any>>>(emptyList())
    }

    var cargando by remember { mutableStateOf(true) }
    var errorCarga by remember { mutableStateOf<String?>(null) }

    var productoEliminar by remember {
        mutableStateOf<Map<String, Any>?>(null)
    }

    var revisandoCaducidades by remember { mutableStateOf(false) }
    var resultadoRevision by remember { mutableStateOf<String?>(null) }

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
                        errorCarga = error.localizedMessage
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {

                        productos = snapshot.documents.mapNotNull { documento ->
                            // Solo productos activos y siempre con su ID real de Firestore.
                            // Antes se devolvían también los archivados, pero sin ID.
                            InventarioProductos.productoVisible(documento.id, documento.data)
                        }

                        cargando = false
                        errorCarga = null
                    }
                }

            onDispose {
                listener.remove()
            }
        }
    }

    // ------------------------------------------------
    // CONFIRMACIÓN PARA ELIMINAR
    // ------------------------------------------------

    productoEliminar?.let { producto ->

        AlertDialog(
            onDismissRequest = {
                productoEliminar = null
            },

            title = {
                Text(
                    text = "Eliminar producto",
                    fontWeight = FontWeight.Bold
                )
            },

            text = {
                Text(
                    text = "¿Seguro que deseas eliminar \"${producto["nombre"]}\"?\n\nEsta acción no se puede deshacer."
                )
            },

            confirmButton = {

                Button(
                    onClick = {

                        val id = producto["id"]?.toString()

                        if (id.isNullOrBlank()) {

                            Toast.makeText(
                                context,
                                "No se encontró el ID del producto",
                                Toast.LENGTH_SHORT
                            ).show()

                            productoEliminar = null

                        } else {

                            db.collection("productos")
                                .document(id)
                                .delete()
                                .addOnSuccessListener {

                                    FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
                                        NotificationHelper.cancelarAlerta(context, uid, id)
                                    }
                                    Toast.makeText(
                                        context,
                                        "Producto eliminado",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    productoEliminar = null
                                }
                                .addOnFailureListener { error ->

                                    Toast.makeText(
                                        context,
                                        "Error al eliminar: ${error.localizedMessage}",
                                        Toast.LENGTH_LONG
                                    ).show()

                                    productoEliminar = null
                                }
                        }
                    },

                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD32F2F),
                        contentColor = Color.White
                    )
                ) {

                    Text(
                        text = "Eliminar",
                        fontWeight = FontWeight.Bold
                    )
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        productoEliminar = null
                    }
                ) {

                    Text(
                        text = "Cancelar",
                        color = Color.Gray
                    )
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F5FA))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 80.dp, bottom = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "ExpiryAlert",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF6750A4)
        )

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = "¡Bienvenido!",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = usuario?.email ?: "",
            fontSize = 16.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(35.dp))

        Text(
            text = "Mis productos",
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(15.dp))

        when {

            cargando -> {

                Spacer(modifier = Modifier.height(25.dp))

                CircularProgressIndicator(
                    color = Color(0xFF6750A4)
                )

                Spacer(modifier = Modifier.height(25.dp))
            }

            errorCarga != null -> {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color.White,
                            RoundedCornerShape(20.dp)
                        )
                        .padding(24.dp)
                ) {

                    Column {

                        Text(
                            text = "No se pudieron cargar los productos",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = errorCarga ?: "",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            productos.isEmpty() -> {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color.White,
                            RoundedCornerShape(20.dp)
                        )
                        .padding(24.dp)
                ) {

                    Column {

                        Text(
                            text = "Todavía no tienes productos",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Presiona “Agregar producto” para registrar el primero.",
                            fontSize = 16.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            else -> {

                productos.forEach { producto ->

                    TarjetaProducto(
                        producto = producto,

                        editar = {
                            editarProducto(producto)
                        },

                        eliminar = {
                            productoEliminar = producto
                        }
                    )

                    Spacer(modifier = Modifier.height(15.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedButton(
            onClick = {
                usuario?.uid?.let { uid ->
                    revisandoCaducidades = true
                    resultadoRevision = null
                    RevisionManualCaducidad.revisar(context, uid) { mensaje ->
                        revisandoCaducidades = false
                        resultadoRevision = mensaje
                    }
                }
            },
            enabled = !revisandoCaducidades,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (revisandoCaducidades) "Revisando productos…" else "🔔 Revisar caducidades ahora")
        }
        if (revisandoCaducidades) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        resultadoRevision?.let { resultado ->
            Text(
                text = resultado,
                color = Color.DarkGray,
                fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = agregarProducto,
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF6750A4),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(15.dp)
        ) {

            Text(
                text = "+ Agregar producto",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        TextButton(
            onClick = cerrarSesion
        ) {

            Text(
                text = "Cerrar sesión",
                color = Color.Gray
            )
        }
    }
}

// ----------------------------------------------------
// TARJETA DEL PRODUCTO
// ----------------------------------------------------

@Composable
fun TarjetaProducto(
    producto: Map<String, Any>,
    editar: () -> Unit,
    eliminar: () -> Unit
) {

    val nombre =
        producto["nombre"]?.toString() ?: "Sin nombre"

    val categoria =
        producto["categoria"]?.toString() ?: "Sin categoría"

    val cantidad = (producto["cantidad"] as? Number)?.let {
        if (it.toDouble() % 1.0 == 0.0) it.toLong().toString() else it.toString()
    } ?: producto["cantidad"]?.toString() ?: "0"
    val unidadMostrada = producto["unidad"]?.toString() ?: "pzas"

    val fechaCaducidad =
        producto["fechaCaducidad"]?.toString() ?: ""

    val estado =
        calcularEstadoCaducidad(fechaCaducidad)
    val marcaMostrada = producto["marca"]?.toString().orEmpty()

    val colorEstado = when (estado) {

        "Vencido" ->
            Color(0xFFD32F2F)

        "Próximo a caducar" ->
            Color(0xFFF57C00)

        "Caducidad pendiente", "Fecha inválida" -> Color.Gray

        else ->
            Color(0xFF2E7D32)
    }

    val emojiEstado = when (estado) {

        "Vencido" -> "🔴"

        "Próximo a caducar" -> "🟡"

        "Caducidad pendiente", "Fecha inválida" -> "⚪"

        else -> "🟢"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {

        Column(
            modifier = Modifier.padding(22.dp)
        ) {

            Text(
                text = nombre,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Categoría: $categoria",
                fontSize = 16.sp,
                color = Color.Gray
            )
            if (marcaMostrada.isNotBlank()) {
                Text(
                    text = "Marca: $marcaMostrada",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Cantidad: $cantidad $unidadMostrada",
                fontSize = 16.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (fechaCaducidad.isBlank()) "Caducidad: pendiente" else "Caduca: $fechaCaducidad",
                fontSize = 16.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(15.dp))

            Text(
                text = "$emojiEstado $estado",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = colorEstado
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                OutlinedButton(
                    onClick = editar,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {

                    Text(
                        text = "✏️ Editar",
                        color = Color(0xFF6750A4),
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = eliminar,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {

                    Text(
                        text = "🗑️ Eliminar",
                        color = Color(0xFFD32F2F),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------
// CALCULAR ESTADO
// ----------------------------------------------------

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
fun PantallaAgregarProducto(
    regresar: () -> Unit,
    productoGuardado: () -> Unit
) {

    var nombre by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf("Sin categoría") }
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

            val fechaSeleccionada =
                Calendar.getInstance()

            fechaSeleccionada.set(
                year,
                month,
                day
            )

            val formato =
                SimpleDateFormat(
                    "dd/MM/yyyy",
                    Locale.getDefault()
                )

            fechaCaducidad =
                formato.format(
                    fechaSeleccionada.time
                )
        },
        calendario.get(Calendar.YEAR),
        calendario.get(Calendar.MONTH),
        calendario.get(Calendar.DAY_OF_MONTH)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F5FA))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 60.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "ExpiryAlert",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF6750A4)
        )

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = "Agregar producto",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Escanea el producto o regístralo manualmente",
            fontSize = 16.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(18.dp))

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

        Text(
            "Catálogo de productos: Open Food Facts (licencia ODbL). " +
                "Verifica nombre, presentación y cantidad antes de guardar.",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))

        CapturaOCRProducto { fecha, cantidadDetectada, unidadDetectada ->
            if (fecha.isNotBlank()) fechaCaducidad = fecha
            if (cantidadDetectada.isNotBlank()) {
                cantidad = cantidadDetectada
                unidad = unidadDetectada.ifBlank { unidad }
            }
            Toast.makeText(context, "Datos de etiqueta aplicados. Revisa y guarda.", Toast.LENGTH_LONG).show()
        }

        Spacer(modifier = Modifier.height(30.dp))

        OutlinedTextField(
            value = nombre,
            onValueChange = {
                nombre = it
            },
            label = {
                Text("Nombre del producto")
            },
            placeholder = {
                Text("Ejemplo: Leche")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(15.dp))

        OutlinedTextField(
            value = categoria,
            onValueChange = {
                categoria = it
            },
            label = {
                Text("Categoría")
            },
            placeholder = {
                Text("Ejemplo: Lácteos")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(15.dp))

        OutlinedTextField(
            value = marca,
            onValueChange = { marca = it },
            label = { Text("Marca (opcional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(15.dp))

        OutlinedTextField(
            value = cantidad,
            onValueChange = { nuevoValor ->

                val limpio = nuevoValor.replace(',', '.')
                if (limpio.matches(Regex("""\d*(?:\.\d{0,3})?"""))) {
                    cantidad = limpio
                }
            },
            label = {
                Text("Cantidad")
            },
            placeholder = {
                Text("Ejemplo: 2")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal
            )
        )

        Spacer(modifier = Modifier.height(15.dp))

        OutlinedTextField(
            value = unidad,
            onValueChange = { unidad = it },
            label = { Text("Unidad (pzas, g, kg, ml, L)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(15.dp))

        OutlinedTextField(
            value = fechaCaducidad,
            onValueChange = {},
            readOnly = true,
            label = {
                Text("Fecha de caducidad (opcional)")
            },
            placeholder = {
                Text("Selecciona una fecha")
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = {
                selectorFecha.show()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(15.dp)
        ) {

            Text(
                text = "📅 Seleccionar fecha",
                color = Color(0xFF6750A4)
            )
        }

        if (fechaCaducidad.isBlank()) {
            Text(
                "Puedes guardar sin caducidad y agregarla después. " +
                    "Hasta entonces no se programarán alertas para este producto.",
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            TextButton(onClick = { fechaCaducidad = "" }) { Text("Dejar caducidad pendiente") }
        }

        Spacer(modifier = Modifier.height(30.dp))

        Button(
            onClick = {

                val usuario =
                    auth.currentUser

                when {

                    usuario == null -> {

                        Toast.makeText(
                            context,
                            "No hay una sesión iniciada",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    nombre.isBlank() -> {

                        Toast.makeText(
                            context,
                            "Escribe el nombre del producto",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    categoria.isBlank() -> {

                        Toast.makeText(
                            context,
                            "Escribe una categoría",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    cantidad.isBlank() -> {

                        Toast.makeText(
                            context,
                            "Escribe la cantidad",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    cantidad.toDoubleOrNull()?.let { it > 0.0 && it.isFinite() } != true -> {

                        Toast.makeText(
                            context,
                            "La cantidad debe ser mayor que 0",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    unidad.isBlank() -> {
                        Toast.makeText(context, "Escribe una unidad", Toast.LENGTH_SHORT).show()
                    }

                    fechaCaducidad.isNotBlank() && FechasCaducidad.diasRestantes(fechaCaducidad) == null -> {

                        Toast.makeText(
                            context,
                            "Fecha inválida. Corrígela o déjala pendiente.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    else -> {

                        cargando = true

                        val producto =
                            hashMapOf(
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
                                "fechaCreacion" to Timestamp.now()
                            )

                        db.collection("productos")
                            .add(producto)
                            .addOnSuccessListener {

                                // Revisión inmediata con la app abierta; WorkManager sigue
                                // realizando la revisión periódica en segundo plano.
                                if (fechaCaducidad.isNotBlank()) {
                                    RevisionManualCaducidad.revisar(context, usuario.uid)
                                }
                                cargando = false

                                Toast.makeText(
                                    context,
                                    "Producto guardado correctamente",
                                    Toast.LENGTH_SHORT
                                ).show()

                                productoGuardado()
                            }
                            .addOnFailureListener { error ->

                                cargando = false

                                Toast.makeText(
                                    context,
                                    "Error al guardar: ${error.localizedMessage}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    }
                }
            },
            enabled = !cargando,
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF6750A4),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(15.dp)
        ) {

            if (cargando) {

                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )

            } else {

                Text(
                    text = "Guardar producto",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(15.dp))

        TextButton(
            onClick = regresar
        ) {

            Text(
                text = "← Volver",
                color = Color.Gray
            )
        }
    }
}

// ----------------------------------------------------
// EDITAR PRODUCTO
// ----------------------------------------------------

@Composable
fun PantallaEditarProducto(
    producto: Map<String, Any>,
    regresar: () -> Unit,
    productoActualizado: () -> Unit
) {

    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val usuario = FirebaseAuth.getInstance().currentUser

    val idProducto =
        producto["id"]?.toString() ?: ""

    var nombre by remember(producto) {
        mutableStateOf(
            producto["nombre"]?.toString() ?: ""
        )
    }

    var categoria by remember(producto) {
        mutableStateOf(
            producto["categoria"]?.toString() ?: ""
        )
    }

    var cantidad by remember(producto) {
        mutableStateOf(
            producto["cantidad"]?.toString() ?: ""
        )
    }

    var unidad by remember(producto) {
        mutableStateOf(producto["unidad"]?.toString() ?: "pzas")
    }

    var marca by remember(producto) {
        mutableStateOf(producto["marca"]?.toString().orEmpty())
    }

    var codigoBarras by remember(producto) {
        mutableStateOf(producto["codigoBarras"]?.toString().orEmpty())
    }

    var fechaCaducidad by remember(producto) {
        mutableStateOf(
            producto["fechaCaducidad"]?.toString() ?: ""
        )
    }

    var cargando by remember {
        mutableStateOf(false)
    }

    val calendario =
        Calendar.getInstance()

    // Intentamos iniciar el calendario en la fecha actual del producto.
    try {

        val formato =
            SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.getDefault()
            )

        formato.isLenient = false

        val fecha =
            formato.parse(fechaCaducidad)

        if (fecha != null) {
            calendario.time = fecha
        }

    } catch (_: Exception) {
    }

    val selectorFecha =
        DatePickerDialog(
            context,
            { _, year, month, day ->

                val fechaSeleccionada =
                    Calendar.getInstance()

                fechaSeleccionada.set(
                    year,
                    month,
                    day
                )

                val formato =
                    SimpleDateFormat(
                        "dd/MM/yyyy",
                        Locale.getDefault()
                    )

                fechaCaducidad =
                    formato.format(
                        fechaSeleccionada.time
                    )
            },
            calendario.get(Calendar.YEAR),
            calendario.get(Calendar.MONTH),
            calendario.get(Calendar.DAY_OF_MONTH)
        )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F5FA))
            .verticalScroll(
                rememberScrollState()
            )
            .padding(horizontal = 24.dp)
            .padding(
                top = 60.dp,
                bottom = 40.dp
            ),
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Text(
            text = "ExpiryAlert",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF6750A4)
        )

        Spacer(
            modifier = Modifier.height(30.dp)
        )

        Text(
            text = "Editar producto",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "Modifica los datos del producto",
            fontSize = 16.sp,
            color = Color.Gray
        )

        Spacer(
            modifier = Modifier.height(30.dp)
        )

        OutlinedTextField(
            value = nombre,
            onValueChange = {
                nombre = it
            },
            label = {
                Text("Nombre del producto")
            },
            modifier =
                Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(
            modifier = Modifier.height(15.dp)
        )

        OutlinedTextField(
            value = categoria,
            onValueChange = {
                categoria = it
            },
            label = {
                Text("Categoría")
            },
            modifier =
                Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(15.dp))

        OutlinedTextField(
            value = marca,
            onValueChange = { marca = it },
            label = { Text("Marca (opcional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(15.dp))

        OutlinedTextField(
            value = cantidad,
            onValueChange = { nuevoValor ->

                val limpio = nuevoValor.replace(',', '.')
                if (limpio.matches(Regex("""\d*(?:\.\d{0,3})?"""))) {
                    cantidad = limpio
                }
            },
            label = {
                Text("Cantidad")
            },
            modifier =
                Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions =
                KeyboardOptions(
                    keyboardType =
                        KeyboardType.Decimal
                )
        )

        Spacer(modifier = Modifier.height(15.dp))

        OutlinedTextField(
            value = unidad,
            onValueChange = { unidad = it },
            label = { Text("Unidad (pzas, g, kg, ml, L)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(
            modifier = Modifier.height(15.dp)
        )

        OutlinedTextField(
            value = fechaCaducidad,
            onValueChange = {},
            readOnly = true,
            label = {
                Text("Fecha de caducidad (opcional)")
            },
            modifier =
                Modifier.fillMaxWidth()
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        OutlinedButton(
            onClick = {
                selectorFecha.show()
            },
            modifier =
                Modifier.fillMaxWidth(),
            shape =
                RoundedCornerShape(15.dp)
        ) {

            Text(
                text = "📅 Cambiar fecha",
                color = Color(0xFF6750A4)
            )
        }

        if (fechaCaducidad.isBlank()) {
            Text(
                "Sin fecha todavía: este producto no generará alertas de caducidad.",
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            TextButton(onClick = { fechaCaducidad = "" }) {
                Text("Dejar caducidad pendiente")
            }
        }

        Spacer(
            modifier = Modifier.height(30.dp)
        )

        Button(
            onClick = {

                when {

                    usuario == null -> {

                        Toast.makeText(
                            context,
                            "No hay una sesión iniciada",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    idProducto.isBlank() -> {

                        Toast.makeText(
                            context,
                            "No se encontró el producto",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    nombre.isBlank() -> {

                        Toast.makeText(
                            context,
                            "Escribe el nombre del producto",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    categoria.isBlank() -> {

                        Toast.makeText(
                            context,
                            "Escribe una categoría",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    cantidad.isBlank() -> {

                        Toast.makeText(
                            context,
                            "Escribe la cantidad",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    cantidad.toDoubleOrNull()?.let { it > 0.0 && it.isFinite() } != true -> {

                        Toast.makeText(
                            context,
                            "La cantidad debe ser mayor que 0",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    unidad.isBlank() -> {
                        Toast.makeText(context, "Escribe una unidad", Toast.LENGTH_SHORT).show()
                    }

                    fechaCaducidad.isNotBlank() && FechasCaducidad.diasRestantes(fechaCaducidad) == null -> {

                        Toast.makeText(
                            context,
                            "Fecha inválida. Corrígela o déjala pendiente.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    else -> {

                        cargando = true

                        val cambios =
                            hashMapOf<String, Any>(
                                "nombre" to nombre.trim(),
                                "categoria" to categoria.trim(),
                                "marca" to marca.trim(),
                                "codigoBarras" to codigoBarras,
                                "cantidad" to cantidad.toDouble(),
                                "unidad" to unidad.trim(),
                                "fechaCaducidad" to fechaCaducidad,
                                "caducidadPendiente" to fechaCaducidad.isBlank(),
                                "usuarioId" to usuario.uid,
                                "correoUsuario" to (usuario.email ?: "")
                            )

                        db.collection("productos")
                            .document(idProducto)
                            .update(cambios)
                            .addOnSuccessListener {

                                // Cancelar avisos obsoletos y comprobar la nueva fecha.
                                NotificationHelper.cancelarAlerta(context, usuario.uid, idProducto)
                                if (fechaCaducidad.isNotBlank()) {
                                    RevisionManualCaducidad.revisar(context, usuario.uid)
                                }
                                cargando = false

                                Toast.makeText(
                                    context,
                                    "Producto actualizado correctamente",
                                    Toast.LENGTH_SHORT
                                ).show()

                                productoActualizado()
                            }
                            .addOnFailureListener { error ->

                                cargando = false

                                Toast.makeText(
                                    context,
                                    "Error al actualizar: ${error.localizedMessage}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    }
                }
            },

            enabled = !cargando,

            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),

            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        Color(0xFF6750A4),
                    contentColor =
                        Color.White
                ),

            shape =
                RoundedCornerShape(15.dp)
        ) {

            if (cargando) {

                CircularProgressIndicator(
                    modifier =
                        Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )

            } else {

                Text(
                    text = "Guardar cambios",
                    fontSize = 17.sp,
                    fontWeight =
                        FontWeight.Bold
                )
            }
        }

        Spacer(
            modifier = Modifier.height(15.dp)
        )

        TextButton(
            onClick = regresar
        ) {

            Text(
                text = "← Cancelar",
                color = Color.Gray
            )
        }
    }
}


// ----------------------------------------------------
// CONFIRMACIÓN DESDE UNA NOTIFICACIÓN
// ----------------------------------------------------
@Composable
fun DialogoConfirmacionCaducidad(
    uid: String,
    productoId: String,
    cerrar: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { FirebaseFirestore.getInstance() }
    var producto by remember(uid, productoId) { mutableStateOf<Map<String, Any>?>(null) }
    var error by remember(uid, productoId) { mutableStateOf<String?>(null) }
    var cargando by remember(uid, productoId) { mutableStateOf(true) }
    var guardando by remember(uid, productoId) { mutableStateOf(false) }

    LaunchedEffect(uid, productoId) {
        db.collection("productos").document(productoId).get()
            .addOnSuccessListener { doc ->
                val datos = doc.data
                if (FirebaseAuth.getInstance().currentUser?.uid != uid ||
                    !doc.exists() ||
                    datos?.get("usuarioId") != uid ||
                    datos["activo"] == false
                ) {
                    error = "El producto ya no está disponible en tu inventario."
                } else {
                    producto = datos
                }
                cargando = false
            }
            .addOnFailureListener {
                error = "No fue posible consultar el producto: ${it.localizedMessage}"
                cargando = false
            }
    }

    fun confirmar(conservaProducto: Boolean) {
        if (guardando || FirebaseAuth.getInstance().currentUser?.uid != uid) return
        guardando = true
        val cambios = mutableMapOf<String, Any>(
            "ultimaConfirmacion" to Timestamp.now()
        )
        if (!conservaProducto) {
            cambios["activo"] = false
            cambios["cantidad"] = 0
        }
        db.collection("productos").document(productoId).update(cambios)
            .addOnSuccessListener {
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
        title = { Text("¿Todavía tienes este producto?") },
        text = {
            Column {
                if (cargando) {
                    CircularProgressIndicator()
                } else if (producto != null) {
                    Text("${producto?.get("nombre") ?: "Producto"} — caduca: ${producto?.get("fechaCaducidad") ?: "Sin fecha"}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Puedes conservarlo o archivarlo si ya lo consumiste o desechaste.")
                }
                if (error != null) Text(error ?: "", color = Color(0xFFD32F2F))
            }
        },
        confirmButton = {
            TextButton(
                enabled = !cargando && !guardando && producto != null,
                onClick = { confirmar(true) }
            ) { Text("Sí, todavía lo tengo") }
        },
        dismissButton = {
            Column {
                TextButton(
                    enabled = !cargando && !guardando && producto != null,
                    onClick = { confirmar(false) }
                ) { Text("Ya no lo tengo") }
                TextButton(enabled = !guardando, onClick = cerrar) {
                    Text("Más tarde")
                }
            }
        }
    )
}

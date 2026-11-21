package com.example.expiryalert

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BienvenidaInicio(irLogin: () -> Unit, irRegistro: () -> Unit) {
    Column(Modifier.fillMaxSize().background(BgPrimary).safeDrawingPadding()
        .verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(IconosExpiry.Hoja, null, Modifier.size(28.dp), tint = AccentSage)
            Spacer(Modifier.width(10.dp))
            Text("ExpiryAlert", Modifier.weight(1f), color = TextPrimary, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            BotonModoOscuro()
        }
        Spacer(Modifier.height(5.dp))
        Box(Modifier.size(92.dp).clip(RoundedCornerShape(28.dp)).background(StatusSafeBg), contentAlignment = Alignment.Center) {
            Icon(IconosExpiry.Hoja, null, Modifier.size(52.dp), tint = AccentSage)
        }
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Tu despensa,\nsiempre a mano.", color = TextPrimary, fontSize = 34.sp, lineHeight = 40.sp,
                fontWeight = FontWeight.Bold, letterSpacing = (-0.8).sp)
            Text("Organiza tus alimentos, recibe avisos de caducidad y descubre qué cocinar.",
                color = TextSecondary, fontSize = 15.sp, lineHeight = 23.sp)
        }
        PanelInventario {
            BeneficioInicio(IconosExpiry.Hoja, "Sabe lo que tienes", "Registra tus productos y sus cantidades.")
            HorizontalDivider(color = BorderSubtle)
            BeneficioInicio(IconosExpiry.Revisar, "Úsalo a tiempo", "Recibe avisos antes de que se desperdicie.")
            HorizontalDivider(color = BorderSubtle)
            BeneficioInicio(IconosExpiry.Recetas, "Dale una idea a tu cocina", "Encuentra recetas con tu despensa.")
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            BotonInventario("Iniciar sesión", onClick = irLogin)
            OutlinedButton(irRegistro, Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderSubtle), contentPadding = PaddingValues(vertical = 15.dp)) {
                Text("Crear una cuenta", color = TextPrimary, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun BeneficioInicio(icono: ImageVector, titulo: String, detalle: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(icono, null, Modifier.size(24.dp), tint = AccentSage)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(titulo, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(detalle, color = TextSecondary, fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
fun CabeceraDespensa(uid: String, nombre: String, irPerfil: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(shape = RoundedCornerShape(14.dp), color = StatusSafeBg) {
            Icon(IconosExpiry.Hoja, null, Modifier.padding(10.dp).size(25.dp), tint = AccentSage)
        }
        Text("ExpiryAlert", Modifier.weight(1f), fontSize = 20.sp, fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp, color = TextPrimary)
        BotonModoOscuro()
        AvatarPerfil(uid, nombre, 46.dp, onClick = irPerfil)
    }
}

@Composable
fun BienvenidaDespensa(nombre: String, total: Int, proximos: Int) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), color = Color(0xFF175D47)) {
        Box(Modifier.background(Brush.linearGradient(listOf(Color(0xFF176A52), Color(0xFF254F3A))))) {
            Canvas(Modifier.matchParentSize()) {
                drawCircle(Color.White.copy(alpha = 0.06f), size.width * 0.42f, Offset(size.width, 0f))
                drawCircle(Color.White.copy(alpha = 0.04f), size.width * 0.28f, Offset(size.width * 0.89f, size.height))
            }
            Icon(IconosExpiry.Hoja, null, Modifier.align(Alignment.CenterEnd).padding(16.dp).size(76.dp),
                tint = Color.White.copy(alpha = 0.11f))
            Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("Hola, $nombre", color = Color(0xFFCFEBDA), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text("Mi despensa", color = Color.White, fontSize = 32.sp, lineHeight = 36.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = (-0.8).sp)
                Text(if (proximos > 0) "$proximos productos por vencer. Dales prioridad hoy."
                    else if (total > 0) "$total productos en casa. Aprovecha cada ingrediente."
                    else "Tu próximo buen hábito empieza con un producto.",
                    color = Color(0xFFD9EEE1), fontSize = 13.sp, lineHeight = 19.sp)
            }
        }
    }
}

@Composable
fun BotonNuevoProducto(onClick: () -> Unit) {
    Button(onClick, Modifier.fillMaxWidth().heightIn(min = 56.dp),
        shape = RoundedCornerShape(18.dp), contentPadding = PaddingValues(horizontal = 22.dp, vertical = 17.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AccentDark, contentColor = OnAccent),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)) {
        Icon(IconosExpiry.Agregar, null, Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Text("Nuevo producto", fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center, modifier = Modifier.weight(1f, fill = false))
    }
}

@Composable
fun ResumenDespensa(total: Int, seguros: Int, proximos: Int, vencidos: Int, incluirTotal: Boolean = true) {
    data class Dato(val titulo: String, val valor: Int, val color: Color)
    val datos = buildList {
        if (incluirTotal) add(Dato("Productos", total, TextPrimary))
        add(Dato("En orden", seguros, StatusSafe))
        add(Dato("Por vencer", proximos, StatusWarn))
        add(Dato("Vencidos", vencidos, StatusDanger))
    }
    val escala = LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columnas = when {
            maxWidth.value / escala < 230 -> 1
            !incluirTotal && maxWidth >= 310.dp && escala <= 1.15f -> 3
            incluirTotal && maxWidth.value / escala >= 600 -> 4
            else -> 2
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            datos.chunked(columnas).forEach { fila ->
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    fila.forEach { dato ->
                        Surface(Modifier.weight(1f).fillMaxHeight(), shape = RoundedCornerShape(20.dp),
                            color = SurfaceCard, border = BorderStroke(1.dp, BorderSubtle.copy(alpha = 0.8f))) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(7.dp).clip(CircleShape).background(dato.color))
                                    Spacer(Modifier.width(7.dp))
                                    Text(dato.titulo, Modifier.weight(1f), fontSize = 12.sp, lineHeight = 17.sp,
                                        fontWeight = FontWeight.Medium, color = TextSecondary)
                                }
                                Text(dato.valor.toString(), Modifier.padding(top = 10.dp), color = dato.color, fontSize = 30.sp,
                                    lineHeight = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp)
                            }
                        }
                    }
                    repeat(columnas - fila.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
fun AccesosDespensa(irRecetas: () -> Unit, irCompras: () -> Unit, irDesayuno: () -> Unit) {
    val escala = LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val enFila = maxWidth >= 300.dp && escala <= 1.2f
        if (enFila) {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AccesoDespensa("Recetas", IconosExpiry.Recetas, Modifier.weight(1f).fillMaxHeight(), false, irRecetas)
                AccesoDespensa("Compras", IconosExpiry.Compras, Modifier.weight(1f).fillMaxHeight(), false, irCompras)
                AccesoDespensa("Desayuno", IconosExpiry.Desayuno, Modifier.weight(1f).fillMaxHeight(), false, irDesayuno)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AccesoDespensa("Recetas", IconosExpiry.Recetas, Modifier.fillMaxWidth(), true, irRecetas)
                AccesoDespensa("Compras", IconosExpiry.Compras, Modifier.fillMaxWidth(), true, irCompras)
                AccesoDespensa("Desayuno", IconosExpiry.Desayuno, Modifier.fillMaxWidth(), true, irDesayuno)
            }
        }
    }
}

@Composable
private fun AccesoDespensa(titulo: String, icono: ImageVector, modifier: Modifier, horizontal: Boolean, onClick: () -> Unit) {
    Surface(onClick, modifier, shape = RoundedCornerShape(20.dp), color = SurfaceCard,
        border = BorderStroke(1.dp, BorderSubtle.copy(alpha = 0.8f))) {
        if (horizontal) {
            Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Icon(icono, null, Modifier.size(24.dp), tint = AccentSage)
                Text(titulo, Modifier.weight(1f), color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Icon(IconosExpiry.Siguiente, null, Modifier.size(17.dp), tint = TextSecondary)
            }
        } else {
            Column(Modifier.padding(vertical = 17.dp, horizontal = 10.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Icon(icono, null, Modifier.size(25.dp), tint = AccentSage)
                Text(titulo, fontSize = 13.sp, lineHeight = 18.sp, color = TextPrimary,
                    fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun AccesoRevision(pendientes: Int, onClick: () -> Unit) {
    Surface(onClick, Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = StatusSafeBg.copy(alpha = 0.65f)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(IconosExpiry.Revisar, null, Modifier.size(24.dp), tint = AccentSage)
            Column(Modifier.weight(1f)) {
                Text("Revisar cantidades", fontSize = 14.sp, lineHeight = 20.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Text(if (pendientes > 0) "$pendientes productos por confirmar" else "Mantén tus cantidades al día",
                    fontSize = 12.sp, lineHeight = 17.sp, color = TextSecondary)
            }
            Icon(IconosExpiry.Siguiente, null, Modifier.size(17.dp), tint = AccentSage)
        }
    }
}

@Composable
fun TarjetaProducto(
    producto: ProductoMapa, editar: () -> Unit, eliminar: () -> Unit, bajoStock: Boolean,
    consumir: () -> Unit, ajustar: () -> Unit, agregarCompra: () -> Unit
) {
    val cantidad = ControlCantidades.numero(producto["cantidad"]) ?: 0.0
    val inicial = ControlCantidades.numero(producto["cantidadInicial"])?.takeIf { it > 0 }
    val unidad = producto["unidad"]?.toString() ?: "pzas"
    val fecha = producto["fechaCaducidad"]?.toString().orEmpty()
    val estado = calcularEstadoCaducidad(fecha)
    val color = when (estado) { "Vencido" -> StatusDanger; "Próximo a caducar" -> StatusWarn; "Seguro" -> StatusSafe; else -> TextSecondary }
    val fondo = when (estado) { "Vencido" -> StatusDangerBg; "Próximo a caducar" -> StatusWarnBg; "Seguro" -> StatusSafeBg; else -> SurfaceMuted }
    var menu by remember { mutableStateOf(false) }
    PanelInventario {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(producto["nombre"]?.toString() ?: "Sin nombre", color = TextPrimary,
                    fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold)
                Text(listOfNotNull(producto["categoria"]?.toString(), producto["marca"]?.toString()?.takeIf { it.isNotBlank() })
                    .joinToString(" · "), fontSize = 12.sp, lineHeight = 17.sp, color = TextSecondary)
            }
            Box {
                IconButton(onClick = { menu = true }, Modifier.size(40.dp)) {
                    Icon(IconosExpiry.Mas, "Opciones del producto", tint = TextSecondary)
                }
                DropdownMenu(menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Editar datos") }, onClick = { menu = false; editar() })
                    DropdownMenuItem(text = { Text("Eliminar producto", color = StatusDanger) }, onClick = { menu = false; eliminar() })
                }
            }
        }
        Surface(shape = RoundedCornerShape(99.dp), color = fondo) {
            Row(Modifier.padding(horizontal = 11.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(color)); Spacer(Modifier.width(6.dp))
                Text(estado, color = color, fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
        Surface(shape = RoundedCornerShape(16.dp), color = SurfaceMuted) {
            Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Te queda", fontSize = 12.sp, color = TextSecondary)
                    Text("${ControlCantidades.texto(cantidad)} $unidad", color = TextPrimary,
                        fontSize = 18.sp, lineHeight = 23.sp, fontWeight = FontWeight.SemiBold)
                    if (inicial != null) Text("De ${ControlCantidades.texto(inicial)} $unidad", fontSize = 11.sp, color = TextSecondary)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Caducidad", fontSize = 12.sp, color = TextSecondary)
                    Text(fecha.ifBlank { "Por confirmar" }, color = TextPrimary, fontSize = 15.sp, lineHeight = 21.sp,
                        fontWeight = FontWeight.Medium)
                }
            }
        }
        if (bajoStock) Text(if (cantidad == 0.0) "Agotado · añádelo a tu próxima compra" else "Queda poco · considera reponerlo",
            color = StatusWarn, fontSize = 12.sp, lineHeight = 17.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(consumir, Modifier.weight(1f).heightIn(min = 48.dp), enabled = cantidad > 0,
                shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp)) {
                Text("Consumir", textAlign = TextAlign.Center, fontSize = 13.sp, lineHeight = 18.sp)
            }
            OutlinedButton(ajustar, Modifier.weight(1f).heightIn(min = 48.dp), shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp), border = BorderStroke(1.dp, BorderSubtle)) {
                Text("Ajustar cantidad", color = TextPrimary, textAlign = TextAlign.Center, fontSize = 13.sp, lineHeight = 18.sp)
            }
        }
        if (bajoStock) TextButton(agregarCompra, Modifier.fillMaxWidth()) {
            Icon(IconosExpiry.Compras, null, Modifier.size(17.dp)); Spacer(Modifier.width(8.dp))
            Text("Agregar a compras", textAlign = TextAlign.Center)
        }
    }
}

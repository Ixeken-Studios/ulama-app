package com.ixeken.worldcupinfo.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.ixeken.worldcupinfo.R
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.widget.Toast
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.CloudOff

/**
 * Cabecera superior unificada que mantiene coherencia visual y evita código duplicado.
 */
@Composable
fun AppHeader(
    showAlarmsOnly: Boolean,
    onToggleAlarmsOnly: () -> Unit,
    modifier: Modifier = Modifier,
    showFavoritesOnly: Boolean = false,
    onToggleShowFavoritesOnly: (() -> Unit)? = null,
    onSettingsClick: (() -> Unit)? = null,
    onFavoritesClick: (() -> Unit)? = null,
    onBackClick: (() -> Unit)? = null,
    isFavoritesActive: Boolean = false,
    onRefreshClick: (() -> Unit)? = null
) {
    var rotationAngle by remember { mutableStateOf(0f) }
    val animatedRotation by animateFloatAsState(
        targetValue = rotationAngle,
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
        label = "rotation"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Lado Izquierdo: Botón Atrás o Ajustes
        when {
            onBackClick != null -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF6A994E)) // Verde
                        .clickable { onBackClick() }
                ) {
                    BackArrowIcon()
                }
            }
            onSettingsClick != null -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF6A994E)) // Verde
                        .clickable { onSettingsClick() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(id = R.string.desc_settings),
                        tint = Color(0xFFF2E8CF), // Blanco Vainilla
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            else -> {
                Spacer(modifier = Modifier.size(40.dp))
            }
        }

        // Lado Derecho: Pill de filtros de Notificaciones y Favoritos
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF6A994E)) // Verde de fondo para el Pill
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            val isOnline = rememberIsOnline()
            val context = LocalContext.current
            val offlineToast = stringResource(R.string.toast_no_internet_fallback)

            if (!isOnline) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable {
                            Toast.makeText(
                                context,
                                offlineToast,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = stringResource(id = R.string.desc_no_internet),
                        tint = Color(0xFFF2E8CF),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            if (onRefreshClick != null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable {
                            if (isOnline) {
                                rotationAngle += 360f
                                onRefreshClick()
                            } else {
                                Toast.makeText(
                                    context,
                                    offlineToast,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(id = R.string.settings_force_refresh_title),
                        tint = Color(0xFFF2E8CF),
                        modifier = Modifier
                            .size(18.dp)
                            .graphicsLayer(rotationZ = animatedRotation)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            // Filtro de Notificaciones (Campana)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (showAlarmsOnly) Color(0xFF386641) else Color.Transparent) // Verde Oscuro si activo
                    .clickable { onToggleAlarmsOnly() }
            ) {
                Icon(
                    imageVector = if (showAlarmsOnly) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                    contentDescription = stringResource(id = R.string.desc_alarms_only),
                    tint = Color(0xFFF2E8CF),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Filtro de Favoritos (Estrella)
            val isStarHighlighted = isFavoritesActive || showFavoritesOnly
            val starBackground = if (isStarHighlighted) Color(0xFF386641) else Color.Transparent
            val starIcon = if (isStarHighlighted) Icons.Filled.Star else Icons.Outlined.Star
            
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(starBackground)
                    .clickable {
                        when {
                            isFavoritesActive -> { /* Ya en Favoritos */ }
                            onFavoritesClick != null -> onFavoritesClick()
                            onToggleShowFavoritesOnly != null -> onToggleShowFavoritesOnly()
                        }
                    }
            ) {
                Icon(
                    imageVector = starIcon,
                    contentDescription = stringResource(id = R.string.desc_favorites),
                    tint = Color(0xFFF2E8CF),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Icono de flecha izquierda animada dibujado en Canvas.
 */
@Composable
fun BackArrowIcon() {
    Canvas(modifier = Modifier.size(20.dp)) {
        val strokeWidth = 2.dp.toPx()
        val w = size.width
        val h = size.height
        // Línea horizontal
        drawLine(
            color = Color(0xFFF9F2E1), // Blanco Vainilla Claro
            start = Offset(w * 0.2f, h * 0.5f),
            end = Offset(w * 0.8f, h * 0.5f),
            strokeWidth = strokeWidth
        )
        // Diagonal superior
        drawLine(
            color = Color(0xFFF9F2E1),
            start = Offset(w * 0.2f, h * 0.5f),
            end = Offset(w * 0.45f, h * 0.25f),
            strokeWidth = strokeWidth
        )
        // Diagonal inferior
        drawLine(
            color = Color(0xFFF9F2E1),
            start = Offset(w * 0.2f, h * 0.5f),
            end = Offset(w * 0.45f, h * 0.75f),
            strokeWidth = strokeWidth
        )
    }
}

/**
 * Verifica si hay conexión a internet disponible.
 */
private fun isNetworkAvailable(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    if (connectivityManager != null) {
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        if (capabilities != null) {
            return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                   capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                   capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        }
    }
    return false
}

/**
 * Escucha reactivamente la disponibilidad de red de manera reactiva en Compose.
 */
@Composable
fun rememberIsOnline(): Boolean {
    val context = LocalContext.current
    return produceState(initialValue = isNetworkAvailable(context)) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (connectivityManager == null) {
            value = true
            return@produceState
        }
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                value = true
            }
            override fun onLost(network: Network) {
                value = false
            }
        }
        try {
            connectivityManager.registerDefaultNetworkCallback(callback)
        } catch (e: Exception) {
            value = isNetworkAvailable(context)
        }
        awaitDispose {
            try {
                connectivityManager.unregisterNetworkCallback(callback)
            } catch (e: Exception) {
                // Ignorar
            }
        }
    }.value
}


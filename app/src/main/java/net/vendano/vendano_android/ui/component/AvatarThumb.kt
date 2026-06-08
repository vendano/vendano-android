package net.vendano.vendano_android.ui.component

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import net.vendano.vendano_android.ui.theme.LocalVendanoTheme
import net.vendano.vendano_android.ui.theme.VendanoColors

/**
 * Circular avatar component.
 * Displays: localBytes > url > initials fallback.
 * Mirrors iOS AvatarThumb.swift.
 */
@Composable
fun AvatarThumb(
    localBytes: ByteArray?,
    url: String?,
    name: String,
    size: Dp = 56.dp,
    onClick: () -> Unit = {},
) {
    val theme = LocalVendanoTheme.current
    val initials = name.trim().split(" ")
        .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
        .take(2).joinToString("")
        .ifBlank { "?" }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(theme.accent)
            .clickable { onClick() },
    ) {
        when {
            localBytes != null -> {
                val bmp = BitmapFactory.decodeByteArray(localBytes, 0, localBytes.size)
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(size),
                    )
                } else {
                    InitialsText(initials, size)
                }
            }
            !url.isNullOrBlank() -> {
                AsyncImage(
                    model = url,
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape),
                )
            }
            else -> {
                InitialsText(initials, size)
            }
        }
    }
}

@Composable
private fun InitialsText(initials: String, containerSize: Dp) {
    Text(
        text = initials,
        color = VendanoColors.TextReversedLight,
        fontWeight = FontWeight.Bold,
        fontSize = (containerSize.value * 0.35f).sp,
    )
}

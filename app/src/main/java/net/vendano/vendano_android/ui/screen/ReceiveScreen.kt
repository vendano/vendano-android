package net.vendano.vendano_android.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import net.vendano.vendano_android.ui.component.LightGradientBackground
import net.vendano.vendano_android.ui.theme.LocalVendanoTheme
import net.vendano.vendano_android.ui.viewmodel.WalletViewModel

/**
 * Receive screen: displays wallet address as QR and copyable string.
 * Mirrors iOS ReceiveView.swift.
 */
@Composable
fun ReceiveScreen(
    walletViewModel: WalletViewModel,
    onClose: () -> Unit,
) {
    val theme = LocalVendanoTheme.current
    val address by walletViewModel.walletAddress.collectAsState(initial = "")
    val context = LocalContext.current

    val qrBitmap = remember(address) {
        if (address.isBlank()) null
        else generateQrBitmap(address, size = 400)
    }

    LightGradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = theme.textPrimary)
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Receive ADA", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = theme.textPrimary)
            Spacer(Modifier.height(4.dp))
            Text("Scan this QR code or copy your address.", fontSize = 14.sp, color = theme.textSecondary)

            Spacer(Modifier.height(32.dp))

            if (qrBitmap != null) {
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "QR Code",
                    modifier = Modifier
                        .size(240.dp)
                        .background(androidx.compose.ui.graphics.Color.White, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                )
            } else {
                CircularProgressIndicator(color = theme.accent)
            }

            Spacer(Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .background(theme.fieldBackground, RoundedCornerShape(12.dp))
                    .padding(16.dp),
            ) {
                Text(
                    text = address.ifBlank { "Loading…" },
                    color = theme.textPrimary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    val cm = ContextCompat.getSystemService(context, ClipboardManager::class.java)
                    cm?.setPrimaryClip(ClipData.newPlainText("Cardano Address", address))
                },
                colors = ButtonDefaults.buttonColors(containerColor = theme.fieldBackground),
                shape = RoundedCornerShape(50),
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = theme.accent, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Copy Address", color = theme.textPrimary, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private fun generateQrBitmap(content: String, size: Int): Bitmap {
    val writer = QRCodeWriter()
    val matrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
    for (x in 0 until size) {
        for (y in 0 until size) {
            bitmap.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
        }
    }
    return bitmap
}

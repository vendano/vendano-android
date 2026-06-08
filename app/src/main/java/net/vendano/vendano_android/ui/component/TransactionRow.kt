package net.vendano.vendano_android.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.vendano.vendano_android.domain.model.TxRowViewModel
import net.vendano.vendano_android.ui.theme.LocalVendanoTheme
import java.text.SimpleDateFormat
import java.util.Locale

private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

/**
 * Single transaction row in the home screen activity list.
 * Mirrors iOS TransactionRow.swift.
 */
@Composable
fun TransactionRow(
    tx: TxRowViewModel,
    modifier: Modifier = Modifier,
) {
    val theme = LocalVendanoTheme.current
    val isOutgoing = tx.outgoing

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Direction icon
        Icon(
            imageVector = if (isOutgoing) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
            contentDescription = if (isOutgoing) "Sent" else "Received",
            tint = if (isOutgoing) theme.negative else theme.positive,
            modifier = Modifier.size(20.dp),
        )

        // Counterparty + date
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tx.name ?: shortenAddress(tx.counterpartyAddress),
                color = theme.textPrimary,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = dateFormat.format(tx.date),
                color = theme.textSecondary,
                fontSize = 12.sp,
            )
        }

        // Amount
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${if (isOutgoing) "-" else "+"}${"%.2f".format(tx.amount)} ₳",
                color = if (isOutgoing) theme.negative else theme.positive,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
            )
            Text(
                text = "${"%.1f".format(tx.balanceAfter)} ₳",
                color = theme.textSecondary,
                fontSize = 11.sp,
            )
        }
    }
}

private fun shortenAddress(address: String): String {
    if (address.length <= 16) return address
    return "${address.take(8)}…${address.takeLast(8)}"
}

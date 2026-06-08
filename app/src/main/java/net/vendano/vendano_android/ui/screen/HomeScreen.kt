package net.vendano.vendano_android.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import net.vendano.vendano_android.ui.component.*
import net.vendano.vendano_android.ui.theme.LocalVendanoTheme
import net.vendano.vendano_android.ui.viewmodel.*
import java.util.Locale

/**
 * Home screen: balance, recent transactions, NFT grid, and send/receive CTA.
 * Mirrors iOS HomeView.swift.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    appViewModel: AppViewModel,
    walletViewModel: WalletViewModel,
    nftViewModel: NFTGalleryViewModel,
    onSend: () -> Unit,
    onReceive: () -> Unit,
) {
    val theme = LocalVendanoTheme.current
    val context = LocalContext.current

    val displayName by appViewModel.displayName.collectAsState()
    val avatarBytes by appViewModel.avatarBytes.collectAsState()
    val avatarUrl by appViewModel.avatarUrl.collectAsState()
    val env by appViewModel.environment.collectAsState()
    val walletAddress by walletViewModel.walletAddress.collectAsState()
    val toastMsg by appViewModel.toastMessage.collectAsState()

    val adaBalance by walletViewModel.adaBalance.collectAsState()
    val fiatRate by walletViewModel.adaFiatRate.collectAsState()
    val fiatCurrency by walletViewModel.fiatCurrency.collectAsState()
    val recentTxs by walletViewModel.recentTxs.collectAsState()
    val nfts by nftViewModel.nfts.collectAsState()
    val nftsLoading by nftViewModel.loading.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Transactions", "NFTs")

    var showSettings by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        DarkGradientBackground {
            Column(modifier = Modifier.fillMaxSize()) {

                // Status-bar safe area
                Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))

                // Top bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AvatarThumb(
                        localBytes = avatarBytes,
                        url = avatarUrl,
                        name = displayName,
                        size = 40.dp,
                    )
                    Text(
                        text = displayName.ifBlank { "Vendano" },
                        color = theme.textReversed,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                    )
                    IconButton(onClick = { showSettings = true }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = theme.textReversed.copy(alpha = 0.75f),
                        )
                    }
                }

                // Balance card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(
                            brush = Brush.linearGradient(
                                listOf(theme.textReversed.copy(alpha = 0.18f),
                                       theme.textReversed.copy(alpha = 0.08f))
                            ),
                            shape = RoundedCornerShape(20.dp),
                        )
                        .padding(24.dp),
                ) {
                    Column {
                        Text("Total Balance", color = theme.textReversed.copy(alpha = 0.7f), fontSize = 13.sp)
                        Text(
                            "₳ ${String.format(Locale.US, "%,.2f", adaBalance)}",
                            color = theme.textReversed,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        if (fiatRate != null) {
                            val fiatVal = adaBalance * (fiatRate ?: 0.0)
                            Text(
                                text = "${fiatCurrency.symbol}${String.format(Locale.US, "%,.2f", fiatVal)} ${fiatCurrency.code}",
                                color = theme.textReversed.copy(alpha = 0.6f),
                                fontSize = 14.sp,
                            )
                        }

                        Spacer(Modifier.height(24.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = onSend,
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = theme.textReversed,
                                    contentColor = theme.accent,
                                ),
                            ) {
                                Icon(Icons.Default.ArrowUpward, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Send", fontWeight = FontWeight.SemiBold)
                            }
                            Button(
                                onClick = onReceive,
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = theme.textReversed.copy(alpha = 0.2f),
                                    contentColor = theme.textReversed,
                                ),
                            ) {
                                Icon(Icons.Default.ArrowDownward, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Receive", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Tab row
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = theme.backgroundStart.copy(alpha = 0f),
                    contentColor = theme.textReversed,
                    divider = { HorizontalDivider(color = theme.textReversed.copy(alpha = 0.2f)) },
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = {
                                selectedTab = index
                                if (index == 1) nftViewModel.loadNFTs(walletAddress, env)
                            },
                            text = {
                                Text(
                                    title,
                                    color = if (selectedTab == index) theme.textReversed
                                            else theme.textReversed.copy(alpha = 0.5f),
                                    fontWeight = if (selectedTab == index) FontWeight.SemiBold
                                                 else FontWeight.Normal,
                                )
                            },
                        )
                    }
                }

                // Content area
                if (selectedTab == 0) {
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 8.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        if (recentTxs.isEmpty()) {
                            item {
                                Box(
                                    Modifier.fillParentMaxWidth().padding(32.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        "No transactions yet",
                                        color = theme.textReversed.copy(alpha = 0.5f),
                                        fontSize = 14.sp,
                                    )
                                }
                            }
                        } else {
                            items(recentTxs, key = { it.id }) { tx ->
                                TransactionRow(
                                    tx = tx,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                )
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.weight(1f)) {
                        if (nftsLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center),
                                color = theme.textReversed,
                            )
                        } else if (nfts.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No NFTs found", color = theme.textReversed.copy(alpha = 0.5f), fontSize = 14.sp)
                            }
                        } else {
                            LazyColumn(contentPadding = PaddingValues(16.dp)) {
                                items(nfts.chunked(2)) { row ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        row.forEach { nft ->
                                            NFTCard(nft = nft, modifier = Modifier.weight(1f))
                                        }
                                        if (row.size == 1) Spacer(Modifier.weight(1f))
                                    }
                                    Spacer(Modifier.height(12.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Toast overlay
        ToastBanner(
            message = toastMsg,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    // ─── Settings bottom sheet ────────────────────────────────────────────────

    if (showSettings) {
        ModalBottomSheet(
            onDismissRequest = { showSettings = false },
            containerColor = theme.cellBackground,
            dragHandle = {
                Box(
                    Modifier
                        .padding(vertical = 12.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .background(theme.textSecondary.copy(alpha = 0.4f), CircleShape)
                )
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = displayName.ifBlank { "Your Profile" },
                    color = theme.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                )
                Text(
                    text = walletAddress.take(24).let { if (walletAddress.length > 24) "$it…" else it },
                    color = theme.textSecondary,
                    fontSize = 12.sp,
                )

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = theme.textSecondary.copy(alpha = 0.2f))
                Spacer(Modifier.height(8.dp))

                // Contact developer
                SettingsRow(
                    icon = Icons.Default.Chat,
                    label = "Contact Developer",
                    onClick = {
                        showSettings = false
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:hello@vendano.net")
                            putExtra(Intent.EXTRA_SUBJECT, "Vendano Android Feedback")
                        }
                        context.startActivity(Intent.createChooser(intent, "Send email"))
                    },
                )

                // Log out
                SettingsRow(
                    icon = Icons.Default.Logout,
                    label = "Sign Out",
                    labelColor = theme.negative,
                    onClick = {
                        showSettings = false
                        FirebaseAuth.getInstance().signOut()
                        appViewModel.nukeAccount()
                    },
                )
            }
        }
    }
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    labelColor: androidx.compose.ui.graphics.Color = LocalVendanoTheme.current.textPrimary,
    onClick: () -> Unit,
) {
    val theme = LocalVendanoTheme.current
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 0.dp),
    ) {
        Icon(icon, contentDescription = null, tint = labelColor, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, color = labelColor, fontSize = 16.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun NFTCard(nft: net.vendano.vendano_android.domain.model.NFT, modifier: Modifier = Modifier) {
    val theme = LocalVendanoTheme.current
    Column(
        modifier = modifier
            .background(theme.cellBackground, RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        coil.compose.AsyncImage(
            model = nft.imageURL,
            contentDescription = nft.name,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(theme.fieldBackground),
        )
        Spacer(Modifier.height(8.dp))
        Text(nft.name, color = theme.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        if (nft.description != null) {
            Text(
                nft.description,
                color = theme.textSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
    }
}

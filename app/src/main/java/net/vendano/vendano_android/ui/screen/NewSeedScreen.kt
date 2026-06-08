package net.vendano.vendano_android.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bloxbean.cardano.client.account.Account
import com.bloxbean.cardano.client.common.model.Networks
import com.bloxbean.cardano.client.crypto.bip39.MnemonicCode
import net.vendano.vendano_android.ui.component.LightGradientBackground
import net.vendano.vendano_android.ui.component.PrimaryButton
import net.vendano.vendano_android.ui.theme.LocalVendanoTheme
import net.vendano.vendano_android.ui.viewmodel.AppViewModel
import java.security.SecureRandom

/** Generate a BIP39 mnemonic with the given word count (12, 15, or 24). */
private fun generateMnemonic(wordCount: Int): List<String> {
    if (wordCount == 24) {
        // bloxbean Account default is 24 words — fast path
        return Account(Networks.mainnet()).mnemonic().split(" ")
    }
    // For other lengths derive the correct entropy size:
    //   12 words → 128 bits (16 bytes)
    //   15 words → 160 bits (20 bytes)
    val entropyBytes = wordCount * 4 / 3
    val entropy = ByteArray(entropyBytes)
    SecureRandom().nextBytes(entropy)
    return MnemonicCode.INSTANCE.toMnemonic(entropy)
}

/**
 * New seed phrase screen: generates a fresh BIP39 phrase, lets the user pick 15 or 24 words.
 * Mirrors iOS NewSeedView.swift.
 */
@Composable
fun NewSeedScreen(
    appViewModel: AppViewModel,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    val theme = LocalVendanoTheme.current
    val wordCounts = listOf(15, 24)
    var selectedWordCount by remember { mutableIntStateOf(24) }

    val words = remember(selectedWordCount) {
        generateMnemonic(selectedWordCount)
    }

    LaunchedEffect(words) {
        appViewModel.setSeedWords(words)
    }

    LightGradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 24.dp),
        ) {
            // Top bar
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = theme.accent,
                    )
                }
                Text(
                    "Your Seed Phrase",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.textPrimary,
                )
            }

            Spacer(Modifier.height(4.dp))
            Text(
                "Write these words down in order. This is the ONLY way to recover your wallet. Never share it.",
                fontSize = 14.sp,
                color = theme.textSecondary,
                modifier = Modifier.padding(horizontal = 8.dp),
            )

            Spacer(Modifier.height(12.dp))

            // Word count toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                wordCounts.forEach { count ->
                    val selected = count == selectedWordCount
                    FilterChip(
                        selected = selected,
                        onClick = { selectedWordCount = count },
                        label = { Text("$count words") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = theme.accent,
                            selectedLabelColor = theme.textReversed,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
            ) {
                itemsIndexed(words) { index, word ->
                    Row(
                        modifier = Modifier
                            .background(theme.cellBackground, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${index + 1}.",
                            color = theme.textSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier.width(22.dp),
                        )
                        Text(
                            text = word,
                            color = theme.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            PrimaryButton(
                text = "I've Written This Down",
                onClick = onNext,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
    }
}

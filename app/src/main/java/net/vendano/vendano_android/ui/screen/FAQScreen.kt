package net.vendano.vendano_android.ui.screen

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import net.vendano.vendano_android.R
import net.vendano.vendano_android.ui.component.DarkGradientBackground
import net.vendano.vendano_android.ui.theme.LocalVendanoTheme

// ─── FAQ data models ──────────────────────────────────────────────────────────

data class FaqDocument(val version: Int, val sections: List<FaqSection>)
data class FaqSection(val id: String, val title: String, val items: List<FaqItem>)
data class FaqItem(
    val id: String,
    val icon: String,
    val question: String,
    val tldr: String,
    val answer: String,
    val clarify: String,
    val details: String,
)

private fun loadOnboardingFaqs(context: Context): List<FaqItem> = runCatching {
    val json = context.resources.openRawResource(R.raw.faqs).bufferedReader().use { it.readText() }
    Gson().fromJson(json, FaqDocument::class.java)
        .sections.find { it.id == "onboarding" }?.items ?: emptyList()
}.getOrDefault(emptyList())

// ─── FAQ Wizard Screen ────────────────────────────────────────────────────────

/**
 * Onboarding FAQ wizard.
 * Shows onboarding FAQ items one at a time with progress dots.
 * User can page through with Next or skip all at once.
 */
@Composable
fun FAQScreen(onFinish: () -> Unit) {
    val theme = LocalVendanoTheme.current
    val context = LocalContext.current
    val faqs = remember { loadOnboardingFaqs(context) }
    var index by remember { mutableIntStateOf(0) }

    if (faqs.isEmpty()) {
        // No FAQs loaded — go straight through
        LaunchedEffect(Unit) { onFinish() }
        return
    }

    val faq = faqs[index]
    val isLast = index == faqs.lastIndex

    DarkGradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 56.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Progress dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                faqs.indices.forEach { i ->
                    Box(
                        modifier = Modifier
                            .size(if (i == index) 10.dp else 6.dp)
                            .background(
                                color = if (i == index) theme.textReversed
                                        else theme.textReversed.copy(alpha = 0.35f),
                                shape = CircleShape,
                            ),
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Card — slides in/out with crossfade
            AnimatedContent(
                targetState = index,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { it } + fadeIn()).togetherWith(
                            slideOutHorizontally { -it } + fadeOut()
                        )
                    } else {
                        (slideInHorizontally { -it } + fadeIn()).togetherWith(
                            slideOutHorizontally { it } + fadeOut()
                        )
                    }
                },
                label = "faq_card",
                modifier = Modifier.weight(1f),
            ) { cardIndex ->
                val item = faqs[cardIndex]
                FaqCard(item = item)
            }

            Spacer(Modifier.height(24.dp))

            // Navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onFinish) {
                    Text(
                        text = "Skip",
                        color = theme.textReversed.copy(alpha = 0.65f),
                        fontWeight = FontWeight.Medium,
                    )
                }

                Button(
                    onClick = { if (isLast) onFinish() else index++ },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = theme.textReversed,
                        contentColor = theme.accent,
                    ),
                    modifier = Modifier.defaultMinSize(minWidth = 120.dp),
                ) {
                    Text(
                        text = if (isLast) "Get Started" else "Next",
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun FaqCard(item: FaqItem) {
    val theme = LocalVendanoTheme.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = theme.textReversed.copy(alpha = 0.12f),
                shape = RoundedCornerShape(20.dp),
            )
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Icon + question
        if (item.icon.length == 1 || item.icon.any { it.code > 127 }) {
            Text(item.icon, fontSize = 40.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth())
        }

        Text(
            text = item.question,
            color = theme.textReversed,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        HorizontalDivider(color = theme.textReversed.copy(alpha = 0.2f))

        // TL;DR pill
        Box(
            modifier = Modifier
                .background(theme.accent.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(
                text = "TL;DR: ${item.tldr}",
                color = theme.textReversed,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
        }

        // Answer
        Text(
            text = item.answer,
            color = theme.textReversed.copy(alpha = 0.9f),
            fontSize = 15.sp,
            lineHeight = 22.sp,
        )

        // Clarify
        if (item.clarify.isNotBlank()) {
            Text(
                text = item.clarify,
                color = theme.textReversed.copy(alpha = 0.7f),
                fontSize = 13.sp,
                lineHeight = 19.sp,
            )
        }
    }
}

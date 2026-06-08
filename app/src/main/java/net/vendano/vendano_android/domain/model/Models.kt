package net.vendano.vendano_android.domain.model

import java.util.Date
import java.util.UUID

// MARK: - Onboarding

enum class OnboardingStep {
    LOADING, SPLASH, FAQ, AUTH, OTP, PROFILE, WALLET_CHOICE,
    NEW_SEED, IMPORT_SEED, CONFIRM_SEED, HOME, SEND, RECEIVE
}

// MARK: - Environment

enum class AppEnvironment(val rawValue: String) {
    MAINNET("mainnet"),
    TESTNET("testnet"),
    APP_STORE_REVIEW("appstorereview");

    val isMainnet get() = this == MAINNET || this == APP_STORE_REVIEW
}

// MARK: - Fiat Currency

enum class FiatCurrency(val code: String, val symbol: String, val displayName: String) {
    USD("USD", "$", "US Dollar"),
    EUR("EUR", "€", "Euro"),
    GBP("GBP", "£", "British Pound"),
    JPY("JPY", "¥", "Japanese Yen"),
    MXN("MXN", "$", "Mexican Peso"),
    KRW("KRW", "₩", "South Korean Won"),
    PHP("PHP", "₱", "Philippine Peso"),
    INR("INR", "₹", "Indian Rupee");

    val pricePair: String get() = "ADA-$code"
}

// MARK: - Mnemonic Language

enum class MnemonicLanguage(val rawValue: String) {
    APP("app"),
    ENGLISH("english"),
    JAPANESE("japanese"),
    KOREAN("korean"),
    SPANISH("spanish"),
    CHINESE_SIMPLIFIED("chineseSimplified"),
    CHINESE_TRADITIONAL("chineseTraditional"),
    FRENCH("french"),
    ITALIAN("italian"),
    CZECH("czech"),
    PORTUGUESE("portuguese");

    val displaySeparator: String
        get() = if (this == JAPANESE) "\u3000" else " "
}

// MARK: - Transactions

data class RawTx(
    val hash: String,
    val date: Date,
    val blockHeight: Int,
    val inputs: List<TxIO>,
    val outputs: List<TxIO>,
) {
    fun isOutgoing(forWallet: String): Boolean {
        val inSum = inputs.filter { it.address == forWallet }.sumOf { it.amount }
        val outSum = outputs.filter { it.address == forWallet }.sumOf { it.amount }
        return (outSum.toLong() - inSum.toLong()) < 0
    }
}

data class TxIO(
    val address: String,
    val amount: ULong,
)

data class TxRowViewModel(
    val id: String,
    val date: Date,
    val outgoing: Boolean,
    val amount: Double,
    val counterpartyAddress: String,
    val name: String?,
    val avatarURL: String?,
    val balanceAfter: Double,
)

// MARK: - Recipient

data class Recipient(
    val name: String,
    val avatarURL: String?,
    val address: String,
)

// MARK: - NFT

data class NFT(
    val id: String,
    val name: String,
    val imageURL: String?,
    val description: String?,
    val traits: Map<String, String>?,
)

// MARK: - Pricing

enum class PricingCurrency(val rawValue: String) {
    FIAT("fiat"),
    ADA("ada");
}

// MARK: - Payment Request (Quick Pay / Store)

data class VendanoPaymentRequest(
    val id: String = UUID.randomUUID().toString(),
    val createdAt: Date = Date(),
    val expiresAt: Date,
    val quickPayMatchId: String,
    val storeName: String,
    val merchantAddress: String,
    val pricingCurrency: PricingCurrency,
    val fiatCurrencyCode: String?,
    val fiatSubtotal: Double?,
    val exchangeRateFiatPerAda: Double?,
    val bufferPercent: Double,
    val baseAda: Double,
    val tipsEnabled: Boolean,
) {
    val isExpired: Boolean get() = Date() >= expiresAt
}

data class VendanoPaymentResponse(
    val requestId: String,
    val status: PaymentResponseStatus,
    val txHash: String?,
    val errorMessage: String?,
) {
    enum class PaymentResponseStatus {
        DECLINED, ACCEPTED, PAID, FAILED, EXPIRED, CANCELLED
    }
}

// MARK: - Share

data class ShareMessage(val text: String)

// MARK: - Send method

enum class SendMethod { EMAIL, PHONE, ADDRESS }

// MARK: - Appearance

enum class AppearancePreference { SYSTEM, LIGHT, DARK }

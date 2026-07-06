package net.vendano.vendano_android.ui.navigation

import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import net.vendano.vendano_android.domain.model.OnboardingStep
import net.vendano.vendano_android.ui.screen.*
import net.vendano.vendano_android.ui.viewmodel.AppViewModel
import net.vendano.vendano_android.ui.viewmodel.NFTGalleryViewModel
import net.vendano.vendano_android.ui.viewmodel.SendViewModel
import net.vendano.vendano_android.ui.viewmodel.WalletViewModel

object Routes {
    const val LOADING        = "loading"
    const val SPLASH         = "splash"
    const val FAQ            = "faq"
    const val AUTH           = "auth"
    const val OTP            = "otp"
    const val PROFILE        = "create_profile"
    const val WALLET_CHOICE  = "wallet_choice"
    const val NEW_SEED       = "new_seed"
    const val IMPORT_SEED    = "import_seed"
    const val CONFIRM_SEED   = "confirm_seed"
    const val HOME           = "home"
    const val SEND           = "send"
    const val RECEIVE        = "receive"
}

/**
 * Main navigation graph.
 *
 * Always starts at LOADING. Bootstrap drives state changes via [AppViewModel.onboardingStep].
 * A [LaunchedEffect] reacts to those changes and calls navController.navigate() — the only
 * correct pattern for state-driven navigation in Compose (startDestination is one-shot only).
 */
@Composable
fun VendanoNavGraph(
    navController: NavHostController,
    appViewModel: AppViewModel,
    walletViewModel: WalletViewModel,
) {
    val step by appViewModel.onboardingStep.collectAsState()

    // Reactively navigate when bootstrap resolves the onboarding step.
    // popUpTo(0) { inclusive = true } clears the entire back stack so the user can't
    // press Back to return to the loading / previous auth screens.
    LaunchedEffect(step) {
        val route = when (step) {
            OnboardingStep.LOADING       -> return@LaunchedEffect   // stay — bootstrap is still running
            OnboardingStep.SPLASH        -> Routes.SPLASH
            OnboardingStep.FAQ           -> Routes.FAQ
            OnboardingStep.AUTH          -> Routes.AUTH
            OnboardingStep.OTP           -> Routes.OTP
            OnboardingStep.PROFILE       -> Routes.PROFILE
            OnboardingStep.WALLET_CHOICE -> Routes.WALLET_CHOICE
            OnboardingStep.NEW_SEED      -> Routes.NEW_SEED
            OnboardingStep.IMPORT_SEED   -> Routes.IMPORT_SEED
            OnboardingStep.CONFIRM_SEED  -> Routes.CONFIRM_SEED
            OnboardingStep.HOME          -> Routes.HOME
            OnboardingStep.SEND          -> Routes.SEND
            OnboardingStep.RECEIVE       -> Routes.RECEIVE
        }
        // Only navigate if we're not already at the target destination
        if (navController.currentDestination?.route != route) {
            navController.navigate(route) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.LOADING,
        enterTransition = { fadeIn() },
        exitTransition  = { fadeOut() },
    ) {

        composable(Routes.LOADING) {
            SplashScreen(loading = true, appViewModel = appViewModel)
        }

        composable(Routes.SPLASH) {
            SplashScreen(loading = false, appViewModel = appViewModel, onNext = {
                navController.navigate(Routes.FAQ)
            })
        }

        composable(Routes.FAQ) {
            FAQScreen(onFinish = {
                navController.navigate(Routes.AUTH) {
                    popUpTo(Routes.SPLASH) { inclusive = true }
                }
            })
        }

        composable(Routes.AUTH) {
            AuthScreen(
                appViewModel = appViewModel,
                onOtpSent = { navController.navigate(Routes.OTP) },
                onEmailSent = { },
                onSuccess = { navController.navigate(Routes.PROFILE) }
            )
        }

        composable(Routes.OTP) {
            OtpScreen(
                appViewModel = appViewModel,
                onSuccess = {
                    val route = if (appViewModel.displayName.value.isBlank()) Routes.PROFILE else Routes.WALLET_CHOICE
                    navController.navigate(route)
                }
            )
        }

        composable(Routes.PROFILE) {
            CreateProfileScreen(
                appViewModel = appViewModel,
                onSuccess = { navController.navigate(Routes.WALLET_CHOICE) }
            )
        }

        composable(Routes.WALLET_CHOICE) {
            WalletChoiceScreen(
                onCreateNew = { navController.navigate(Routes.NEW_SEED) },
                onImport    = { navController.navigate(Routes.IMPORT_SEED) }
            )
        }

        composable(Routes.NEW_SEED) {
            NewSeedScreen(
                appViewModel = appViewModel,
                onNext = { navController.navigate(Routes.CONFIRM_SEED) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.IMPORT_SEED) {
            ImportSeedScreen(
                appViewModel = appViewModel,
                walletViewModel = walletViewModel,
                onSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.WALLET_CHOICE) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.CONFIRM_SEED) {
            ConfirmSeedScreen(
                appViewModel = appViewModel,
                walletViewModel = walletViewModel,
                onSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.WALLET_CHOICE) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.HOME) {
            val nftVm: NFTGalleryViewModel = hiltViewModel()

            // On cold start bootstrap() loads the wallet into WalletRepositoryImpl but never
            // calls walletViewModel.configure(), so _walletAddress stays "" and every balance /
            // transaction refresh guard fires `if (_walletAddress.isEmpty()) return` without
            // loading anything.  React to appViewModel.walletAddress so that regardless of
            // whether we arrive here from bootstrap (returning user) or from the import/create
            // flow, the WalletViewModel is always properly seeded.
            val appWalletAddress by appViewModel.walletAddress.collectAsState()
            val appEnv by appViewModel.environment.collectAsState()
            LaunchedEffect(appWalletAddress, appEnv) {
                if (appWalletAddress.isNotEmpty() &&
                    walletViewModel.walletAddress.value != appWalletAddress
                ) {
                    walletViewModel.configure(
                        walletAddress = appWalletAddress,
                        stakeAddress = "",
                        allAddresses = listOf(appWalletAddress),
                        env = appEnv,
                    )
                    walletViewModel.refreshOnChainData()
                }
            }

            HomeScreen(
                appViewModel = appViewModel,
                walletViewModel = walletViewModel,
                nftViewModel = nftVm,
                onSend = { navController.navigate(Routes.SEND) },
                onReceive = { navController.navigate(Routes.RECEIVE) },
            )
        }

        composable(Routes.SEND) {
            val sendVm: SendViewModel = hiltViewModel()
            SendScreen(
                appViewModel = appViewModel,
                walletViewModel = walletViewModel,
                sendViewModel = sendVm,
                onClose = { navController.popBackStack() },
            )
        }

        composable(Routes.RECEIVE) {
            ReceiveScreen(
                walletViewModel = walletViewModel,
                onClose = { navController.popBackStack() },
            )
        }
    }
}

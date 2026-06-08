package net.vendano.vendano_android.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import net.vendano.vendano_android.data.prefs.AppPreferences
import net.vendano.vendano_android.data.secure.SecureKeyStore
import net.vendano.vendano_android.domain.model.AppEnvironment
import net.vendano.vendano_android.domain.repository.WalletRepository
import net.vendano.vendano_android.util.Config
import java.util.concurrent.TimeUnit

/**
 * Periodic background worker that refreshes on-chain balance and recent
 * transactions. Mirrors iOS BackgroundTask / BGAppRefreshTask.
 */
@HiltWorker
class BalanceSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val walletRepository: WalletRepository,
    private val secureKeyStore: SecureKeyStore,
    private val prefs: AppPreferences,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val words = walletRepository.loadSeedWords() ?: return Result.success()
            val env = AppEnvironment.MAINNET

            val result = walletRepository.importWallet(words, env)
            result.onSuccess { address ->
                val projectId = Config.blockfrostKey(env)
                walletRepository.fetchBalances(address, "", projectId, env)
                walletRepository.fetchTransactions(address, listOf(address), projectId, env)
            }
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 2) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "balance_sync"

        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<BalanceSyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}

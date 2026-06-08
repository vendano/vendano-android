package net.vendano.vendano_android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.vendano.vendano_android.domain.model.AppEnvironment
import net.vendano.vendano_android.domain.model.NFT
import net.vendano.vendano_android.domain.repository.WalletRepository
import net.vendano.vendano_android.util.Config
import javax.inject.Inject

@HiltViewModel
class NFTGalleryViewModel @Inject constructor(
    private val walletRepo: WalletRepository,
) : ViewModel() {

    private val _nfts = MutableStateFlow<List<NFT>>(emptyList())
    val nfts: StateFlow<List<NFT>> = _nfts.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    fun loadNFTs(walletAddress: String, env: AppEnvironment) {
        if (walletAddress.isEmpty()) return
        _loading.value = true
        viewModelScope.launch {
            val projectId = Config.blockfrostKey(env)
            walletRepo.fetchNFTs(walletAddress, projectId, env)
                .onSuccess { _nfts.value = it }
                .onFailure { /* Silently ignore NFT fetch failures */ }
            _loading.value = false
        }
    }
}

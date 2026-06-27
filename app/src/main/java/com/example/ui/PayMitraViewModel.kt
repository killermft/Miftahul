package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class NetworkQuality {
    EXCELLENT, WEAK
}

sealed class AiInsightsState {
    object Idle : AiInsightsState()
    object Loading : AiInsightsState()
    data class Success(val insights: String) : AiInsightsState()
    data class Error(val message: String) : AiInsightsState()
}

class PayMitraViewModel(private val repository: PayMitraRepository) : ViewModel() {

    private val geminiService = GeminiService()

    // Base flows from repository
    val transactions: StateFlow<List<DbTransaction>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userProfile: StateFlow<DbUserProfile?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val favoriteContacts: StateFlow<List<DbFavoriteContact>> = repository.favoriteContacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Network simulation state
    private val _networkQuality = MutableStateFlow(NetworkQuality.EXCELLENT)
    val networkQuality: StateFlow<NetworkQuality> = _networkQuality.asStateFlow()

    private val _pendingQueue = MutableStateFlow<List<DbTransaction>>(emptyList())
    val pendingQueue: StateFlow<List<DbTransaction>> = _pendingQueue.asStateFlow()

    // AI Insight state
    private val _aiInsights = MutableStateFlow<AiInsightsState>(AiInsightsState.Idle)
    val aiInsights: StateFlow<AiInsightsState> = _aiInsights.asStateFlow()

    // Budget Configuration
    private val _budget = MutableStateFlow(25000.0)
    val budget: StateFlow<Double> = _budget.asStateFlow()

    // Authentication session
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    init {
        // Pre-populate mock contacts if empty
        viewModelScope.launch {
            repository.favoriteContacts.first().let { current ->
                if (current.isEmpty()) {
                    repository.insertContact(DbFavoriteContact("arjun@ybl", "Arjun Sharma", "0xFFE11D48"))
                    repository.insertContact(DbFavoriteContact("priya@okhdfc", "Priya Patel", "0xFF0284C7"))
                    repository.insertContact(DbFavoriteContact("rohan@axl", "Rohan Das", "0xFF16A34A"))
                    repository.insertContact(DbFavoriteContact("ananya@paytm", "Ananya Iyer", "0xFF7C3AED"))
                    repository.insertContact(DbFavoriteContact("vikram@baroda", "Vikram Singh", "0xFFEA580C"))
                }
            }
        }
    }

    fun loginUser(name: String, mobileNumber: String, bankName: String) {
        viewModelScope.launch {
            val upiId = "${name.lowercase().replace(" ", "")}@paymitra"
            val randomAcc = (100000000000L..999999999999L).random().toString()
            val profile = DbUserProfile(
                upiId = upiId,
                name = name,
                balance = 15000.0, // Starting balance
                mobileNumber = mobileNumber,
                bankName = bankName,
                bankAccountNumber = "•••• $randomAcc"
            )
            repository.saveUserProfile(profile)
            _isLoggedIn.value = true
        }
    }

    fun logout() {
        _isLoggedIn.value = false
    }

    fun toggleNetworkMode() {
        val nextMode = if (_networkQuality.value == NetworkQuality.EXCELLENT) {
            NetworkQuality.WEAK
        } else {
            NetworkQuality.EXCELLENT
        }
        _networkQuality.value = nextMode
    }

    fun updateBudget(amount: Double) {
        _budget.value = amount
    }

    fun generateInsights() {
        viewModelScope.launch {
            _aiInsights.value = AiInsightsState.Loading
            try {
                val txs = transactions.value
                val currentBudget = _budget.value
                val result = geminiService.getSpendingInsights(txs, currentBudget)
                _aiInsights.value = AiInsightsState.Success(result)
            } catch (e: Exception) {
                _aiInsights.value = AiInsightsState.Error(e.localizedMessage ?: "Network error generating insights")
            }
        }
    }

    // Process payment
    suspend fun pay(
        recipientName: String,
        recipientUpiId: String,
        amount: Double,
        note: String,
        category: String = "Transfer"
    ): PaymentResult {
        val currentProfile = userProfile.value ?: return PaymentResult.Failure("User Profile not loaded")

        if (currentProfile.balance < amount) {
            return PaymentResult.Failure("Insufficient account balance. Available: ₹${currentProfile.balance}")
        }

        val tx = DbTransaction(
            senderName = currentProfile.name,
            recipientName = recipientName,
            recipientUpiId = recipientUpiId,
            amount = amount,
            note = note,
            category = category,
            status = if (_networkQuality.value == NetworkQuality.WEAK) "PENDING" else "SUCCESS"
        )

        if (_networkQuality.value == NetworkQuality.WEAK) {
            // Queue transaction for offline processing and save as PENDING
            _pendingQueue.update { it + tx }
            repository.insertTransaction(tx)
            // Deduct balance anyway for pessimistic UI representation
            val newBalance = currentProfile.balance - amount
            repository.updateBalance(currentProfile.upiId, newBalance)
            return PaymentResult.Queued(tx)
        } else {
            // Direct immediate transaction
            repository.insertTransaction(tx)
            val newBalance = currentProfile.balance - amount
            repository.updateBalance(currentProfile.upiId, newBalance)
            return PaymentResult.Success(tx)
        }
    }

    fun syncQueue() {
        viewModelScope.launch {
            if (_networkQuality.value == NetworkQuality.WEAK) return@launch
            val queue = _pendingQueue.value
            if (queue.isEmpty()) return@launch

            queue.forEach { tx ->
                // Update its state to SUCCESS in the database
                val updatedTx = tx.copy(status = "SUCCESS")
                repository.insertTransaction(updatedTx)
                delay(100) // Small progress effect
            }
            _pendingQueue.value = emptyList()
        }
    }

    fun addFavorite(name: String, upiId: String) {
        viewModelScope.launch {
            val colors = listOf("0xFFE11D48", "0xFF0284C7", "0xFF16A34A", "0xFF7C3AED", "0xFFEA580C", "0xFFDB2777")
            val randColor = colors.random()
            repository.insertContact(DbFavoriteContact(upiId, name, randColor))
        }
    }
}

sealed class PaymentResult {
    data class Success(val transaction: DbTransaction) : PaymentResult()
    data class Queued(val transaction: DbTransaction) : PaymentResult()
    data class Failure(val message: String) : PaymentResult()
}

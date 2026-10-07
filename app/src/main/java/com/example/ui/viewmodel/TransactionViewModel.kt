package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.backup.BackupManager
import com.example.data.backup.ServerStatus
import com.example.data.local.AppDatabase
import com.example.data.local.TransactionEntity
import com.example.data.repository.TransactionRepository
import com.example.domain.model.FilterPeriod
import com.example.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

class TransactionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TransactionRepository
    val backupManager: BackupManager

    init {
        val db = AppDatabase.getDatabase(application)
        repository = TransactionRepository(db.transactionDao())
        backupManager = BackupManager(application)
    }

    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _filterType = MutableStateFlow("ALL") // "ALL", "PEMASUKAN", "PENGELUARAN"
    val filterType: StateFlow<String> = _filterType.asStateFlow()

    private val _filterPeriod = MutableStateFlow(FilterPeriod.ALL)
    val filterPeriod: StateFlow<FilterPeriod> = _filterPeriod.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _serverStatus = MutableStateFlow<ServerStatus?>(null)
    val serverStatus: StateFlow<ServerStatus?> = _serverStatus.asStateFlow()

    private val _isCheckingServer = MutableStateFlow(false)
    val isCheckingServer: StateFlow<Boolean> = _isCheckingServer.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _previewPhotoPath = MutableStateFlow<String?>(null)
    val previewPhotoPath: StateFlow<String?> = _previewPhotoPath.asStateFlow()

    private val _autoBackupEnabled = MutableStateFlow(backupManager.isAutoBackupEnabled)
    val autoBackupEnabled: StateFlow<Boolean> = _autoBackupEnabled.asStateFlow()

    private val _backupInterval = MutableStateFlow(backupManager.backupInterval)
    val backupInterval: StateFlow<String> = _backupInterval.asStateFlow()

    private val _lastBackupTime = MutableStateFlow(backupManager.lastBackupTime)
    val lastBackupTime: StateFlow<Long> = _lastBackupTime.asStateFlow()

    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        allTransactions,
        _filterType,
        _filterPeriod,
        _searchQuery
    ) { list, type, period, query ->
        val now = Calendar.getInstance()

        list.filter { item ->
            // Filter by type
            val matchType = when (type) {
                "PEMASUKAN" -> item.type == "PEMASUKAN"
                "PENGELUARAN" -> item.type == "PENGELUARAN"
                else -> true
            }

            // Filter by period
            val matchPeriod = when (period) {
                FilterPeriod.ALL -> true
                FilterPeriod.TODAY -> {
                    val cal = Calendar.getInstance().apply { timeInMillis = item.dateMillis }
                    cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                    cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
                }
                FilterPeriod.THIS_WEEK -> {
                    val cal = Calendar.getInstance().apply { timeInMillis = item.dateMillis }
                    cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                    cal.get(Calendar.WEEK_OF_YEAR) == now.get(Calendar.WEEK_OF_YEAR)
                }
                FilterPeriod.THIS_MONTH -> {
                    val cal = Calendar.getInstance().apply { timeInMillis = item.dateMillis }
                    cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                    cal.get(Calendar.MONTH) == now.get(Calendar.MONTH)
                }
            }

            // Filter by search query
            val matchQuery = if (query.isBlank()) true else {
                item.note.contains(query, ignoreCase = true) ||
                item.category.contains(query, ignoreCase = true) ||
                item.amount.toString().contains(query)
            }

            matchType && matchPeriod && matchQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalIncome: StateFlow<Long> = allTransactions.combine(_filterPeriod) { list, period ->
        filterByPeriod(list, period).filter { it.type == "PEMASUKAN" }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val totalExpense: StateFlow<Long> = allTransactions.combine(_filterPeriod) { list, period ->
        filterByPeriod(list, period).filter { it.type == "PENGELUARAN" }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val netBalance: StateFlow<Long> = combine(totalIncome, totalExpense) { inc, exp ->
        inc - exp
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    private fun filterByPeriod(list: List<TransactionEntity>, period: FilterPeriod): List<TransactionEntity> {
        val now = Calendar.getInstance()
        return list.filter { item ->
            when (period) {
                FilterPeriod.ALL -> true
                FilterPeriod.TODAY -> {
                    val cal = Calendar.getInstance().apply { timeInMillis = item.dateMillis }
                    cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                    cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
                }
                FilterPeriod.THIS_WEEK -> {
                    val cal = Calendar.getInstance().apply { timeInMillis = item.dateMillis }
                    cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                    cal.get(Calendar.WEEK_OF_YEAR) == now.get(Calendar.WEEK_OF_YEAR)
                }
                FilterPeriod.THIS_MONTH -> {
                    val cal = Calendar.getInstance().apply { timeInMillis = item.dateMillis }
                    cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                    cal.get(Calendar.MONTH) == now.get(Calendar.MONTH)
                }
            }
        }
    }

    fun setFilterType(type: String) {
        _filterType.value = type
    }

    fun setFilterPeriod(period: FilterPeriod) {
        _filterPeriod.value = period
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showReceiptPreview(path: String?) {
        _previewPhotoPath.value = path
    }

    fun closeReceiptPreview() {
        _previewPhotoPath.value = null
    }

    fun addTransaction(
        type: String,
        amount: Long,
        category: String,
        dateMillis: Long,
        note: String,
        imageUri: Uri?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val savedImagePath = if (imageUri != null) {
                FileUtils.saveReceiptImage(getApplication(), imageUri)
            } else null

            val entity = TransactionEntity(
                type = type,
                amount = amount,
                category = category,
                dateMillis = dateMillis,
                note = note,
                imagePath = savedImagePath,
                isSynced = false
            )

            repository.insertTransaction(entity)
            _snackbarMessage.value = "Transaksi berhasil dicatat"
        }
    }

    fun updateTransaction(
        id: Long,
        type: String,
        amount: Long,
        category: String,
        dateMillis: Long,
        note: String,
        newImageUri: Uri?,
        removeCurrentImage: Boolean,
        currentImagePath: String?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            var finalImagePath = currentImagePath

            if (removeCurrentImage) {
                FileUtils.deleteReceiptImage(currentImagePath)
                finalImagePath = null
            }

            if (newImageUri != null) {
                FileUtils.deleteReceiptImage(currentImagePath)
                finalImagePath = FileUtils.saveReceiptImage(getApplication(), newImageUri)
            }

            val updated = TransactionEntity(
                id = id,
                type = type,
                amount = amount,
                category = category,
                dateMillis = dateMillis,
                note = note,
                imagePath = finalImagePath,
                isSynced = false
            )

            repository.updateTransaction(updated)
            _snackbarMessage.value = "Transaksi berhasil diperbarui"
        }
    }

    fun deleteTransaction(item: TransactionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            FileUtils.deleteReceiptImage(item.imagePath)
            repository.deleteTransaction(item)
            _snackbarMessage.value = "Transaksi berhasil dihapus"
        }
    }

    fun checkServerStatus() {
        viewModelScope.launch {
            _isCheckingServer.value = true
            val status = backupManager.checkServerOnlineStatus()
            _serverStatus.value = status
            _isCheckingServer.value = false
        }
    }

    fun syncAllTransactions() {
        viewModelScope.launch {
            _isSyncing.value = true
            val status = backupManager.checkServerOnlineStatus()
            _serverStatus.value = status

            if (!status.isConnected || !status.serverOnline) {
                _snackbarMessage.value = "Gagal sinkronisasi: Server offline atau tidak ada internet."
                _isSyncing.value = false
                return@launch
            }

            // Simulate cloud server push synchronization
            kotlinx.coroutines.delay(1200)
            repository.markAllAsSynced()
            _isSyncing.value = false
            _snackbarMessage.value = "Sinkronisasi berhasil! Semua transaksi tersimpan di server cloud."
        }
    }

    fun exportBackup(onResult: (File?) -> Unit) {
        viewModelScope.launch {
            val list = allTransactions.value
            val res = backupManager.exportToJson(list)
            if (res.isSuccess) {
                _lastBackupTime.value = backupManager.lastBackupTime
                _snackbarMessage.value = "Backup berhasil dibuat: ${res.getOrNull()?.name}"
                onResult(res.getOrNull())
            } else {
                _snackbarMessage.value = "Gagal membuat backup: ${res.exceptionOrNull()?.localizedMessage}"
                onResult(null)
            }
        }
    }

    fun exportCsv(onResult: (File?) -> Unit) {
        viewModelScope.launch {
            val list = allTransactions.value
            val res = backupManager.exportToCsv(list)
            if (res.isSuccess) {
                _snackbarMessage.value = "Laporan CSV berhasil dibuat: ${res.getOrNull()?.name}"
                onResult(res.getOrNull())
            } else {
                _snackbarMessage.value = "Gagal membuat CSV: ${res.exceptionOrNull()?.localizedMessage}"
                onResult(null)
            }
        }
    }

    fun restoreBackup(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsonString = file.readText()
                val res = backupManager.parseJsonBackup(jsonString)
                if (res.isSuccess && !res.getOrNull().isNullOrEmpty()) {
                    repository.insertAll(res.getOrNull()!!)
                    _snackbarMessage.value = "Berhasil memulihkan ${res.getOrNull()!!.size} data transaksi"
                } else {
                    _snackbarMessage.value = "File backup tidak valid atau kosong"
                }
            } catch (e: Exception) {
                _snackbarMessage.value = "Gagal memulihkan backup: ${e.localizedMessage}"
            }
        }
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        backupManager.isAutoBackupEnabled = enabled
        _autoBackupEnabled.value = enabled
        _snackbarMessage.value = if (enabled) "Pengingat backup otomatis diaktifkan" else "Pengingat backup otomatis dinonaktifkan"
    }

    fun setBackupIntervalPreference(interval: String) {
        backupManager.backupInterval = interval
        _backupInterval.value = interval
    }
}

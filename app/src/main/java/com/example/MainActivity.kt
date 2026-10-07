package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.TransactionEntity
import com.example.ui.components.ReceiptPhotoDialog
import com.example.ui.screens.AddEditTransactionSheet
import com.example.ui.screens.BackupSyncScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ReportScreen
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.TransactionViewModel
import kotlinx.coroutines.launch

enum class AppNavTab(val title: String, val icon: ImageVector) {
    HOME("Buku Kas", Icons.Default.ReceiptLong),
    REPORTS("Laporan", Icons.Default.Assessment),
    BACKUP_SYNC("Backup & Online", Icons.Default.CloudSync)
}

class MainActivity : ComponentActivity() {

    private val viewModel: TransactionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainApp(viewModel: TransactionViewModel) {
    var currentTab by remember { mutableStateOf(AppNavTab.HOME) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val filteredTransactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val totalIncome by viewModel.totalIncome.collectAsStateWithLifecycle()
    val totalExpense by viewModel.totalExpense.collectAsStateWithLifecycle()
    val netBalance by viewModel.netBalance.collectAsStateWithLifecycle()
    val selectedPeriod by viewModel.filterPeriod.collectAsStateWithLifecycle()
    val selectedType by viewModel.filterType.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val serverStatus by viewModel.serverStatus.collectAsStateWithLifecycle()
    val isCheckingServer by viewModel.isCheckingServer.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val snackbarMsg by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val previewPhotoPath by viewModel.previewPhotoPath.collectAsStateWithLifecycle()
    val autoBackupEnabled by viewModel.autoBackupEnabled.collectAsStateWithLifecycle()
    val backupInterval by viewModel.backupInterval.collectAsStateWithLifecycle()
    val lastBackupTime by viewModel.lastBackupTime.collectAsStateWithLifecycle()

    var showAddEditSheet by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }

    // Handle snackbar messages
    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    // Handle system back navigation
    BackHandler(enabled = currentTab != AppNavTab.HOME) {
        currentTab = AppNavTab.HOME
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("main_bottom_nav")
            ) {
                AppNavTab.values().forEach { tab ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(imageVector = tab.icon, contentDescription = tab.title)
                        },
                        label = { Text(tab.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EmeraldPrimary,
                            selectedTextColor = EmeraldPrimary,
                            indicatorColor = EmeraldPrimary.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        when (currentTab) {
            AppNavTab.HOME -> {
                HomeScreen(
                    transactions = filteredTransactions,
                    totalIncome = totalIncome,
                    totalExpense = totalExpense,
                    netBalance = netBalance,
                    selectedPeriod = selectedPeriod,
                    selectedType = selectedType,
                    searchQuery = searchQuery,
                    onPeriodSelected = { viewModel.setFilterPeriod(it) },
                    onTypeSelected = { viewModel.setFilterType(it) },
                    onSearchChanged = { viewModel.setSearchQuery(it) },
                    onAddClick = {
                        editingTransaction = null
                        showAddEditSheet = true
                    },
                    onEditClick = { item ->
                        editingTransaction = item
                        showAddEditSheet = true
                    },
                    onDeleteClick = { item ->
                        viewModel.deleteTransaction(item)
                    },
                    onViewReceipt = { path ->
                        viewModel.showReceiptPreview(path)
                    },
                    onNavigateToSync = {
                        currentTab = AppNavTab.BACKUP_SYNC
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            AppNavTab.REPORTS -> {
                ReportScreen(
                    transactions = allTransactions,
                    onExportCsv = {
                        viewModel.exportCsv { file ->
                            if (file != null) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Laporan CSV disimpan: ${file.name}")
                                }
                            }
                        }
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            AppNavTab.BACKUP_SYNC -> {
                BackupSyncScreen(
                    transactions = allTransactions,
                    serverStatus = serverStatus,
                    isCheckingServer = isCheckingServer,
                    isSyncing = isSyncing,
                    autoBackupEnabled = autoBackupEnabled,
                    backupInterval = backupInterval,
                    lastBackupTime = lastBackupTime,
                    backupFiles = viewModel.backupManager.getBackupFiles(),
                    onCheckServer = { viewModel.checkServerStatus() },
                    onSyncNow = { viewModel.syncAllTransactions() },
                    onExportBackup = {
                        viewModel.exportBackup { file ->
                            if (file != null) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("File backup JSON tersimpan!")
                                }
                            }
                        }
                    },
                    onExportCsv = {
                        viewModel.exportCsv { file ->
                            if (file != null) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("File CSV tersimpan!")
                                }
                            }
                        }
                    },
                    onRestoreFile = { file ->
                        viewModel.restoreBackup(file)
                    },
                    onToggleAutoBackup = { viewModel.setAutoBackupEnabled(it) },
                    onSelectInterval = { viewModel.setBackupIntervalPreference(it) },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }

        // Add/Edit Dialog Sheet
        if (showAddEditSheet) {
            AddEditTransactionSheet(
                existingTransaction = editingTransaction,
                onDismiss = {
                    showAddEditSheet = false
                    editingTransaction = null
                },
                onSave = { type, amount, category, dateMillis, note, newImageUri, removeCurrentImage ->
                    if (editingTransaction != null) {
                        viewModel.updateTransaction(
                            id = editingTransaction!!.id,
                            type = type,
                            amount = amount,
                            category = category,
                            dateMillis = dateMillis,
                            note = note,
                            newImageUri = newImageUri,
                            removeCurrentImage = removeCurrentImage,
                            currentImagePath = editingTransaction!!.imagePath
                        )
                    } else {
                        viewModel.addTransaction(
                            type = type,
                            amount = amount,
                            category = category,
                            dateMillis = dateMillis,
                            note = note,
                            imageUri = newImageUri
                        )
                    }
                    showAddEditSheet = false
                    editingTransaction = null
                }
            )
        }

        // Receipt Photo Fullscreen Dialog
        previewPhotoPath?.let { path ->
            ReceiptPhotoDialog(
                photoPath = path,
                onDismiss = { viewModel.closeReceiptPreview() }
            )
        }
    }
}

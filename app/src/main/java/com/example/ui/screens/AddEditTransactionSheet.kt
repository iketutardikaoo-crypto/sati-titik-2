package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.local.TransactionEntity
import com.example.domain.model.CategoryRegistry
import com.example.domain.model.TransactionType
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.util.CurrencyUtils
import com.example.util.DateUtils
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditTransactionSheet(
    existingTransaction: TransactionEntity? = null,
    onDismiss: () -> Unit,
    onSave: (
        type: String,
        amount: Long,
        category: String,
        dateMillis: Long,
        note: String,
        newImageUri: Uri?,
        removeCurrentImage: Boolean
    ) -> Unit
) {
    val isEditing = existingTransaction != null

    var selectedType by remember {
        mutableStateOf(existingTransaction?.type ?: "PEMASUKAN")
    }
    var amountString by remember {
        mutableStateOf(existingTransaction?.amount?.toString() ?: "")
    }
    var selectedCategory by remember {
        mutableStateOf(
            existingTransaction?.category ?: if (selectedType == "PEMASUKAN") "Penjualan" else "Belanja Stok"
        )
    }
    var selectedDateMillis by remember {
        mutableLongStateOf(existingTransaction?.dateMillis ?: System.currentTimeMillis())
    }
    var noteText by remember {
        mutableStateOf(existingTransaction?.note ?: "")
    }
    var selectedPhotoUri by remember {
        mutableStateOf<Uri?>(null)
    }
    var removeCurrentImage by remember {
        mutableStateOf(false)
    }
    var showDatePicker by remember {
        mutableStateOf(false)
    }
    var amountError by remember {
        mutableStateOf(false)
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedPhotoUri = uri
            removeCurrentImage = false
        }
    }

    val categories = if (selectedType == "PEMASUKAN") {
        CategoryRegistry.incomeCategories
    } else {
        CategoryRegistry.expenseCategories
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 32.dp),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEditing) "Edit Catatan Kas" else "Catat Transaksi Baru",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("dismiss_add_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Type selector (Pemasukan vs Pengeluaran)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Pemasukan button
                        val isIncome = selectedType == "PEMASUKAN"
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isIncome) IncomeGreen else Color.Transparent)
                                .clickable {
                                    selectedType = "PEMASUKAN"
                                    if (CategoryRegistry.incomeCategories.none { it.name == selectedCategory }) {
                                        selectedCategory = CategoryRegistry.incomeCategories.first().name
                                    }
                                }
                                .padding(vertical = 12.dp)
                                .testTag("type_income_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Pemasukan (+)",
                                color = if (isIncome) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        // Pengeluaran button
                        val isExpense = selectedType == "PENGELUARAN"
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isExpense) ExpenseRed else Color.Transparent)
                                .clickable {
                                    selectedType = "PENGELUARAN"
                                    if (CategoryRegistry.expenseCategories.none { it.name == selectedCategory }) {
                                        selectedCategory = CategoryRegistry.expenseCategories.first().name
                                    }
                                }
                                .padding(vertical = 12.dp)
                                .testTag("type_expense_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Pengeluaran (-)",
                                color = if (isExpense) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Amount input
                    Text(
                        text = "Nominal Transaksi (Rp)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = amountString,
                        onValueChange = { input ->
                            val clean = input.filter { it.isDigit() }
                            amountString = clean
                            amountError = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("amount_input_field"),
                        placeholder = { Text("0") },
                        prefix = { Text("Rp ", fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = amountError,
                        supportingText = {
                            val parsed = amountString.toLongOrNull() ?: 0L
                            if (parsed > 0) {
                                Text(
                                    text = "Terbaca: ${CurrencyUtils.formatRupiah(parsed)}",
                                    color = if (selectedType == "PEMASUKAN") IncomeGreen else ExpenseRed,
                                    fontWeight = FontWeight.Medium
                                )
                            } else if (amountError) {
                                Text("Nominal harus lebih dari 0", color = ExpenseRed)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (selectedType == "PEMASUKAN") IncomeGreen else ExpenseRed
                        )
                    )

                    // Quick amount increment buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(10000L, 50000L, 100000L, 500000L).forEach { addVal ->
                            val label = when (addVal) {
                                10000L -> "+10rb"
                                50000L -> "+50rb"
                                100000L -> "+100rb"
                                else -> "+500rb"
                            }
                            TextButton(
                                onClick = {
                                    val current = amountString.toLongOrNull() ?: 0L
                                    amountString = (current + addVal).toString()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            ) {
                                Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Category selection
                    Text(
                        text = "Pilih Kategori",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { cat ->
                            val isSelected = selectedCategory == cat.name
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = cat.name },
                                label = { Text(cat.name, fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = cat.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isSelected) Color.White else cat.color
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = cat.color,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("category_chip_${cat.name}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Date selector
                    Text(
                        text = "Tanggal Transaksi",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .clickable { showDatePicker = true }
                            .padding(horizontal = 14.dp, vertical = 14.dp)
                            .testTag("date_picker_button"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = DateUtils.formatFullDate(selectedDateMillis),
                                fontSize = 14.sp
                            )
                        }
                        Text(
                            text = "Ubah",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Note input
                    Text(
                        text = "Catatan / Keterangan (Opsional)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("note_input_field"),
                        placeholder = { Text("Contoh: Pembelian stok beras 5 karung") },
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // RECEIPT PHOTO (Fitur Baru Versi 3)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Foto Bukti / Nota Transaksi",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Lampirkan foto struk atau kuitansi pembayaran",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }

                        // Photo Picker Trigger
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("pick_receipt_photo_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddAPhoto,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Pilih Foto", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Show preview if image is selected or existing image is present
                    val hasExistingImage = !existingTransaction?.imagePath.isNullOrBlank() && !removeCurrentImage
                    val hasNewImage = selectedPhotoUri != null

                    if (hasNewImage || hasExistingImage) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.05f))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        ) {
                            if (hasNewImage) {
                                AsyncImage(
                                    model = selectedPhotoUri,
                                    contentDescription = "Nota terpilih",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else if (hasExistingImage) {
                                AsyncImage(
                                    model = File(existingTransaction!!.imagePath!!),
                                    contentDescription = "Nota yang sudah tersimpan",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            // Delete photo overlay button
                            IconButton(
                                onClick = {
                                    selectedPhotoUri = null
                                    removeCurrentImage = true
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(ExpenseRed)
                                    .testTag("remove_receipt_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Hapus Nota",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    } else {
                        // Empty photo placeholder
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Belum ada foto nota. Ketuk untuk menambah.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Submit Save Button
                Button(
                    onClick = {
                        val amount = amountString.toLongOrNull() ?: 0L
                        if (amount <= 0L) {
                            amountError = true
                            return@Button
                        }
                        onSave(
                            selectedType,
                            amount,
                            selectedCategory,
                            selectedDateMillis,
                            noteText.trim(),
                            selectedPhotoUri,
                            removeCurrentImage
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_transaction_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedType == "PEMASUKAN") IncomeGreen else ExpenseRed
                    )
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isEditing) "Simpan Perubahan" else "Catat Transaksi",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            selectedDateMillis = it
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Pilih")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Batal")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

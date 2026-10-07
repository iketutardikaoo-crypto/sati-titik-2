package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.TransactionEntity
import com.example.domain.model.CategoryRegistry
import com.example.domain.model.FilterPeriod
import com.example.domain.model.TransactionType
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.util.CurrencyUtils
import java.util.Calendar

@Composable
fun ReportScreen(
    transactions: List<TransactionEntity>,
    onExportCsv: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPeriod by remember { mutableStateOf(FilterPeriod.THIS_MONTH) }
    var activeTab by remember { mutableStateOf("EXPENSE") } // "EXPENSE" or "INCOME"

    val now = Calendar.getInstance()
    val periodTransactions = transactions.filter { item ->
        when (selectedPeriod) {
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

    val totalIncome = periodTransactions.filter { it.type == "PEMASUKAN" }.sumOf { it.amount }
    val totalExpense = periodTransactions.filter { it.type == "PENGELUARAN" }.sumOf { it.amount }
    val netCashFlow = totalIncome - totalExpense

    // Calculate category breakdowns
    val relevantTransactions = periodTransactions.filter {
        if (activeTab == "EXPENSE") it.type == "PENGELUARAN" else it.type == "PEMASUKAN"
    }
    val relevantTotal = if (activeTab == "EXPENSE") totalExpense else totalIncome

    val categoryMap = relevantTransactions.groupBy { it.category }
        .mapValues { (_, list) -> list.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("report_screen")
    ) {
        // Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Laporan & Analisis",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Ringkasan keuangan dan analisis per kategori",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Button(
                onClick = onExportCsv,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                modifier = Modifier.testTag("export_csv_report_button")
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("CSV", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Period filter chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterPeriod.values().forEach { period ->
                val isSelected = selectedPeriod == period
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedPeriod = period },
                    label = { Text(period.displayName, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmeraldPrimary,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.testTag("report_period_${period.name}")
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Cash Flow Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Arus Kas Periode Ini",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = IncomeGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Total Pemasukan", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                        Text(
                            text = CurrencyUtils.formatRupiah(totalIncome),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = IncomeGreen
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = ExpenseRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Total Pengeluaran", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                        Text(
                            text = CurrencyUtils.formatRupiah(totalExpense),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = ExpenseRed
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Ratio progress bar
                val totalBoth = (totalIncome + totalExpense).coerceAtLeast(1L)
                val incomeRatio = totalIncome.toFloat() / totalBoth.toFloat()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Rasio Masuk: ${(incomeRatio * 100).toInt()}%",
                        fontSize = 11.sp,
                        color = IncomeGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Rasio Keluar: ${((1f - incomeRatio) * 100).toInt()}%",
                        fontSize = 11.sp,
                        color = ExpenseRed,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(ExpenseRed.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(incomeRatio)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(IncomeGreen)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (netCashFlow >= 0) IncomeGreen.copy(alpha = 0.1f) else ExpenseRed.copy(alpha = 0.1f)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (netCashFlow >= 0) "Surplus / Bersih" else "Defisit / Kurang",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = CurrencyUtils.formatRupiah(netCashFlow),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (netCashFlow >= 0) IncomeGreen else ExpenseRed
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Breakdown Tabs (Pengeluaran vs Pemasukan)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val isExp = activeTab == "EXPENSE"
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isExp) ExpenseRed else Color.Transparent)
                    .clickable { activeTab = "EXPENSE" }
                    .padding(vertical = 10.dp)
                    .testTag("report_tab_expense"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Breakdown Pengeluaran",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (isExp) Color.White else MaterialTheme.colorScheme.onSurface
                )
            }

            val isInc = activeTab == "INCOME"
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isInc) IncomeGreen else Color.Transparent)
                    .clickable { activeTab = "INCOME" }
                    .padding(vertical = 10.dp)
                    .testTag("report_tab_income"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Breakdown Pemasukan",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (isInc) Color.White else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Breakdown items
        if (categoryMap.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Tidak ada data transaksi untuk kategori ini di periode yang dipilih.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    categoryMap.forEachIndexed { index, (catName, amount) ->
                        val percent = if (relevantTotal > 0) (amount.toFloat() / relevantTotal.toFloat()) else 0f
                        val transType = if (activeTab == "EXPENSE") TransactionType.PENGELUARAN else TransactionType.PEMASUKAN
                        val catInfo = CategoryRegistry.getCategoryInfo(catName, transType)

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(catInfo.color.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = catInfo.icon,
                                        contentDescription = null,
                                        tint = catInfo.color,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = catName,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = CurrencyUtils.formatRupiah(amount),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (activeTab == "EXPENSE") ExpenseRed else IncomeGreen
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        LinearProgressIndicator(
                                            progress = { percent },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = catInfo.color,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${(percent * 100).toInt()}%",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }

                            if (index < categoryMap.size - 1) {
                                Spacer(modifier = Modifier.height(14.dp))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

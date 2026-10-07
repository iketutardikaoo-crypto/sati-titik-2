package com.example.domain.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class TransactionType(val displayName: String) {
    PEMASUKAN("Pemasukan"),
    PENGELUARAN("Pengeluaran")
}

enum class FilterPeriod(val displayName: String) {
    ALL("Semua"),
    TODAY("Hari Ini"),
    THIS_WEEK("Minggu Ini"),
    THIS_MONTH("Bulan Ini")
}

data class TransactionCategory(
    val name: String,
    val type: TransactionType,
    val icon: ImageVector,
    val color: Color
)

object CategoryRegistry {
    val incomeCategories = listOf(
        TransactionCategory("Penjualan", TransactionType.PEMASUKAN, Icons.Default.Store, Color(0xFF10B981)),
        TransactionCategory("Gaji", TransactionType.PEMASUKAN, Icons.Default.Work, Color(0xFF059669)),
        TransactionCategory("Bonus & Komisi", TransactionType.PEMASUKAN, Icons.Default.CardGiftcard, Color(0xFF34D399)),
        TransactionCategory("Investasi", TransactionType.PEMASUKAN, Icons.Default.TrendingUp, Color(0xFF0D9488)),
        TransactionCategory("Piutang", TransactionType.PEMASUKAN, Icons.Default.AttachMoney, Color(0xFF0284C7)),
        TransactionCategory("Lain-lain", TransactionType.PEMASUKAN, Icons.Default.MoreHoriz, Color(0xFF64748B))
    )

    val expenseCategories = listOf(
        TransactionCategory("Belanja Stok", TransactionType.PENGELUARAN, Icons.Default.ShoppingBag, Color(0xFFEF4444)),
        TransactionCategory("Operasional", TransactionType.PENGELUARAN, Icons.Default.Receipt, Color(0xFFF97316)),
        TransactionCategory("Makan & Minum", TransactionType.PENGELUARAN, Icons.Default.Fastfood, Color(0xFFEAB308)),
        TransactionCategory("Transportasi", TransactionType.PENGELUARAN, Icons.Default.DirectionsCar, Color(0xFF8B5CF6)),
        TransactionCategory("Listrik & Tagihan", TransactionType.PENGELUARAN, Icons.Default.Lightbulb, Color(0xFFEC4899)),
        TransactionCategory("Gaji Karyawan", TransactionType.PENGELUARAN, Icons.Default.AccountBalance, Color(0xFF6366F1)),
        TransactionCategory("Lain-lain", TransactionType.PENGELUARAN, Icons.Default.MoreHoriz, Color(0xFF64748B))
    )

    fun getCategoryInfo(name: String, type: TransactionType): TransactionCategory {
        val list = if (type == TransactionType.PEMASUKAN) incomeCategories else expenseCategories
        return list.firstOrNull { it.name.equals(name, ignoreCase = true) }
            ?: (incomeCategories + expenseCategories).firstOrNull { it.name.equals(name, ignoreCase = true) }
            ?: TransactionCategory(name, type, Icons.Default.Receipt, if (type == TransactionType.PEMASUKAN) Color(0xFF10B981) else Color(0xFFEF4444))
    }
}

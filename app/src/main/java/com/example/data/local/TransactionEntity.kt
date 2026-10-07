package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // "PEMASUKAN" or "PENGELUARAN"
    val amount: Long,
    val category: String,
    val dateMillis: Long,
    val note: String,
    val imagePath: String? = null, // Path to receipt / invoice photo
    val isSynced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

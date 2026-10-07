package com.example.data.repository

import com.example.data.local.TransactionDao
import com.example.data.local.TransactionEntity
import kotlinx.coroutines.flow.Flow

class TransactionRepository(private val dao: TransactionDao) {
    val allTransactions: Flow<List<TransactionEntity>> = dao.getAllTransactions()

    suspend fun getTransactionById(id: Long): TransactionEntity? = dao.getTransactionById(id)

    suspend fun insertTransaction(transaction: TransactionEntity): Long = dao.insertTransaction(transaction)

    suspend fun insertAll(transactions: List<TransactionEntity>) = dao.insertAll(transactions)

    suspend fun updateTransaction(transaction: TransactionEntity) = dao.updateTransaction(transaction)

    suspend fun deleteTransaction(transaction: TransactionEntity) = dao.deleteTransaction(transaction)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun deleteAll() = dao.deleteAll()

    suspend fun markAllAsSynced() = dao.markAllAsSynced()
}

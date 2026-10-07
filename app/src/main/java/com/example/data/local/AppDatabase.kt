package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [TransactionEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pembukuan_database.db"
                )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Pre-populate with realistic initial sample records
                        CoroutineScope(Dispatchers.IO).launch {
                            val dao = getDatabase(context).transactionDao()
                            val now = System.currentTimeMillis()
                            val oneDay = 86400000L
                            dao.insertAll(
                                listOf(
                                    TransactionEntity(
                                        type = "PEMASUKAN",
                                        amount = 3500000L,
                                        category = "Penjualan",
                                        dateMillis = now - (oneDay * 2),
                                        note = "Penjualan paket produk grosir Toko Berkah",
                                        isSynced = true
                                    ),
                                    TransactionEntity(
                                        type = "PENGELUARAN",
                                        amount = 450000L,
                                        category = "Belanja Stok",
                                        dateMillis = now - (oneDay * 2) + 3600000L,
                                        note = "Restok bahan baku kemasan dan kantong kresek",
                                        isSynced = true
                                    ),
                                    TransactionEntity(
                                        type = "PEMASUKAN",
                                        amount = 1750000L,
                                        category = "Penjualan",
                                        dateMillis = now - oneDay,
                                        note = "Order online marketplace & cash register",
                                        isSynced = true
                                    ),
                                    TransactionEntity(
                                        type = "PENGELUARAN",
                                        amount = 185000L,
                                        category = "Makan & Minum",
                                        dateMillis = now - oneDay + 7200000L,
                                        note = "Makan siang tim dan minuman operasional",
                                        isSynced = true
                                    ),
                                    TransactionEntity(
                                        type = "PENGELUARAN",
                                        amount = 320000L,
                                        category = "Listrik & Tagihan",
                                        dateMillis = now,
                                        note = "Token listrik PLN toko & kuota internet Wi-Fi",
                                        isSynced = false
                                    )
                                )
                            )
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

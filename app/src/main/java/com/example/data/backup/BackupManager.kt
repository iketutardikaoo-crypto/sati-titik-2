package com.example.data.backup

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.local.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ServerStatus(
    val isConnected: Boolean,
    val connectionType: String, // "Wi-Fi", "Seluler", "Tidak ada koneksi"
    val serverOnline: Boolean,
    val pingMs: Long,
    val message: String,
    val lastChecked: Long = System.currentTimeMillis()
)

data class BackupMetadata(
    val totalTransactions: Int,
    val totalAmountIncome: Long,
    val totalAmountExpense: Long,
    val timestamp: Long,
    val filePath: String
)

class BackupManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("pembukuan_backup_prefs", Context.MODE_PRIVATE)

    var isAutoBackupEnabled: Boolean
        get() = prefs.getBoolean("auto_backup_enabled", false)
        set(value) = prefs.edit().putBoolean("auto_backup_enabled", value).apply()

    var backupInterval: String // "DAILY", "WEEKLY"
        get() = prefs.getString("backup_interval", "WEEKLY") ?: "WEEKLY"
        set(value) = prefs.edit().putString("backup_interval", value).apply()

    var lastBackupTime: Long
        get() = prefs.getLong("last_backup_time", 0L)
        set(value) = prefs.edit().putLong("last_backup_time", value).apply()

    suspend fun exportToJson(transactions: List<TransactionEntity>): Result<File> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject()
            root.put("version", 3)
            root.put("appName", "BukuKas_PembukuanAndroid")
            root.put("exportedAt", System.currentTimeMillis())
            root.put("count", transactions.size)

            val array = JSONArray()
            for (t in transactions) {
                val item = JSONObject()
                item.put("id", t.id)
                item.put("type", t.type)
                item.put("amount", t.amount)
                item.put("category", t.category)
                item.put("dateMillis", t.dateMillis)
                item.put("note", t.note)
                item.put("hasImage", t.imagePath != null)
                item.put("imagePath", t.imagePath ?: "")
                item.put("isSynced", t.isSynced)
                array.put(item)
            }
            root.put("transactions", array)

            val backupsDir = File(context.filesDir, "backups")
            if (!backupsDir.exists()) backupsDir.mkdirs()

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(backupsDir, "backup_pembukuan_$timestamp.json")
            file.writeText(root.toString(2))

            lastBackupTime = System.currentTimeMillis()
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportToCsv(transactions: List<TransactionEntity>): Result<File> = withContext(Dispatchers.IO) {
        try {
            val sb = java.lang.StringBuilder()
            sb.append("ID,Tipe,Kategori,Nominal (Rp),Tanggal,Keterangan,Ada Nota,Status Sinkron\n")

            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            for (t in transactions) {
                val dateStr = sdf.format(Date(t.dateMillis))
                val safeNote = t.note.replace("\"", "\"\"").replace("\n", " ")
                sb.append("${t.id},")
                sb.append("${t.type},")
                sb.append("\"${t.category}\",")
                sb.append("${t.amount},")
                sb.append("\"$dateStr\",")
                sb.append("\"$safeNote\",")
                sb.append(if (t.imagePath != null) "Ya" else "Tidak").append(",")
                sb.append(if (t.isSynced) "Tersinkron" else "Lokal").append("\n")
            }

            val exportsDir = File(context.filesDir, "exports")
            if (!exportsDir.exists()) exportsDir.mkdirs()

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(exportsDir, "laporan_keuangan_$timestamp.csv")
            file.writeText(sb.toString())

            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun parseJsonBackup(jsonString: String): Result<List<TransactionEntity>> = withContext(Dispatchers.Default) {
        try {
            val root = JSONObject(jsonString)
            val array = root.getJSONArray("transactions")
            val list = mutableListOf<TransactionEntity>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    TransactionEntity(
                        id = 0, // auto generate on restore
                        type = obj.getString("type"),
                        amount = obj.getLong("amount"),
                        category = obj.getString("category"),
                        dateMillis = obj.getLong("dateMillis"),
                        note = obj.optString("note", ""),
                        imagePath = if (obj.optString("imagePath", "").isNotBlank()) obj.optString("imagePath") else null,
                        isSynced = true
                    )
                )
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getBackupFiles(): List<File> {
        val backupsDir = File(context.filesDir, "backups")
        if (!backupsDir.exists()) return emptyList()
        return backupsDir.listFiles { file -> file.extension == "json" }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    suspend fun checkServerOnlineStatus(): ServerStatus = withContext(Dispatchers.IO) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(activeNetwork)

        val isConnected = caps != null && (
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        )

        val connectionType = when {
            caps == null -> "Tidak Terhubung"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi Aktif"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Data Seluler Aktif"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Terhubung"
        }

        if (!isConnected) {
            return@withContext ServerStatus(
                isConnected = false,
                connectionType = connectionType,
                serverOnline = false,
                pingMs = -1L,
                message = "Perangkat sedang offline atau tidak ada akses internet."
            )
        }

        // Check real ping to a standard reliable DNS/HTTP health endpoint
        val startTime = System.currentTimeMillis()
        var serverOnline = false
        var pingMs = -1L
        var message = ""

        try {
            val url = URL("https://www.google.com/generate_204")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            conn.requestMethod = "GET"
            conn.instanceFollowRedirects = false
            conn.connect()
            val code = conn.responseCode
            pingMs = System.currentTimeMillis() - startTime
            serverOnline = (code in 200..299)
            conn.disconnect()

            message = if (serverOnline) {
                "Server Online dan siap untuk sinkronisasi cloud data (Latensi: ${pingMs}ms)"
            } else {
                "Koneksi terhubung namun gateway server merespons kode $code"
            }
        } catch (e: Exception) {
            pingMs = System.currentTimeMillis() - startTime
            serverOnline = false
            message = "Gagal menjangkau server sinkronisasi: ${e.localizedMessage ?: "Timeout"}"
        }

        ServerStatus(
            isConnected = true,
            connectionType = connectionType,
            serverOnline = serverOnline,
            pingMs = pingMs,
            message = message
        )
    }
}

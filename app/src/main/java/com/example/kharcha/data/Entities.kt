package com.example.kharcha.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "entries")
data class Entry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entryDate: String,      // yyyy-MM-dd
    val amount: Double,
    val category: String,
    val item: String,
    val tag: String,
    val note: String,
    val originalText: String,   // jo user ne bola ya likha
    val source: String,         // voice | text
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "audit_log")
data class AuditLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entryId: Long,
    val action: String,         // created | edited | deleted
    val field: String,
    val oldValue: String?,
    val newValue: String?,
    val changedAt: Long
)

@Dao
interface EntryDao {
    @Insert
    suspend fun insert(entry: Entry): Long

    @Update
    suspend fun update(entry: Entry)

    @Query("DELETE FROM entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM entries ORDER BY entryDate DESC, createdAt DESC")
    fun observeAll(): Flow<List<Entry>>

    @Query("SELECT * FROM entries ORDER BY entryDate ASC")
    suspend fun getAll(): List<Entry>

    @Query("SELECT * FROM entries WHERE entryDate BETWEEN :from AND :to ORDER BY entryDate DESC, createdAt DESC")
    fun observeRange(from: String, to: String): Flow<List<Entry>>

    @Insert
    suspend fun insertAudit(log: AuditLog)

    @Query("SELECT * FROM audit_log WHERE entryId = :entryId ORDER BY changedAt DESC")
    fun observeAudit(entryId: Long): Flow<List<AuditLog>>
}

@Database(entities = [Entry::class, AuditLog::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() {

    abstract fun dao(): EntryDao

    companion object {
        @Volatile
        private var instance: AppDb? = null

        fun get(context: Context): AppDb =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDb::class.java,
                    "kharcha.db"
                ).build().also { instance = it }
            }
    }
}

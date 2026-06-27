package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "transactions")
data class DbTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val senderName: String,
    val recipientName: String,
    val recipientUpiId: String,
    val amount: Double,
    val note: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String, // "SUCCESS", "FAILED", "PENDING"
    val category: String // "Recharge", "Electricity", "Transfer", "Food", "Shopping", "Salary", "Investment", "Water", "Gas", "FASTag", "Broadband", "DTH", "Insurance"
)

@Entity(tableName = "user_profile")
data class DbUserProfile(
    @PrimaryKey val upiId: String,
    val name: String,
    val balance: Double,
    val mobileNumber: String,
    val bankName: String,
    val bankAccountNumber: String,
    val isBiometricEnabled: Boolean = false
)

@Entity(tableName = "favorite_contacts")
data class DbFavoriteContact(
    @PrimaryKey val upiId: String,
    val name: String,
    val avatarColorHex: String,
    val isFavorite: Boolean = true
)

@Dao
interface PayMitraDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<DbTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: DbTransaction)

    @Query("SELECT * FROM user_profile LIMIT 1")
    fun getUserProfile(): Flow<DbUserProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserProfile(profile: DbUserProfile)

    @Query("UPDATE user_profile SET balance = :newBalance WHERE upiId = :upiId")
    suspend fun updateBalance(upiId: String, newBalance: Double)

    @Query("SELECT * FROM favorite_contacts")
    fun getFavoriteContacts(): Flow<List<DbFavoriteContact>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: DbFavoriteContact)
}

@Database(entities = [DbTransaction::class, DbUserProfile::class, DbFavoriteContact::class], version = 1, exportSchema = false)
abstract class PayMitraDatabase : RoomDatabase() {
    abstract fun payMitraDao(): PayMitraDao
}

class PayMitraRepository(private val dao: PayMitraDao) {
    val allTransactions: Flow<List<DbTransaction>> = dao.getAllTransactions()
    val userProfile: Flow<DbUserProfile?> = dao.getUserProfile()
    val favoriteContacts: Flow<List<DbFavoriteContact>> = dao.getFavoriteContacts()

    suspend fun insertTransaction(tx: DbTransaction) = dao.insertTransaction(tx)
    suspend fun saveUserProfile(profile: DbUserProfile) = dao.saveUserProfile(profile)
    suspend fun updateBalance(upiId: String, balance: Double) = dao.updateBalance(upiId, balance)
    suspend fun insertContact(contact: DbFavoriteContact) = dao.insertContact(contact)
}

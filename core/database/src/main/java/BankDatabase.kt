import androidx.room.Database
import androidx.room.RoomDatabase

import dao.BankDao
import entity.AccountsEntity
import entity.BankEntity
import entity.CardEntity

/**
 * Room Database for the BankApp.
 * 
 * Includes all entities and DAOs.
 * Version should be incremented when schema changes.
 */
@Database(
    entities = [BankEntity::class, AccountsEntity::class, CardEntity::class],
    version = 1,
    exportSchema = true
)
abstract class BankDatabase : RoomDatabase() {

    abstract fun bankDao(): BankDao

    companion object {
        const val DATABASE_NAME = "bank_app_database"
    }
}




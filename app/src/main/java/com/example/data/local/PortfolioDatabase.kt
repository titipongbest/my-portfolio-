package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.model.Holding

@Database(
    entities = [Holding::class],
    version = 1,
    exportSchema = false
)
abstract class PortfolioDatabase : RoomDatabase() {
    abstract fun holdingDao(): HoldingDao

    companion object {
        @Volatile
        private var INSTANCE: PortfolioDatabase? = null

        fun getInstance(context: Context): PortfolioDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PortfolioDatabase::class.java,
                    "portpulse_local.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

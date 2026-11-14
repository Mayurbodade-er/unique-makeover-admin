package com.uniquemakeover.admin.core.database

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import javax.inject.Inject
import javax.inject.Singleton

private const val RATE_HAIRCUT = 150
private const val RATE_THREADING = 50
private const val RATE_GOLD_FACIAL = 800
private const val RATE_KERATIN_SPA = 1500
private const val RATE_MANICURE = 400
private const val RATE_PEDICURE = 500
private const val RATE_BRIDAL_PACKAGE = 5000

@Singleton
class SeedCallback @Inject constructor() : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        seedInitialServices(db)
    }

    private fun seedInitialServices(db: SupportSQLiteDatabase) {
        val seedServices = listOf(
            Triple("s1", "Haircut", RATE_HAIRCUT),
            Triple("s2", "Threading", RATE_THREADING),
            Triple("s3", "Gold Facial", RATE_GOLD_FACIAL),
            Triple("s4", "Keratin Spa", RATE_KERATIN_SPA),
            Triple("s5", "Manicure", RATE_MANICURE),
            Triple("s6", "Pedicure", RATE_PEDICURE),
            Triple("s7", "Bridal Package", RATE_BRIDAL_PACKAGE)
        )

        db.beginTransaction()
        try {
            seedServices.forEachIndexed { index, (id, name, rate) ->
                val sortOrder = index + 1
                val sql = "INSERT INTO services (id, name, rate, imagePath, sortOrder) " +
                    "VALUES ('$id', '$name', $rate, NULL, $sortOrder);"
                db.execSQL(sql)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}

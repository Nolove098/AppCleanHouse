package com.example.appcleanhouse.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class BookingDbHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE_BOOKINGS)
        db.execSQL(CREATE_TABLE_SERVICES)
        db.execSQL(CREATE_TABLE_CLEANERS)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            migrateV1ToV2(db)
        }
        if (oldVersion < 3) {
            migrateV2ToV3(db)
        }
    }

    private fun migrateV1ToV2(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE_SERVICES)
        db.execSQL(CREATE_TABLE_CLEANERS)
    }

    private fun migrateV2ToV3(db: SQLiteDatabase) {
        db.execSQL("ALTER TABLE $TABLE_SERVICES ADD COLUMN $SVC_COL_UPDATED_AT INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE $TABLE_CLEANERS ADD COLUMN $CLR_COL_UPDATED_AT INTEGER NOT NULL DEFAULT 0")
    }

    companion object {
        const val DATABASE_NAME = "app_clean_house.db"
        const val DATABASE_VERSION = 3

        const val TABLE_BOOKINGS = "bookings"
        const val COL_ID = "id"
        const val COL_USER_ID = "user_id"
        const val COL_SERVICE_ID = "service_id"
        const val COL_CLEANER_ID = "cleaner_id"
        const val COL_CLEANER_NAME = "cleaner_name"
        const val COL_DATE = "date"
        const val COL_TIME = "time"
        const val COL_STATUS = "status"
        const val COL_TOTAL_PRICE = "total_price"
        const val COL_ADDRESS = "address"
        const val COL_RATING = "rating"
        const val COL_TIMESTAMP = "timestamp"

        const val TABLE_SERVICES = "services"
        const val SVC_COL_ID = "id"
        const val SVC_COL_NAME = "name"
        const val SVC_COL_DESCRIPTION = "description"
        const val SVC_COL_PRICE_PER_HOUR = "price_per_hour"
        const val SVC_COL_RATING = "rating"
        const val SVC_COL_UPDATED_AT = "updated_at"

        const val TABLE_CLEANERS = "cleaners"
        const val CLR_COL_ID = "id"
        const val CLR_COL_NAME = "name"
        const val CLR_COL_RATING = "rating"
        const val CLR_COL_JOB_COUNT = "job_count"
        const val CLR_COL_SPECIALTY = "specialty"
        const val CLR_COL_EXPERIENCE = "experience"
        const val CLR_COL_ABOUT = "about"
        const val CLR_COL_PRICE_PER_HOUR = "price_per_hour"
        const val CLR_COL_DISTANCE_KM = "distance_km"
        const val CLR_COL_UPDATED_AT = "updated_at"

        private val CREATE_TABLE_BOOKINGS = """
            CREATE TABLE IF NOT EXISTS $TABLE_BOOKINGS (
                $COL_ID TEXT PRIMARY KEY,
                $COL_USER_ID TEXT NOT NULL,
                $COL_SERVICE_ID TEXT NOT NULL,
                $COL_CLEANER_ID TEXT NOT NULL,
                $COL_CLEANER_NAME TEXT NOT NULL,
                $COL_DATE TEXT NOT NULL,
                $COL_TIME TEXT NOT NULL,
                $COL_STATUS TEXT NOT NULL,
                $COL_TOTAL_PRICE REAL NOT NULL,
                $COL_ADDRESS TEXT NOT NULL,
                $COL_RATING INTEGER,
                $COL_TIMESTAMP INTEGER NOT NULL
            )
        """.trimIndent()

        private val CREATE_TABLE_SERVICES = """
            CREATE TABLE IF NOT EXISTS $TABLE_SERVICES (
                $SVC_COL_ID TEXT PRIMARY KEY,
                $SVC_COL_NAME TEXT NOT NULL,
                $SVC_COL_DESCRIPTION TEXT NOT NULL,
                $SVC_COL_PRICE_PER_HOUR INTEGER NOT NULL,
                $SVC_COL_RATING REAL NOT NULL,
                $SVC_COL_UPDATED_AT INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent()

        private val CREATE_TABLE_CLEANERS = """
            CREATE TABLE IF NOT EXISTS $TABLE_CLEANERS (
                $CLR_COL_ID TEXT PRIMARY KEY,
                $CLR_COL_NAME TEXT NOT NULL,
                $CLR_COL_RATING REAL NOT NULL,
                $CLR_COL_JOB_COUNT INTEGER NOT NULL,
                $CLR_COL_SPECIALTY TEXT NOT NULL,
                $CLR_COL_EXPERIENCE TEXT NOT NULL,
                $CLR_COL_ABOUT TEXT NOT NULL,
                $CLR_COL_PRICE_PER_HOUR INTEGER NOT NULL,
                $CLR_COL_DISTANCE_KM REAL NOT NULL,
                $CLR_COL_UPDATED_AT INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent()
    }
}

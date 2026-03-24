package com.example.appcleanhouse.local

import android.content.ContentValues
import android.content.Context
import com.example.appcleanhouse.models.Cleaner
import com.example.appcleanhouse.models.Order
import com.example.appcleanhouse.models.Service

class LocalBookingDataSource(context: Context) {

    private val dbHelper = BookingDbHelper(context.applicationContext)
    private val appContext = context.applicationContext

    fun upsertOrders(orders: List<Order>) {
        if (orders.isEmpty()) return

        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            orders.forEach { order ->
                db.insertWithOnConflict(
                    BookingDbHelper.TABLE_BOOKINGS,
                    null,
                    order.toContentValues(),
                    android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getOrdersByUser(userId: String): List<Order> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            BookingDbHelper.TABLE_BOOKINGS,
            null,
            "${BookingDbHelper.COL_USER_ID} = ?",
            arrayOf(userId),
            null,
            null,
            "${BookingDbHelper.COL_TIMESTAMP} DESC"
        )

        return cursor.use {
            val orders = mutableListOf<Order>()
            while (it.moveToNext()) {
                orders += Order(
                    id = it.getString(it.getColumnIndexOrThrow(BookingDbHelper.COL_ID)),
                    userId = it.getString(it.getColumnIndexOrThrow(BookingDbHelper.COL_USER_ID)),
                    serviceId = it.getString(it.getColumnIndexOrThrow(BookingDbHelper.COL_SERVICE_ID)),
                    cleanerId = it.getString(it.getColumnIndexOrThrow(BookingDbHelper.COL_CLEANER_ID)),
                    cleanerName = it.getString(it.getColumnIndexOrThrow(BookingDbHelper.COL_CLEANER_NAME)),
                    date = it.getString(it.getColumnIndexOrThrow(BookingDbHelper.COL_DATE)),
                    time = it.getString(it.getColumnIndexOrThrow(BookingDbHelper.COL_TIME)),
                    status = it.getString(it.getColumnIndexOrThrow(BookingDbHelper.COL_STATUS)),
                    totalPrice = it.getDouble(it.getColumnIndexOrThrow(BookingDbHelper.COL_TOTAL_PRICE)),
                    address = it.getString(it.getColumnIndexOrThrow(BookingDbHelper.COL_ADDRESS)),
                    rating = if (it.isNull(it.getColumnIndexOrThrow(BookingDbHelper.COL_RATING))) {
                        null
                    } else {
                        it.getInt(it.getColumnIndexOrThrow(BookingDbHelper.COL_RATING))
                    },
                    timestamp = it.getLong(it.getColumnIndexOrThrow(BookingDbHelper.COL_TIMESTAMP))
                )
            }
            orders
        }
    }

    fun updateOrderService(orderId: String, serviceId: String, totalPrice: Double) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(BookingDbHelper.COL_SERVICE_ID, serviceId)
            put(BookingDbHelper.COL_TOTAL_PRICE, totalPrice)
        }
        db.update(
            BookingDbHelper.TABLE_BOOKINGS,
            values,
            "${BookingDbHelper.COL_ID} = ?",
            arrayOf(orderId)
        )
    }

    fun updateOrderStatus(orderId: String, newStatus: String) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(BookingDbHelper.COL_STATUS, newStatus)
        }
        db.update(
            BookingDbHelper.TABLE_BOOKINGS,
            values,
            "${BookingDbHelper.COL_ID} = ?",
            arrayOf(orderId)
        )
    }

    fun upsertServices(services: List<Service>) {
        if (services.isEmpty()) return

        val now = System.currentTimeMillis()
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            services.forEach { service ->
                db.insertWithOnConflict(
                    BookingDbHelper.TABLE_SERVICES,
                    null,
                    service.toContentValues(now),
                    android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        saveLastReferenceSyncAt(now)
    }

    fun getServicesMap(): Map<String, Service> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            BookingDbHelper.TABLE_SERVICES,
            null,
            null,
            null,
            null,
            null,
            BookingDbHelper.SVC_COL_NAME
        )

        return cursor.use {
            val map = linkedMapOf<String, Service>()
            while (it.moveToNext()) {
                val service = Service(
                    id = it.getString(it.getColumnIndexOrThrow(BookingDbHelper.SVC_COL_ID)),
                    name = it.getString(it.getColumnIndexOrThrow(BookingDbHelper.SVC_COL_NAME)),
                    description = it.getString(it.getColumnIndexOrThrow(BookingDbHelper.SVC_COL_DESCRIPTION)),
                    pricePerHour = it.getInt(it.getColumnIndexOrThrow(BookingDbHelper.SVC_COL_PRICE_PER_HOUR)),
                    rating = it.getDouble(it.getColumnIndexOrThrow(BookingDbHelper.SVC_COL_RATING))
                )
                map[service.id] = service
            }
            map
        }
    }

    fun upsertCleaners(cleaners: List<Cleaner>) {
        if (cleaners.isEmpty()) return

        val now = System.currentTimeMillis()
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            cleaners.forEach { cleaner ->
                db.insertWithOnConflict(
                    BookingDbHelper.TABLE_CLEANERS,
                    null,
                    cleaner.toContentValues(now),
                    android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        saveLastReferenceSyncAt(now)
    }

    fun getLastReferenceSyncAt(): Long {
        return appContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_REFERENCE_SYNC_AT, 0L)
    }

    private fun saveLastReferenceSyncAt(timestamp: Long) {
        appContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_REFERENCE_SYNC_AT, timestamp)
            .apply()
    }

    fun getCleanersMap(): Map<String, Cleaner> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            BookingDbHelper.TABLE_CLEANERS,
            null,
            null,
            null,
            null,
            null,
            BookingDbHelper.CLR_COL_NAME
        )

        return cursor.use {
            val map = linkedMapOf<String, Cleaner>()
            while (it.moveToNext()) {
                val cleaner = Cleaner(
                    id = it.getString(it.getColumnIndexOrThrow(BookingDbHelper.CLR_COL_ID)),
                    name = it.getString(it.getColumnIndexOrThrow(BookingDbHelper.CLR_COL_NAME)),
                    rating = it.getDouble(it.getColumnIndexOrThrow(BookingDbHelper.CLR_COL_RATING)),
                    jobCount = it.getInt(it.getColumnIndexOrThrow(BookingDbHelper.CLR_COL_JOB_COUNT)),
                    specialty = it.getString(it.getColumnIndexOrThrow(BookingDbHelper.CLR_COL_SPECIALTY)),
                    experience = it.getString(it.getColumnIndexOrThrow(BookingDbHelper.CLR_COL_EXPERIENCE)),
                    about = it.getString(it.getColumnIndexOrThrow(BookingDbHelper.CLR_COL_ABOUT)),
                    pricePerHour = it.getInt(it.getColumnIndexOrThrow(BookingDbHelper.CLR_COL_PRICE_PER_HOUR)),
                    distanceKm = it.getDouble(it.getColumnIndexOrThrow(BookingDbHelper.CLR_COL_DISTANCE_KM))
                )
                map[cleaner.id] = cleaner
            }
            map
        }
    }

    private fun Order.toContentValues(): ContentValues = ContentValues().apply {
        put(BookingDbHelper.COL_ID, id)
        put(BookingDbHelper.COL_USER_ID, userId)
        put(BookingDbHelper.COL_SERVICE_ID, serviceId)
        put(BookingDbHelper.COL_CLEANER_ID, cleanerId)
        put(BookingDbHelper.COL_CLEANER_NAME, cleanerName)
        put(BookingDbHelper.COL_DATE, date)
        put(BookingDbHelper.COL_TIME, time)
        put(BookingDbHelper.COL_STATUS, status)
        put(BookingDbHelper.COL_TOTAL_PRICE, totalPrice)
        put(BookingDbHelper.COL_ADDRESS, address)
        if (rating != null) {
            put(BookingDbHelper.COL_RATING, rating)
        } else {
            putNull(BookingDbHelper.COL_RATING)
        }
        put(BookingDbHelper.COL_TIMESTAMP, timestamp)
    }

    private fun Service.toContentValues(updatedAt: Long): ContentValues = ContentValues().apply {
        put(BookingDbHelper.SVC_COL_ID, id)
        put(BookingDbHelper.SVC_COL_NAME, name)
        put(BookingDbHelper.SVC_COL_DESCRIPTION, description)
        put(BookingDbHelper.SVC_COL_PRICE_PER_HOUR, pricePerHour)
        put(BookingDbHelper.SVC_COL_RATING, rating)
        put(BookingDbHelper.SVC_COL_UPDATED_AT, updatedAt)
    }

    private fun Cleaner.toContentValues(updatedAt: Long): ContentValues = ContentValues().apply {
        put(BookingDbHelper.CLR_COL_ID, id)
        put(BookingDbHelper.CLR_COL_NAME, name)
        put(BookingDbHelper.CLR_COL_RATING, rating)
        put(BookingDbHelper.CLR_COL_JOB_COUNT, jobCount)
        put(BookingDbHelper.CLR_COL_SPECIALTY, specialty)
        put(BookingDbHelper.CLR_COL_EXPERIENCE, experience)
        put(BookingDbHelper.CLR_COL_ABOUT, about)
        put(BookingDbHelper.CLR_COL_PRICE_PER_HOUR, pricePerHour)
        put(BookingDbHelper.CLR_COL_DISTANCE_KM, distanceKm)
        put(BookingDbHelper.CLR_COL_UPDATED_AT, updatedAt)
    }

    companion object {
        private const val PREFS_NAME = "local_cache_meta"
        private const val KEY_LAST_REFERENCE_SYNC_AT = "last_reference_sync_at"
    }
}

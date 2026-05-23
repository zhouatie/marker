package com.atie.marker.phone

import android.app.Application
import androidx.room.Room
import com.atie.marker.phone.data.PhoneMarkerDatabase
import com.atie.marker.phone.data.PhoneMarkerRepository

class MarkerPhoneApp : Application() {
    val database: PhoneMarkerDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            PhoneMarkerDatabase::class.java,
            "marker-phone.db",
        )
            .addMigrations(PhoneMarkerDatabase.MIGRATION_1_2)
            .build()
    }

    val markerRepository: PhoneMarkerRepository by lazy {
        PhoneMarkerRepository(
            markerDao = database.markerDao(),
            labelDao = database.intervalLabelDao(),
            deletionDao = database.markerDeletionDao(),
        )
    }
}

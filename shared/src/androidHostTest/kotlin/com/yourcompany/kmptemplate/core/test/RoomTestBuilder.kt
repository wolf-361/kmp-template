package com.yourcompany.kmptemplate.core.test

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.yourcompany.kmptemplate.core.data.db.AppDatabase

fun createInMemoryDatabase(): AppDatabase =
    Room.inMemoryDatabaseBuilder<AppDatabase>(
        context = ApplicationProvider.getApplicationContext(),
    )
        .allowMainThreadQueries()
        .build()

package com.yourcompany.kmptemplate.core.data.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

// TODO: Replace PlaceholderEntity with your actual @Entity classes
@Database(entities = [PlaceholderEntity::class], version = 1)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase()

@Suppress("NO_ACTUAL_FOR_EXPECT", "ABSTRACT_MEMBER_NOT_IMPLEMENTED")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase>

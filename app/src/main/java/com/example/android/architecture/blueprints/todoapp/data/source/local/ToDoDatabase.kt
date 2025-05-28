/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.architecture.blueprints.todoapp.data.source.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * The Room Database that contains the Task table.
 *
 * Note that exportSchema should be true in production databases.
 */
@Database(entities = [LocalTask::class], version = 4) // Increment version
abstract class ToDoDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: ToDoDatabase? = null

        fun getInstance(context: Context): ToDoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ToDoDatabase::class.java,
                    "task"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4) // Add both migrations
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE task ADD COLUMN conductivity REAL NOT NULL DEFAULT 0.0")

        db.execSQL("ALTER TABLE task ADD COLUMN densityAndHeatCapacity REAL NOT NULL DEFAULT 0.0")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE task ADD COLUMN geometryFile TEXT")
        db.execSQL("ALTER TABLE task ADD COLUMN surfaceTempFile TEXT")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create a new table with the updated schema (without densityAndHeatCapacity, with radius and depth)
        database.execSQL("""
            CREATE TABLE task_new (
                id TEXT NOT NULL,
                title TEXT NOT NULL,
                description TEXT NOT NULL,
                conductivity REAL NOT NULL,
                radius REAL NOT NULL DEFAULT 0.0,
                depth REAL NOT NULL DEFAULT 0.0,
                geometryFile TEXT,
                surfaceTempFile TEXT,
                isCompleted INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(id)
            )
        """)
        // Copy data from the old table to the new table, excluding densityAndHeatCapacity
        database.execSQL("""
            INSERT INTO task_new (id, title, description, conductivity, geometryFile, surfaceTempFile, isCompleted)
            SELECT id, title, description, conductivity, geometryFile, surfaceTempFile, isCompleted
            FROM task
        """)
        // Drop the old table
        database.execSQL("DROP TABLE task")
        // Rename the new table to the original name
        database.execSQL("ALTER TABLE task_new RENAME TO task")
    }
}

package com.example.waltest

import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private val db by lazy {
        SQLiteDatabase.openDatabase(
            getDatabasePath("test.db").path,
            null,
            SQLiteDatabase.CREATE_IF_NECESSARY or SQLiteDatabase.ENABLE_WRITE_AHEAD_LOGGING,
        )
    }
    private val reader = Executors.newSingleThreadScheduledExecutor()
    private val writer = Executors.newSingleThreadScheduledExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dropTable()
        createTable()
        scheduleWork()
    }

    private fun scheduleWork() {
        writer.scheduleAtFixedRate({
            // Small delay to make sure reading is started when `beginTransactionNonExclusive` was already called
            reader.schedule({
                println("WAL read start")
                fastReadQuery()
                println("WAL read end")
            }, 10, TimeUnit.MILLISECONDS)

            println("WAL write start")
            db.beginTransactionNonExclusive()
            slowWriteQuery()
            db.setTransactionSuccessful()
            db.endTransaction()
            println("WAL write end")
        }, 0, 5, TimeUnit.SECONDS)
    }

    private fun dropTable() {
        val query = "drop table if exists employees;"
        db.execSQL(query)
    }

    // Add or remove "-- comment" in this query to see the difference.
    // Without the comment, the reading occurs concurrently with writing and finishes in 0-1ms.
    // With the comment, the reading always finishes after the writing is done. (See the Logcat)
    private fun fastReadQuery() {
        val query = """
            -- comment
            select * from employees limit 1;
            """.trimIndent()

        db.rawQuery(query, emptyArray()).use {
            if (it.moveToFirst()) {
                println("WAL " + it.getString(it.getColumnIndexOrThrow("name")))
            }
        }
    }

    private fun slowWriteQuery() {
//        val query = """
//            WITH RECURSIVE r(i) AS (
//            VALUES(0)
//            UNION ALL
//            SELECT i FROM r
//            LIMIT 5000000
//          )
//          SELECT i FROM r WHERE i = 1;
//        """.trimIndent()
//        db.execSQL(query)

        repeat(20000) {
            val insertQuery = "insert into employees(name) values('Employee name');"
            db.execSQL(insertQuery, emptyArray())
        }
    }

    private fun createTable() {
        val createTable =
            "create table if not exists employees(lol integer PRIMARY KEY autoincrement, name text);"

        db.execSQL(createTable)
    }
}
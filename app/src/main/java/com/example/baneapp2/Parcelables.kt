package com.example.baneapp2

import android.os.Parcelable
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteOpenHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.parcelize.Parcelize
import java.sql.Time
import java.util.Calendar

@Parcelize
data class User(
    var token: String?,
    var name: String,
    var num: String,
    var id: String,
    var refresh_token: String?
    ) : Parcelable {
        constructor() : this(null, "", "", "", null)
    }

@Entity
data class Person(
    @PrimaryKey
    val id: String,
    val name: String,
    val num: String,
    val image: String,
    val lastAccess: Calendar
)

@Dao
interface PersonDao {
    @Query("select * from person where id = :id")
    fun personById(id: String): Person

    @Query("select * from person order by name asc")
    fun personsAlphabetical() : Flow<List<Person>>

    @Query("select * from person order by date desc")
    fun personsRecently() : Flow<List<Person>>

    @Query("select COUNT(id) from person")
    fun count() : Flow<Int>

    @Query("update person set lastAccess = :date where id = :id")
    fun accessed(id: String, time: Calendar = Calendar.getInstance())
}

@Entity
data class Message(
    @PrimaryKey
    val sender: String,
    val self: Boolean,
    val message: String,
    val time: Calendar
)

@Dao
interface MessageDao {
    @Query("select * from message where id = :id order by time desc")
    fun messagesById(id: String): Flow<List<Message>>
    @Insert
    fun insert(message: Message)

}

@Database(version = 0)
abstract class DataBase : RoomDatabase() {
    abstract fun messageDao() : MessageDao
    abstract fun personDao() : PersonDao
}
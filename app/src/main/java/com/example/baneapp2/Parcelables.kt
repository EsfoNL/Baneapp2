package com.example.baneapp2

import android.os.Parcelable
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteOpenHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.parcelize.Parcelize
import java.sql.Time
import java.time.Instant
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
    val lastAccess: Instant = Instant.now()
)

@Dao
interface PersonDao {
    @Query("select * from person where id = :id")
    suspend fun personById(id: String): Person

    @Query("select * from person order by name asc")
    fun personsAlphabetical() : Flow<List<Person>>

    @Query("select * from person order by lastAccess desc")
    fun personsRecently() : Flow<List<Person>>

    @Query("select COUNT(id) from person")
    fun count() : Flow<Int>

    @Query("update person set lastAccess = :time where id = :id")
    suspend fun accessed(id: String, time: Instant = Instant.now())

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg persons: Person)
}

@Entity
data class Message(
    @PrimaryKey
    val sender: String,
    val self: Boolean,
    val message: String,
    val time: Instant = Instant.now()
)

@Dao
interface MessageDao {
    @Query("select * from message where sender = :id order by time desc")
    fun messagesById(id: String): Flow<List<Message>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: Message)

}

@Database(version = 1, entities = [Message::class, Person::class], exportSchema = false)
@TypeConverters(Convert::class)
abstract class DataBase : RoomDatabase() {
    abstract fun messageDao() : MessageDao
    abstract fun personDao() : PersonDao
}

@ProvidedTypeConverter
class Convert {
    @TypeConverter
    fun InstantToLong(instant: Instant): Long {
        return instant.epochSecond
    }
    @TypeConverter
    fun LongToInstant(long: Long): Instant {
        return Instant.ofEpochSecond(long)
    }
}
package com.maussieEsfo.baneapp2

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.Instant


@Entity
data class Person(
    @PrimaryKey
    val id: String,
    val name: String,
    val num: String,
    val image: String,
    val favourite: Boolean,
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

    @Query("select * from person where favourite = true order by name asc")
    fun personsFavourite() : Flow<List<Person>>

    @Query("update person set favourite = :newFavourite where id = :id")
    fun favourite(id: String, newFavourite: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg persons: Person)
}

@Entity
data class Message(
    val sender: String,
    val self: Boolean,
    val message: String,
    val time: Instant = Instant.now(),
    @PrimaryKey(autoGenerate = true)
    val messageId: Long = 0,
)

@Dao
interface MessageDao {
    @Query("select * from message where sender = :id order by time desc")
    fun messagesById(id: String): Flow<List<Message>>
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(message: Message): Long

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
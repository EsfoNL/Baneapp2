package com.maussieEsfo.baneapp2

import android.app.NotificationManager
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Context.NOTIFICATION_SERVICE
import android.util.Log
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.maussieEsfo.baneapp2.MainActivity.Companion.PREF_NAME
import com.maussieEsfo.baneapp2.settings.Settings
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

class PollingWorker(applicationContext: Context, workerParameters: WorkerParameters) : CoroutineWorker(applicationContext, workerParameters) {
    val db = Room.databaseBuilder(applicationContext, DataBase::class.java, "db")
        .addTypeConverter(Convert()).build()
    val sharedPreferences = applicationContext.getSharedPreferences(PREF_NAME, MODE_PRIVATE)
    val settings = Settings(sharedPreferences)
    val okHttpClient = OkHttpClient()


    override suspend fun doWork(): Result {
        try {

            val nm = applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

            val token = settings.token
            if (token != null) {
                val res = okHttpClient.newCall(
                    Request.Builder().url("https://esfokk.nl/api/v0/poll_messages").header("id", settings.id).header("token", token).build()
                ).execute()
                when (res.code) {
                    401 -> {
                        invalidateTokens()
                    }
                    410 -> {
                        refresh_tokens()
                    }
                    200 -> {
                        val bodyString = res.body!!.string()
                        if (bodyString != "[]") {
                            val json = JSONArray(bodyString)
                            Log.d("Worker", json.toString())
                            for (i in 0 until json.length()) {
                                try {
                                    val obj = json[i] as JSONObject

                                    val message = Message(
                                        sender = obj.getString("sender"),
                                        self = false,
                                        message = obj.getString("message"),
                                    )
                                    val id = db.messageDao().insert(
                                        message
                                    )
//                                    val builder = NotificationCompat.Builder(applicationContext, "BaneChannel")
//                                        .setSmallIcon(R.drawable.bane_logo)
//                                        .setContentTitle(db.personDao().personById(mesg.sender).name)
//                                        .setContentText(mesg.message)
//                                        .setContentIntent(PendingIntent.getActivity(applicationContext, 0, Intent(applicationContext, MainActivity::class.java), 0))
//                                        .setPriority(NotificationCompat.PRIORITY_DEFAULT).build()
//                                    nm.notify(id.toInt(), builder)

                                } catch (e: Throwable) {
                                    Log.e("Worker, depth 2", e.stackTraceToString())
                                    throw e
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            Log.e("Worker, depth 1", e.stackTraceToString())
            return Result.failure()
        }
        return Result.success()
    }

    private suspend fun refresh_tokens() {
        val res = okHttpClient.newCall(
            Request.Builder().url("https://esfokk.nl/api/v0/refresh")
                .header("refresh_token", settings.refresh_token.orEmpty())
                .header("id", settings.id).build()
        ).execute()
        Log.d("refresh_tokens", res.toString())
        if (res.code == 401 || res.code == 410) {
            settings.apply {
                token = null
                refresh_token = null
            }
        } else if (res.code == 200) {
            val json = JSONObject(res.body!!.string())
            settings.apply {
                token = json.getString("token")
                refresh_token = json.getString("refresh_token")
            }
        }
        settings.save(sharedPreferences)
    }

    fun invalidateTokens() {
        settings.apply {
            token = null
            refresh_token = null
        }
        settings.save(sharedPreferences)
    }
}

package com.example.baneapp2

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.content.withStyledAttributes
import androidx.room.Room
import com.example.baneapp2.settings.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONObject

class WebsocketService():  Service() {
    var websocket: WebSocket? = null
    lateinit var dataBase: DataBase
    val binder = LocalBinder()
    val okHttpClient = OkHttpClient()
    var settings: Settings? = null
    var bound = false
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val res = super.onStartCommand(intent, flags, startId)
        dataBase = Room.databaseBuilder(applicationContext, DataBase::class.java, "db").addTypeConverter(Convert()).build()
        return START_STICKY
    }

    override fun onUnbind(intent: Intent?): Boolean {
        bound = false
        return super.onUnbind(intent)
    }

    override fun onBind(intent: Intent?): IBinder {

        bound = true
        return binder
    }

    fun connectWebSocket(): Boolean {
        val token = settings?.token
        val id = settings?.id
        Log.e("hello world", token.toString())
        return if (token != null && id != null) {
            websocket = okHttpClient.newWebSocket(
                request = Request.Builder().addHeader("Id", id).addHeader("Token", token).url("wss://esfokk.nl/api/v0/ws").build(),
                listener = WsListener()
            )
            true
        } else {
            false
        }
    }
    fun sendAndStoreMessage(message: String, id: String) {
        Log.e("message", "$message, ${websocket.toString()}")
        try {
            websocket?.send(
                JSONObject().put("type", "Message").put("message", message).put("receiver", id.toInt())
                    .toString()
            )
        } catch (e: Throwable) {
            Log.e("sendAndStoreMessage", e.toString())
            return Unit
        }
        MainScope().launch {
            Log.e("hello world", dataBase.toString())
            dataBase.messageDao().insert(Message(id, true, message));
        }
    }

    inner class LocalBinder(): Binder() {
        fun getServiceClass(): WebsocketService {
            return this@WebsocketService
        }
        fun getDataBase(): DataBase {
            return this@WebsocketService.dataBase
        }
    }



    inner class WsListener : WebSocketListener() {

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            super.onClosed(webSocket, code, reason)
            websocket = null
        }

        override fun onOpen(webSocket: WebSocket, response: Response) {
            super.onOpen(webSocket, response)
            Log.e("Websocket onOpen", response.toString())
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            super.onFailure(webSocket, t, response)
            Log.e("Websocket onFailure", t.toString() + ' ' + response?.body.toString())
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            super.onMessage(webSocket, text)
            Log.e("message", text.toString())
            MainScope().launch(Dispatchers.IO) {
                try {
                    val json = JSONObject(text)
                    val message = Message(
                        sender = json.getString("sender"),
                        self = false,
                        message = json.getString("message")
                    )
                    dataBase.messageDao().insert(
                        message
                    )
                    if (!bound) {
                        Log.e("unbound message", message.toString())
                    }
                } catch (e: Throwable) {
                    Log.e("Websocket onMessage", e.toString())
                }
            }
        }
    }


}
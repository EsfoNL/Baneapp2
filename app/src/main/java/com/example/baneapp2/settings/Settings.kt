package com.example.baneapp2.settings

import android.content.SharedPreferences
import androidx.compose.material.darkColors
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb


class Settings(
    var fg_color: MutableState<Color>,
    var bg_color: MutableState<Color>,
    var text_color: MutableState<Color>,
    var token: String?,
    var name: String,
    var num: String,
    var id: String,
    var email: String,
    var refresh_token: String?) {
    companion object {
        const val USER_BG_KEY = "bgcolor"
        const val USER_FG_KEY = "fgcolor"
        const val USER_TEXT_KEY = "textcolor"
        const val USER_TOKEN = "token"
        const val USER_NAME = "name"
        const val USER_NUM = "num"
        const val USER_ID = "id"
        const val USER_EMAIL = "email"
        const val USER_REFRESH_TOKEN = "refresh_token"
        val DEFAULT = Settings()
    }

    fun save(sharedPreferences: SharedPreferences) {
        val editor = sharedPreferences.edit();
        editor.clear()
        editor.putString(USER_TOKEN, token)
        editor.putString(USER_NAME, name)
        editor.putString(USER_NUM, num)
        editor.putString(USER_ID, id)
        editor.putString(USER_EMAIL, email)
        editor.putString(USER_REFRESH_TOKEN, refresh_token)
        editor.putInt(USER_BG_KEY, bg_color.value.toArgb())
        editor.putInt(USER_FG_KEY, fg_color.value.toArgb())
        editor.putInt(USER_TEXT_KEY, text_color.value.toArgb())
        editor.apply()
    }

    fun savesync(sharedPreferences: SharedPreferences) {
        val editor = sharedPreferences.edit();
        editor.clear()
        editor.putString(USER_TOKEN, token)
        editor.putString(USER_NAME, name)
        editor.putString(USER_NUM, num)
        editor.putString(USER_ID, id)
        editor.putString(USER_EMAIL, email)
        editor.putString(USER_REFRESH_TOKEN, refresh_token)
        editor.putInt(USER_BG_KEY, bg_color.value.toArgb())
        editor.putInt(USER_FG_KEY, fg_color.value.toArgb())
        editor.putInt(USER_TEXT_KEY, text_color.value.toArgb())
        editor.commit()
    }

    constructor() : this(
        mutableStateOf(Color(0)),
        mutableStateOf(Color(0)),
        mutableStateOf(Color(0)),
        null, "", "", "", "", null) {
        val defaulttheme = darkColors()
        fg_color.value = defaulttheme.surface
        bg_color.value = defaulttheme.background
        text_color.value = defaulttheme.onBackground
    }
    constructor(sharedPreferences: SharedPreferences) : this() {
        token = sharedPreferences.getString(USER_TOKEN, DEFAULT.token);
        name = sharedPreferences.getString(USER_NAME, DEFAULT.name)!!;
        num = sharedPreferences.getString(USER_NUM, DEFAULT.num)!!;
        id = sharedPreferences.getString(USER_NAME , DEFAULT.id)!!;
        email = sharedPreferences.getString("email", DEFAULT.email)!!;
        refresh_token = sharedPreferences.getString("refresh_token", DEFAULT.refresh_token);
        fg_color = mutableStateOf(Color(sharedPreferences.getInt(USER_FG_KEY, DEFAULT.fg_color.value.toArgb())))
        bg_color = mutableStateOf(Color(sharedPreferences.getInt(USER_BG_KEY, DEFAULT.fg_color.value.toArgb())))
        text_color = mutableStateOf(Color(sharedPreferences.getInt(USER_TEXT_KEY, DEFAULT.text_color.value.toArgb())))
    }

}
package com.example.baneapp2

import android.os.Bundle
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

@Parcelize
data class UserInfo(
    val token: String,
    val name: String,
    val num: String,
    val refresh_token: String) : Parcelable
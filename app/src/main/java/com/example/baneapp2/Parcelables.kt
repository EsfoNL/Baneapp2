package com.example.baneapp2

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

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

@Parcelize
data class Person(
    val name: String,
    val num: String
) : Parcelable

@Parcelize data class Message(
    val sender: String,
    val self: Boolean,
    val message: String
) : Parcelable
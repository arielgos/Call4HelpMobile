package com.agos.call4help.model

import java.io.Serializable

data class User(
    var name: String,
    var email: String,
    var photoUrl: String,
    var token: String = "",
    var alerts: MutableList<Alert> = mutableListOf()
) : Serializable {
    constructor() : this("", "", "", "", mutableListOf())
}
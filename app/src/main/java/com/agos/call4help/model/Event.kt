package com.agos.call4help.model

import java.io.Serializable
import java.util.*

data class Event(
    var date: Date,
    var user: String,
    var detail: String
) : Serializable {
    constructor() : this(Date(), "", "")
}
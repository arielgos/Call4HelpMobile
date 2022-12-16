package com.agos.call4help.model

import java.io.Serializable
import java.util.*

data class Alert(
    var id: String,
    var date: Date,
    var tags: String,
    var objects: String,
    var description: String,
    var image: String,
    var status: String,
    var latitude: Double,
    var longitude: Double,
    var events: MutableList<Event> = mutableListOf()
) : Serializable {
    constructor() : this("", Date(), "", "", "", "", "", 0.0, 0.0, mutableListOf())
}
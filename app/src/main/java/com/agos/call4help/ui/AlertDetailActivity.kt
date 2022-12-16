package com.agos.call4help.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.agos.call4help.R
import com.agos.call4help.Utils
import com.agos.call4help.databinding.AlertDetailActivityBinding
import com.agos.call4help.model.Alert
import com.agos.call4help.toFormattedString
import com.agos.call4help.ui.adapter.EventAdapter
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class AlertDetailActivity : AppCompatActivity() {

    private lateinit var binding: AlertDetailActivityBinding
    private var firebaseUser: FirebaseUser? = null
    private var alert: Alert = Alert()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AlertDetailActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        firebaseUser = Firebase.auth.currentUser

        alert = intent.extras?.get("alert") as Alert

        with(binding) {
            if (alert.image.isNotEmpty()) {
                Glide.with(applicationContext)
                    .asBitmap()
                    .load("${Utils.imageUrl}thumb_${alert.image}?alt=media")
                    .into(image)
            }
            title.text = alert.status
            date.text = alert.date.toFormattedString()
            description.setText(alert.description)

            if (alert.status.lowercase().contains("pendiente")) {
                title.setBackgroundResource(R.drawable.curve_pending)
            }
            if (alert.status.lowercase().contains("proceso")) {
                title.setBackgroundResource(R.drawable.curve_in_process)
            }
            if (alert.status.lowercase().contains("cancelado")) {
                title.setBackgroundResource(R.drawable.curve_canceled)
            }
            if (alert.status.lowercase().contains("solucionado")) {
                title.setBackgroundResource(R.drawable.curve_success)
            }

            back.setOnClickListener {
                onBackPressed()
            }
        }

        val eventAdapter = EventAdapter(applicationContext)
        eventAdapter.submitList(alert.events.sortedByDescending { it.date })
        binding.list.hasFixedSize()
        binding.list.layoutManager = LinearLayoutManager(applicationContext)
        binding.list.adapter = eventAdapter
    }
}
package com.agos.call4help.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.agos.call4help.R
import com.agos.call4help.Utils
import com.agos.call4help.databinding.MainActivityBinding
import com.agos.call4help.model.Alert
import com.agos.call4help.service.GpsService
import com.agos.call4help.ui.adapter.AlertAdapter
import com.agos.call4help.ui.adapter.OnClickListener
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class MainActivity : AppCompatActivity() {

    private var serviceIntent: Intent? = null
    private lateinit var binding: MainActivityBinding
    private var firebaseUser: FirebaseUser? = null
    private var alerts = mutableListOf<Alert>()

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        if (intent?.extras != null) {
            val bundle = intent.extras
            if (bundle != null) {
                for (key in bundle.keySet()) {
                    Log.d(Utils.tag, "Main Activity - Key: $key - Value: ${bundle.get(key)}")
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.logout.setOnClickListener {
            Firebase.auth.signOut()
            startActivity(Intent(this@MainActivity, LoginActivity::class.java))
            finish()
        }

        binding.addAlert.setOnClickListener {
            val intent = Intent(this@MainActivity, AlertActivity::class.java)
            intent.putExtra("alert", Alert())
            startActivityForResult(intent, Utils.requestNewAlert)
        }

        firebaseUser = Firebase.auth.currentUser

        Glide.with(this@MainActivity)
            .asBitmap()
            .load(firebaseUser?.photoUrl)
            .apply(RequestOptions.circleCropTransform())
            .into(binding.user)

        /**
         * Analytics
         */
        Firebase.analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, "Main")
        }

        /**
         * Loading my alerts
         */
        Firebase.firestore
            .collection("users")
            .document(firebaseUser?.uid.toString())
            .collection("alerts")
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    for (documentChange in snapshot.documentChanges) {
                        val alert = documentChange.document.toObject(Alert::class.java)
                        when (documentChange.type) {
                            DocumentChange.Type.ADDED -> {
                                Log.d(Utils.tag, "New Alert: ${alert.date}")
                                alerts.add(alert)
                            }
                            DocumentChange.Type.MODIFIED -> {
                                Log.d(Utils.tag, "Modified Alert: ${alert.date}")
                                alerts[alerts.indexOfFirst {
                                    it.id == alert.id
                                }] = alert
                            }
                            DocumentChange.Type.REMOVED -> {
                                Log.d(Utils.tag, "Removed Alert: ${alert.date}")
                                alerts.removeAt(alerts.indexOfFirst {
                                    it.id == alert.id
                                })
                            }
                        }
                    }
                }

                binding.alerts.text = if (alerts.size > 0) {
                    getString(R.string.registered_alerts, alerts.size)
                } else {
                    getString(R.string.no_alerts)
                }
                binding.list.adapter?.notifyDataSetChanged()
            }

        val alertAdapter = AlertAdapter(
            context = applicationContext,
            onEditClickListener = OnClickListener { item ->
                val intent = Intent(this@MainActivity, AlertActivity::class.java)
                intent.putExtra("alert", item)
                startActivityForResult(intent, Utils.requestNewAlert)
            },
            onImageClickListener = OnClickListener { item ->
                val intent = Intent(this@MainActivity, AlertDetailActivity::class.java)
                intent.putExtra("alert", item)
                startActivity(intent)
            })

        alertAdapter.submitList(alerts)
        binding.list.hasFixedSize()
        binding.list.layoutManager = LinearLayoutManager(applicationContext)
        binding.list.adapter = alertAdapter

    }

    override fun onResume() {
        super.onResume()
        if (serviceIntent == null) {
            serviceIntent = Intent(this@MainActivity, GpsService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (serviceIntent != null) {
            stopService(serviceIntent)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                Utils.requestNewAlert -> {
                    /**
                     * Firestore
                     */
                    Firebase.firestore
                        .collection("users")
                        .document(firebaseUser?.uid.toString())
                        .collection("alerts")
                        .orderBy("date", Query.Direction.DESCENDING)
                        .get()
                        .addOnSuccessListener { documents ->
                            alerts.clear()
                            for (document in documents) {
                                alerts.add(document.toObject(Alert::class.java))
                            }
                            binding.alerts.text = if (alerts.size > 0) {
                                getString(R.string.registered_alerts, alerts.size)
                            } else {
                                getString(R.string.no_alerts)
                            }
                            binding.list.adapter?.notifyDataSetChanged()
                        }.addOnFailureListener {
                            it.printStackTrace()
                            Log.e(Utils.tag, it.message, it)
                        }
                }
            }
        }
    }
}
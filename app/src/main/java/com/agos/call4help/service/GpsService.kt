package com.agos.call4help.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.agos.call4help.R
import com.agos.call4help.Utils
import com.google.android.gms.location.*


class GpsService : Service() {

    private var locationCallback: LocationCallback? = null
    private var locationRequest: LocationRequest? = null
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null

    companion object {
        var myLocation: Location? = null
    }

    override fun onCreate() {
        super.onCreate()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_channel_id)
            val description = getString(R.string.app_name)
            val notificationChannel = NotificationChannel(name, name, NotificationManager.IMPORTANCE_LOW)
            notificationChannel.description = description
            notificationChannel.enableVibration(false)
            notificationChannel.enableLights(false)
            notificationChannel.setSound(null, null)
            notificationManager.deleteNotificationChannel(notificationChannel.id)
            notificationManager.createNotificationChannel(notificationChannel)
        }
        val notificationIntent = Intent(this@GpsService, GpsService::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification: Notification = NotificationCompat.Builder(this, getString(R.string.notification_channel_id))
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.location_message))
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentIntent(pendingIntent)
            .build()
        startForeground(1, notification)
    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                locationResult.lastLocation
                Log.i(Utils.tag, "GPS Foreground: " + locationResult.lastLocation.toString())
                if (locationResult.lastLocation!!.accuracy < 30) {
                    Log.i(Utils.tag, "Posicion exacta: " + locationResult.lastLocation.toString())
                }
                myLocation = locationResult.lastLocation
            }
        }



        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 60000).apply {
            setMinUpdateDistanceMeters(50F)
            setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
            setWaitForAccurateLocation(true)
        }.build()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationProviderClient!!.requestLocationUpdates(locationRequest!!, locationCallback!!, Looper.getMainLooper())
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationProviderClient?.removeLocationUpdates(locationCallback!!)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

}
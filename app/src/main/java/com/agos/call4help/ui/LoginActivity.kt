package com.agos.call4help.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.agos.call4help.R
import com.agos.call4help.Utils
import com.agos.call4help.databinding.LoginActivityBinding
import com.agos.call4help.model.User
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class LoginActivity : AppCompatActivity() {

    private lateinit var binding: LoginActivityBinding

    private lateinit var oneTapClient: SignInClient
    private lateinit var signInRequest: BeginSignInRequest
    private val REQUEST_ONE_TAP = 200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LoginActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        oneTapClient = Identity.getSignInClient(this)
        signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(getString(R.string.web_client_id))
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            )
            .build()

        binding.access.setOnClickListener {
            oneTapClient.beginSignIn(signInRequest)
                .addOnSuccessListener(this) { result ->
                    try {
                        startIntentSenderForResult(
                            result.pendingIntent.intentSender, REQUEST_ONE_TAP,
                            null, 0, 0, 0, null
                        )
                    } catch (e: IntentSender.SendIntentException) {
                        e.printStackTrace()
                    }
                }
                .addOnFailureListener(this) { e ->
                    e.printStackTrace()
                }
        }

        /**
         * Analytics
         */
        Firebase.analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, "Login")
        }

        /**
         * Cloud Messaging
         */
        if (intent?.extras != null) {
            val bundle = intent.extras
            if (bundle != null) {
                for (key in bundle.keySet()) {
                    Log.d(Utils.tag, "Key: $key - Value: ${bundle.get(key)}")
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        binding.access.visibility = View.GONE

        if (checkPlayServices() && checkPermissions()) {
            binding.access.visibility = View.VISIBLE

            if (Firebase.auth.currentUser != null) {
                val mainIntent = Intent(this@LoginActivity, MainActivity::class.java)
                if (intent.extras != null) {
                    val bundle = intent.extras
                    if (bundle != null) {
                        for (key in bundle.keySet()) {
                            mainIntent.putExtra(key, bundle.get(key).toString())
                        }
                    }
                }
                startActivity(mainIntent)
                finish()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_ONE_TAP -> {
                    try {
                        val credential = oneTapClient.getSignInCredentialFromIntent(data)
                        val idToken = credential.googleIdToken
                        if (idToken != null) {
                            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                            Firebase.auth.signInWithCredential(firebaseCredential)
                                .addOnCompleteListener(this) { task ->
                                    if (task.isSuccessful) {
                                        val currentUser = FirebaseAuth.getInstance().currentUser

                                        /**
                                         * Firestore
                                         */
                                        val collectionReference = Firebase.firestore.collection("users")
                                        collectionReference.document(currentUser?.uid!!).get()
                                            .addOnSuccessListener {
                                                var user = it.toObject(User::class.java)
                                                if (user == null) {
                                                    user = User(
                                                        name = currentUser.displayName!!,
                                                        email = currentUser.email!!,
                                                        photoUrl = currentUser.photoUrl!!.toString()
                                                    )
                                                    it.reference.set(user)
                                                }
                                                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                                                finish()
                                            }.addOnFailureListener {
                                                it.printStackTrace()
                                            }
                                    }
                                }
                        }
                    } catch (e: ApiException) {
                        Log.e(Utils.tag, e.message, e)
                    }
                }
            }
        }
    }

    private fun checkPlayServices(): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val status = googleApiAvailability.isGooglePlayServicesAvailable(this@LoginActivity)
        if (status != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(status)) {
                googleApiAvailability.getErrorDialog(this, status, 2404)!!.show()
            }
            return false
        }
        return true
    }

    private fun checkPermissions(): Boolean {
        if (
            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ), 101
            )
        } else {
            return true
        }

        return false
    }

    override fun onBackPressed() {
        //evitamos la accion del boton atras
    }
}
package com.example.expiryalert

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnitRunner
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

/** El backend de demostración solo existe en el proceso del APK de pruebas. */
class ExpiryAlertPruebasRunner : AndroidJUnitRunner() {
    override fun onStart() {
        if (InstrumentationRegistry.getArguments().getString("expiry.emuladores") == "true") {
            FirebaseApp.getApps(targetContext).forEach { it.delete() }
            FirebaseApp.initializeApp(targetContext, FirebaseOptions.Builder()
                .setProjectId("demo-expiryalert-pruebas")
                .setApplicationId("1:1234567890:android:0123456789abcdef")
                .setApiKey("AIza" + "A".repeat(35)).build())
            FirebaseAuth.getInstance().useEmulator("10.0.2.2", 9099)
            FirebaseFirestore.getInstance().apply {
                useEmulator("10.0.2.2", 8080)
                firestoreSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(false).build()
            }
        }
        super.onStart()
    }
}

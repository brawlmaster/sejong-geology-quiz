package com.notibeam.data

import android.content.Context
import android.util.Log
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import java.util.UUID

object FirebaseRepository {
    private const val TAG = "FirebaseRepo"
    private val db by lazy { FirebaseFirestore.getInstance() }

    suspend fun ensureSignedIn() {
        if (Firebase.auth.currentUser == null) {
            Firebase.auth.signInAnonymously().await()
        }
    }

    suspend fun getFcmToken(): String {
        return FirebaseMessaging.getInstance().token.await()
    }

    suspend fun registerReceiverDevice(channelId: String, deviceId: String = UUID.randomUUID().toString(), displayName: String? = null) {
        ensureSignedIn()
        val token = getFcmToken()
        val deviceDoc = db.collection("channels").document(channelId).collection("devices").document(deviceId)
        deviceDoc.set(
            mapOf(
                "deviceId" to deviceId,
                "channelId" to channelId,
                "token" to token,
                "displayName" to (displayName ?: deviceId)
            )
        ).await()
    }

    suspend fun postNotification(channelId: String, payload: Map<String, Any?>) {
        ensureSignedIn()
        val msgDoc = db.collection("channels").document(channelId).collection("messages").document()
        msgDoc.set(payload).await()
        Log.d(TAG, "Posted message to $channelId")
    }

    suspend fun removeDevice(channelId: String, deviceId: String) {
        db.collection("channels").document(channelId).collection("devices").document(deviceId).delete().await()
    }
}
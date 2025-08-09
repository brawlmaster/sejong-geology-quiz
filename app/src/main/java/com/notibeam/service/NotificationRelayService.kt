package com.notibeam.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.notibeam.data.AppPreferences
import com.notibeam.data.FirebaseRepository

class NotificationRelayService : NotificationListenerService() {

    private val scope = CoroutineScope(Dispatchers.IO + Job())

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("NotiBeam", "Notification listener connected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        scope.launch {
            val role = AppPreferences.getRole(applicationContext)
            if (role != "sender") return@launch

            val allowed = AppPreferences.getEnabledPackages(applicationContext)
            if (allowed.isNotEmpty() && !allowed.contains(sbn.packageName)) {
                return@launch
            }
            val channelId = AppPreferences.getChannelId(applicationContext) ?: return@launch
            val extras = sbn.notification.extras
            val title = extras.getCharSequence("android.title")?.toString()
            val text = extras.getCharSequence("android.text")?.toString()
            val bigText = extras.getCharSequence("android.bigText")?.toString()
            val payload = mapOf(
                "when" to System.currentTimeMillis(),
                "package" to sbn.packageName,
                "title" to (title ?: ""),
                "text" to (bigText ?: text ?: "")
            )
            try {
                FirebaseRepository.postNotification(channelId, payload)
            } catch (e: Exception) {
                Log.e("NotiBeam", "Failed to send notification: ${e.message}")
            }
        }
    }
}
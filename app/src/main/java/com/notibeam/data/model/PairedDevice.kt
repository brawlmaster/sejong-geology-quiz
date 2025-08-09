package com.notibeam.data.model

data class PairedDevice(
    val deviceId: String = "",
    val channelId: String = "",
    val fcmToken: String? = null,
    val displayName: String? = null
)
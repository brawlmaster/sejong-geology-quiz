# Keep Firebase models and messaging service
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

-keep class com.notibeam.messaging.AppFirebaseMessagingService { *; }
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**
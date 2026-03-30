# Retrofit & OkHttp
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*

# Gson serialization — keep all data/remote model classes
-keep class com.workly.helpprovider.data.model.** { *; }
-keep class com.workly.helpprovider.data.remote.ApiResponse { *; }
-keep class com.workly.helpprovider.data.remote.AuthResponse { *; }
-keep class com.workly.helpprovider.data.remote.LoginRequest { *; }
-keep class com.workly.helpprovider.data.remote.OtpRequest { *; }
-keep class com.workly.helpprovider.data.remote.ReviewRequest { *; }

# Retrofit API interface
-keep interface com.workly.helpprovider.data.remote.ApiService { *; }

# Hilt — keep generated components
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# Room — keep entity and DAO classes
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# WorkManager
-keep class androidx.work.** { *; }
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Firebase Cloud Messaging
-keep class com.google.firebase.messaging.** { *; }

# AndroidX Security (EncryptedSharedPreferences)
-keep class androidx.security.crypto.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

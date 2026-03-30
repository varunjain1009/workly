# Retrofit & OkHttp
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*

# Gson serialization — keep all data/network model classes
-keep class com.workly.helpseeker.data.model.** { *; }
-keep class com.workly.helpseeker.data.network.ApiResponse { *; }
-keep class com.workly.helpseeker.data.network.AuthResponse { *; }
-keep class com.workly.helpseeker.data.network.LoginRequest { *; }
-keep class com.workly.helpseeker.data.network.OtpRequest { *; }
-keep class com.workly.helpseeker.data.network.ReviewRequest { *; }

# Retrofit API interface
-keep interface com.workly.helpseeker.data.network.ApiService { *; }

# Hilt — keep generated components
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# Room — keep entity and DAO classes
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# Firebase Cloud Messaging
-keep class com.google.firebase.messaging.** { *; }

# AndroidX Security (EncryptedSharedPreferences)
-keep class androidx.security.crypto.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

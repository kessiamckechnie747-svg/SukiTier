# Kotlin
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }
-dontwarn kotlin.**

# Jetpack Compose
-keep class androidx.compose.** { *; }

# Room
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# Hilt
-keep class * implements dagger.hilt.android.lifecycle.HiltViewModel { *; }
-keep class * extends dagger.hilt.android.internal.lifecycle.HiltViewModelFactory { *; }

# OTA/Updates
-keep class android.os.** { *; }
-keep class com.sukitier.core.ota.** { *; }

# Serialization
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}

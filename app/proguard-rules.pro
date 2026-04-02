# ARCore
-keep class com.google.ar.** { *; }
-keep class com.google.ar.core.** { *; }

# Kotlin coroutines
-keepclassmembernames class kotlinx.** { volatile <fields>; }

# Compose
-keep class androidx.compose.** { *; }

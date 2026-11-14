# Keep kotlinx.serialization models for backup JSON serialization
-keepattributes *Annotation*,Signature
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <init>(...);
}

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# ZXing Core
-keep class com.google.zxing.** { *; }

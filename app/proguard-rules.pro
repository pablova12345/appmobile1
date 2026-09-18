# Proguard rules for DigiD
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

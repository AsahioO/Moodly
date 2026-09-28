# kotlinx.serialization: conserva los serializadores generados de nuestros modelos.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers @kotlinx.serialization.Serializable class com.asahioo.moodly.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class com.asahioo.moodly.** {
    kotlinx.serialization.KSerializer serializer(...);
}

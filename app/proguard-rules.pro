# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.rowshr.siddur.** { *** Companion; }
-keepclasseswithmembers class com.rowshr.siddur.** { kotlinx.serialization.KSerializer serializer(...); }

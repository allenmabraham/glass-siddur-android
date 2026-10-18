# kotlinx.serialization (official rules)
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.rowshr.siddur.**$$serializer { *; }
-keepclassmembers class com.rowshr.siddur.** { *** Companion; }
-keepclasseswithmembers class com.rowshr.siddur.** { kotlinx.serialization.KSerializer serializer(...); }
# Keep the enum names the settings store persists by name
-keepclassmembers enum com.rowshr.siddur.** { *; }

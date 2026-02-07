# Moshi
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}

# Keep the generated JSON adapters.
-keep class *JsonAdapter { *; }
-keep class org.grakovne.lissen.lib.domain.** { *; }

# Keep Moshi's internal classes that use reflection.
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }

# Attributes for Moshi's reflection.
-keepattributes Signature, AnnotationDefault, RuntimeVisibleAnnotations, InnerClasses, EnclosingMethod

# Hilt and Dagger rules (usually bundled, but good to ensure)
-keep class dagger.hilt.android.internal.** { *; }
-keep class *__HiltBindingModule { *; }
-keep class org.grakovne.lissen.**_HiltComponents$* { *; }

# Microsoft Clarity
-keep class com.microsoft.clarity.** { *; }
-keep interface com.microsoft.clarity.** { *; }
# Proguard rules for NewPipeExtractor JavaScript engine compatibility
-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.classfile.ClassFileWriter
-dontwarn org.mozilla.javascript.tools.**
-dontwarn java.beans.**
-dontwarn javax.script.**
-dontwarn jdk.dynalink.**
-dontwarn org.mozilla.javascript.**

# Keep all of NewPipeExtractor classes and interfaces (highly dynamic HTML parsing)
-keep class org.schabi.newpipe.extractor.** { *; }
-keep interface org.schabi.newpipe.extractor.** { *; }

# Keep Room database and entity classes (reflection serialization)
-keep class com.personal.mymusic.data.database.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }

# Keep WorkManager workers (reflection instantiation)
-keep class * extends androidx.work.ListenableWorker { *; }
-keep class com.personal.mymusic.worker.** { *; }

# Keep AndroidX Media3 classes and service overrides
-keep class androidx.media3.session.** { *; }
-keep class * extends androidx.media3.session.MediaSessionService { *; }
-keep class * extends androidx.media3.session.MediaLibraryService { *; }

# Keep all project classes to prevent reflection/obfuscation runtime crashes
-keep class com.personal.mymusic.** { *; }


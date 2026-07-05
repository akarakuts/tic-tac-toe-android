# Compose / Kotlin
-keepattributes *Annotation*
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Game enums and progress JSON
-keepclassmembers class ru.akarakuts.tictactoe.game.** { *; }
-keepclassmembers class ru.akarakuts.tictactoe.progress.** { *; }

# Multiplayer wire protocol (enum/JSON parsing across the Bluetooth link)
-keepclassmembers class ru.akarakuts.tictactoe.multiplayer.** { *; }

# Line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

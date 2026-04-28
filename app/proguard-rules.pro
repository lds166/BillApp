# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the SDK installation.
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

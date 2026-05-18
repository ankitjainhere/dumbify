# Keep Room entities and DAOs
-keep class com.dumbify.app.data.entities.** { *; }
-keep class com.dumbify.app.data.dao.** { *; }

# Keep DeviceAdminReceiver public API
-keep class com.dumbify.app.admin.DumbifyDeviceAdminReceiver { *; }

# Argon2-jvm uses JNI
-keep class de.mkammerer.argon2.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }

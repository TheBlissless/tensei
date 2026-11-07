cmake_minimum_required(VERSION 3.22.1)
project("tensei_discord")

# The Discord Social SDK is proprietary and not committed to the repository.
# When app/libs/discord_partner_sdk.aar is absent the JNI bridge is not built
# and Discord Rich Presence is disabled at runtime (DiscordNative reports the
# library as unavailable). See README.md -> "Building" for how to add the SDK.
find_package(discord_partner_sdk QUIET CONFIG)

if(discord_partner_sdk_FOUND)
    add_library(tensei_discord SHARED
        discord_jni.cpp
    )

    target_link_libraries(tensei_discord
        discord_partner_sdk::discord_partner_sdk
        android
        log
    )
else()
    message(STATUS "discord_partner_sdk not found - building without Discord Rich Presence support")
endif()

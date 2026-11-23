#include <jni.h>
#include <string>
#include <memory>
#include <android/log.h>

#define DISCORDPP_IMPLEMENTATION
#include <discordpp.h>

#define TAG "DiscordNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static std::shared_ptr<discordpp::Client> g_client;
static JavaVM *g_jvm = nullptr;

extern "C" {

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *) {
    g_jvm = vm;
    LOGI("JNI_OnLoad: Discord JNI initialized");
    return JNI_VERSION_1_6;
}

JNIEXPORT void JNICALL
Java_com_blissless_tensei_discord_DiscordNative_nativeInitialize(
        JNIEnv *env, jobject /* this */, jstring appId) {
    const char *app_id_str = env->GetStringUTFChars(appId, nullptr);
    LOGI("nativeInitialize: appId=%s", app_id_str);

    uint64_t app_id = std::stoull(std::string(app_id_str));
    g_client = std::make_shared<discordpp::Client>();

    g_client->AddLogCallback([](const std::string& message, discordpp::LoggingSeverity severity) {
        LOGI("SDK: [%d] %s", static_cast<int>(severity), message.c_str());
    }, discordpp::LoggingSeverity::Verbose);

    g_client->SetApplicationId(app_id);

    LOGI("nativeInitialize: client created, appId=%s", app_id_str);
    env->ReleaseStringUTFChars(appId, app_id_str);
}

JNIEXPORT void JNICALL
Java_com_blissless_tensei_discord_DiscordNative_nativeSetPresence(
        JNIEnv *env, jobject /* this */,
        jstring name, jstring details, jstring state,
        jint type, jlong startTimestamp, jlong endTimestamp,
        jstring largeImage, jstring largeText,
        jstring smallImage, jstring smallText,
        jstring largeUrl, jstring smallUrl,
        jstring button1Label, jstring button1Url,
        jstring button2Label, jstring button2Url) {

    if (!g_client) {
        LOGE("nativeSetPresence: client not initialized");
        return;
    }

    const char *c_name = name ? env->GetStringUTFChars(name, nullptr) : nullptr;
    const char *c_details = details ? env->GetStringUTFChars(details, nullptr) : nullptr;
    const char *c_state = state ? env->GetStringUTFChars(state, nullptr) : nullptr;
    const char *c_largeImage = largeImage ? env->GetStringUTFChars(largeImage, nullptr) : nullptr;
    const char *c_largeText = largeText ? env->GetStringUTFChars(largeText, nullptr) : nullptr;
    const char *c_smallImage = smallImage ? env->GetStringUTFChars(smallImage, nullptr) : nullptr;
    const char *c_smallText = smallText ? env->GetStringUTFChars(smallText, nullptr) : nullptr;
    const char *c_largeUrl = largeUrl ? env->GetStringUTFChars(largeUrl, nullptr) : nullptr;
    const char *c_smallUrl = smallUrl ? env->GetStringUTFChars(smallUrl, nullptr) : nullptr;
    const char *c_button1Label = button1Label ? env->GetStringUTFChars(button1Label, nullptr) : nullptr;
    const char *c_button1Url = button1Url ? env->GetStringUTFChars(button1Url, nullptr) : nullptr;
    const char *c_button2Label = button2Label ? env->GetStringUTFChars(button2Label, nullptr) : nullptr;
    const char *c_button2Url = button2Url ? env->GetStringUTFChars(button2Url, nullptr) : nullptr;

    LOGI("nativeSetPresence: name=%s details=%s state=%s type=%d startTs=%lld endTs=%lld largeImg=%s largeTxt=%s largeUrl=%s smallImg=%s smallTxt=%s smallUrl=%s btn1=%s btn2=%s",
         c_name ?: "", c_details ?: "", c_state ?: "", type, (long long)startTimestamp, (long long)endTimestamp,
         c_largeImage ?: "(null)", c_largeText ?: "(null)", c_largeUrl ?: "(null)",
         c_smallImage ?: "(null)", c_smallText ?: "(null)", c_smallUrl ?: "(null)",
         c_button1Label ?: "(null)", c_button2Label ?: "(null)");

    discordpp::Activity activity;

    // The activity name renders next to the verb, so with type Watching this is
    // what makes the presence header read "Watching Tensei". Never hand a null
    // char* to SetName — the Kotlin layer always provides a name, this is just
    // a safety net for the JNI boundary.
    activity.SetName(c_name ? c_name : "");

    switch (type) {
        case 0: activity.SetType(discordpp::ActivityTypes::Playing); break;
        case 1: activity.SetType(discordpp::ActivityTypes::Streaming); break;
        case 2: activity.SetType(discordpp::ActivityTypes::Listening); break;
        case 3: activity.SetType(discordpp::ActivityTypes::Watching); break;
        default: activity.SetType(discordpp::ActivityTypes::Playing); break;
    }

    if (c_details) activity.SetDetails(c_details);
    if (c_state) activity.SetState(c_state);

    if (startTimestamp > 0 || endTimestamp > 0) {
        discordpp::ActivityTimestamps timestamps;
        if (startTimestamp > 0) timestamps.SetStart(static_cast<uint64_t>(startTimestamp));
        if (endTimestamp > 0) timestamps.SetEnd(static_cast<uint64_t>(endTimestamp));
        activity.SetTimestamps(timestamps);
    }

    discordpp::ActivityAssets assets;
    if (c_largeImage) assets.SetLargeImage(c_largeImage);
    if (c_largeText) assets.SetLargeText(c_largeText);
    // A null small image (the "hide the small image" setting) simply leaves the
    // small asset unset.
    if (c_smallImage) assets.SetSmallImage(c_smallImage);
    if (c_smallText) assets.SetSmallText(c_smallText);
    if (c_largeUrl) assets.SetLargeUrl(c_largeUrl);
    if (c_smallUrl) assets.SetSmallUrl(c_smallUrl);

    activity.SetAssets(std::move(assets));

    // Up to two custom buttons under the presence (e.g. "AniList" / "MyAnimeList"
    // opening the anime's page on each site). Skipped when label or URL is blank.
    if (c_button1Label && c_button1Url && c_button1Label[0] != '\0' && c_button1Url[0] != '\0') {
        discordpp::ActivityButton button1;
        button1.SetLabel(c_button1Label);
        button1.SetUrl(c_button1Url);
        activity.AddButton(button1);
    }
    if (c_button2Label && c_button2Url && c_button2Label[0] != '\0' && c_button2Url[0] != '\0') {
        discordpp::ActivityButton button2;
        button2.SetLabel(c_button2Label);
        button2.SetUrl(c_button2Url);
        activity.AddButton(button2);
    }

    g_client->UpdateRichPresence(activity, [](discordpp::ClientResult result) {
        if (result.Successful()) {
            LOGI("UpdateRichPresence: success");
        } else {
            LOGE("UpdateRichPresence: failed - type=%d error='%s' code=%d retryable=%d",
                 static_cast<int>(result.Type()), result.Error().c_str(), result.ErrorCode(), result.Retryable());
        }
    });

    if (c_name) env->ReleaseStringUTFChars(name, c_name);
    if (c_details) env->ReleaseStringUTFChars(details, c_details);
    if (c_state) env->ReleaseStringUTFChars(state, c_state);
    if (c_largeImage) env->ReleaseStringUTFChars(largeImage, c_largeImage);
    if (c_largeText) env->ReleaseStringUTFChars(largeText, c_largeText);
    if (c_smallImage) env->ReleaseStringUTFChars(smallImage, c_smallImage);
    if (c_smallText) env->ReleaseStringUTFChars(smallText, c_smallText);
    if (c_largeUrl) env->ReleaseStringUTFChars(largeUrl, c_largeUrl);
    if (c_smallUrl) env->ReleaseStringUTFChars(smallUrl, c_smallUrl);
    if (c_button1Label) env->ReleaseStringUTFChars(button1Label, c_button1Label);
    if (c_button1Url) env->ReleaseStringUTFChars(button1Url, c_button1Url);
    if (c_button2Label) env->ReleaseStringUTFChars(button2Label, c_button2Label);
    if (c_button2Url) env->ReleaseStringUTFChars(button2Url, c_button2Url);
}

JNIEXPORT void JNICALL
Java_com_blissless_tensei_discord_DiscordNative_nativeClearPresence(
        JNIEnv *env, jobject /* this */) {
    if (!g_client) {
        LOGE("nativeClearPresence: client not initialized");
        return;
    }
    LOGI("nativeClearPresence");
    discordpp::Activity activity;
    g_client->UpdateRichPresence(activity, [](discordpp::ClientResult result) {
        if (result.Successful()) {
            LOGI("ClearPresence: success");
        } else {
            LOGE("ClearPresence: failed - %s", result.Error().c_str());
        }
    });
}

JNIEXPORT void JNICALL
Java_com_blissless_tensei_discord_DiscordNative_nativeRunCallbacks(
        JNIEnv * /* env */, jobject /* this */) {
    discordpp::RunCallbacks();
}

JNIEXPORT void JNICALL
Java_com_blissless_tensei_discord_DiscordNative_nativeDestroy(
        JNIEnv *env, jobject /* this */) {
    LOGI("nativeDestroy");
    g_client = nullptr;
}

}

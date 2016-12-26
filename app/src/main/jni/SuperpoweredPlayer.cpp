#include "SuperpoweredPlayer.h"
#include "SuperpoweredSimple.h"
#include <jni.h>
#include <stdio.h>
#include <android/log.h>
#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_AndroidConfiguration.h>

static SuperpoweredPlayer *player = NULL;
JavaVM *gJavaVM;
jobject callbackObject = NULL;

static void handlingCallback(int event) {
    JNIEnv *env;
    int isAttached = 0;

    if (!callbackObject) {
        return;
    }

    if ((gJavaVM->GetEnv((void **) &env, JNI_VERSION_1_6)) < 0) {
        if ((gJavaVM->AttachCurrentThread(&env, NULL)) < 0) {
            return;
        }
        isAttached = 1;
    }

    jclass cls = env->GetObjectClass(callbackObject);
    if (!cls) {
        if (isAttached) {
            gJavaVM->DetachCurrentThread();
        }
        return;
    }

    jmethodID method = env->GetMethodID(cls, "playStatusCallback", "(I)V");
    if (!method) {
        if (isAttached) {
            gJavaVM->DetachCurrentThread();
        }
        return;
    }

    env->CallVoidMethod(callbackObject, method, event);

    if (isAttached) {
        gJavaVM->DetachCurrentThread();
    }
}

static void playerEventCallback(void *clientData, SuperpoweredAdvancedAudioPlayerEvent event,
                                void *__unused value) {

    SuperpoweredAdvancedAudioPlayer *player = *((SuperpoweredAdvancedAudioPlayer **) clientData);
    switch (event) {
        case SuperpoweredAdvancedAudioPlayerEvent_LoadSuccess:
//            player->setPosition(player->firstBeatMs, false, false);
            __android_log_print(ANDROID_LOG_DEBUG, "MediaCenter", "LOAD SUCCESS");
            break;
        case SuperpoweredAdvancedAudioPlayerEvent_LoadError:
            __android_log_print(ANDROID_LOG_DEBUG, "MediaCenter", "Open error: %s", (char *) value);
            break;
        case SuperpoweredAdvancedAudioPlayerEvent_EOF:
            __android_log_print(ANDROID_LOG_DEBUG, "MediaCenter", "END SONG");
            handlingCallback(1);
            break;
        default:;
    };
}

static bool audioProcessing(void *clientdata, short int *audioIO, int numberOfSamples,
                            int __unused samplerate) {
//    __android_log_print(ANDROID_LOG_VERBOSE, "MediaCenter", "audioProcessing");

    return ((SuperpoweredPlayer *) clientdata)->process(audioIO, (unsigned int) numberOfSamples);
}

SuperpoweredPlayer::SuperpoweredPlayer(unsigned int samplerate, unsigned int buffersize) : volume(
        1.0f) {

    buffer = (float *) memalign(16, (buffersize + 16) * sizeof(float) * 2);

    player = new SuperpoweredAdvancedAudioPlayer(&player, playerEventCallback, samplerate, 0);
    player->syncMode = SuperpoweredAdvancedAudioPlayerSyncMode_None;

    audioSystem = new SuperpoweredAndroidAudioIO(samplerate, buffersize, false, true,
                                                 audioProcessing, this, -1, SL_ANDROID_STREAM_MEDIA,
                                                 buffersize * 2);

    mixer = new SuperpoweredStereoMixer();

    left = 1.0f;
    right = 1.0f;


    bandEQ = new Superpowered3BandEQ(samplerate);
}

bool SuperpoweredPlayer::process(short int *output, unsigned int numberOfSamples) {


    bool silence = !player->process(buffer, false, numberOfSamples, volume);

//    float *mixerInputs[4] = {buffer, NULL, NULL, NULL};
//
//    float *mixerOutputs[2] = {buffer, NULL};
//
//    float mixerInputLevels[8] = {1.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f};
//
//    float mixerOutputLevels[2] = {left, right};
//
//
//    mixer->process(mixerInputs, mixerOutputs, mixerInputLevels, mixerOutputLevels, NULL, NULL,
//                   numberOfSamples);

    if (!silence) {
//        SuperpoweredFloatToShortInt(mixerOutputs[0], output, numberOfSamples);

        bandEQ->process(buffer, buffer, numberOfSamples);



        SuperpoweredFloatToShortInt(buffer, output, numberOfSamples);


    }

    return !silence;
}

void SuperpoweredPlayer::togglePlayback() {
    player->togglePlayback();
}


SuperpoweredPlayer::~SuperpoweredPlayer() {
    delete bandEQ;
    delete mixer;
    delete audioSystem;
    delete player;
    free(buffer);
}

void SuperpoweredPlayer::setVolume(float value) {
    volume = value * 0.01f;
}

void SuperpoweredPlayer::setSeek(int value) {
    player->seek(value * 0.01);
}

unsigned int SuperpoweredPlayer::getDuration() {
    return player->durationSeconds;
}

unsigned int SuperpoweredPlayer::getPosition() {
    return player->positionSeconds;
}

float SuperpoweredPlayer::getPositionPercent() {
    return player->positionPercent;
}


bool SuperpoweredPlayer::isPlaying() {
    return player->playing;
}

void SuperpoweredPlayer::setLooping(bool looping) {
    player->looping = looping;
}

void SuperpoweredPlayer::open(const char *path) {
    player->open(path);
}

void SuperpoweredPlayer::setEQBands(int index, int value) {
    if (index < 0 || index > 2) {
        return;
    }
    float bandF = 1.0f;
    if (value < 50) {
        bandF = (float) value / 50.0f;
    } else if (value > 50) {
        bandF = 1.0f + (float) (value - 50) / 7.0f;
    }
    __android_log_print(ANDROID_LOG_VERBOSE, "MediaCenter", "setEQBands index = %i bandF = %f",
                        index, bandF);
    bandEQ->bands[index] = bandF;
}

void SuperpoweredPlayer::enableEQ(bool enable) {
    bandEQ->enable(enable);
}

void SuperpoweredPlayer::onBackground() {
    audioSystem->onBackground();
}

void SuperpoweredPlayer::onForeground() {
    audioSystem->onForeground();
}

float SuperpoweredPlayer::getVolume() {
    return volume;
}

bool SuperpoweredPlayer::isLooping() {
    return player->looping;
}

bool SuperpoweredPlayer::isEnableEQ() {
    return bandEQ->enabled;
}


JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    gJavaVM = vm;
    return JNI_VERSION_1_6;
}

extern "C" JNIEXPORT void
Java_com_fesskiev_player_services_PlaybackService_onDestroyAudioPlayer(JNIEnv *env,
                                                                       jobject instance) {
    player->~SuperpoweredPlayer();
    __android_log_print(ANDROID_LOG_DEBUG, "MediaCenter", "DESTROY");
}

static inline void setFloatField(JNIEnv *javaEnvironment, jobject obj, jclass thisClass,
                                 const char *name, float value) {
    javaEnvironment->SetFloatField(obj, javaEnvironment->GetFieldID(thisClass, name, "F"), value);
}

static inline void setIntField(JNIEnv *javaEnvironment, jobject obj, jclass thisClass,
                               const char *name, unsigned int value) {
    javaEnvironment->SetIntField(obj, javaEnvironment->GetFieldID(thisClass, name, "I"), value);
}

static inline void setBoolField(JNIEnv *javaEnvironment, jobject obj, jclass thisClass,
                                const char *name, bool value) {
    javaEnvironment->SetBooleanField(obj, javaEnvironment->GetFieldID(thisClass, name, "Z"),
                                     (jboolean) value);
}


extern "C" JNIEXPORT void
Java_com_fesskiev_player_services_PlaybackService_updatePlaybackState(JNIEnv *javaEnvironment,
                                                                      jobject obj) {
    jclass thisClass = javaEnvironment->GetObjectClass(obj);

    setIntField(javaEnvironment, obj, thisClass, "duration", player->getDuration());
    setIntField(javaEnvironment, obj, thisClass, "position", player->getPosition());
    setFloatField(javaEnvironment, obj, thisClass, "volume", player->getVolume());
    setFloatField(javaEnvironment, obj, thisClass, "positionPercent", player->getPositionPercent());
    setBoolField(javaEnvironment, obj, thisClass, "playing", player->isPlaying());
    setBoolField(javaEnvironment, obj, thisClass, "looping", player->isLooping());
    setBoolField(javaEnvironment, obj, thisClass, "enableEQ", player->isEnableEQ());
}

extern "C" JNIEXPORT void
Java_com_fesskiev_player_services_PlaybackService_onBackground(JNIEnv *env, jobject instance) {
    player->onBackground();
}

extern "C" JNIEXPORT void
Java_com_fesskiev_player_services_PlaybackService_onForeground(JNIEnv *env, jobject instance) {
    player->onForeground();
}


extern "C" JNIEXPORT void
Java_com_fesskiev_player_services_PlaybackService_registerCallback(JNIEnv *env, jobject instance) {
    callbackObject = env->NewGlobalRef(instance);
}


extern "C" JNIEXPORT void
Java_com_fesskiev_player_services_PlaybackService_unregisterCallback(JNIEnv *env,
                                                                     jobject instance) {
    env->DeleteGlobalRef(callbackObject);
    callbackObject = NULL;
}

extern "C" JNIEXPORT void
Java_com_fesskiev_player_services_PlaybackService_enableEQ(JNIEnv *env, jobject instance,
                                                           jboolean enable) {
    player->enableEQ(enable);
}



extern "C" JNIEXPORT void
Java_com_fesskiev_player_services_PlaybackService_setEQBands(JNIEnv *javaEnvironment, jobject obj,
                                                             jint band, jint value) {
    player->setEQBands(band, value);
}

extern "C" JNIEXPORT void
Java_com_fesskiev_player_services_PlaybackService_setLoopingAudioPlayer(JNIEnv *env,
                                                                        jobject instance,
                                                                        jboolean isLooping) {
    player->setLooping(isLooping);
}

extern "C" JNIEXPORT void
Java_com_fesskiev_player_services_PlaybackService_createAudioPlayer(JNIEnv *env, jobject instance,
                                                                    jint sampleRate,
                                                                    jint bufferSize) {
    player = new SuperpoweredPlayer((unsigned int) sampleRate, (unsigned int) bufferSize);
}

extern "C" JNIEXPORT void
Java_com_fesskiev_player_services_PlaybackService_openAudioFile(JNIEnv *env, jobject instance,
                                                                jstring path) {
    const char *str = env->GetStringUTFChars(path, 0);

    player->open(str);

    env->ReleaseStringUTFChars(path, str);
}


extern "C" JNIEXPORT void
Java_com_fesskiev_player_services_PlaybackService_togglePlayback(JNIEnv *env, jobject instance) {
    player->togglePlayback();
}

extern "C" JNIEXPORT void
Java_com_fesskiev_player_services_PlaybackService_setVolumeAudioPlayer(JNIEnv *env,
                                                                       jobject instance,
                                                                       jfloat value) {
    player->setVolume(value);
}

extern "C" JNIEXPORT void
Java_com_fesskiev_player_services_PlaybackService_setSeekAudioPlayer(JNIEnv *env, jobject instance,
                                                                     jint value) {
    player->setSeek(value);
}
//
// Created by belyx on 1/2/2024.
//
#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>

#include <android/log.h>
#include "i2c.h"
#include <jni.h>

int test() {
    start();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_bely_dominogpt_MainActivity_start(JNIEnv *env, jobject thiz) {
    start();
}
extern "C"
JNIEXPORT void JNICALL
Java_com_bely_dominogpt_MainActivity_stop(JNIEnv *env, jobject thiz) {
    stop();
}
extern "C"
JNIEXPORT void JNICALL
Java_com_bely_dominogpt_MainActivity_setPWM(JNIEnv *env, jobject thiz, jint channel, jint angle) {
    set_pwm(channel, angle);
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_bely_dominogpt_MainActivity_getAngle(JNIEnv *env, jobject thiz, jint channel) {
    return readAngle(channel);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_bely_dominogpt_MainActivity_testSetOffValue(JNIEnv *env, jobject thiz, jint channel,
                                                     jint offvalue) {
    writeValue(channel, offvalue);
}
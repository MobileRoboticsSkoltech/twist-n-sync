#include <iostream>
#include <vector>
#include "jni.h"
#include "com_googleresearch_capturesync_wrapperJNI.h"

extern "C"
JNIEXPORT jfloat JNICALL
Java_com_googleresearch_capturesync_wrapperJNI_sendData(JNIEnv *env, jclass clazz,
                                                        jbyteArray byte_array) {
    jbyte *bufferPtr = env->GetByteArrayElements(byte_array, nullptr);
    jsize lengthOfArray = env->GetArrayLength(byte_array);
    env->ReleaseByteArrayElements(byte_array, bufferPtr, JNI_ABORT);

    std::vector<jfloat> currentVector;
    std::vector<jbyte> tempByteVector;
    int flagSeparator = 0;

    for (int i = 0; i < lengthOfArray; ++i) {
        if (bufferPtr[i] == 44 || bufferPtr[i] == 10) {
            flagSeparator++;
        } else {
            tempByteVector.push_back(bufferPtr[i]);
        }
    }

    std::string currentString(tempByteVector.begin(), tempByteVector.end());
    jfloat currentFloatNumber = std::stof(currentString);

    return currentFloatNumber ;
}
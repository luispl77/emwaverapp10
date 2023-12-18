#include <jni.h>
#include <string>
#include <vector>

std::vector<char> dataBuffer;

bool isRecording = true;

bool isRecordingContinuous = false;

extern "C" {

JNIEXPORT void JNICALL Java_com_example_emwaver10_SerialService_setRecording(JNIEnv *env, jobject, jboolean recording) {
    isRecording = recording;
}

JNIEXPORT jboolean JNICALL Java_com_example_emwaver10_SerialService_getRecording(JNIEnv *env, jobject) {
    return isRecording;
}

JNIEXPORT void JNICALL Java_com_example_emwaver10_SerialService_setRecordingContinuous(JNIEnv *env, jobject, jboolean recording) {
    isRecordingContinuous = recording;
}

JNIEXPORT jboolean JNICALL Java_com_example_emwaver10_SerialService_getRecordingContinuous(JNIEnv *env, jobject) {
    return isRecordingContinuous;
}

JNIEXPORT void JNICALL Java_com_example_emwaver10_SerialService_addToBuffer(JNIEnv *env, jobject, jbyteArray data) {
    if (!isRecording) {
        return;
    }

    jbyte* bufferPtr = env->GetByteArrayElements(data, nullptr);
    jsize lengthOfArray = env->GetArrayLength(data);

    dataBuffer.insert(dataBuffer.end(), bufferPtr, bufferPtr + lengthOfArray);
    env->ReleaseByteArrayElements(data, bufferPtr, JNI_ABORT);
}

JNIEXPORT jint JNICALL Java_com_example_emwaver10_SerialService_getBufferLength(JNIEnv *env, jobject) {
    return static_cast<jint>(dataBuffer.size());
}

JNIEXPORT void JNICALL Java_com_example_emwaver10_SerialService_clearBuffer(JNIEnv *env, jobject) {
    dataBuffer.clear();
}


JNIEXPORT jbyteArray JNICALL Java_com_example_emwaver10_SerialService_pollData(JNIEnv *env, jobject, jint length) {
    int lenToPoll = std::min(static_cast<int>(dataBuffer.size()), length);
    jbyteArray returnArray = env->NewByteArray(lenToPoll);

    if (lenToPoll > 0) {
        auto startIt = dataBuffer.begin();
        auto endIt = startIt + lenToPoll;

        // Copy the data into a temporary buffer
        std::vector<char> tempBuffer(startIt, endIt);
        env->SetByteArrayRegion(returnArray, 0, lenToPoll, reinterpret_cast<const jbyte*>(tempBuffer.data()));

        // Remove the polled data from the buffer
        dataBuffer.erase(startIt, endIt);
    }

    return returnArray;
}


JNIEXPORT jobjectArray JNICALL Java_com_example_emwaver10_SerialService_compressData(JNIEnv *env, jobject, jint rangeStart, jint rangeEnd, jint numberBins) {
    float totalPointsInRange = rangeEnd - rangeStart;
    std::vector<float> timeValues;
    std::vector<float> dataValues;

    jclass floatArrayClass = env->FindClass("[F");
    jobjectArray result = env->NewObjectArray(2, floatArrayClass, nullptr);

    if (totalPointsInRange <= numberBins) {
        for (int i = rangeStart; i < rangeEnd; ++i) {
            if (i < dataBuffer.size()) {
                timeValues.push_back(static_cast<float>(i));
                dataValues.push_back(static_cast<float>(dataBuffer[i] & 0xFF));
            }
        }
    } else {
        float binWidth = totalPointsInRange / (float)numberBins;
        for (int i = 0; i < numberBins; ++i) {
            int binStart = (int) (rangeStart + i * binWidth);
            int binEnd = (int) (binStart + binWidth);
            binEnd = std::min(binEnd, rangeEnd);
            int maxVal = INT_MIN;
            int minVal = INT_MAX;

            for (int j = binStart; j < binEnd; ++j) {
                if (j < dataBuffer.size()) {
                    int val = dataBuffer[j] & 0xFF;
                    maxVal = std::max(maxVal, val);
                    minVal = std::min(minVal, val);
                }
            }

            timeValues.push_back(static_cast<float>(binStart));
            dataValues.push_back(static_cast<float>(minVal));
            timeValues.push_back(static_cast<float>(binEnd - 1));
            dataValues.push_back(static_cast<float>(maxVal));
        }
    }

    jfloatArray timeArray = env->NewFloatArray(timeValues.size());
    jfloatArray dataArray = env->NewFloatArray(dataValues.size());
    env->SetFloatArrayRegion(timeArray, 0, timeValues.size(), timeValues.data());
    env->SetFloatArrayRegion(dataArray, 0, dataValues.size(), dataValues.data());

    env->SetObjectArrayElement(result, 0, timeArray);
    env->SetObjectArrayElement(result, 1, dataArray);

    env->DeleteLocalRef(timeArray);
    env->DeleteLocalRef(dataArray);

    return result;
}




}
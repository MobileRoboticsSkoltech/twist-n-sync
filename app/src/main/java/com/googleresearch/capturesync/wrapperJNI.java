package com.googleresearch.capturesync;

public class wrapperJNI {

    static {
        System.loadLibrary("native");
    }

    native public static float sendData(byte[] byteArray);
}
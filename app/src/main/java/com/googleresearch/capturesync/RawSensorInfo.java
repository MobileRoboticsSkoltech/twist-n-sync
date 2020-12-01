/*
 * Copyright 2020 Mobile Robotics Lab. at Skoltech.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.googleresearch.capturesync;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;


/**
 * Handles gyroscope and accelerometer raw info recording
 */
public class RawSensorInfo implements SensorEventListener {
    private static final String TAG = "RawSensorInfo";
    private static final String SENSOR_TYPE_GYRO = "gyro";
    private static final String CSV_SEPARATOR = ",";

    final private SensorManager mSensorManager;
    final private Sensor mSensor;
    private PrintWriter mGyroBufferedWriter;
    private String mLastGyroPath;
    private File mGyroFile;

    private boolean mIsRecording;

    public RawSensorInfo(Context context) {
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        Log.d(TAG, "RawSensorInfo");
        if (mSensor == null) {
            Log.d(TAG, "gyroscope not available");
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (mIsRecording && event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            for (int j = 0; j < 3; j++) {
                mGyroBufferedWriter.write(
                        event.values[j] + CSV_SEPARATOR
                );
            }
            mGyroBufferedWriter.write(String.valueOf(event.timestamp));
            mGyroBufferedWriter.append("\n");
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO: Add logs for when sensor accuracy decreased
    }

    /**
     * Handles sensor info file creation, uses StorageUtils to work both with SAF and standard file
     * access. Saves files /{dirPath}/{sensor type}_timestamp.csv
     */
    private File getRawSensorInfoFileWriter(
            Context context, String sensorType,
            String dirPath, String timeStamp
    ) {
        File directory = new File(context.getExternalFilesDir(null), dirPath);
        if (! directory.exists()){
            directory.mkdir();
        }
        cleanupDir(directory);
        File saveFile = new File(
                directory, sensorType + "_" + timeStamp + ".csv"
        );
        Log.d(TAG, "Save gyro file to: " + saveFile.getAbsolutePath());
        return saveFile;
    }

    private PrintWriter setupRawSensorInfoWriter(
            Context context, String sensorType, File saveFile
    ) throws IOException {
        FileWriter rawSensorInfoFileWriter = new FileWriter(saveFile);
        PrintWriter rawSensorInfoWriter = new PrintWriter(
                new BufferedWriter(rawSensorInfoFileWriter)
        );
        return rawSensorInfoWriter;
    }

    public void startRecording(
            Context context, String dirPath, String timeStamp
    ) {
        try {
            mGyroFile = getRawSensorInfoFileWriter(context, SENSOR_TYPE_GYRO, dirPath, timeStamp);

            mGyroBufferedWriter = setupRawSensorInfoWriter(
                    context, SENSOR_TYPE_GYRO, mGyroFile
            );
            mIsRecording = true;
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Unable to setup sensor info writer");
        }
    }

    public void stopRecording() {
        mLastGyroPath = mGyroFile.getAbsolutePath();
        Log.d(TAG, "Close all files");
        if (mGyroBufferedWriter != null) {
            mGyroBufferedWriter.flush();
            mGyroBufferedWriter.close();
        }
        mIsRecording = false;
    }

    public String getLastGyroPath() {
        return mLastGyroPath;
    }

    public boolean isRecording() {
        return mIsRecording;
    }

    public void enableSensors(int gyroSampleRate) {
        Log.d(TAG, "enableSensors");
        if (mSensor != null) {
            mSensorManager.registerListener(this, mSensor, gyroSampleRate);
        }
    }

    public void disableSensors() {
        Log.d(TAG, "disableSensors");
        mSensorManager.unregisterListener(this);
    }

    private void cleanupDir(File fileOrDirectory) {
        File[] listFiles = fileOrDirectory.listFiles();
        if (fileOrDirectory.isDirectory() && listFiles != null)
            for (File child : listFiles)
                child.delete();
    }
}

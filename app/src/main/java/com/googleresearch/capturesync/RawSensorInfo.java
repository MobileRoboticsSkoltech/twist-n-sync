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
    private static final String SENSOR_TYPE_ACCEL = "accel";
    private static final String SENSOR_TYPE_GYRO = "gyro";
    private static final String CSV_SEPARATOR = ",";

    final private SensorManager mSensorManager;
    final private Sensor mSensor;
    final private Sensor mSensorAccel;
    private PrintWriter mGyroBufferedWriter;
    private PrintWriter mAccelBufferedWriter;
    private String mLastGyroPath;
    private String mLastAccelPath;
    private File mGyroFile;
    private File mAccelFile;

    private boolean mIsRecording;

    public RawSensorInfo(Context context) {
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mSensorAccel = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        Log.d(TAG, "RawSensorInfo");
        if (mSensor == null) {
            Log.d(TAG, "gyroscope not available");
        }
        if (mSensorAccel == null) {
            Log.d(TAG, "accelerometer not available");
        }
    }

    public int getSensorMinDelay(int sensorType) {
        if (sensorType == Sensor.TYPE_ACCELEROMETER) {
            return mSensorAccel.getMinDelay();
        } else if (sensorType == Sensor.TYPE_GYROSCOPE) {
            return mSensor.getMinDelay();
        } else {
            // Unsupported sensorType
                Log.d(TAG, "Unsupported sensor type was provided");
            return 0;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (mIsRecording) {
            StringBuffer sensorData = new StringBuffer();

            for (int j = 0; j < 3; j++) {
                sensorData.append(event.values[j]);
                sensorData.append(CSV_SEPARATOR);
            }
            sensorData.append(event.timestamp);
            sensorData.append('\n');

            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                mAccelBufferedWriter.write(sensorData.toString());
            } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                mGyroBufferedWriter.write(sensorData.toString());
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO: Add logs for when sensor accuracy decreased
    }

    /**
     * Handles sensor info file creation, uses StorageUtils to work both with SAF and standard file
     * access. Saves files /{dirPath}/{sensor type}_timeStamp.csv
     */
    private File getRawSensorInfoFileWriter(
            Context context, String sensorType,
            String dirPath, String timeStamp
    ) {
        File directory = new File(context.getExternalFilesDir(null), dirPath);
        if (! directory.exists()){
            directory.mkdir();
        }
        File saveFile = new File(
                directory, sensorType + "_" + timeStamp + ".csv"
        );
        Log.d(TAG, "save to: " + saveFile.getAbsolutePath());
        return saveFile;
    }

    private PrintWriter setupRawSensorInfoWriter(Context context, String sensorType,
                                                 File saveFile) throws IOException {
        FileWriter rawSensorInfoFileWriter = new FileWriter(saveFile);
        PrintWriter rawSensorInfoWriter = new PrintWriter(
                new BufferedWriter(rawSensorInfoFileWriter)
        );
        return rawSensorInfoWriter;
    }

    public boolean startRecording(Context context, String dirPath, String timeStamp) {
        try {
            mAccelFile = getRawSensorInfoFileWriter(context, SENSOR_TYPE_ACCEL, dirPath, timeStamp);
            mGyroFile = getRawSensorInfoFileWriter(context, SENSOR_TYPE_GYRO, dirPath, timeStamp);

            mGyroBufferedWriter = setupRawSensorInfoWriter(
                    context, SENSOR_TYPE_GYRO, mGyroFile
            );
            mAccelBufferedWriter = setupRawSensorInfoWriter(
                    context, SENSOR_TYPE_ACCEL, mAccelFile
            );
            mIsRecording = true;
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Unable to setup sensor info writer");
            return false;
        }
    }

    public void stopRecording() {
        mLastGyroPath = mGyroFile.getAbsolutePath();
        mLastAccelPath = mAccelFile.getAbsolutePath();
        Log.d(TAG, "Close all files");
        if (mGyroBufferedWriter != null) {
            mGyroBufferedWriter.flush();
            mGyroBufferedWriter.close();
        }
        if (mAccelBufferedWriter != null) {
            mAccelBufferedWriter.flush();
            mAccelBufferedWriter.close();
        }
        mIsRecording = false;
    }

    public String getLastGyroPath() {
        return mLastGyroPath;
    }

    public boolean isRecording() {
        return mIsRecording;
    }

    public void enableSensors(int accelSampleRate, int gyroSampleRate) {
        Log.d(TAG, "enableSensors");
        if (mSensor != null) {
            mSensorManager.registerListener(this, mSensor, gyroSampleRate);
        }
        if (mSensorAccel != null) {
            mSensorManager.registerListener(this, mSensorAccel, accelSampleRate);
        }
    }

    public void disableSensors() {
        Log.d(TAG, "disableSensors");
        mSensorManager.unregisterListener(this);
    }
}

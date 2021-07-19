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

package com.googleresearch.capturesync.softwaresync;

import android.media.AudioManager;
import android.media.ToneGenerator;
import android.util.Log;
import android.widget.Toast;

import com.googleresearch.capturesync.Constants;
import com.googleresearch.capturesync.MainActivity;
import com.googleresearch.capturesync.RawSensorInfo;
import com.googleresearch.capturesync.wrapperJNI;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
/**
 * IMU time sync class, is used to synchronize device local clocks.
 * The synchronization algorithm itself should be running on server,
 * the class only exchanges information with it and handles offset receiving
 *
 * <p>Provides a doTimeSync function allowing the leader to initiate synchronization with a client
 * address. The ImuTimeSyncListener executes the client side.
 */
public class ImuTimeSync extends TimeSyncProtocol {
    private static final String TAG = "ImuTimeSync";
    private final ExecutorService mTimeSyncExecutor = Executors.newSingleThreadExecutor();
    private final MainActivity mContext;
    private final FileTransferUtils mFileUtils;

    @Override
    protected ExecutorService getTimeSyncExecutor() {
        return mTimeSyncExecutor;
    }

    public ImuTimeSync(
            Ticker localClock, DatagramSocket timeSyncSocket, int timeSyncPort, SoftwareSyncLeader leader, MainActivity context) {
        super(localClock, timeSyncSocket, timeSyncPort, leader);
        mContext = context;
        mFileUtils = new FileTransferUtils(context);
    }

    /**
     *  Is executed on leader smartphone,
     *  exchanges information with gyro sync algorithm
     *  which is executed on PC. Returns calculated offset.
     */
    @Override
    protected TimeSyncOffsetResponse doTimeSync(InetAddress clientAddress) {
        // TODO: move sound of start and stop recording to VIEW somehow?
        ToneGenerator beep = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
        byte[] bufferStart = ByteBuffer.allocate(SyncConstants.RPC_BUFFER_SIZE).putInt(
                SyncConstants.METHOD_MSG_START_RECORDING
        ).array();
        byte[] bufferStop = ByteBuffer.allocate(SyncConstants.RPC_BUFFER_SIZE).putInt(
                SyncConstants.METHOD_MSG_STOP_RECORDING
        ).array();
        File gyroFileClient;
        RawSensorInfo recorder = new RawSensorInfo(mContext);
        DatagramPacket packetStart = new DatagramPacket(bufferStart, bufferStart.length, clientAddress, mTimeSyncPort);
        try (
                ServerSocket recServerSocket = new ServerSocket(mTimeSyncPort)
        ) {
            mTimeSyncSocket.send(packetStart);
            Log.d(TAG, "Sent packet start recording to client, recording...");

            recorder.enableSensors(Constants.GYRO_PERIOD_US);
            String timeStamp = new SimpleDateFormat("dd.MM.HH.mm.ss").format(new Date());
            recorder.startRecording(mContext, Constants.LOCAL_SENSOR_DIR, timeStamp);
            // Recording process
            Log.d(TAG, "Started recording");
            beep.startTone(ToneGenerator.TONE_CDMA_PIP,150);

            Thread.sleep(SyncConstants.SENSOR_REC_PERIOD_MILLIS);
            recorder.stopRecording();
            recorder.disableSensors();
            Log.d(TAG, "Stopped recording");
            beep.startTone(ToneGenerator.TONE_CDMA_PIP,150);

            Log.d(TAG, "Sent stop recording packet to client");

            // Accept file
            Log.d(TAG, "Connecting to Client...");
            Socket receiveSocket = recServerSocket.accept();
            Log.d(TAG, "Connected to Client...");
            gyroFileClient = mFileUtils.receiveFile("gyro_client.csv", receiveSocket);

            // Send files to PC
            File gyroFileLeader = new File(recorder.getLastGyroPath());
            return doGyroSyncInLibrary(
                    gyroFileClient, gyroFileLeader
            );
        } catch (IOException | InterruptedException e) {
            showMessageOnUi("Sync failed: couldn't collect sensor files");
            e.printStackTrace();
            return TimeSyncOffsetResponse.create(0, 0, false);
        } finally {
            beep.release();
            if (recorder.isRecording()) {
                recorder.stopRecording();
                recorder.disableSensors();
            }
        }
    }

    /**
     * Computes offset in library
     * @return
     */
    private TimeSyncOffsetResponse doGyroSyncInLibrary(
            File gyroFileClient,
            File gyroFileLeader
    ) {
        return TimeSyncOffsetResponse.create(0, 0, false);
    }

    private void showMessageOnUi(String message) {
        mContext.runOnUiThread(
                () -> Toast.makeText(mContext, message, Toast.LENGTH_LONG).show()
        );
    }
}

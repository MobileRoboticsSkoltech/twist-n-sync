package com.googleresearch.capturesync.softwaresync;

import android.content.Context;
import android.util.Log;

import com.googleresearch.capturesync.Constants;
import com.googleresearch.capturesync.RawSensorInfo;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ImuTimeSyncListener extends Thread {
    private static final String TAG = "ImuTimeSyncListener";
    private boolean running;
    private final DatagramSocket imuTimeSyncSocket;
    private final int imuTimeSyncPort;
    private final Ticker localClock;
    private final Context mContext;
    private final FileTransferUtils mFileUtils;

    public ImuTimeSyncListener(Ticker localClock, DatagramSocket imuTimeSyncSocket, int imuTimeSyncPort, Context context) {
        this.localClock = localClock;
        this.imuTimeSyncSocket = imuTimeSyncSocket;
        this.imuTimeSyncPort = imuTimeSyncPort;
        this.mContext = context;
        mFileUtils = new FileTransferUtils(context);
    }

    public void stopRunning() {
        running = false;
    }

    @Override
    public void run() {
        running = true;

        Log.w(TAG, "Starting IMU Time Sync Listener thread.");
        final int longSize = Long.SIZE / Byte.SIZE;

        byte[] buf = new byte[longSize * 3];

        RawSensorInfo recorder = new RawSensorInfo(mContext);

        while (running && !imuTimeSyncSocket.isClosed()) {
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            try {
                // Listen for recording start messages
                imuTimeSyncSocket.receive(packet);

                ByteBuffer packetByteBuffer = ByteBuffer.wrap(packet.getData());
                int method = packetByteBuffer.getInt();
                if (method != SyncConstants.METHOD_MSG_START_RECORDING) {
                    Log.e(
                            TAG,
                            "Received UDP message with incorrect method "
                                    + method
                                    + ", skipping.");
                    continue;
                }

                recorder.enableSensors(Constants.GYRO_PERIOD_US);
                String timeStamp = new SimpleDateFormat("dd.MM.HH.mm.ss").format(new Date());
                recorder.startRecording(mContext, Constants.LOCAL_SENSOR_DIR, timeStamp);
                // Recording process
                Log.d(TAG, "Started recording");
                Thread.sleep(SyncConstants.SENSOR_REC_PERIOD_MILLIS);
                recorder.stopRecording();
                recorder.disableSensors();
                Log.d(TAG, "Stopped recording");

                // File transfer
                File file = new File(recorder.getLastGyroPath());
                Log.d(TAG, "Sensor file opened");
                Socket sendSocket = new Socket(packet.getAddress(), imuTimeSyncPort);
                Log.d(TAG, "Connected to leader");
                mFileUtils.sendFile(file, sendSocket);
            } catch (SocketTimeoutException | InterruptedException e) {
                // TODO: autogenerated
            } catch (IOException e) {
                if (running || imuTimeSyncSocket.isClosed()) {
                    Log.w(TAG, "Shutdown arrived in middle of a socket receive, ignoring error.");
                } else {
                    throw new IllegalStateException("Socket Receive/Send error: " + e);
                }
            } finally {
                if (recorder.isRecording()) {
                    recorder.stopRecording();
                    recorder.disableSensors();
                }
            }
        }
        Log.w(TAG, "Time Sync Listener thread finished.");
    }
}

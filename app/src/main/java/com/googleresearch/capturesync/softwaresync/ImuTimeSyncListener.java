package com.googleresearch.capturesync.softwaresync;

import android.content.Context;
import android.util.Log;

import com.googleresearch.capturesync.RawSensorInfo;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
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
    private final Context mContext;
    private final Ticker localClock;

    public ImuTimeSyncListener(Ticker localClock, DatagramSocket imuTimeSyncSocket, int imuTimeSyncPort, Context context) {
        this.localClock = localClock;
        this.imuTimeSyncSocket = imuTimeSyncSocket;
        this.imuTimeSyncPort = imuTimeSyncPort;
        this.mContext = context;
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

                RawSensorInfo recorder = new RawSensorInfo(mContext);
                recorder.enableSensors(0, 0);
                String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
                recorder.startRecording(mContext, timeStamp);
                // Recording process
                Log.d(TAG, "Started recording");

                // Receive stop recording message,
                // need to temporarily increase socket wait time
                imuTimeSyncSocket.setSoTimeout(
                        SyncConstants.SOCKET_WAIT_TIME_MS +
                        SyncConstants.SENSOR_REC_PERIOD_MILLIS
                );
                imuTimeSyncSocket.receive(packet);
                // Return short wait time. TODO: more elegant implementation?
                imuTimeSyncSocket.setSoTimeout(
                        SyncConstants.SOCKET_WAIT_TIME_MS
                );
                packetByteBuffer = ByteBuffer.wrap(packet.getData());
                method = packetByteBuffer.getInt();
                if (method != SyncConstants.METHOD_MSG_STOP_RECORDING) {
                    Log.e(
                            TAG,
                            "Received UDP message with incorrect method "
                                    + method
                                    + ", skipping.");
                    continue;
                }
                recorder.stopRecording();
                recorder.disableSensors();

                // File transfer TODO: move to separate method
                Socket sendSocket;
                FileDetails details;
                byte data[];
                String path = mContext.getExternalFilesDir(null).getPath() + "/" + timeStamp;
                // TODO: add unique filenames!!!! Move to constants
                File file = new File(path, "gyro_" + timeStamp);
                Log.d(TAG, "Sensor file opened");
                sendSocket = new Socket(packet.getAddress(), imuTimeSyncPort);
                // File Object for accesing file Details
                Log.d(TAG, "Connected to Server...");
                data = new byte[2048];
                details = new FileDetails();
                details.setDetails(file.getName(), file.length());

                // Sending file details to the client
                Log.d(TAG, "Sending file details...");
                ObjectOutputStream sendDetails = new ObjectOutputStream(sendSocket.getOutputStream());
                sendDetails.writeObject(details);
                sendDetails.flush();
                // Sending File Data
                Log.d(TAG, "Sending file data...");
                FileInputStream fileStream = new FileInputStream(file);
                BufferedInputStream fileBuffer = new BufferedInputStream(fileStream);
                OutputStream out = sendSocket.getOutputStream();
                int count;
                while ((count = fileBuffer.read(data)) > 0) {
                    Log.d(TAG, "Data Sent : " + count);
                    out.write(data, 0, count);
                    out.flush();
                }
                out.close();
                fileBuffer.close();
                fileStream.close();

            } catch (SocketTimeoutException e) {
                // TODO
            } catch (IOException e) {
                if (running || imuTimeSyncSocket.isClosed()) {
                    Log.w(TAG, "Shutdown arrived in middle of a socket receive, ignoring error.");
                } else {
                    throw new IllegalStateException("Socket Receive/Send error: " + e);
                }
            }
        }
        Log.w(TAG, "Time Sync Listener thread finished.");
    }
}

package com.googleresearch.capturesync.softwaresync;

import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.util.Log;

import com.googleresearch.capturesync.Constants;
import com.googleresearch.capturesync.RawSensorInfo;

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

public class ImuTimeSync extends TimeSyncProtocol {
    private static final String TAG = "ImuTimeSync";
    private final ExecutorService mTimeSyncExecutor = Executors.newSingleThreadExecutor();
    private final Context mContext;
    private final FileTransferUtils mFileUtils;

    @Override
    protected ExecutorService getTimeSyncExecutor() {
        return mTimeSyncExecutor;
    }

    public ImuTimeSync(
            Ticker localClock, DatagramSocket timeSyncSocket, int timeSyncPort, SoftwareSyncLeader leader, Context context) {
        super(localClock, timeSyncSocket, timeSyncPort, leader);
        mContext = context;
        mFileUtils = new FileTransferUtils(context);
    }

    /**
     *  Is executed on leader
     *  smartphone, performs gyroSync
     *  algorithm and returns calculated offset
     * @param clientAddress
     * @return
     */
    @Override
    protected TimeSyncOffsetResponse doTimeSync(InetAddress clientAddress) {
        // TODO: specify Dialog in ui, then call OnResponse here and perform sync

        // TODO: move sound of start and stop recording to VIEW (callbacks?)
        ToneGenerator beep = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
        byte[] bufferStart = ByteBuffer.allocate(SyncConstants.RPC_BUFFER_SIZE).putInt(
                SyncConstants.METHOD_MSG_START_RECORDING
        ).array();
        byte[] bufferStop = ByteBuffer.allocate(SyncConstants.RPC_BUFFER_SIZE).putInt(
                SyncConstants.METHOD_MSG_STOP_RECORDING
        ).array();

        DatagramPacket packetStart = new DatagramPacket(bufferStart, bufferStart.length, clientAddress, mTimeSyncPort);
        DatagramPacket packetStop = new DatagramPacket(bufferStop, bufferStop.length, clientAddress, mTimeSyncPort);
        try (
                ServerSocket recServerSocket = new ServerSocket(mTimeSyncPort)
        ) {
            mTimeSyncSocket.send(packetStart);
            Log.d(TAG, "Sent packet start recording to client, recording...");
            RawSensorInfo recorder = new RawSensorInfo(mContext);
            recorder.enableSensors(0, 0);
            String timeStamp = new SimpleDateFormat("dd.MM.HH.mm.ss").format(new Date());
            recorder.startRecording(mContext, Constants.LOCAL_SENSOR_DIR, timeStamp);
            // Recording process
            Log.d(TAG, "Started recording");
            beep.startTone(ToneGenerator.TONE_CDMA_PIP,150);

            Thread.sleep(SyncConstants.SENSOR_REC_PERIOD_MILLIS);
            recorder.stopRecording();
            recorder.disableSensors();
            Log.d(TAG, "Stopped recording");
            mTimeSyncSocket.send(packetStop);
            beep.startTone(ToneGenerator.TONE_CDMA_PIP,150);

            Log.d(TAG, "Sent stop recording packet to client");


            // Accept file
            Log.d(TAG, "Connecting to Client...");
            Socket receiveSocket = recServerSocket.accept();
            Log.d(TAG, "Connected to Client...");
            File gyroFileClient = mFileUtils.receiveFile("gyro_client.csv", receiveSocket);

            // Send files to PC
            File gyroFileLeader = new File(recorder.getLastGyroPath());
            return doGyroSyncOnServer(
                    gyroFileClient, gyroFileLeader
            );
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return TimeSyncOffsetResponse.create(0, 0, false);
        } finally {
            beep.release();
        }
    }

    /**
     *
     * @return
     */
    private TimeSyncOffsetResponse doGyroSyncOnServer(
            File gyroFileClient,
            File gyroFileLeader
    ) {
        try {
            // Send client file to PC
            mFileUtils.sendFile(
                    gyroFileClient, new Socket(
                            InetAddress.getByName(Constants.PC_SERVER_IP),
                            mTimeSyncPort
                    )
            );

            // Send leader file to PC
            mFileUtils.sendFile(
                    gyroFileLeader, new Socket(
                            InetAddress.getByName(Constants.PC_SERVER_IP),
                            mTimeSyncPort
                    )
            );
        } catch (UnknownHostException e) {
            e.printStackTrace();
            Log.e(TAG, "Could not determine server host IP address");
            return TimeSyncOffsetResponse.create(0, 0, false);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to compute offset on server");
            return TimeSyncOffsetResponse.create(0, 0, false);
        }

        try (
            Socket sc = new Socket(
                InetAddress.getByName(Constants.PC_SERVER_IP),
                mTimeSyncPort
            )
        ) {
            InputStream in = sc.getInputStream();
            DataInputStream dataInputStream = new DataInputStream(in);
            double offsetNs = dataInputStream.readDouble();
            if (offsetNs > 0) {
                //double offsetNs = offsetData.
                Log.d(TAG, "Success! Received offset from server: " + offsetNs / 1e9 + " seconds");
                // TODO: modify response with more error values and remove sync accuracy logic
                return TimeSyncOffsetResponse.create((long) offsetNs, -1, true);
            } else {
                Log.d(TAG, "Couldn't get offset from server");
                return TimeSyncOffsetResponse.create(0, 0, false);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to receive offset from server");
            return TimeSyncOffsetResponse.create(0, 0, false);
        }
    }

}

package com.googleresearch.capturesync.softwaresync;

import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.util.Log;
import android.widget.Toast;

import com.googleresearch.capturesync.Constants;
import com.googleresearch.capturesync.MainActivity;
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
     *  Is executed on leader
     *  smartphone, performs gyroSync
     *  algorithm and returns calculated offset
     * @param clientAddress
     * @return
     */
    @Override
    protected TimeSyncOffsetResponse doTimeSync(InetAddress clientAddress) {
        // TODO: move sound of start and stop recording to VIEW (callbacks?)
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
            beep.startTone(ToneGenerator.TONE_CDMA_PIP,150);

            Log.d(TAG, "Sent stop recording packet to client");

            // Accept file
            Log.d(TAG, "Connecting to Client...");
            Socket receiveSocket = recServerSocket.accept();
            Log.d(TAG, "Connected to Client...");
            gyroFileClient = mFileUtils.receiveFile("gyro_client.csv", receiveSocket);

            // Send files to PC
            File gyroFileLeader = new File(recorder.getLastGyroPath());
            return doGyroSyncOnServer(
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
     * Computes offset on server
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
            showMessageOnUi("Sync failed: couldn't determine server host IP adderss");
            Log.e(TAG, "Could not determine server host IP address");
            return TimeSyncOffsetResponse.create(0, 0, false);
        } catch (IOException e) {
            e.printStackTrace();
            showMessageOnUi("Sync failed: couldn't send files to server");
            Log.e(TAG, "Failed to send files to server");
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
            showMessageOnUi("Sync successful: Received offset from server: " + offsetNs / 1e9 + " seconds");
            Log.d(TAG, "Success! Received offset from server: " + offsetNs / 1e9 + " seconds");
            // TODO: modify response with more error values and remove sync accuracy logic
            return TimeSyncOffsetResponse.create((long) offsetNs, 0, true);

        } catch (IOException e) {
            e.printStackTrace();
            showMessageOnUi("Sync failed: couldn't receive offset from server");
            Log.e(TAG, "Failed to receive offset from server");
            return TimeSyncOffsetResponse.create(0, 0, false);
        }
    }

    private void showMessageOnUi(String message) {
        mContext.runOnUiThread(
                () -> Toast.makeText(mContext, message, Toast.LENGTH_LONG).show()
        );
    }
}

package com.googleresearch.capturesync.softwaresync;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class ImuTimeSync extends TimeSyncProtocol {
    private static final String TAG = "ImuTimeSync";

    public ImuTimeSync(
            Ticker localClock, DatagramSocket timeSyncSocket, int timeSyncPort, SoftwareSyncLeader leader) {
        super(localClock, timeSyncSocket, timeSyncPort, leader);
    }

    @Override
    protected TimeSyncOffsetResponse doTimeSync(InetAddress clientAddress) {
        // TODO: gyroSync implementation

        return TimeSyncOffsetResponse.create(42, 42, false);
    }
}

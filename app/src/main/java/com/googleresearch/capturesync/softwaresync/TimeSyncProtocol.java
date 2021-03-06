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

import android.util.Log;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

abstract class TimeSyncProtocol implements AutoCloseable {
    private static final String TAG = "TimeSyncProtocol";

    /** Sequentially manages time synchronization of clients. */
    protected abstract ExecutorService getTimeSyncExecutor();

    protected final DatagramSocket mTimeSyncSocket;
    protected final int mTimeSyncPort;

    /** Keeps track of client sync tasks already in the pipeline to avoid duplicate requests. */
    protected final Set<InetAddress> mClientSyncTasks = new HashSet<>();

    protected final Object mClientSyncTasksLock = new Object();
    protected final SoftwareSyncLeader mLeader;
    protected final Ticker mLocalClock;

    public TimeSyncProtocol(
            Ticker localClock, DatagramSocket timeSyncSocket, int timeSyncPort, SoftwareSyncLeader leader) {
        this.mLocalClock = localClock;
        this.mTimeSyncSocket = timeSyncSocket;
        this.mTimeSyncPort = timeSyncPort;
        this.mLeader = leader;
    }

    /**
     * Check if requesting client is already in the queue. If not, then submit a new task to do time
     * synchronization with that client. Synchronization involves sending and receiving messages on
     * the time sync socket, calculating the clock offsetNs, and finally sending an rpc to update the
     * offsetNs on the client.
     */
    @SuppressWarnings("FutureReturnValueIgnored")
    void submitNewSyncRequest(final InetAddress clientAddress) {
        // Skip if we have already enqueued a sync task with this client.
        synchronized (mClientSyncTasksLock) {
            if (mClientSyncTasks.contains(clientAddress)) {
                Log.w(TAG, "Already queued sync with " + clientAddress + ", skipping.");
                return;
            } else {
                mClientSyncTasks.add(clientAddress);
            }
        }

        // Add time sync request to executor queue.
        this.getTimeSyncExecutor().submit(
                () -> preTimeSyncExecution(clientAddress));
    }

    protected boolean preTimeSyncExecution(InetAddress clientAddress) {
        // If the client no longer exists, no need to synchronize.
        if (!mLeader.getClients().containsKey(clientAddress)) {
            Log.w(TAG, "Client was removed, exiting time sync routine.");
            return true;
        }

        Log.d(TAG, "Starting sync with client" + clientAddress);
        // Calculate clock offsetNs between client and leader using a time sync protocol
        TimeSyncOffsetResponse response = doTimeSync(clientAddress);

        if (response.status()) {
            // Apply local offsetNs to bestOffset so everyone has the same offsetNs.
            final long alignedOffset = response.offsetNs() + mLeader.getLeaderFromLocalNs();

            // Update client sync accuracy locally.
            mLeader.updateClientWithOffsetResponse(clientAddress, response);

            // Send an RPC to update the offsetNs on the client.
            Log.d(TAG, "Sending offsetNs update to " + clientAddress + ": " + alignedOffset);
            mLeader.sendRpc(
                    SyncConstants.METHOD_OFFSET_UPDATE, String.valueOf(alignedOffset), clientAddress);
        }

        // Pop client from the queue regardless of success state. Clients  will be added back in
        // the queue as needed based on their state at the next heartbeat.
        synchronized (mClientSyncTasksLock) {
            mClientSyncTasks.remove(clientAddress);
        }

        if (response.status()) {
            mLeader.onRpc(SyncConstants.METHOD_MSG_OFFSET_UPDATED, clientAddress.toString());
        }

        return response.status();
    }

    protected abstract TimeSyncOffsetResponse doTimeSync(InetAddress clientAddress);

    @Override
    public void close() {
        this.getTimeSyncExecutor().shutdown();
        // Wait up to 0.5 seconds for the executor service to finish.
        try {
            this.getTimeSyncExecutor().awaitTermination(500, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new IllegalStateException("Time sync Executor didn't close gracefully: " + e);
        }
    }
}

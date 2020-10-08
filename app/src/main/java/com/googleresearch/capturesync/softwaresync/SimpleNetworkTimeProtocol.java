/**
 * Copyright 2019 The Google Research Authors.
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
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;

/**
 * Simple Network Time Protocol (SNTP) for clock synchronization logic between leader and clients.
 * This implements the leader half of the protocol, with SntpListener implementing the client side.
 *
 * <p>Provides a doSNTP function allowing the leader to initiate synchronization with a client
 * address. The SntpListener class is used by the clients to handle responding to these messages.
 */
public class SimpleNetworkTimeProtocol extends TimeSyncProtocol {
  private static final String TAG = "SimpleNetworkTimeProtocol";

  public SimpleNetworkTimeProtocol(
      Ticker localClock, DatagramSocket nptpSocket, int nptpPort, SoftwareSyncLeader leader) {
    super(localClock, nptpSocket, nptpPort, leader);
  }

  /**
   * Performs Min filter SNTP synchronization with the client over the socket using UDP.
   *
   * <p>Naive PTP protocol is as follows:
   *
   * <p>[1]At time t0 in the leader clock domain, Leader sends the message (t0).
   *
   * <p>[2]At time t1 in the client clock domain, Client receives the message (t0).
   *
   * <p>[3]At time t2 in the client clock domain, Client sends the message (t0,t1,t2).
   *
   * <p>[4]At time t3 in the leader clock domain, Leader receives the message (t0,t1,t2).
   *
   * <p>Clock offsetNs = ((t1 - t0) + (t2 - t3)) / 2. [Client] current_time_in_leader_domain = now()
   * - offsetNs.
   *
   * <p>Round-trip latency = (t3 - t0) - (t2 - t1).
   *
   * <p>Final Clock offsetNs is calculated using the message with the smallest round-trip latency.
   *
   * @param clientAddress The client InetAddress to perform synchronization with.
   * @return SntpOffsetResponse containing the offsetNs and sync accuracy with the client.
   */
  @Override
  protected TimeSyncOffsetResponse doTimeSync(InetAddress clientAddress) throws IOException {
    final int longSize = Long.SIZE / Byte.SIZE;
    byte[] buf = new byte[longSize * 3];
    long bestLatency = Long.MAX_VALUE; // Start with initial high round trip
    long bestOffset = 0;
    // If there are several failed SNTP round trip sync messages, fail out.
    int missingMessageCountdown = 10;
    TimeSyncOffsetResponse failureResponse =
        TimeSyncOffsetResponse.create(/*offset=*/ 0, /*syncAccuracy=*/ 0, false);

    for (int i = 0; i < SyncConstants.NUM_SNTP_CYCLES; i++) {
      // 1 - Send UDP SNTP message to the client with t0 at time t0.
      long t0 = mLocalClock.read();
      ByteBuffer t0bytebuffer = ByteBuffer.allocate(longSize);
      t0bytebuffer.putLong(t0);
      mTimeSyncSocket.send(new DatagramPacket(t0bytebuffer.array(), longSize, clientAddress, mTimeSyncPort));

      // Steps 2 and 3 happen on client side B.
      // 4 - Recv UDP message with t0,t0',t1 at time t1'.
      DatagramPacket packet = new DatagramPacket(buf, buf.length);
      try {
        mTimeSyncSocket.receive(packet);
      } catch (SocketTimeoutException e) {
        // If we didn't receive a message in time, then skip this PTP pair and continue.
        Log.w(TAG, "UDP PTP message missing, skipping");
        missingMessageCountdown--;
        if (missingMessageCountdown <= 0) {
          Log.w(
              TAG, String.format("Missed too many messages, leaving doTimeSync for %s", clientAddress));
          return failureResponse;
        }
        continue;
      }
      final long t3 = mLocalClock.read();

      if (packet.getLength() != 3 * longSize) {
        Log.w(TAG, "Corrupted UDP message, skipping");
        continue;
      }
      ByteBuffer t3buffer = ByteBuffer.allocate(longSize * 3);
      t3buffer.put(packet.getData(), 0, packet.getLength());
      t3buffer.flip();
      LongBuffer longbuffer = t3buffer.asLongBuffer();
      final long t0Msg = longbuffer.get();
      final long t1Msg = longbuffer.get();
      final long t2Msg = longbuffer.get();

      // Confirm that the received message contains the same t0  as the t0 from this cycle,
      // otherwise skip.
      if (t0Msg != t0) {
        Log.w(
            TAG,
            String.format(
                "Out of order PTP message received, skipping: Expected %d vs %d", t0, t0Msg));

        // Note: Wait or catch and throw away the next message to get back in sync.
        try {
          mTimeSyncSocket.receive(packet);
        } catch (SocketTimeoutException e) {
          // If still waiting, continue.
        }
        // Since this was an incorrect cycle, move on to a new cycle.
        continue;
      }

      final long timeOffset = ((t1Msg - t0) + (t2Msg - t3)) / 2;
      final long roundTripLatency = (t3 - t0) - (t2Msg - t1Msg);

      Log.v(
          TAG,
          String.format(
              "% 3d | PTP: %d,%d,%d,%d | Latency: %,.3f ms",
              i, t0, t1Msg, t2Msg, t3, TimeUtils.nanosToMillis((double) roundTripLatency)));

      if (roundTripLatency < bestLatency) {
        bestOffset = timeOffset;
        bestLatency = roundTripLatency;
        // If round trip latency is under minimum round trip latency desired, stop here.
        if (roundTripLatency < SyncConstants.MIN_ROUND_TRIP_LATENCY_NS) {
          break;
        }
      }
    }

    Log.v(
        TAG,
        String.format(
            "Client %s : SNTP best latency %,d ns, offsetNs %,d ns",
            clientAddress, bestLatency, bestOffset));

    return TimeSyncOffsetResponse.create(bestOffset, bestLatency, true);
  }
}

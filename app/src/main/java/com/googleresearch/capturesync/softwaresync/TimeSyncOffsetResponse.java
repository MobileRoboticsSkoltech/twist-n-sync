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
 *
 * Modifications copyright (C) 2020 Mobile Robotics Lab. at Skoltech
 */

package com.googleresearch.capturesync.softwaresync;

/** AutoValue class for time sync offsetNs, synchronization accuracy and status. */
public final class TimeSyncOffsetResponse {
  private final long offsetNs;
  private final long syncAccuracyNs;
  private final boolean status;

  static TimeSyncOffsetResponse create(long offset, long syncAccuracy, boolean status) {
    return new TimeSyncOffsetResponse(offset, syncAccuracy, status);
  }

  private TimeSyncOffsetResponse(long offsetNs, long syncAccuracyNs, boolean status) {
    this.offsetNs = offsetNs;
    this.syncAccuracyNs = syncAccuracyNs;
    this.status = status;
  }

  /**
   * The time delta (leader - client) in nanoseconds of the AP SystemClock domain.
   *
   * <p>The client can take their local_time to get leader_time via: local_time (leader - client) =
   * leader_time.
   */
  public long offsetNs() {
    return offsetNs;
  }

  /**
   * The worst case error in the clock domains between leader and client for this response, in
   * nanoseconds of the AP SystemClock domain.
   */
  public long syncAccuracyNs() {
    return syncAccuracyNs;
  }

  /** The success status of this response. */
  public boolean status() {
    return status;
  }
}

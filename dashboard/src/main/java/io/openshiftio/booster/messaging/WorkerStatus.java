/*
 * Copyright 2018 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.openshiftio.booster.messaging;

import java.util.Objects;

public class WorkerStatus {

    private final long timestamp;

    private final long requestsProcessed;

    public WorkerStatus(long timestamp, long requestsProcessed) {
        this.timestamp = timestamp;
        this.requestsProcessed = requestsProcessed;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getRequestsProcessed() {
        return requestsProcessed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        WorkerStatus that = (WorkerStatus) o;

        return timestamp == that.timestamp && requestsProcessed == that.requestsProcessed;
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, requestsProcessed);
    }

    @Override
    public String toString() {
        return String.format("WorkerStatus{timestamp=%s, requestsProcessed=%s}", timestamp, requestsProcessed);
    }

}

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

public class WorkerResponse {

    private final String workerId;

    private final String body;

    public WorkerResponse(String workerId, String body) {
        this.workerId = workerId;
        this.body = body;
    }

    public String getWorkerId() {
        return workerId;
    }

    public String getBody() {
        return body;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        WorkerResponse that = (WorkerResponse) o;

        return Objects.equals(workerId, that.workerId) && Objects.equals(body, that.body);
    }

    @Override
    public int hashCode() {
        return Objects.hash(workerId, body);
    }

    @Override
    public String toString() {
        return String.format("WorkerResponse{workerId=%s, body=%s}", workerId, body);
    }

}

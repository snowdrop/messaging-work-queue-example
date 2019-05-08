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

package dev.snowdrop.example.messaging;

import org.springframework.jms.support.JmsHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

public class Response {

    private final String requestId;

    private final String workerId;

    private final String text;

    public Response(Message<String> message) {
        MessageHeaders headers = message.getHeaders();
        this.requestId = headers.get(JmsHeaders.CORRELATION_ID, String.class);
        this.workerId = headers.get(FrontendHeaders.WORKER_ID, String.class);
        this.text = message.getPayload();
    }

    public String getRequestId() {
        return requestId;
    }

    public String getWorkerId() {
        return workerId;
    }

    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return String.format("Response{requestId=%s, workerId=%s, text=%s}", requestId, workerId, text);
    }

}

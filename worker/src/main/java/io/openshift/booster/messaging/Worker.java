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

package io.openshift.booster.messaging;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.JmsHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class Worker {

    private final WorkerProperties properties;

    private final JmsTemplate jmsTemplate;

    private final AtomicInteger requestsProcessed;

    public Worker(WorkerProperties properties, JmsTemplate jmsTemplate) {
        this(properties, jmsTemplate, new AtomicInteger(0));
    }

    Worker(WorkerProperties properties, JmsTemplate jmsTemplate, AtomicInteger requestsProcessed) {
        this.properties = properties;
        this.jmsTemplate = jmsTemplate;
        this.requestsProcessed = requestsProcessed;
    }

    @JmsListener(destination = "upstate/requests")
    public Message<String> handleRequest(Message<String> request) {
        System.out.println("WORKER-SPRING: Received request '" + request.getPayload() + "'");
        String responsePayload = processRequest(request);
        System.out.println("WORKER-SPRING: Sending response '" + responsePayload + "'");

        Message<String> response = MessageBuilder.withPayload(responsePayload)
                .setHeader(JmsHeaders.CORRELATION_ID, request.getHeaders().get(MessageHeaders.ID))
                .setHeader("worker_id", properties.getId())
                .build();

        requestsProcessed.incrementAndGet();

        return response;
    }

    @Scheduled(fixedRate = 5 * 1000)
    public void sendStatusUpdate() {
        System.out.println("WORKER-SPRING: Sending status update");

        jmsTemplate.send("upstate/worker-status", session -> {
            javax.jms.Message message = session.createTextMessage();
            message.setStringProperty("worker_id", properties.getId());
            message.setLongProperty("timestamp", System.currentTimeMillis());
            message.setLongProperty("requests_processed", requestsProcessed.get());
            return message;
        });
    }

    private String processRequest(Message<String> request) {
        return request.getPayload().toUpperCase();
    }

}

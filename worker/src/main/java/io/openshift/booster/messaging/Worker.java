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

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jms.TextMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class Worker {

    static final String REQUEST_QUEUE_NAME = "work-queue/requests";

    static final String UPDATE_QUEUE_NAME = "work-queue/worker-updates";

    private final Logger logger = LoggerFactory.getLogger(Worker.class);

    private final JmsTemplate jmsTemplate;

    private final String id;

    private final AtomicInteger requestsProcessed;

    private final AtomicInteger processingErrors;

    @Autowired
    public Worker(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
        this.id = "worker-spring-" + UUID.randomUUID().toString().substring(0, 4);
        this.requestsProcessed = new AtomicInteger(0);
        this.processingErrors = new AtomicInteger(0);
    }

    Worker(JmsTemplate jmsTemplate, String id, AtomicInteger requestsProcessed, AtomicInteger processingErrors) {
        this.jmsTemplate = jmsTemplate;
        this.id = id;
        this.requestsProcessed = requestsProcessed;
        this.processingErrors = processingErrors;
    }

    @JmsListener(destination = REQUEST_QUEUE_NAME)
    public Message<String> handleRequest(Message<String> request) {
        String responsePayload;
        try {
            responsePayload = processRequest(request);
        } catch (Exception e) {
            processingErrors.incrementAndGet();
            logger.error("{}: Failed processing message: {}", id, e.getMessage());
            return null;
        }

        Message<String> response = MessageBuilder.withPayload(responsePayload)
                .setHeader(WorkerHeaders.WORKER_ID, id)
                .build();

        requestsProcessed.incrementAndGet();
        logger.info("{}: Sent {}", id, response);

        return response;
    }

    @Scheduled(fixedRate = 5 * 1000)
    public void sendStatusUpdate() {
        logger.debug("{}: Sending status update", id);

        jmsTemplate.send(UPDATE_QUEUE_NAME, session -> {
            TextMessage message = session.createTextMessage();
            message.setStringProperty(WorkerHeaders.WORKER_ID, id);
            message.setLongProperty(WorkerHeaders.REQUESTS_PROCESSED, requestsProcessed.get());
            message.setLongProperty(WorkerHeaders.PROCESSING_ERRORS, processingErrors.get());
            return message;
        });
    }

    private String processRequest(Message<String> request) {
        boolean uppercase = request.getHeaders().get(WorkerHeaders.UPPERCASE, Boolean.class);
        boolean reverse = request.getHeaders().get(WorkerHeaders.REVERSE, Boolean.class);
        String text = request.getPayload();

        logger.info("{}: Processing request: text='{}', uppercase='{}', reverse='{}'", id, text, uppercase, reverse);

        if (uppercase) {
            text = text.toUpperCase();
        }

        if (reverse) {
            text = new StringBuilder(text).reverse().toString();
        }

        return text;
    }

}

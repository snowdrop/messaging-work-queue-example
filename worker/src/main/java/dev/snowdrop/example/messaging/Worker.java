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

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.TextMessage;
import javax.jms.Topic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class Worker {

    static final String UPDATE_TOPIC_NAME = "work-queue/worker-updates";

    private static final String REQUEST_QUEUE_NAME = "work-queue/requests";

    private final Logger logger = LoggerFactory.getLogger(Worker.class);

    private final ConnectionFactory connectionFactory;

    private final String id;

    private final AtomicInteger requestsProcessed;

    private final AtomicInteger processingErrors;

    @Autowired
    public Worker(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
        this.id = "worker-spring-" + UUID.randomUUID().toString().substring(0, 4);
        this.requestsProcessed = new AtomicInteger(0);
        this.processingErrors = new AtomicInteger(0);
    }

    Worker(ConnectionFactory connectionFactory, String id, AtomicInteger requestsProcessed,
            AtomicInteger processingErrors) {
        this.connectionFactory = connectionFactory;
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
    public void sendStatusUpdate() throws JMSException {
        logger.debug("{}: Sending status update", id);

        try (JMSContext jmsContext = connectionFactory.createContext()) {
            TextMessage message = jmsContext.createTextMessage();
            message.setStringProperty(WorkerHeaders.WORKER_ID, id);
            message.setLongProperty(WorkerHeaders.REQUESTS_PROCESSED, requestsProcessed.get());
            message.setLongProperty(WorkerHeaders.PROCESSING_ERRORS, processingErrors.get());

            JMSProducer producer = jmsContext.createProducer();

            Topic topic = jmsContext.createTopic(UPDATE_TOPIC_NAME);
            producer.send(topic, message);
        }
    }

    private String processRequest(Message<String> request) {
        String text = request.getPayload();

        MessageHeaders headers = request.getHeaders();
        boolean uppercase = headers.get(WorkerHeaders.UPPERCASE, Boolean.class);
        boolean reverse = headers.get(WorkerHeaders.REVERSE, Boolean.class);

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

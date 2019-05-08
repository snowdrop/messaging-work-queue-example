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

import java.util.Map;
import java.util.UUID;

import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.Queue;
import javax.jms.TextMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.Message;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FrontendController {

    static final String REQUEST_QUEUE_NAME = "work-queue/requests";

    static final String RESPONSE_QUEUE_NAME = "work-queue/responses";

    private static final String UPDATE_TOPIC_NAME = "work-queue/worker-updates";

    private final Logger logger = LoggerFactory.getLogger(FrontendController.class);

    private final String id = "frontend-spring-" + UUID.randomUUID().toString().substring(0, 4);

    private final Data data = new Data();

    private final ConnectionFactory connectionFactory;

    public FrontendController(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @GetMapping(path = "/api/data", produces = "application/json; charset=utf-8")
    public Data getData() {
        return data;
    }

    @GetMapping(path = "/api/receive-response", produces = "application/json; charset=utf-8")
    public ResponseEntity<Response> getResponse(@RequestParam("request") String requestId) {
        Response response = data.getResponses().get(requestId);

        if (response == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping(path = "/api/send-request")
    public ResponseEntity<String> sendRequest(@RequestBody Request request) throws JMSException {
        logger.info("{}: Sending {}", id, request);

        String requestId;

        try (JMSContext jmsContext = connectionFactory.createContext()) {
            TextMessage message = jmsContext.createTextMessage(request.getText());
            message.setBooleanProperty(FrontendHeaders.UPPERCASE, request.isUppercase());
            message.setBooleanProperty(FrontendHeaders.REVERSE, request.isReverse());

            Queue responseQueue = jmsContext.createQueue(RESPONSE_QUEUE_NAME);
            message.setJMSReplyTo(responseQueue);

            JMSProducer producer = jmsContext.createProducer();
            Queue requestQueue = jmsContext.createQueue(REQUEST_QUEUE_NAME);
            producer.send(requestQueue, message);

            requestId = message.getJMSMessageID();
        }

        data.getRequestIds().add(requestId);

        return ResponseEntity.accepted().body(requestId);
    }

    @JmsListener(destination = RESPONSE_QUEUE_NAME)
    public void handleResponse(Message<String> message) {
        Response response = new Response(message);
        data.getResponses().put(response.getRequestId(), response);
        logger.info("{}: Received {}", id, response);
    }

    @JmsListener(destination = UPDATE_TOPIC_NAME, containerFactory = "topicJmsListenerContainerFactory")
    public void handleWorkerUpdate(Message message) {
        WorkerUpdate workerUpdate = new WorkerUpdate(message.getHeaders());
        data.getWorkers().put(workerUpdate.getWorkerId(), workerUpdate);
        logger.debug("{}: Received {}", id, message);
    }

    @Scheduled(fixedRate = 5 * 1000)
    public void pruneStaleWorkers() {
        logger.debug("{}: Pruning stale workers", id);

        Map<String, WorkerUpdate> workers = data.getWorkers();
        long now = System.currentTimeMillis();

        data.getWorkers()
                .entrySet()
                .stream()
                .filter(entry -> now - entry.getValue().getTimestamp() > 10 * 1000)
                .peek(entry -> workers.remove(entry.getKey()))
                .forEach(entry -> logger.info("{}: Pruned {}", id, entry.getKey()));
    }

}

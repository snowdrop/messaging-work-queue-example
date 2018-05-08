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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.jms.JMSException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DashboardController {

    private final JmsTemplate jmsTemplate;

    private final List<WorkerResponse> workerResponses;

    private final Map<String, WorkerStatus> workerStatuses;

    private final Logger logger;

    public DashboardController(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
        this.workerResponses = new LinkedList<>();
        this.workerStatuses = new HashMap<>();
        this.logger = LoggerFactory.getLogger(DashboardController.class);
    }

    @GetMapping(path = "data")
    public DashboardDataWrapper getData() {
        DashboardDataWrapper data = new DashboardDataWrapper(workerResponses, workerStatuses);
        logger.info("Returning dashboard data: {}", data);
        return data;
    }

    @PostMapping(path = "send-request")
    public void sendRequest(@RequestBody String requestData) throws JMSException {
        logger.info("Sending request: {}", requestData);
        javax.jms.Message responseMessage =
                jmsTemplate.sendAndReceive("upstate/requests", (s) -> s.createTextMessage(requestData));
        String workerId = responseMessage.getStringProperty("worker_id");
        String body = responseMessage.getBody(String.class);

        WorkerResponse workerResponse = new WorkerResponse(workerId, body);
        logger.info("Received response: {}", workerResponse);
        workerResponses.add(workerResponse);
    }

    @JmsListener(destination = "upstate/worker-status")
    public void handleStatusUpdate(Message<String> status) {
        MessageHeaders headers = status.getHeaders();
        String workerId = headers.get("worker_id", String.class);
        long timestamp = headers.get("timestamp", Long.class);
        long requestsProcessed = headers.get("requests_processed", Long.class);

        WorkerStatus workerStatus = new WorkerStatus(timestamp, requestsProcessed);
        logger.info("Received status of {}: {}", workerId, workerStatus);
        workerStatuses.put(workerId, workerStatus);
    }

    static class DashboardDataWrapper {

        private final List<WorkerResponse> responses;

        private final Map<String, WorkerStatus> workerStatus;

        public DashboardDataWrapper(List<WorkerResponse> responses, Map<String, WorkerStatus> workerStatus) {
            this.responses = responses;
            this.workerStatus = workerStatus;
        }

        public List<WorkerResponse> getResponses() {
            return responses;
        }

        public Map<String, WorkerStatus> getWorkerStatus() {
            return workerStatus;
        }

        @Override
        public String toString() {
            return String.format("DashboardDataWrapper{responses='%s', workerStatus='%s'}", responses, workerStatus);
        }
    }

}

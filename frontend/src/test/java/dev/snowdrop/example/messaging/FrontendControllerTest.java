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

import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.Queue;
import javax.jms.TextMessage;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import static dev.snowdrop.example.messaging.FrontendController.REQUEST_QUEUE_NAME;
import static dev.snowdrop.example.messaging.FrontendController.RESPONSE_QUEUE_NAME;
import static dev.snowdrop.example.messaging.FrontendHeaders.PROCESSING_ERRORS;
import static dev.snowdrop.example.messaging.FrontendHeaders.REQUESTS_PROCESSED;
import static dev.snowdrop.example.messaging.FrontendHeaders.REVERSE;
import static dev.snowdrop.example.messaging.FrontendHeaders.TIMESTAMP;
import static dev.snowdrop.example.messaging.FrontendHeaders.UPPERCASE;
import static dev.snowdrop.example.messaging.FrontendHeaders.WORKER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.jms.support.JmsHeaders.CORRELATION_ID;

@RunWith(MockitoJUnitRunner.class)
public class FrontendControllerTest {

    private static final String TEST_TEXT = "test-text";

    private static final String TEST_WORKER_ID = "test-worker-id";

    private static final String TEST_REQUEST_ID = "test-request-id";

    @Mock
    private ConnectionFactory mockConnectionFactory;

    @Mock
    private JMSContext mockJmsContext;

    @Mock
    private JMSProducer mockJmsProducer;

    @Mock
    private Message<String> mockMessage;

    @Mock
    private MessageHeaders mockMessageHeaders;

    @Mock
    private TextMessage mockTextMessage;

    @Mock
    private Queue mockQueue;

    private FrontendController controller;

    @Before
    public void before() {
        controller = new FrontendController(mockConnectionFactory);
    }

    @Test
    public void shouldGetData() {
        Data data = controller.getData();

        assertThat(data.getRequestIds()).isEmpty();
        assertThat(data.getResponses()).isEmpty();
        assertThat(data.getWorkers()).isEmpty();
    }

    @Test
    public void shouldFailToGetResponse() {
        ResponseEntity<Response> responseEntity = controller.getResponse(TEST_REQUEST_ID);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    public void shouldSendRequest() throws JMSException {
        given(mockConnectionFactory.createContext()).willReturn(mockJmsContext);
        given(mockJmsContext.createTextMessage(TEST_TEXT)).willReturn(mockTextMessage);
        given(mockJmsContext.createQueue(RESPONSE_QUEUE_NAME)).willReturn(mockQueue);
        given(mockJmsContext.createQueue(REQUEST_QUEUE_NAME)).willReturn(mockQueue);
        given(mockJmsContext.createProducer()).willReturn(mockJmsProducer);
        given(mockTextMessage.getJMSMessageID()).willReturn(TEST_REQUEST_ID);

        Request request = new Request();
        request.setText(TEST_TEXT);
        request.setUppercase(true);
        request.setReverse(true);
        ResponseEntity<String> responseEntity = controller.sendRequest(request);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(responseEntity.getBody()).isEqualTo(TEST_REQUEST_ID);
        assertThat(controller.getData().getRequestIds()).containsOnly(TEST_REQUEST_ID);

        verify(mockTextMessage).setBooleanProperty(UPPERCASE, true);
        verify(mockTextMessage).setBooleanProperty(REVERSE, true);
        verify(mockTextMessage).setJMSReplyTo(mockQueue);

        verify(mockJmsProducer).send(mockQueue, mockTextMessage);
    }

    @Test
    public void shouldHandleResponse() {
        given(mockMessage.getPayload()).willReturn(TEST_TEXT);
        given(mockMessage.getHeaders()).willReturn(mockMessageHeaders);
        given(mockMessageHeaders.get(WORKER_ID, String.class)).willReturn(TEST_WORKER_ID);
        given(mockMessageHeaders.get(CORRELATION_ID, String.class)).willReturn(TEST_REQUEST_ID);

        controller.handleResponse(mockMessage);
        Response response = controller.getData().getResponses().get(TEST_REQUEST_ID);

        assertThat(response).isNotNull();
        assertThat(response.getWorkerId()).isEqualTo(TEST_WORKER_ID);
        assertThat(response.getRequestId()).isEqualTo(TEST_REQUEST_ID);
        assertThat(response.getText()).isEqualTo(TEST_TEXT);
    }

    @Test
    public void shouldHandleWorkerUpdate() {
        given(mockMessage.getHeaders()).willReturn(mockMessageHeaders);
        given(mockMessageHeaders.get(WORKER_ID, String.class)).willReturn(TEST_WORKER_ID);
        given(mockMessageHeaders.get(TIMESTAMP, Long.class)).willReturn(1L);
        given(mockMessageHeaders.get(REQUESTS_PROCESSED, Long.class)).willReturn(2L);
        given(mockMessageHeaders.get(PROCESSING_ERRORS, Long.class)).willReturn(3L);

        controller.handleWorkerUpdate(mockMessage);
        WorkerUpdate workerUpdate = controller.getData().getWorkers().get(TEST_WORKER_ID);

        assertThat(workerUpdate).isNotNull();
        assertThat(workerUpdate.getWorkerId()).isEqualTo(TEST_WORKER_ID);
        assertThat(workerUpdate.getTimestamp()).isEqualTo(1L);
        assertThat(workerUpdate.getRequestsProcessed()).isEqualTo(2L);
        assertThat(workerUpdate.getProcessingErrors()).isEqualTo(3L);
    }

    @Test
    public void shouldPruneStaleWorkers() {
        long now = System.currentTimeMillis();
        WorkerUpdate newWorker = new WorkerUpdate("new-worker", now, 0, 0);
        WorkerUpdate oldWorker = new WorkerUpdate("old-worker", now - 11 * 1000, 0, 0);
        Map<String, WorkerUpdate> workers = controller.getData().getWorkers();
        workers.put(newWorker.getWorkerId(), newWorker);
        workers.put(oldWorker.getWorkerId(), oldWorker);

        controller.pruneStaleWorkers();

        assertThat(workers).hasSize(1);
        assertThat(workers).containsEntry(newWorker.getWorkerId(), newWorker);
    }

}

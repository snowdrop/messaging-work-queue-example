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

import java.util.concurrent.atomic.AtomicInteger;

import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.TextMessage;
import javax.jms.Topic;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import static dev.snowdrop.example.messaging.Worker.UPDATE_TOPIC_NAME;
import static dev.snowdrop.example.messaging.WorkerHeaders.PROCESSING_ERRORS;
import static dev.snowdrop.example.messaging.WorkerHeaders.REQUESTS_PROCESSED;
import static dev.snowdrop.example.messaging.WorkerHeaders.REVERSE;
import static dev.snowdrop.example.messaging.WorkerHeaders.UPPERCASE;
import static dev.snowdrop.example.messaging.WorkerHeaders.WORKER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class WorkerTest {

    private static final String TEST_TEXT = "test-text";

    private static final String PROCESSED_TEST_TEXT = "TXET-TSET";

    private static final String TEST_WORKER_ID = "test-worker-id";

    @Mock
    private ConnectionFactory mockConnectionFactory;

    @Mock
    private JMSContext mockJmsContext;

    @Mock
    private JMSProducer mockJmsProducer;

    @Mock
    private Topic mockTopic;

    @Mock
    private Message<String> mockMessage;

    @Mock
    private TextMessage mockTextMessage;

    @Mock
    private MessageHeaders mockMessageHeaders;

    @Test
    public void shouldHandleRequest() {
        given(mockMessage.getPayload()).willReturn(TEST_TEXT);
        given(mockMessage.getHeaders()).willReturn(mockMessageHeaders);
        given(mockMessageHeaders.get(UPPERCASE, Boolean.class)).willReturn(true);
        given(mockMessageHeaders.get(REVERSE, Boolean.class)).willReturn(true);

        AtomicInteger requestsProcessed = new AtomicInteger(0);
        AtomicInteger processingErrors = new AtomicInteger(0);
        Worker worker = new Worker(mockConnectionFactory, TEST_WORKER_ID, requestsProcessed, processingErrors);

        Message<String> result = worker.handleRequest(mockMessage);

        assertThat(result.getPayload()).isEqualTo(PROCESSED_TEST_TEXT);
        assertThat(result.getHeaders()).containsEntry(WORKER_ID, TEST_WORKER_ID);
        assertThat(requestsProcessed.get()).isEqualTo(1);
        assertThat(processingErrors.get()).isEqualTo(0);
    }

    @Test
    public void shouldSendStatusUpdate() throws JMSException {
        given(mockConnectionFactory.createContext()).willReturn(mockJmsContext);
        given(mockJmsContext.createTextMessage()).willReturn(mockTextMessage);
        given(mockJmsContext.createProducer()).willReturn(mockJmsProducer);
        given(mockJmsContext.createTopic(UPDATE_TOPIC_NAME)).willReturn(mockTopic);

        Worker worker = new Worker(mockConnectionFactory, TEST_WORKER_ID, new AtomicInteger(1), new AtomicInteger(2));
        worker.sendStatusUpdate();

        verify(mockTextMessage).setStringProperty(WORKER_ID, TEST_WORKER_ID);
        verify(mockTextMessage).setLongProperty(REQUESTS_PROCESSED, 1);
        verify(mockTextMessage).setLongProperty(PROCESSING_ERRORS, 2);
        verify(mockJmsProducer).send(mockTopic, mockTextMessage);
    }

}

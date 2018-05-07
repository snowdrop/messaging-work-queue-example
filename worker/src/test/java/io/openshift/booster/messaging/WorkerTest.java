package io.openshift.booster.messaging;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.JmsHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@RunWith(MockitoJUnitRunner.class)
public class WorkerTest {

    @Mock
    private WorkerProperties mockWorkerProperties;

    @Mock
    private JmsTemplate mockJmsTemplate;

    @Mock
    private Message<String> mockMessage;

    @Mock
    private MessageHeaders mockMessageHeaders;

    @Test
    public void shouldHandleRequest() {
        given(mockWorkerProperties.getId()).willReturn("test-worker-id");
        given(mockMessage.getPayload()).willReturn("test-message");
        given(mockMessageHeaders.get(MessageHeaders.ID)).willReturn("test-message-id");
        given(mockMessage.getHeaders()).willReturn(mockMessageHeaders);

        AtomicInteger requestsProcessed = new AtomicInteger();
        Worker worker = new Worker(mockWorkerProperties, mockJmsTemplate, requestsProcessed);
        Message<String> result = worker.handleRequest(mockMessage);

        assertThat(result.getPayload()).isEqualTo("TEST-MESSAGE");
        assertThat(result.getHeaders()).containsEntry(JmsHeaders.CORRELATION_ID, "test-message-id");
        assertThat(result.getHeaders()).containsEntry("worker_id", "test-worker-id");
        assertThat(requestsProcessed.get()).isEqualTo(1);
    }

    /*
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
     */

}
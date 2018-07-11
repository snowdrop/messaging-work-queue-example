package io.openshift.booster.messaging;

import java.util.concurrent.atomic.AtomicInteger;

import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import static io.openshift.booster.messaging.Worker.UPDATE_QUEUE_NAME;
import static io.openshift.booster.messaging.WorkerHeaders.PROCESSING_ERRORS;
import static io.openshift.booster.messaging.WorkerHeaders.REQUESTS_PROCESSED;
import static io.openshift.booster.messaging.WorkerHeaders.REVERSE;
import static io.openshift.booster.messaging.WorkerHeaders.UPPERCASE;
import static io.openshift.booster.messaging.WorkerHeaders.WORKER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class WorkerTest {

    @Mock
    private JmsTemplate mockJmsTemplate;

    @Mock
    private Message<String> mockMessage;

    @Mock
    private TextMessage mockTextMessage;

    @Mock
    private Session mockSession;

    @Mock
    private MessageHeaders mockMessageHeaders;

    @Test
    public void shouldHandleRequest() {
        given(mockMessage.getPayload()).willReturn("test-message");
        given(mockMessage.getHeaders()).willReturn(mockMessageHeaders);
        given(mockMessageHeaders.get(UPPERCASE, Boolean.class)).willReturn(true);
        given(mockMessageHeaders.get(REVERSE, Boolean.class)).willReturn(true);

        AtomicInteger requestsProcessed = new AtomicInteger(0);
        AtomicInteger processingErrors = new AtomicInteger(0);
        Worker worker = new Worker(mockJmsTemplate, "test-worker-id", requestsProcessed, processingErrors);

        Message<String> result = worker.handleRequest(mockMessage);

        assertThat(result.getPayload()).isEqualTo("EGASSEM-TSET");
        assertThat(result.getHeaders()).containsEntry(WORKER_ID, "test-worker-id");
        assertThat(requestsProcessed.get()).isEqualTo(1);
        assertThat(processingErrors.get()).isEqualTo(0);
    }

    @Test
    public void shouldSendStatusUpdate() throws JMSException {
        given(mockSession.createTextMessage()).willReturn(mockTextMessage);

        Worker worker = new Worker(mockJmsTemplate);
        worker.sendStatusUpdate();

        ArgumentCaptor<MessageCreator> messageCreatorCaptor = ArgumentCaptor.forClass(MessageCreator.class);
        verify(mockJmsTemplate).send(eq(UPDATE_QUEUE_NAME), messageCreatorCaptor.capture());

        javax.jms.Message message = messageCreatorCaptor.getValue().createMessage(mockSession);

        assertThat(message).isEqualTo(mockTextMessage);
        verify(mockTextMessage).setStringProperty(eq(WORKER_ID), anyString());
        verify(mockTextMessage).setLongProperty(REQUESTS_PROCESSED, 0);
        verify(mockTextMessage).setLongProperty(PROCESSING_ERRORS, 0);
    }

}
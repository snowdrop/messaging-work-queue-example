package io.openshift.booster.messaging;

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

import static io.openshift.booster.messaging.FrontendController.RESPONSE_QUEUE_NAME;
import static io.openshift.booster.messaging.FrontendHeaders.PROCESSING_ERRORS;
import static io.openshift.booster.messaging.FrontendHeaders.REQUESTS_PROCESSED;
import static io.openshift.booster.messaging.FrontendHeaders.REVERSE;
import static io.openshift.booster.messaging.FrontendHeaders.TIMESTAMP;
import static io.openshift.booster.messaging.FrontendHeaders.UPPERCASE;
import static io.openshift.booster.messaging.FrontendHeaders.WORKER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.jms.support.JmsHeaders.CORRELATION_ID;

@RunWith(MockitoJUnitRunner.class)
public class FrontendControllerTest {

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
        ResponseEntity<Response> responseEntity = controller.getResponse("test");

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    public void shouldSendRequest() throws JMSException {
        given(mockConnectionFactory.createContext()).willReturn(mockJmsContext);
        given(mockJmsContext.createTextMessage("test-text")).willReturn(mockTextMessage);
        given(mockJmsContext.createQueue(RESPONSE_QUEUE_NAME)).willReturn(mockQueue);
        given(mockJmsContext.createProducer()).willReturn(mockJmsProducer);
        given(mockTextMessage.getJMSMessageID()).willReturn("test-request-id");

        Request request = new Request();
        request.setText("test-text");
        request.setUppercase(true);
        request.setReverse(true);
        ResponseEntity<String> responseEntity = controller.sendRequest(request);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(responseEntity.getBody()).isEqualTo("test-request-id");
        assertThat(controller.getData().getRequestIds()).containsOnly("test-request-id");

        verify(mockTextMessage).setBooleanProperty(UPPERCASE, true);
        verify(mockTextMessage).setBooleanProperty(REVERSE, true);
        verify(mockTextMessage).setJMSReplyTo(mockQueue);
    }

    @Test
    public void shouldHandleResponse() {
        given(mockMessage.getPayload()).willReturn("test-body");
        given(mockMessage.getHeaders()).willReturn(mockMessageHeaders);
        given(mockMessageHeaders.get(WORKER_ID, String.class)).willReturn("test-worker-id");
        given(mockMessageHeaders.get(CORRELATION_ID, String.class)).willReturn("test-request-id");

        controller.handleResponse(mockMessage);
        Response response = controller.getData().getResponses().get("test-request-id");

        assertThat(response).isNotNull();
        assertThat(response.getWorkerId()).isEqualTo("test-worker-id");
        assertThat(response.getRequestId()).isEqualTo("test-request-id");
        assertThat(response.getText()).isEqualTo("test-body");
    }

    @Test
    public void shouldHandleWorkerUpdate() {
        given(mockMessage.getHeaders()).willReturn(mockMessageHeaders);
        given(mockMessageHeaders.get(WORKER_ID, String.class)).willReturn("test-worker-id");
        given(mockMessageHeaders.get(TIMESTAMP, Long.class)).willReturn(1L);
        given(mockMessageHeaders.get(REQUESTS_PROCESSED, Long.class)).willReturn(2L);
        given(mockMessageHeaders.get(PROCESSING_ERRORS, Long.class)).willReturn(3L);

        controller.handleWorkerUpdate(mockMessage);
        WorkerUpdate workerUpdate = controller.getData().getWorkers().get("test-worker-id");

        assertThat(workerUpdate).isNotNull();
        assertThat(workerUpdate.getWorkerId()).isEqualTo("test-worker-id");
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
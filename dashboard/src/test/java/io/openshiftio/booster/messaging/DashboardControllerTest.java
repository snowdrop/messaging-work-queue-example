package io.openshiftio.booster.messaging;

import javax.jms.JMSException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class DashboardControllerTest {

    @Mock
    private JmsTemplate mockJmsTemplate;

    @Mock
    private Message<String> mockMessage;

    @Mock
    private MessageHeaders mockMessageHeaders;

    @Mock
    private javax.jms.Message mockJmsMessage;

    private DashboardController controller;

    @Before
    public void before() {
        controller = new DashboardController(mockJmsTemplate);
    }

    @Test
    public void shouldGetData() {
        DashboardController.DashboardDataWrapper data = controller.getData();

        assertThat(data.getResponses()).isEmpty();
        assertThat(data.getWorkerStatus()).isEmpty();
    }

    @Test
    public void shouldSendRequest() throws JMSException {
        given(mockJmsTemplate.sendAndReceive(anyString(), any(MessageCreator.class))).willReturn(mockJmsMessage);
        given(mockJmsMessage.getStringProperty("worker_id")).willReturn("test-id");
        given(mockJmsMessage.getBody(String.class)).willReturn("TEST-BODY");

        controller.sendRequest("test-body");

        verify(mockJmsTemplate).send(eq("upstate/requests"), any(MessageCreator.class));
    }

    @Test
    public void shouldHandleResponse() {
        given(mockMessage.getPayload()).willReturn("test-body");
        given(mockMessage.getHeaders()).willReturn(mockMessageHeaders);
        given(mockMessageHeaders.get("worker_id", String.class)).willReturn("test-id");

        controller.handleResponse(mockMessage);

        DashboardController.DashboardDataWrapper data = controller.getData();
        assertThat(data.getResponses()).containsOnly(new WorkerResponse("test-id", "test-body"));
    }

    @Test
    public void shouldHandleStatusUpdate() {
        given(mockMessage.getHeaders()).willReturn(mockMessageHeaders);
        given(mockMessageHeaders.get("worker_id", String.class)).willReturn("test-id");
        given(mockMessageHeaders.get("timestamp", Long.class)).willReturn(1L);
        given(mockMessageHeaders.get("requests_processed", Long.class)).willReturn(2L);

        controller.handleStatusUpdate(mockMessage);
        DashboardController.DashboardDataWrapper data = controller.getData();

        assertThat(data.getWorkerStatus()).hasSize(1);
        assertThat(data.getWorkerStatus()).containsEntry("test-id", new WorkerStatus(1L, 2L));
    }

}
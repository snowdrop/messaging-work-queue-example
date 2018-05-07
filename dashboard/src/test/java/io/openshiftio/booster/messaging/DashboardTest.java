package io.openshiftio.booster.messaging;

import java.util.AbstractMap;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DashboardTest {

    @Test
    public void shouldGetEmptyWorkerResponses() {
        Dashboard dashboard = new Dashboard();
        assertThat(dashboard.getResponses()).isEmpty();
    }

    @Test
    public void addWorkerResponse() {
        Dashboard dashboard = new Dashboard();
        WorkerResponse workerResponse = new WorkerResponse("test-id", "test-body");
        dashboard.addWorkerResponse(workerResponse);
        assertThat(dashboard.getResponses()).containsOnly(workerResponse);
    }

    @Test
    public void getEmptyWorkerStatuses() {
        Dashboard dashboard = new Dashboard();
        assertThat(dashboard.getWorkerStatus()).isEmpty();
    }

    @Test
    public void addWorkerStatus() {
        Dashboard dashboard = new Dashboard();
        String workerId = "test-id";
        WorkerStatus workerStatus = new WorkerStatus(1L, 2);
        dashboard.addWorkerStatus(workerId, workerStatus);
        assertThat(dashboard.getWorkerStatus()).containsOnly(new AbstractMap.SimpleEntry<>(workerId, workerStatus));
    }
}
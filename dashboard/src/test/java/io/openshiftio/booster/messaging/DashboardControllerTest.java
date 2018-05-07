package io.openshiftio.booster.messaging;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DashboardControllerTest {

    @Test
    public void shouldGetDashboard() {
        Dashboard dashboard = new Dashboard();
        DashboardController controller = new DashboardController(dashboard);
        assertThat(controller.getDashboard()).isEqualTo(dashboard);
    }

}
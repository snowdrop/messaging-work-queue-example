package dev.snowdrop.example;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.junit.jupiter.api.AfterEach;

import io.dekorate.testing.annotation.Inject;
import io.dekorate.testing.annotation.KubernetesIntegrationTest;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.LocalPortForward;

@KubernetesIntegrationTest(deployEnabled = false, buildEnabled = false)
public class KubernetesIT extends AbstractTest {

    @Inject
    KubernetesClient client;

    LocalPortForward frontendPort;

    @Override
    protected URL getFrontendUrl() throws MalformedURLException {
        frontendPort = client.services().inNamespace(System.getProperty("kubernetes.namespace"))
                .withName("spring-boot-messaging-work-queue-frontend").portForward(8080);
        return new URL("http://localhost:" + frontendPort.getLocalPort() + "/");
    }

    @AfterEach
    public void tearDown() {
        if (frontendPort != null) {
            try {
                frontendPort.close();
            } catch (IOException ignored) {

            }
        }
    }
}

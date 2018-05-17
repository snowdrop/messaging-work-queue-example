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

package io.openshift.booster;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

import com.jayway.restassured.path.json.JsonPath;
import org.arquillian.cube.openshift.impl.enricher.AwaitRoute;
import org.arquillian.cube.openshift.impl.enricher.RouteURL;
import org.arquillian.cube.openshift.impl.requirement.RequiresOpenshift;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import static com.jayway.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Category(RequiresOpenshift.class)
@RequiresOpenshift
@RunWith(ArquillianConditionalRunner.class)
public class OpenShiftIT {

    @RouteURL("spring-boot-messaging-booster-dashboard")
    @AwaitRoute(path = "/health")
    private URL dashboardUrl;

    private URL dataUrl;

    private URL requestUrl;

    @Before
    public void before() throws MalformedURLException {
        dataUrl = new URL(dashboardUrl, "data");
        requestUrl = new URL(dashboardUrl, "send-request");
    }

    @Test
    public void shouldHandleRequest() {
        String testData = UUID.randomUUID()
                .toString();

        // Issue request
        given().body(testData)
                .post(requestUrl)
                .then()
                .assertThat()
                .statusCode(200);

        // Wait for request result
        await().atMost(5, SECONDS)
                .untilAsserted(() -> {
                    JsonPath response = given().get(dataUrl)
                            .thenReturn()
                            .jsonPath();
                    assertThat(response.getList("responses")).hasSize(1);
                    assertThat(response.getString("responses[0].body")).isEqualTo(testData.toUpperCase());
                    String workerId = response.getString("responses[0].workerId");
                    int requestsProcessed =
                            response.getInt(String.format("workerStatus['%s'].requestsProcessed", workerId));
                    assertThat(requestsProcessed).isEqualTo(1);
                });
    }

}

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

package dev.snowdrop.example;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static io.restassured.RestAssured.withArgs;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.dekorate.testing.annotation.Inject;
import io.dekorate.testing.annotation.Named;
import io.dekorate.testing.openshift.annotation.OpenshiftIntegrationTest;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

@OpenshiftIntegrationTest(deployEnabled = false, buildEnabled = false)
public class OpenShiftIT {

    @Inject
    @Named("spring-boot-messaging-work-queue-frontend")
    URL frontend;

    private URL dataUrl;

    private URL requestUrl;

    private URL responseUrl;

    @BeforeEach
    public void before() throws MalformedURLException {
        dataUrl = new URL(frontend, "api/data");
        requestUrl = new URL(frontend, "api/send-request");
        responseUrl = new URL(frontend, "api/receive-response");
    }

    @Test
    public void shouldHandleRequest() {
        // Issue a request
        Response requestResponse = given()
                .body("{\"text\":\"test-message\",\"uppercase\":true,\"reverse\":true}")
                .and()
                .contentType("application/json")
                .when()
                .post(requestUrl)
                .thenReturn();

        assertEquals(202, requestResponse.getStatusCode());
        String requestId = requestResponse.getBody().asString();

        // Wait for the request to be handled
        await().atMost(10, SECONDS)
                .untilAsserted(() -> given()
                        .queryParam("request", requestId)
                        .when()
                        .get(responseUrl)
                        .then()
                        .statusCode(200)
                        .body("requestId", is(equalTo(requestId)))
                        .body("workerId", not((isEmptyString())))
                        .body("text", is(equalTo("EGASSEM-TSET"))));

        JsonPath responseJson = given()
                .queryParam("request", requestId)
                .when()
                .get(responseUrl)
                .thenReturn()
                .jsonPath();
        String workerId = responseJson.getString("workerId");
        String text = responseJson.getString("text");

        // Verify data
        await().atMost(10, SECONDS)
                .untilAsserted(() -> when()
                        .get(dataUrl)
                        .then()
                        .statusCode(200)
                        .body("requestIds", hasItem(requestId))
                        .body("responses.%s.requestId", withArgs(requestId), is(equalTo(requestId)))
                        .body("responses.%s.workerId", withArgs(requestId), is(equalTo(workerId)))
                        .body("responses.%s.text", withArgs(requestId), is(equalTo(text)))
                        .body("workers.%s.workerId", withArgs(workerId), is(equalTo(workerId)))
                        .body("workers.%s.timestamp", withArgs(workerId), is(notNullValue()))
                        .body("workers.%s.requestsProcessed", withArgs(workerId), is(notNullValue()))
                        .body("workers.%s.processingErrors", withArgs(workerId), is(notNullValue())));
    }

}

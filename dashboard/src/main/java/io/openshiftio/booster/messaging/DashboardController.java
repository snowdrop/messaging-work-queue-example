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

package io.openshiftio.booster.messaging;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DashboardController {

    private final Dashboard dashboard;

    public DashboardController(Dashboard dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping(path = "data")
    public Dashboard getDashboard() {
        return dashboard;
    }

    @PostMapping(path = "send-request")
    public void sendRequest() {
        System.out.println("send request");
    }

}

/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

"use strict";

var upstate = {
    data: null,

    fetchDataPeriodically: function () {
        function handler(data) {
            upstate.data = data;
            window.dispatchEvent(new Event("statechange"));
        }

        gesso.fetchPeriodically("/data", handler);
    },

    sendRequest: function () {
        console.log("Sending request");

        var request = gesso.openRequest("POST", "/send-request", function (event) {
            if (event.target.status === 200) {
                upstate.fetchDataPeriodically();
            }
        });

        var data = JSON.stringify({text: $("#request-form").text.value});

        request.setRequestHeader("Content-type", "application/json");
        request.send(data);

        $("#request-form").reset();
    },

    renderResponses: function (data) {
        console.log("Rendering responses");

        var oldContent = $("#responses");
        var newContent = document.createElement("pre");

        var lines = [];

        for (var response of data.responses) {
            lines.unshift(("<b>" + response["workerId"] + ":</b> ").padEnd(30) + response["body"]);
        }

        newContent.innerHTML = lines.join("\n");
        newContent.setAttribute("id", "responses");

        oldContent.parentNode.replaceChild(newContent, oldContent);
    },

    renderWorkers: function (data) {
        console.log("Rendering workers");

        var oldContent = $("#workers");
        var newContent = document.createElement("pre");

        var lines = [];

        for (var worker_id in data.workerStatus) {
            var status = data.workerStatus[worker_id];
            var timestamp = status["timestamp"];
            var requestsProcessed = status["requestsProcessed"];

            lines.unshift(("<b>" + worker_id + ":</b> ").padEnd(30) + timestamp + ", " + requestsProcessed);
        }

        newContent.innerHTML = lines.join("\n");
        newContent.setAttribute("id", "workers");

        oldContent.parentNode.replaceChild(newContent, oldContent);
    },

    init: function () {
        window.addEventListener("statechange", function (event) {
            upstate.renderResponses(upstate.data);
            upstate.renderWorkers(upstate.data);
        });

        window.addEventListener("load", function (event) {
            upstate.fetchDataPeriodically();
         });
    }
}

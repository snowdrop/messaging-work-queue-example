//
// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
//

"use strict";

const body_parser = require("body-parser");
const express = require("express");
const rhea = require("rhea");

const http_host = process.env.IP || process.env.OPENSHIFT_NODEJS_IP || "0.0.0.0";
const http_port = process.env.PORT || process.env.OPENSHIFT_NODEJS_PORT || 8080;

const amqp_host = process.env.MESSAGING_SERVICE_HOST || "localhost";
const amqp_port = process.env.MESSAGING_SERVICE_PORT || 5672;

// AMQP

const id = Math.floor(Math.random() * (10000 - 1000)) + 1000;
const container = rhea.create_container({id: "web-nodejs-" + id});

var request_sender = null;
var response_receiver = null;
var worker_status_receiver = null;

const requests = [];
const responses = [];
const worker_status = {};

var request_sequence = 0;

function send_requests() {
    if (!response_receiver) {
        return;
    }

    while (request_sender.sendable() && requests.length > 0) {
        var message = {
            id: request_sequence++,
            reply_to: response_receiver.source.address,
            body: requests.shift()
        };

        request_sender.send(message);

        console.log("WEB: Sent request '%s'", message.body);
    }
}

container.on("connection_open", function (event) {
    request_sender = event.connection.open_sender("upstate/requests");
    response_receiver = event.connection.open_receiver({source: {dynamic: true}});
    worker_status_receiver = event.connection.open_receiver("upstate/worker-status");
});

container.on("sendable", function (event) {
    send_requests();
});

container.on("message", function (event) {
    if (event.receiver === worker_status_receiver) {
        var update = event.message.application_properties;
        console.log("WEB: Received status update from %s", update.worker_id);
        worker_status[update.worker_id] = [update.timestamp, update.requests_processed];
        return;
    }

    if (event.receiver === response_receiver) {
        var response = event.message;
        console.log("WEB: Received response '%s'", response.body);
        responses.push([response.application_properties.worker_id, response.body]);
        return;
    }

    throw new Exception();
});

container.connect({host: amqp_host, port: amqp_port});

console.log("Connected to AMQP messaging service at %s:%s", amqp_host, amqp_port);

// HTTP

const app = express();

app.use(express.static("static"));
app.use(body_parser.json());

app.post("/send-request", function (req, resp) {
    requests.push(req.body.text);
    send_requests();
    resp.redirect("/");
});

app.get("/data", function (req, resp) {
    resp.json({responses: responses, worker_status: worker_status});
});

app.listen(http_port, http_host);

console.log("Listening for new HTTP connections at %s:%s", http_host, http_port);

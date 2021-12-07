#!/usr/bin/env bash

# deploy broker
oc apply -f .openshiftio/amq.yaml
oc new-app --template=amq-broker-72-basic -p APPLICATION_NAME=work-queue-broker -p IMAGE_STREAM_NAMESPACE=$(oc project -q) -p AMQ_NAME=work-queue-broker -p AMQ_PROTOCOL=amqp -p AMQ_USER=work-queue -p AMQ_PASSWORD=work-queue

# 1.- Deploy Frontend
./mvnw -s .github/mvn-settings.xml clean verify -pl frontend -Popenshift -Ddekorate.deploy=true

# wait for the app to stand up
sleep 30 # needed in order to bypass the 'Pending' state
timeout 300s bash -c 'while [[ $(oc get pod -o json | jq  ".items[] | select(.metadata.name | contains(\"build\"))  | .status  " | jq -rs "sort_by(.startTme) | last | .phase") == "Running" ]]; do sleep 20; done; echo ""'

# 2.- Deploy Worker
./mvnw -s .github/mvn-settings.xml clean verify -pl worker -Popenshift -Ddekorate.deploy=true

# wait for the app to stand up
sleep 30 # needed in order to bypass the 'Pending' state
timeout 300s bash -c 'while [[ $(oc get pod -o json | jq  ".items[] | select(.metadata.name | contains(\"build\"))  | .status  " | jq -rs "sort_by(.startTme) | last | .phase") == "Running" ]]; do sleep 20; done; echo ""'

# 3.- Run OpenShift Tests
./mvnw -s .github/mvn-settings.xml verify -pl tests -Popenshift-it

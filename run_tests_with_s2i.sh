#!/usr/bin/env bash
SOURCE_REPOSITORY_URL=${1:-https://github.com/snowdrop/messaging-work-queue-example}
SOURCE_REPOSITORY_REF=${2:-sb-2.4.x}

# deploy broker
oc apply -f .openshiftio/amq.yaml
oc new-app --template=amq-broker-72-basic -p APPLICATION_NAME=work-queue-broker -p IMAGE_STREAM_NAMESPACE=$(oc project -q) -p AMQ_NAME=work-queue-broker -p AMQ_PROTOCOL=amqp -p AMQ_USER=work-queue -p AMQ_PASSWORD=work-queue

# deploy frontend
oc apply -f ./frontend/.openshiftio/application.yaml
oc new-app --template=spring-boot-messaging-work-queue-frontend -p SOURCE_REPOSITORY_URL=$SOURCE_REPOSITORY_URL -p SOURCE_REPOSITORY_REF=$SOURCE_REPOSITORY_REF -p SOURCE_REPOSITORY_DIR=frontend

# deploy worker
oc apply -f ./worker/.openshiftio/application.yaml
oc new-app --template=spring-boot-messaging-work-queue-worker -p SOURCE_REPOSITORY_URL=$SOURCE_REPOSITORY_URL -p SOURCE_REPOSITORY_REF=$SOURCE_REPOSITORY_REF -p SOURCE_REPOSITORY_DIR=worker

sleep 30 # needed in order to bypass the 'Pending' state
# wait for the app to stand up
timeout 300s bash -c 'while [[ $(oc get pod -o json | jq  ".items[] | select(.metadata.name | contains(\"build\"))  | .status  " | jq -rs "sort_by(.startTme) | last | .phase") == "Running" ]]; do sleep 20; done; echo ""'

# launch the tests without deploying the application
./mvnw -s .github/mvn-settings.xml clean verify -Popenshift,openshift-it

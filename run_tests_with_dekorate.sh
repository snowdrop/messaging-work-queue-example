#!/usr/bin/env bash

source scripts/waitFor.sh

# deploy broker
oc apply -f .openshiftio/amq.yaml
oc new-app --template=amq-broker-72-basic -p APPLICATION_NAME=work-queue-broker -p IMAGE_STREAM_NAMESPACE=$(oc project -q) -p AMQ_NAME=work-queue-broker -p AMQ_PROTOCOL=amqp -p AMQ_USER=work-queue -p AMQ_PASSWORD=work-queue
if [[ $(waitFor "work-queue-broker" "application") -eq 1 ]] ; then
  echo "AMQ failed to deploy. Aborting"
  exit 1
fi

# 1.- Deploy Frontend
./mvnw -s .github/mvn-settings.xml clean verify -pl frontend -Popenshift -Ddekorate.deploy=true
if [[ $(waitFor "spring-boot-messaging-work-queue-frontend" "app.kubernetes.io/name") -eq 1 ]] ; then
  echo "Frontend failed to deploy. Aborting"
  exit 1
fi

# 2.- Deploy Worker
./mvnw -s .github/mvn-settings.xml clean verify -pl worker -Popenshift -Ddekorate.deploy=true
if [[ $(waitFor "spring-boot-messaging-work-queue-worker" "app.kubernetes.io/name") -eq 1 ]] ; then
  echo "Worker failed to deploy. Aborting"
  exit 1
fi

# 3.- Run OpenShift Tests
./mvnw -s .github/mvn-settings.xml verify -pl tests -Popenshift-it

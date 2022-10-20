#!/usr/bin/env bash
SOURCE_REPOSITORY_URL=${1:-https://github.com/snowdrop/messaging-work-queue-example}
SOURCE_REPOSITORY_REF=${2:-sb-2.7.x}

source scripts/waitFor.sh

# deploy broker
oc apply -f .openshiftio/amq.yaml
oc new-app --template=amq-broker-72-basic -p APPLICATION_NAME=work-queue-broker -p IMAGE_STREAM_NAMESPACE=$(oc project -q) -p AMQ_NAME=work-queue-broker -p AMQ_PROTOCOL=amqp -p AMQ_USER=work-queue -p AMQ_PASSWORD=work-queue
if [[ $(waitFor "work-queue-broker" "application") -eq 1 ]] ; then
  echo "AMQ failed to deploy. Aborting"
  exit 1
fi

# deploy frontend
oc apply -f ./frontend/.openshiftio/application.yaml
oc new-app --template=spring-boot-messaging-work-queue-frontend -p SOURCE_REPOSITORY_URL=$SOURCE_REPOSITORY_URL -p SOURCE_REPOSITORY_REF=$SOURCE_REPOSITORY_REF -p SOURCE_REPOSITORY_DIR=frontend
if [[ $(waitFor "spring-boot-messaging-work-queue-frontend" "app") -eq 1 ]] ; then
  echo "Frontend failed to deploy. Aborting"
  exit 1
fi

# deploy worker
oc apply -f ./worker/.openshiftio/application.yaml
oc new-app --template=spring-boot-messaging-work-queue-worker -p SOURCE_REPOSITORY_URL=$SOURCE_REPOSITORY_URL -p SOURCE_REPOSITORY_REF=$SOURCE_REPOSITORY_REF -p SOURCE_REPOSITORY_DIR=worker
if [[ $(waitFor "spring-boot-messaging-work-queue-worker" "app") -eq 1 ]] ; then
  echo "Worker failed to deploy. Aborting"
  exit 1
fi

# launch the tests without deploying the application
./mvnw -s .github/mvn-settings.xml clean verify -Popenshift,openshift-it

#!/usr/bin/env bash
CONTAINER_REGISTRY=${1:-localhost:5000}
K8S_NAMESPACE=${2:-k8s}
MAVEN_OPTS=${3:-}

source scripts/waitFor.sh

kubectl config set-context --current --namespace=$K8S_NAMESPACE

# deploy broker
oc apply -f .kubernetes/amq.yaml
if [[ $(waitFor "work-queue-broker" "application") -eq 1 ]] ; then
  echo "AMQ failed to deploy. Aborting"
  exit 1
fi

# 1.- Deploy Frontend
./mvnw -s .github/mvn-settings.xml clean verify -pl frontend -Pkubernetes -Ddekorate.docker.registry=$CONTAINER_REGISTRY -Dkubernetes.namespace=$K8S_NAMESPACE -Ddekorate.push=true -Ddekorate.deploy=true $MAVEN_OPTS
if [[ $(waitFor "spring-boot-messaging-work-queue-frontend" "app.kubernetes.io/name") -eq 1 ]] ; then
  echo "Frontend failed to deploy. Aborting"
  exit 1
fi

# 2.- Deploy Worker
./mvnw -s .github/mvn-settings.xml clean verify -pl worker -Pkubernetes -Ddekorate.docker.registry=$CONTAINER_REGISTRY -Dkubernetes.namespace=$K8S_NAMESPACE -Ddekorate.push=true -Ddekorate.deploy=true $MAVEN_OPTS
if [[ $(waitFor "spring-boot-messaging-work-queue-worker" "app.kubernetes.io/name") -eq 1 ]] ; then
  echo "Worker failed to deploy. Aborting"
  exit 1
fi

# 3.- Run OpenShift Tests
./mvnw -s .github/mvn-settings.xml verify -pl tests -Pkubernetes-it $MAVEN_OPTS

#!/usr/bin/env bash
CONTAINER_REGISTRY=${1:-localhost:5000}
K8S_NAMESPACE=${2:-k8s}
MAVEN_OPTS=${3:-}
GROUP=user
TAG=genhelm

source scripts/waitFor.sh

kubectl config set-context --current --namespace=$K8S_NAMESPACE

# deploy broker
oc apply -f .kubernetes/amq.yaml
if [[ $(waitFor "work-queue-broker" "application") -eq 1 ]] ; then
  echo "AMQ failed to deploy. Aborting"
  exit 1
fi

# 1.- Deploy Frontend
./mvnw -s .github/mvn-settings.xml clean verify -pl frontend -Pkubernetes,helm -DskipTests -Ddekorate.docker.registry=$CONTAINER_REGISTRY -Ddekorate.docker.group=$GROUP -Ddekorate.docker.version=$TAG -Dkubernetes.namespace=$K8S_NAMESPACE -Ddekorate.options.properties-profile=helm -Ddekorate.push=true $MAVEN_OPTS
helm install frontend ./frontend/target/classes/META-INF/dekorate/helm/frontend --set app.image=$CONTAINER_REGISTRY/$GROUP/spring-boot-messaging-work-queue-frontend:$TAG -n $K8S_NAMESPACE
if [[ $(waitFor "spring-boot-messaging-work-queue-frontend" "app.kubernetes.io/name") -eq 1 ]] ; then
  echo "Frontend service failed to deploy. Aborting"
  exit 1
fi

# 2.- Deploy Worker
./mvnw -s .github/mvn-settings.xml clean verify -pl worker -Pkubernetes,helm -DskipTests -Ddekorate.docker.registry=$CONTAINER_REGISTRY -Ddekorate.docker.group=$GROUP -Ddekorate.docker.version=$TAG -Dkubernetes.namespace=$K8S_NAMESPACE -Ddekorate.options.properties-profile=helm -Ddekorate.push=true $MAVEN_OPTS
helm install worker ./worker/target/classes/META-INF/dekorate/helm/worker --set app.image=$CONTAINER_REGISTRY/$GROUP/spring-boot-messaging-work-queue-worker:$TAG -n $K8S_NAMESPACE
if [[ $(waitFor "spring-boot-messaging-work-queue-worker" "app.kubernetes.io/name") -eq 1 ]] ; then
  echo "Greeting name service failed to deploy. Aborting"
  exit 1
fi

# 3.- Run Tests
./mvnw -s .github/mvn-settings.xml verify -pl tests -Pkubernetes-it -Dkubernetes.namespace=$K8S_NAMESPACE $MAVEN_OPTS

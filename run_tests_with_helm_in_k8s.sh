#!/usr/bin/env bash
CONTAINER_REGISTRY=${1:-localhost:5000}
K8S_NAMESPACE=${2:-helm}

source scripts/waitFor.sh
oc project $K8S_NAMESPACE

# Build
./mvnw -s .github/mvn-settings.xml clean package

# Create docker image and tag it in registry
## Worker service:
WORKER_IMAGE=messaging-worker:latest
docker build ./worker -t $WORKER_IMAGE
docker tag $WORKER_IMAGE $CONTAINER_REGISTRY/$WORKER_IMAGE
docker push $CONTAINER_REGISTRY/$WORKER_IMAGE

## Frontend service:
FRONTEND_IMAGE=messaging-frontend:latest
docker build ./frontend -t $FRONTEND_IMAGE
docker tag $FRONTEND_IMAGE $CONTAINER_REGISTRY/$FRONTEND_IMAGE
docker push $CONTAINER_REGISTRY/$FRONTEND_IMAGE

helm install cache ./helm -n $K8S_NAMESPACE  --set worker.docker.image=$CONTAINER_REGISTRY/$WORKER_IMAGE --set frontend.docker.image=$CONTAINER_REGISTRY/$FRONTEND_IMAGE
if [[ $(waitFor "spring-boot-messaging-work-queue-worker" "app") -eq 1 ]] ; then
  echo "Worker failed to deploy. Aborting"
  exit 1
fi

if [[ $(waitFor "spring-boot-messaging-work-queue-frontend" "app") -eq 1 ]] ; then
  echo "Frontend failed to deploy. Aborting"
  exit 1
fi

# Run Tests
./mvnw -s .github/mvn-settings.xml clean verify -Pkubernetes-it -Dkubernetes.namespace=$K8S_NAMESPACE

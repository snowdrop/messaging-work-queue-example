#!/usr/bin/env bash
SOURCE_REPOSITORY_URL=${1:-https://github.com/snowdrop/messaging-work-queue-example}
SOURCE_REPOSITORY_REF=${2:-sb-2.5.x}

source scripts/waitFor.sh

helm install messaging ./helm --set frontend.route.expose=true --set frontend.s2i.source.repo=$SOURCE_REPOSITORY_URL --set frontend.s2i.source.ref=$SOURCE_REPOSITORY_REF --set worker.route.expose=true --set worker.s2i.source.repo=$SOURCE_REPOSITORY_URL --set worker.s2i.source.ref=$SOURCE_REPOSITORY_REF
if [[ $(waitFor "spring-boot-messaging-work-queue-worker" "app") -eq 1 ]] ; then
  echo "Worker failed to deploy. Aborting"
  exit 1
fi

if [[ $(waitFor "spring-boot-messaging-work-queue-frontend" "app") -eq 1 ]] ; then
  echo "Frontend failed to deploy. Aborting"
  exit 1
fi

# Run OpenShift Tests
./mvnw -s .github/mvn-settings.xml clean verify -Popenshift,openshift-it

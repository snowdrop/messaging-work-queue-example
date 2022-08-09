#!/usr/bin/env bash
SOURCE_REPOSITORY_URL=${1:-https://github.com/snowdrop/messaging-work-queue-example}
SOURCE_REPOSITORY_REF=${2:-sb-2.5.x}
S2I_BUILDER_IMAGE_REPO=registry.access.redhat.com/ubi8/openjdk-11
S2I_BUILDER_IMAGE_TAG=1.14

source scripts/waitFor.sh

helm install messaging ./helm --set frontend.route.expose=true --set frontend.s2i.source.repo=$SOURCE_REPOSITORY_URL --set frontend.s2i.source.ref=$SOURCE_REPOSITORY_REF --set worker.route.expose=true --set worker.s2i.source.repo=$SOURCE_REPOSITORY_URL --set worker.s2i.source.ref=$SOURCE_REPOSITORY_REF --set frontend.s2i.builderImage.repo=$S2I_BUILDER_IMAGE_REPO --set frontend.s2i.builderImage.tag=$S2I_BUILDER_IMAGE_TAG --set worker.s2i.builderImage.repo=$S2I_BUILDER_IMAGE_REPO --set worker.s2i.builderImage.tag=$S2I_BUILDER_IMAGE_TAG
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

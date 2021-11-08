#!/usr/bin/env bash

# Exit script if you try to use an uninitialized variable.
set -o nounset

# Exit script if a statement returns a non-true return value.
set -o errexit

# Use the error status of the first failure, rather than that of the last item in a pipeline.
set -o pipefail


SCRIPT_ABSOLUTE_DIR="$(cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd)"
PROJECT_ABSOLUTE_DIR=$(dirname ${SCRIPT_ABSOLUTE_DIR})

pushd ${PROJECT_ABSOLUTE_DIR} > /dev/null

# deploy broker
oc apply -f .openshiftio/amq.yaml
oc new-app --template=amq-broker-72-basic -p APPLICATION_NAME=work-queue-broker -p IMAGE_STREAM_NAMESPACE=$(oc project -q) -p AMQ_NAME=work-queue-broker -p AMQ_PROTOCOL=amqp -p AMQ_USER=work-queue -p AMQ_PASSWORD=work-queue

# 1.- Deploy Frontend
./mvnw clean verify -pl frontend -Popenshift -Ddekorate.deploy=true "$@"

# wait for the app to stand up
sleep 30 # needed in order to bypass the 'Pending' state
timeout 300s bash -c 'while [[ $(oc get pod -o json | jq  ".items[] | select(.metadata.name | contains(\"build\"))  | .status  " | jq -rs "sort_by(.startTme) | last | .phase") == "Running" ]]; do sleep 20; done; echo ""'

# 2.- Deploy Worker
./mvnw clean verify -pl worker -Popenshift -Ddekorate.deploy=true "$@"

# wait for the app to stand up
sleep 30 # needed in order to bypass the 'Pending' state
timeout 300s bash -c 'while [[ $(oc get pod -o json | jq  ".items[] | select(.metadata.name | contains(\"build\"))  | .status  " | jq -rs "sort_by(.startTme) | last | .phase") == "Running" ]]; do sleep 20; done; echo ""'

# 3.- Run OpenShift Tests
./mvnw verify -pl tests -Popenshift-it "$@"

popd > /dev/null



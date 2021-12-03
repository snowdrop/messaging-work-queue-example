#!/usr/bin/env bash

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

#!/bin/sh

UPSTREAM_NAME="Red Hat Community"
UPSTREAM_GIT_URL="https://github.com/redhat-openshift-ecosystem/community-operators-prod"
FORK_GIT_URL="${FORK_GIT_URL:-$1}"
PROJECT_NAME="stackgres-community"
DO_PIN_IMAGES=true
HELM_OPERATOR_BUNDLE_IMAGE_TAG_SUFFIX=-openshift

deploy_extra_steps() {
  yq -y -s '.[0] * .[1]' \
    "$FORK_GIT_PATH/operators/$PROJECT_NAME/$STACKGRES_VERSION"/manifests/stackgres.clusterserviceversion.yaml \
    openshift-operator-bundle/red-hat-certified/stackgres.clusterserviceversion.yaml \
    > "$FORK_GIT_PATH/operators/$PROJECT_NAME/$STACKGRES_VERSION"/manifests/stackgres.clusterserviceversion.yaml.new
  mv "$FORK_GIT_PATH/operators/$PROJECT_NAME/$STACKGRES_VERSION"/manifests/stackgres.clusterserviceversion.yaml.new \
    "$FORK_GIT_PATH/operators/$PROJECT_NAME/$STACKGRES_VERSION"/manifests/stackgres.clusterserviceversion.yaml
}

. "$(dirname "$0")/deploy.sh"

#!/bin/sh

UPSTREAM_NAME="Red Hat Certified"
UPSTREAM_GIT_URL="https://github.com/redhat-openshift-ecosystem/certified-operators"
FORK_GIT_URL="${FORK_GIT_URL:-$1}"
PROJECT_NAME="stackgres-certified"
DO_PIN_IMAGES=true
OPERATOR_BUNDLE_IMAGE_TAG_SUFFIX=-openshift

deploy_extra_steps() {
  yq -y -s '.[0] * .[1]' \
    "$FORK_GIT_PATH/operators/$PROJECT_NAME/$STACKGRES_VERSION"/manifests/stackgres.clusterserviceversion.yaml \
    openshift-operator-bundle/red-hat-certified/stackgres.clusterserviceversion.yaml \
    > "$FORK_GIT_PATH/operators/$PROJECT_NAME/$STACKGRES_VERSION"/manifests/stackgres.clusterserviceversion.yaml.new
  mv "$FORK_GIT_PATH/operators/$PROJECT_NAME/$STACKGRES_VERSION"/manifests/stackgres.clusterserviceversion.yaml.new \
    "$FORK_GIT_PATH/operators/$PROJECT_NAME/$STACKGRES_VERSION"/manifests/stackgres.clusterserviceversion.yaml
}

set_previous_version_override() {
  OPENSHIFT_VERSIONS="$(yq -r '.annotations["com.redhat.openshift.versions"]' \
    "$FORK_GIT_PATH/operators/$PROJECT_NAME/$STACKGRES_VERSION/metadata/annotations.yaml")"
  PREVIOUS_OPENSHIFT_VERSIONS="$(yq -r '.annotations["com.redhat.openshift.versions"]' \
    "$FORK_GIT_PATH/operators/$PROJECT_NAME/$PREVIOUS_VERSION/metadata/annotations.yaml")"
  if [ "$OPENSHIFT_VERSIONS" = "$PREVIOUS_OPENSHIFT_VERSIONS" ]
  then
    echo "Setting replaces to stackgres.v$PREVIOUS_VERSION"
    sed -i "s/^\( *\)\(version: $STACKGRES_VERSION\)$/\1\2\n\1replaces: stackgres.v$PREVIOUS_VERSION/" \
      "$FORK_GIT_PATH/operators/$PROJECT_NAME/$STACKGRES_VERSION"/manifests/stackgres.clusterserviceversion.yaml
  else
    SKIP_PREVIOUS_VERSIONS="$(ls -1d "$FORK_GIT_PATH/operators/$PROJECT_NAME"/*/manifests \
      | cut -d / -f 5 | grep -v '.-rc.' | sort -t ' ' -k 1Vr \
      | while read CANDIDATE_SKIP_PREVIOUS_VERSION
        do
          if [ "$CANDIDATE_SKIP_PREVIOUS_VERSION" = "$STACKGRES_VERSION" ]
          then
            continue
          elif [ "$PREVIOUS_OPENSHIFT_VERSIONS" != "$(yq -r '.annotations["com.redhat.openshift.versions"]' \
            "$FORK_GIT_PATH/operators/$PROJECT_NAME/$CANDIDATE_SKIP_PREVIOUS_VERSION/metadata/annotations.yaml")" ]
          then
            break
          fi
          printf '%s\n' "$CANDIDATE_SKIP_PREVIOUS_VERSION"
        done)"
    SKIP_PREVIOUS_VERSIONS_LIST="$(for SKIP_PREVIOUS_VERSION in $SKIP_PREVIOUS_VERSIONS; do printf "stackgres.v%s," "$SKIP_PREVIOUS_VERSION"; done | sed 's/,$//')"
    echo "Setting skips to $SKIP_PREVIOUS_VERSIONS_LIST"
    sed -i "s/^\( *\)\(version: $STACKGRES_VERSION\)$/\1\2\n\1skips: [ $SKIP_PREVIOUS_VERSIONS_LIST ]/" \
      "$FORK_GIT_PATH/operators/$PROJECT_NAME/$STACKGRES_VERSION"/manifests/stackgres.clusterserviceversion.yaml
  fi
}

. "$(dirname "$0")/deploy.sh"

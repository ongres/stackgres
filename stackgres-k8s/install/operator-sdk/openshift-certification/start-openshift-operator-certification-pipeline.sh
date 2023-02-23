#!/bin/sh

set -e

cd "$(dirname "$0")"
PROJECT_PATH=../../../..
TARGET_PATH=target

mkdir -p "$TARGET_PATH"

if [ ! -f "$TARGET_PATH"/setup-completed ]
then
  E2E_ENV=crc CRC_ENABLE_DAEMON=true sh -x "$PROJECT_PATH"/stackgres-k8s/e2e/e2e delete_k8s || true
  rm -f "$TARGET_PATH"/start-completed
  rm -f "$TARGET_PATH"/install-openshift-pipelines-operator-rh-completed
  if [ -n "$CRC_BACKUP_PATH" ] && [ -d "$CRC_BACKUP_PATH" ]
  then
    rm -rf ~/.crc
    cp -a "$CRC_BACKUP_PATH" ~/.crc
  else
    cp ~/.crc/pull-secret ~/pull-secret
    rm -rf ~/.crc
    crc config set skip-check-daemon-systemd-unit true
    crc config set skip-check-daemon-systemd-sockets true
    crc config set network-mode user
    crc config set host-network-access true
    crc config set nameserver 8.8.8.8
    crc setup
    cp ~/pull-secret ~/.crc/pull-secret
  fi
  touch "$TARGET_PATH"/setup-completed
else
  echo
  echo "To repeat CRC setup remove following file:"
  echo "$(pwd)/$TARGET_PATH/setup-completed"
  echo
fi

alias oc="~/.crc/bin/oc/oc"

if [ ! -f "$TARGET_PATH"/start-completed ]
then
  rm -f "$TARGET_PATH"/install-openshift-pipelines-operator-rh-completed
  E2E_ENV=crc CRC_ENABLE_DAEMON=true sh -x "$PROJECT_PATH"/stackgres-k8s/e2e/e2e reset_k8s
  touch "$TARGET_PATH"/start-completed
else
  oc project default

  echo
  echo "To restart CRC remove following file:"
  echo "$(pwd)/$TARGET_PATH/start-completed"
  echo
  E2E_ENV=crc CRC_ENABLE_DAEMON=true sh -x "$PROJECT_PATH"/stackgres-k8s/e2e/e2e reuse_k8s
fi

if ! kubectl get ns certification > /dev/null 2>&1
then
  oc adm new-project certification
fi

oc project certification

oc delete secret kubeconfig || true
oc create secret generic kubeconfig --from-file=kubeconfig="$HOME/.kube/config"

oc delete secret github-api-token || true
oc create secret generic github-api-token --from-literal GITHUB_TOKEN="$OPENSHIFT_CERTIFICATION_GITHUB_TOKEN"

oc delete secret pyxis-api-secret || true
oc create secret generic pyxis-api-secret --from-literal pyxis_api_key="$PYXIS_API_TOKEN"

oc adm policy add-scc-to-user anyuid -z pipeline

if [ ! -f "$TARGET_PATH"/install-openshift-pipelines-operator-rh-completed ]
then
  LATEST_OPENSHIFT_PIPELINES_OPERATOR_CSV="$(
  oc get packagemanifests openshift-pipelines-operator-rh \
      --template '{{ range .status.channels }}{{ if eq .name "latest" }}{{ .currentCSV }}{{ "\n" }}{{ end }}{{ end }}')"

  cat << EOF | kubectl apply -f -
apiVersion: operators.coreos.com/v1alpha1
kind: Subscription
metadata:
  name: openshift-pipelines-operator-rh
  namespace: openshift-operators
spec:
  channel: latest
  installPlanApproval: Automatic
  name: openshift-pipelines-operator-rh
  source: redhat-operators
  sourceNamespace: openshift-marketplace
  startingCSV: $LATEST_OPENSHIFT_PIPELINES_OPERATOR_CSV
EOF
  rm -rf "$TARGET_PATH"/operator-pipelines
  git clone https://github.com/redhat-openshift-ecosystem/operator-pipelines "$TARGET_PATH"/operator-pipelines
  yq -y '.
      | del(.spec.tasks[0].taskref.bundle)
      | del(.spec.tasks[2].taskRef.bundle)
      | del(.spec.tasks[3].taskRef.bundle)
      | .spec.params = (.spec.params | map(if has("type") then . else .type = "string" end))
      ' \
    "$TARGET_PATH"/operator-pipelines/ansible/roles/operator-pipeline/templates/openshift/pipelines/community-signing-pipeline.yml \
    > "$TARGET_PATH"/operator-pipelines/ansible/roles/operator-pipeline/templates/openshift/pipelines/community-signing-pipeline.yml.tmp
  mv "$TARGET_PATH"/operator-pipelines/ansible/roles/operator-pipeline/templates/openshift/pipelines/community-signing-pipeline.yml.tmp \
    "$TARGET_PATH"/operator-pipelines/ansible/roles/operator-pipeline/templates/openshift/pipelines/community-signing-pipeline.yml
  yq -y '.
      | del(.spec.tasks[19].taskRef.bundle)
      | del(.spec.tasks[20].taskRef.bundle)
      | .spec.params = (.spec.params | map(if has("type") then . else .type = "string" end))
      ' \
    "$TARGET_PATH"/operator-pipelines/ansible/roles/operator-pipeline/templates/openshift/pipelines/operator-release-pipeline.yml \
    > "$TARGET_PATH"/operator-pipelines/ansible/roles/operator-pipeline/templates/openshift/pipelines/operator-release-pipeline.yml.tmp
  mv "$TARGET_PATH"/operator-pipelines/ansible/roles/operator-pipeline/templates/openshift/pipelines/operator-release-pipeline.yml.tmp \
    "$TARGET_PATH"/operator-pipelines/ansible/roles/operator-pipeline/templates/openshift/pipelines/operator-release-pipeline.yml
  yq -y '.
      | .spec.params = (.spec.params | map(if has("type") then . else .type = "string" end))
      ' \
    "$TARGET_PATH"/operator-pipelines/ansible/roles/operator-pipeline/templates/openshift/pipelines/operator-ci-pipeline.yml \
    > "$TARGET_PATH"/operator-pipelines/ansible/roles/operator-pipeline/templates/openshift/pipelines/operator-ci-pipeline.yml.tmp
  mv "$TARGET_PATH"/operator-pipelines/ansible/roles/operator-pipeline/templates/openshift/pipelines/operator-ci-pipeline.yml.tmp \
    "$TARGET_PATH"/operator-pipelines/ansible/roles/operator-pipeline/templates/openshift/pipelines/operator-ci-pipeline.yml
  until kubectl get crd pipelines.tekton.dev > /dev/null 2>&1 \
    && kubectl get crd tasks.tekton.dev > /dev/null 2>&1
  do
    echo -n .
    sleep 1
  done
  echo
  oc apply -R -f "$TARGET_PATH"/operator-pipelines/ansible/roles/operator-pipeline/templates/openshift/pipelines
  oc apply -R -f "$TARGET_PATH"/operator-pipelines/ansible/roles/operator-pipeline/templates/openshift/tasks
  oc import-image certified-operator-index \
    --from=registry.redhat.io/redhat/certified-operator-index \
    --reference-policy local \
    --scheduled \
    --confirm \
    --all > /dev/null
  oc import-image redhat-marketplace-index \
    --from=registry.redhat.io/redhat/redhat-marketplace-index \
    --reference-policy local \
    --scheduled \
    --confirm \
    --all > /dev/null
  touch "$TARGET_PATH"/install-openshift-pipelines-operator-rh-completed
else
  echo
  echo "To reinstall openshift-pipelines-operator-rh remove following file:"
  echo "$(pwd)/$TARGET_PATH/install-openshift-pipelines-operator-rh-completed"
  echo
fi

STACKGRES_VERSION="${STACKGRES_VERSION:-$(sh "$PROJECT_PATH"/stackgres-k8s/ci/build/version.sh)}"
tkn pipeline start operator-ci-pipeline \
  --param git_repo_url="$OPENSHIFT_CERTIFICATION_GITHUB_REPO" \
  --param git_branch=main \
  --param upstream_repo_name=redhat-openshift-ecosystem/certified-operators \
  --param bundle_path="operators/stackgres-operator/$STACKGRES_VERSION" \
  --param env=prod \
  --workspace name=pipeline,volumeClaimTemplateFile="$TARGET_PATH"/operator-pipelines/templates/workspace-template.yml \
  --pod-template "$TARGET_PATH"/operator-pipelines/templates/crc-pod-template.yml \
  --use-param-defaults \
  --showlog \
  --param submit=true
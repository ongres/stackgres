#!/bin/sh

set -e

cd "$(dirname "$0")"

OUTPUT_TEMPLATE="target/templates/stackgres-operator.qute.yaml"

STACKGRES_VERSION="$(sh ../../ci/build/version.sh)"

rm -Rf "target/stackgres-operator" "target/templates"
mkdir -p "target"

# Fix for Error: chart requires kubeVersion: X which is incompatible with Kubernetes Y
cp -a stackgres-operator "target/."
sed -i '/^kubeVersion: .*$/d' "target/stackgres-operator/Chart.yaml"

find target/stackgres-operator -type l \
  | while read LINK
    do
      ln -f -s "../$(readlink "$LINK")" "$LINK"
    done

mkdir -p "target/templates"

# Helm 3.6.0+ required to set the --kube-version
helm template --namespace '$namespace$' stackgres-operator "target/stackgres-operator" \
  --kube-version="v1.22" \
  --set-string adminui.service.type='$adminui-service-type$' \
  --set-string grafana.autoEmbed='true' \
  --set-string qutePreprocess='true' \
  > $OUTPUT_TEMPLATE

(
  BLOCK_SEPARATOR_INDEXES="$(grep '^---$' -n $OUTPUT_TEMPLATE)"
  BLOCK_SEPARATOR_INDEXES="$(printf '%s' "$BLOCK_SEPARATOR_INDEXES" | cut -d : -f 1)"
  INDEX=1
  PREVIOUS_BLOCK_SEPARATOR_INDEX=
  while true
  do
    BLOCK_SEPARATOR_INDEX="$(printf '%s' "$BLOCK_SEPARATOR_INDEXES" | head -n "$INDEX" | tail -n 1)"
    if [ "$BLOCK_SEPARATOR_INDEX" = "$PREVIOUS_BLOCK_SEPARATOR_INDEX" ]
    then
      break
    fi
    if [ -n "$PREVIOUS_BLOCK_SEPARATOR_INDEX" ]
    then
      tail -n +"$((PREVIOUS_BLOCK_SEPARATOR_INDEX+1))" $OUTPUT_TEMPLATE \
        | head -n "$((BLOCK_SEPARATOR_INDEX-PREVIOUS_BLOCK_SEPARATOR_INDEX-1))" \
        > "target/templates/stackgres-operator-qute-template-$INDEX.yaml"
    fi
    INDEX="$((INDEX+1))"
    PREVIOUS_BLOCK_SEPARATOR_INDEX="$BLOCK_SEPARATOR_INDEX"
  done
)

{
  cat << EOF
---
apiVersion: v1
kind: Namespace
metadata:
  name: {namespace}
EOF
  for CRD in ../../src/common/src/main/resources/crds/*.yaml
  do
    printf '\n%s\n' '---'
    cat "$CRD"
  done
  for RESOURCE in target/templates/stackgres-operator-qute-template-*.yaml
  do
    if [ "$(yq -r '[.]
      | any(
          (
            has("metadata")
            and (.metadata|has("annotations"))
            and (.metadata.annotations|has("helm.sh/hook"))
            | not)
          or (.metadata.annotations["helm.sh/hook"]
            | match("install"))
        )' "$RESOURCE")" = false ]
    then
      continue
    fi
    printf '\n%s\n' '---'
    cat "$RESOURCE"
  done
} > $OUTPUT_TEMPLATE

sed -i 's/\$namespace\$/{namespace}/g' $OUTPUT_TEMPLATE
sed -i 's/\$adminui-service-type\$/{adminui-service-type}/g' $OUTPUT_TEMPLATE

sed -i 's/#grafana.preprocess.start/{#if grafana-autoEmbed}/g' $OUTPUT_TEMPLATE
sed -i 's/#grafana.preprocess.end/{\/if}/g' $OUTPUT_TEMPLATE

sed -i 's/#comment.preprocess.start/{|/g' $OUTPUT_TEMPLATE
sed -i 's/#comment.preprocess.end/|}/g' $OUTPUT_TEMPLATE

sed -i 's/password: \".*\"/password: {webapi-password}/g' $OUTPUT_TEMPLATE
sed -i 's/clearPassword: \".*\"/clearPassword: {webapi-password-clear}/g' $OUTPUT_TEMPLATE

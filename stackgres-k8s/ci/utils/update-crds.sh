#!/bin/sh

set -e

. "$(dirname "$0")/utils"

KUBERNETES_API_BASE_URL='https://kubernetes.io/docs/reference/generated/kubernetes-api'

update_k8s_swagger

for CRD in "$PROJECT_PATH"/stackgres-k8s/src/common/src/main/resources/crds/*.yaml
do
  grep -n '#!jq_placeholder' "$CRD" \
    | sed 's/^\([^:]\+\):.*#!jq_placeholder \+\([^ ]\+\)\( \+\([^ ]\+\)\)\? *$/\1 \2 \4/' \
    | {
      PREV_LINE_NUMBER=0
      while read LINE_NUMBER JQ_EXPRESSION K8S_WEB_HASH
      do
        if [ "x$LINE_NUMBER" = x ] || [ "x$JQ_EXPRESSION" = x ]
        then
          echo "Bad jq_placeholder: LINE_NUMBER=$LINE_NUMBER JQ_EXPRESSION=$JQ_EXPRESSION K8S_WEB_HASH=$K8S_WEB_HASH"
          exit 1
        fi
        head -n "$LINE_NUMBER" "$CRD" | tail -n "$((LINE_NUMBER - PREV_LINE_NUMBER))" \
          | sed "s|$KUBERNETES_API_BASE_URL/[^/]\+/|$KUBERNETES_API_BASE_URL/v$K8S_VERSION/|g"
        head -n "$((LINE_NUMBER+1))" "$CRD" | tail -n 1 | sed 's/^\( *\)[^ ].*$/\1/' | tr -d '\n'
        if [ "x$K8S_WEB_HASH" != x ]
        then
          jq -M -c "$JQ_EXPRESSION
            | if has(\"description\")
              then .description = .description + \"\n\nSee $KUBERNETES_API_BASE_URL/v$K8S_VERSION/$K8S_WEB_HASH\"
              else . end" "$MERGED_DEFINITIONS_SWAGGER_FILE"
        else
          jq -M -c "$JQ_EXPRESSION" "$MERGED_DEFINITIONS_SWAGGER_FILE"
        fi
        PREV_LINE_NUMBER="$((LINE_NUMBER+1))"
      done
      tail -n +"$((PREV_LINE_NUMBER+1))" "$CRD" \
          | sed "s|$KUBERNETES_API_BASE_URL/[^/]\+/|$KUBERNETES_API_BASE_URL/v$K8S_VERSION/|g"
      } > "$CRD".tmp
  mv "$CRD".tmp "$CRD"
done

#!/bin/sh

run_op() {
  set -e

  update_status init

  echo "Starting sharded dbops $NORMALIZED_OP_NAME"

  local DBOPS_STATUS_SET
  DBOPS_STATUS_SET="$("$KUBECTL_BIN_PATH" get "$SHARDED_CLUSTER_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$SHARDED_CLUSTER_NAME" \
    --template='{{ if .status.dbOps }}{{ if .status.dbOps.majorVersionUpgrade }}true{{ end }}{{ end }}')"
  if [ "$DBOPS_STATUS_SET" != true ]
  then
    echo "Setting $NORMALIZED_OP_NAME status for $SHARDED_CLUSTER_CRD_KIND $SHARDED_CLUSTER_NAME"
    if ! "$KUBECTL_BIN_PATH" patch "$SHARDED_CLUSTER_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$SHARDED_CLUSTER_NAME" --type merge \
      -p "{\"status\":{\"dbOps\":{\"majorVersionUpgrade\":{\"sourcePostgresVersion\":$(printf %s "$SOURCE_POSTGRES_VERSION" | to_json_string),\"targetPostgresVersion\":$(printf %s "$TARGET_POSTGRES_VERSION" | to_json_string),\"sourceSgPostgresConfig\":$(printf %s "$SOURCE_SG_POSTGRES_CONFIG" | to_json_string)}}}}" \
      > /tmp/dbops-update-sharded-cluster 2>&1
    then
      echo "FAILURE=$NORMALIZED_OP_NAME failed. Can not update $SHARDED_CLUSTER_CRD_KIND status: $(cat /tmp/dbops-update-sharded-cluster)" >> "$SHARED_PATH/$KEBAB_OP_NAME.out"
      exit 1
    fi
  else
    SOURCE_POSTGRES_VERSION="$("$KUBECTL_BIN_PATH" get "$SHARDED_CLUSTER_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$SHARDED_CLUSTER_NAME" \
      --template='{{ .status.dbOps.majorVersionUpgrade.sourcePostgresVersion }}')"
  fi

  echo "Setting postgres version $TARGET_POSTGRES_VERSION and SGPostgresConfig $SG_POSTGRES_CONFIG for $SHARDED_CLUSTER_CRD_KIND $SHARDED_CLUSTER_NAME"
  if ! "$KUBECTL_BIN_PATH" patch "$SHARDED_CLUSTER_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$SHARDED_CLUSTER_NAME" --type merge \
    -p "{\"spec\":{\"postgres\":{\"version\":$(printf %s "$TARGET_POSTGRES_VERSION" | to_json_string)},\"coordinator\":{\"configurations\":{\"sgPostgresConfig\":$(printf %s "$SG_POSTGRES_CONFIG" | to_json_string)}},\"workers\":{\"configurations\":{\"sgPostgresConfig\":$(printf %s "$SG_POSTGRES_CONFIG" | to_json_string)}}}}" \
    > /tmp/dbops-update-sharded-cluster 2>&1
  then
    echo "FAILURE=$NORMALIZED_OP_NAME failed. Can not update $SHARDED_CLUSTER_CRD_KIND: $(cat /tmp/dbops-update-sharded-cluster)" >> "$SHARED_PATH/$KEBAB_OP_NAME.out"
    exit 1
  fi

  rm -f /tmp/current-dbops /tmp/completed-dbops
  local CLUSTER_NAME
  local DBOPS_NAME
  local BACKUP_PATH
  local OPTIONAL_FIELDS
  local INDEX=1
  for CLUSTER_NAME in $CLUSTER_NAMES
  do
    DBOPS_NAME="${SHARDED_DBOPS_NAME}-${CLUSTER_NAME#${SHARDED_CLUSTER_NAME}-}"
    echo "Creating $DBOPS_CRD_KIND $DBOPS_NAME for $CLUSTER_CRD_KIND $CLUSTER_NAME"
    echo "$DBOPS_NAME" >> /tmp/current-dbops
    BACKUP_PATH="$(printf %s "$BACKUP_PATHS" | cut -d ' ' -f "$INDEX")"
    CLUSTER_SG_POSTGRES_CONFIG="$(printf %s "$CLUSTER_SG_POSTGRES_CONFIGS" | cut -d ' ' -f "$INDEX")"
    DBOPS_YAML="$(cat << EOF
apiVersion: $DBOPS_CRD_APIVERSION
kind: $DBOPS_CRD_KIND
metadata:
  namespace: $CLUSTER_NAMESPACE
  name: $DBOPS_NAME
  labels: $DBOPS_LABELS_JSON
  ownerReferences:
  - apiVersion: $SHARDED_DBOPS_CRD_APIVERSION
    kind: $SHARDED_DBOPS_CRD_KIND
    name: $SHARDED_DBOPS_NAME
    uid: $SHARDED_DBOPS_UID
spec:
  sgCluster: $CLUSTER_NAME
  op: majorVersionUpgrade
  majorVersionUpgrade:
    postgresVersion: $(printf %s "$TARGET_POSTGRES_VERSION" | to_json_string)
    sgPostgresConfig: $(printf %s "$CLUSTER_SG_POSTGRES_CONFIG" | to_json_string)
    link: $LINK
    clone: $CLONE
    check: $CHECK
    manualRollback: true
$(
    if [ -n "$BACKUP_PATH" ]
    then
      cat << INNER_EOF
    backupPath: $(printf %s "$BACKUP_PATH" | to_json_string)
INNER_EOF
    fi
    if [ -n "$POSTGRES_EXTENSIONS_JSON" ]
    then
      cat << INNER_EOF
    postgresExtensions: $POSTGRES_EXTENSIONS_JSON
INNER_EOF
    fi
    if [ -n "$TO_INSTALL_POSTGRES_EXTENSIONS_JSON" ]
    then
      cat << INNER_EOF
    toInstallPostgresExtensions: $TO_INSTALL_POSTGRES_EXTENSIONS_JSON
INNER_EOF
    fi
    if [ -n "$MAX_ERRORS_AFTER_UPGRADE" ]
    then
      cat << INNER_EOF
    maxErrorsAfterUpgrade: $MAX_ERRORS_AFTER_UPGRADE
INNER_EOF
    fi
    if [ -n "$MAX_ERRORS_AFTER_CONTINUE_ON_FAILURE" ]
    then
      cat << INNER_EOF
    maxErrorsAfterContinueOnFailure: $MAX_ERRORS_AFTER_CONTINUE_ON_FAILURE
INNER_EOF
    fi
)
EOF
)"
    if ! printf %s "$DBOPS_YAML" | "$KUBECTL_BIN_PATH" replace --force -f - > /tmp/dbops-create-dbops 2>&1
    then
      echo "FAILURE=$NORMALIZED_OP_NAME failed. Can not create SGDbOps: $(cat /tmp/dbops-create-dbops)" >> "$SHARED_PATH/$KEBAB_OP_NAME.out"
      exit 1
    fi
    cat /tmp/dbops-create-dbops
    INDEX="$((INDEX + 1))"
  done

  echo "Setting the list of created $DBOPS_CRD_KIND on $SHARDED_CLUSTER_CRD_KIND $SHARDED_CLUSTER_NAME status"
  if ! "$KUBECTL_BIN_PATH" patch "$SHARDED_CLUSTER_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$SHARDED_CLUSTER_NAME" --type merge \
    -p "{\"status\":{\"dbOps\":{\"majorVersionUpgrade\":{\"sgDbOps\":[$(
      cat /tmp/current-dbops | sed 's/^\(.*\)$/"\1"/' | tr '\n' ',' | sed 's/,$//'
    )]}}}}" \
    > /tmp/dbops-update-sharded-cluster 2>&1
  then
    echo "FAILURE=$NORMALIZED_OP_NAME failed. Can not update $SHARDED_CLUSTER_CRD_KIND status: $(cat /tmp/dbops-update-sharded-cluster)" >> "$SHARED_PATH/$KEBAB_OP_NAME.out"
    exit 1
  fi

  # Each child SGDbOps is created with manualRollback enabled, so on failure it pauses with
  # .status.majorVersionUpgrade.phase = wait-post-failed-upgrade-decision and, on success, before
  # its cleanup with .status.majorVersionUpgrade.phase = wait-post-upgrade-decision, in both cases
  # waiting for its .status.majorVersionUpgrade.rollback to be set. Wait for every child to reach
  # such a decision point (or to terminate with a hard failure) before taking a single coordinated
  # decision for all of them.
  echo "Waiting for SGDbOps $(cat /tmp/current-dbops | tr '\n' ' ' | tr -s ' ') to reach the rollback decision point"
  local ANY_FAILED
  while true
  do
    local ALL_AT_DECISION_POINT=true
    ANY_FAILED=false
    for DBOPS_NAME in $(cat /tmp/current-dbops)
    do
      CHILD_PHASE="$(child_dbops_phase "$DBOPS_NAME")"
      DBOPS_STATUS="$("$KUBECTL_BIN_PATH" get "$DBOPS_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$DBOPS_NAME" \
        --template '{{ range .status.conditions }}{{ if eq .status "True" }} {{ .type }} {{ end }}{{ end }}')"
      case "$CHILD_PHASE" in
        (wait-post-failed-upgrade-decision)
        ANY_FAILED=true
        ;;
        (wait-post-upgrade-decision)
        ;;
        (*)
        if printf %s "$DBOPS_STATUS" | grep -q " $DBOPS_FAILED "
        then
          # Hard failure: the child could not even reach the decision point
          ANY_FAILED=true
        elif printf %s "$DBOPS_STATUS" | grep -q " $DBOPS_COMPLETED "
        then
          : # already completed, treat as a success that needs no decision
        else
          ALL_AT_DECISION_POINT=false
        fi
        ;;
      esac
    done
    update_status
    if "$ALL_AT_DECISION_POINT"
    then
      break
    fi
    retry_backoff
  done

  # Coordinated unique decision: roll back all SGDbOps if any of them failed, otherwise (all the
  # SGDbOps succeeded) continue all of them. When this SGShardedDbOps has its own manualRollback
  # enabled the decision is delegated to the user through this SGShardedDbOps
  # .status.majorVersionUpgrade.rollback.
  local DECISION
  if [ "$MANUAL_ROLLBACK" = true ]
  then
    local AGGREGATE_STATUS=succeeded
    if "$ANY_FAILED"
    then
      AGGREGATE_STATUS=failed
    fi
    echo "All $DBOPS_CRD_KIND reached the decision point (status: $AGGREGATE_STATUS)." \
      "Waiting for the rollback decision to be set on $SHARDED_DBOPS_CRD_KIND $SHARDED_DBOPS_NAME .status.majorVersionUpgrade.rollback..."
    "$KUBECTL_BIN_PATH" patch "$SHARDED_DBOPS_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$SHARDED_DBOPS_NAME" --type merge \
      -p "{\"status\":{\"majorVersionUpgrade\":{\"status\":\"$AGGREGATE_STATUS\"}}}" >/dev/null 2>&1 || true
    DECISION="$(wait_for_sharded_rollback_decision)"
    "$KUBECTL_BIN_PATH" patch "$SHARDED_DBOPS_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$SHARDED_DBOPS_NAME" --type merge \
      -p '{"status":{"majorVersionUpgrade":{"status":null,"rollback":null}}}' >/dev/null 2>&1 || true
  elif "$ANY_FAILED"
  then
    DECISION=true
  else
    DECISION=false
  fi

  echo "Taking coordinated rollback decision $DECISION for all $DBOPS_CRD_KIND"
  for DBOPS_NAME in $(cat /tmp/current-dbops)
  do
    case "$(child_dbops_phase "$DBOPS_NAME")" in
      (wait-post-failed-upgrade-decision|wait-post-upgrade-decision)
      echo "Setting rollback decision $DECISION on $DBOPS_CRD_KIND $DBOPS_NAME"
      "$KUBECTL_BIN_PATH" patch "$DBOPS_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$DBOPS_NAME" --type merge \
        -p "{\"status\":{\"majorVersionUpgrade\":{\"rollback\":$DECISION}}}" >/dev/null 2>&1 || true
      ;;
    esac
  done

  echo "Waiting for SGDbOps $(cat /tmp/current-dbops | tr '\n' ' ' | tr -s ' ') to complete"
  rm -f /tmp/completed-dbops
  touch /tmp/completed-dbops
  local FAILED=false
  while true
  do
    local COMPLETED=true
    for DBOPS_NAME in $(cat /tmp/current-dbops)
    do
      if ! grep -qxF "$DBOPS_NAME" /tmp/completed-dbops
      then
        DBOPS_STATUS="$("$KUBECTL_BIN_PATH" get "$DBOPS_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$DBOPS_NAME" \
          --template '{{ range .status.conditions }}{{ if eq .status "True" }} {{ .type }} {{ end }}{{ end }}')"
        if ! printf %s "$DBOPS_STATUS" | grep -q " \($DBOPS_COMPLETED\|$DBOPS_FAILED\) "
        then
          COMPLETED=false
          continue
        fi
        printf '%s\n' "$DBOPS_NAME" >> /tmp/completed-dbops
        update_status
        if printf %s "$DBOPS_STATUS" | grep -q " $DBOPS_FAILED "
        then
          echo "...$DBOPS_NAME failed"
          FAILED=true
        else
          echo "...$DBOPS_NAME completed"
        fi
      fi
    done
    if "$COMPLETED"
    then
      break
    fi
    retry_backoff
  done

  if "$FAILED"
  then
    echo "FAILURE=$NORMALIZED_OP_NAME failed. One or more SGDbOps failed" >> "$SHARED_PATH/$KEBAB_OP_NAME.out"
    exit 1
  fi

  echo "Removing $NORMALIZED_OP_NAME status from $SHARDED_CLUSTER_CRD_KIND $SHARDED_CLUSTER_NAME"
  if ! "$KUBECTL_BIN_PATH" patch "$SHARDED_CLUSTER_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$SHARDED_CLUSTER_NAME" --type json \
    -p '[{"op":"remove","path":"/status/dbOps"}]' \
    > /tmp/dbops-update-sharded-cluster 2>&1
  then
    echo "FAILURE=$NORMALIZED_OP_NAME failed. Can not update $SHARDED_CLUSTER_CRD_KIND status: $(cat /tmp/dbops-update-sharded-cluster)" >> "$SHARED_PATH/$KEBAB_OP_NAME.out"
    exit 1
  fi

  echo "Sharded DbOps $NORMALIZED_OP_NAME completed"
}

# Returns the child SGDbOps .status.majorVersionUpgrade.phase (empty if unset). A child created with
# manualRollback enabled pauses at phase wait-post-failed-upgrade-decision on failure and at phase
# wait-post-upgrade-decision on success (before its cleanup), in both cases waiting for its
# .status.majorVersionUpgrade.rollback to be set.
child_dbops_phase() {
  "$KUBECTL_BIN_PATH" get "$DBOPS_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$1" -o json 2>/dev/null \
    | jq -r 'if .status.majorVersionUpgrade.phase == null then "" else .status.majorVersionUpgrade.phase end' \
    2>/dev/null || printf ''
}

wait_for_sharded_rollback_decision() {
  local DECISION
  while true
  do
    DECISION="$("$KUBECTL_BIN_PATH" get "$SHARDED_DBOPS_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$SHARDED_DBOPS_NAME" -o json 2>/dev/null \
      | jq -r 'if .status.majorVersionUpgrade.rollback == null then "" else (.status.majorVersionUpgrade.rollback | tostring) end' \
      2>/dev/null || printf '')"
    if [ "$DECISION" = true ] || [ "$DECISION" = false ]
    then
      printf %s "$DECISION"
      return 0
    fi
    retry_backoff
  done
}

update_status() {
  if [ "$1" = "init" ]
  then
    PENDING_TO_RESTART_CLUSTERS="$CLUSTER_NAMES"
    RESTARTED_CLUSTERS=""
  else
    DBOPS_STATUSES="$("$KUBECTL_BIN_PATH" get "$DBOPS_CRD_NAME" -n "$CLUSTER_NAMESPACE" -l "$DBOPS_LABELS" \
      --template '{{ range .items }}{{ .spec.sgCluster }}/{{ range .status.conditions }}{{ if eq .status "True" }} {{ .type }} {{ end }}{{ end }}{{ "\n" }}{{ end }}')"
    PENDING_TO_RESTART_CLUSTERS="$(echo "$CLUSTER_NAMES" | tr ' ' '\n' | grep -vxF '' \
      | while read CLUSTER
        do
          if ! printf '%s' "$DBOPS_STATUSES" | cut -d / -f 1 | grep -q "^$CLUSTER$" \
            || ! printf '%s' "$DBOPS_STATUSES" | grep -q "^$CLUSTER/.* $DBOPS_COMPLETED .*$"
          then
            echo "$CLUSTER"
          fi
        done)"
    RESTARTED_CLUSTERS="$(echo "$CLUSTER_NAMES" | tr ' ' '\n' \
      | while read CLUSTER
        do
          if printf '%s' "$DBOPS_STATUSES" | grep -q "^$CLUSTER/.* $DBOPS_COMPLETED .*$"
          then
            echo "$CLUSTER"
          fi
        done)"
  fi
  PENDING_TO_RESTART_CLUSTERS_COUNT="$(echo "$PENDING_TO_RESTART_CLUSTERS" | tr ' ' 's' | tr '\n' ' ' | wc -w)"
  echo "Pending to $NORMALIZED_OP_NAME clusters:"
  if [ "$PENDING_TO_RESTART_CLUSTERS_COUNT" = 0 ]
  then
    echo '<none>'
  else
    echo "$PENDING_TO_RESTART_CLUSTERS" | tr ' ' '\n' | grep -vxF '' | sed 's/^/ - /'
  fi
  echo

  OPERATION="$("$KUBECTL_BIN_PATH" get "$SHARDED_DBOPS_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$SHARDED_DBOPS_NAME" \
    --template='{{ if .status.majorVersionUpgrade }}replace{{ else }}add{{ end }}')"
  "$KUBECTL_BIN_PATH" patch "$SHARDED_DBOPS_CRD_NAME" -n "$CLUSTER_NAMESPACE" "$SHARDED_DBOPS_NAME" --type=json \
    -p "$(cat << EOF
[
  {"op":"$OPERATION","path":"/status/majorVersionUpgrade","value":{
      "sourcePostgresVersion": $(printf %s "$SOURCE_POSTGRES_VERSION" | to_json_string),
      "targetPostgresVersion": $(printf %s "$TARGET_POSTGRES_VERSION" | to_json_string),
      "pendingToRestartSgClusters": [$(
        FIRST=true
        for CLUSTER in $PENDING_TO_RESTART_CLUSTERS
        do
          if "$FIRST"
          then
            printf '%s' "\"$CLUSTER\""
            FIRST=false
          else
            printf '%s' ",\"$CLUSTER\""
          fi
        done
        )],
      "restartedSgClusters": [$(
        FIRST=true
        for CLUSTER in $RESTARTED_CLUSTERS
        do
          if "$FIRST"
          then
            printf '%s' "\"$CLUSTER\""
            FIRST=false
          else
            printf '%s' ",\"$CLUSTER\""
          fi
        done
        )]
    }
  }
]
EOF
    )"
}

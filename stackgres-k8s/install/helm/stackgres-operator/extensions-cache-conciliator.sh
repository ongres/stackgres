#!/bin/sh
# shellcheck disable=SC2039
# shellcheck disable=SC2010
# shellcheck disable=SC2012

set -e

{ [ "$EXTENSIONS_CACHE_LOG_LEVEL" != DEBUG ] && [ "$EXTENSIONS_CACHE_LOG_LEVEL" != TRACE ]; } || set -x

run () {
  # shellcheck disable=SC2153
  [ -n "$EXTENSIONS_REPOSITORY_URLS" ]
  [ -n "$1" ]
  [ -n "$2" ]

  NAMESPACE="$1"
  STATEFULSET_NAME="$2"
  SERVICEACCOUNT_NAME="${3:-$2}"
  PERSISTENTVOLUMECLAIM_NAME="${4:-$2}"
  SERVICEACCOUNT_JSON_FILE=serviceaccount.json
  STATEFULSET_JSON_FILE=statefulset.json
  ANY_IMAGE_REPOSITORY_URL="$(any_image_repository_url && echo true || echo false)"

  EXTENSION_METADATA_VERSION=v2
  DEFAULT_FLAVOR=pg
  DEFAULT_BUILD_ARCH=x86_64
  DEFAULT_BUILD_OS=linux
  NOT_FOUND_URL_REGEXP='^[^ ]\+ - - \[\([^]]\+\)\] "GET \([^ ]\+\) HTTP\/1\.1" 404 [^ ]\+ "[^"]*" "[^"]*" "[^"]*"$'

  while true
  do
    set +e
    (
    set -e

    echo "Updating indexes..."
    pull_indexes
    echo "done"
    echo

    if [ "$ANY_IMAGE_REPOSITORY_URL" = true ]
    then
      echo "Updating repositories credentials..."
      (
      [ "$EXTENSIONS_CACHE_LOG_LEVEL" = TRACE ] || set +x
      SERVICEACCOUNT="$(kubectl get serviceaccount -n "$NAMESPACE" "$SERVICEACCOUNT_NAME" -o json)"
      printf '%s' "$SERVICEACCOUNT" > "$SERVICEACCOUNT_JSON_FILE"
      )
      update_repositories_credentials
      (
      [ "$EXTENSIONS_CACHE_LOG_LEVEL" = TRACE ] || set +x
      kubectl patch serviceaccount -n "$NAMESPACE" "$SERVICEACCOUNT_NAME" --type merge -p "$(cat "$SERVICEACCOUNT_JSON_FILE")"
      )
      echo "done"
      echo
    fi

    echo "Updating extensions..."
    (
    [ "$EXTENSIONS_CACHE_LOG_LEVEL" = TRACE ] || set +x
    STATEFULSET="$(kubectl get statefulset -n "$NAMESPACE" "$STATEFULSET_NAME" -o json)"
    printf '%s' "$STATEFULSET" > "$STATEFULSET_JSON_FILE"
    )
    (
    [ "$EXTENSIONS_CACHE_LOG_LEVEL" = TRACE ] || set +x
    get_to_install_extensions > full_to_install_extensions
    sort full_to_install_extensions \
      | uniq | { grep -v '^$' || true; } > to_install_extensions
    TO_INSTALL_EXTENSIONS_JSON_STRING="$(jq -sR . to_install_extensions)"
    PULLED_TO_INSTALL_EXTENSIONS_JSON_STRING="$(
      jq '.metadata.annotations
        | if . != null then .pulled_extensions else "" end
        | if . != null then . else "" end' "$STATEFULSET_JSON_FILE")"
    printf '%s' "$PULLED_TO_INSTALL_EXTENSIONS_JSON_STRING" | jq -r . > already_pulled_to_install_extensions
    if [ "$TO_INSTALL_EXTENSIONS_JSON_STRING" != "$PULLED_TO_INSTALL_EXTENSIONS_JSON_STRING" ]
    then
      while read -r LINE
      do
        grep -q -xF "$LINE" already_pulled_to_install_extensions || printf '%s\n' "$LINE"
      done < to_install_extensions > pull_to_install_extensions
    else
      true > pull_to_install_extensions
    fi
    get_not_found_to_install_extensions > not_found_to_install_extensions
    )
    rm -f pulled_to_install_extensions
    touch pulled_to_install_extensions
    cat pull_to_install_extensions not_found_to_install_extensions \
      | while read -r TO_INSTALL_REPOSITORY PUBLISHER NAME VERSION POSTGRES_VERSION FLAVOR BUILD_ARCH BUILD_OS BUILD
        do
          try_function pull_extension "$TO_INSTALL_REPOSITORY" "$PUBLISHER" "$NAME" \
              "$VERSION" "$POSTGRES_VERSION" "$FLAVOR" "$BUILD_ARCH" "$BUILD_OS" "$BUILD"
          if "$RESULT"
          then
            echo "$TO_INSTALL_REPOSITORY" "$PUBLISHER" "$NAME" \
              "$VERSION" "$POSTGRES_VERSION" "$FLAVOR" "$BUILD_ARCH" "$BUILD_OS" "$BUILD" >> pulled_to_install_extensions
          else
            echo "Warning: error while trying to pull extension " \
              "$NAME-$VERSION-$FLAVOR$POSTGRES_VERSION$([ -z "$BUILD" ] || printf '%s' "-build-$BUILD")" \
              "(publisher $PUBLISHER from $TO_INSTALL_REPOSITORY for $BUILD_ARCH/$BUILD_OS)" >&2
          fi
        done
    (
    [ "$EXTENSIONS_CACHE_LOG_LEVEL" = TRACE ] || set +x
    ALREADY_PULLED_TO_INSTALL_EXTENSIONS_JSON_STRING="$(
      jq '.metadata.annotations.pulled_extensions' "$STATEFULSET_JSON_FILE")"
    PULLED_TO_INSTALL_EXTENSIONS_JSON_STRING="$(
      cat pulled_to_install_extensions already_pulled_to_install_extensions \
        | sort | uniq | jq -sR .)"
    if [ "$ALREADY_PULLED_TO_INSTALL_EXTENSIONS_JSON_STRING" != "$PULLED_TO_INSTALL_EXTENSIONS_JSON_STRING" ]
    then
      RUNNING_IMAGES="$(kubectl get statefulset -n "$NAMESPACE" "$STATEFULSET_NAME" \
        --template '{{ range .spec.template.spec.containers }}{{ printf "%s\n" .image }}{{ end }}')"
      RUNNING_IMAGES="$(printf '%s' "$RUNNING_IMAGES" | sort)"
      REQUIRED_IMAGES="$(jq '.spec.template.spec.containers[].image' "$STATEFULSET_JSON_FILE")"
      REQUIRED_IMAGES="$(printf '%s' "$REQUIRED_IMAGES" | sort)"
      if [ "$RUNNING_IMAGES" != "$REQUIRED_IMAGES" ]
      then
        touch /tmp/need-restart
      fi
      STATEFULSET="$(cat "$STATEFULSET_JSON_FILE")"
      printf '%s' "$STATEFULSET" \
        | jq ".metadata.annotations.pulled_extensions = $PULLED_TO_INSTALL_EXTENSIONS_JSON_STRING" \
        > "$STATEFULSET_JSON_FILE"
      cp "$STATEFULSET_JSON_FILE" "$STATEFULSET_JSON_FILE.new"
      until kubectl replace statefulset -n "$NAMESPACE" "$STATEFULSET_NAME" \
        --cascade orphan -f "$STATEFULSET_JSON_FILE.new"
      do
        sleep 5
        STATEFULSET="$(kubectl get statefulset -n "$NAMESPACE" "$STATEFULSET_NAME" -o json)"
        printf '%s' "$STATEFULSET" > "$STATEFULSET_JSON_FILE.lastest"
        jq -s '.[0].spec.template = .[1].spec.template | .[0]' \
          "$STATEFULSET_JSON_FILE.lastest" "$STATEFULSET_JSON_FILE" > "$STATEFULSET_JSON_FILE.new"
      done
    fi
    if ! any_image_repository_url && test -f /tmp/need-restart
    then
      kubectl delete pod -n "$NAMESPACE" "$STATEFULSET_NAME-0"
    fi
    )
    echo "done"
    echo
    )
    EXIT_CODE="$?"
    if [ "$EXIT_CODE" = 0 ]
    then
      if ! test -f /tmp/need-restart
      then
        touch /tmp/extensions-cache-ready
      fi
      echo
      echo "...wait for next reconciliation cycle"
      echo
      sleep 10
    else
      echo
      echo "...an error occurred during reconciliation cycle, retrying in 10 seconds"
      echo
      sleep 10
    fi
  done
}

any_image_repository_url() {
  local EXTENSIONS_REPOSITORY_URL
  for EXTENSIONS_REPOSITORY_URL in $(echo "$EXTENSIONS_REPOSITORY_URLS" | tr ',' '\n')
  do
    if is_image_repository_url "$EXTENSIONS_REPOSITORY_URL"
    then
      return
    fi
  done
  return 1
}

is_image_repository_url() {
  printf '%s' "$1" | grep -q "[?&]imageTemplate="
}

is_image_repository_with_regcred_secret_url() {
  printf '%s' "$1" | grep "[?&]imageTemplate=" | grep -q "[?&]imageRegcredSecret="
}

is_proxied_repository_url() {
  printf '%s' "$1" | grep -q "[?&]proxyUrl="
}

is_cache_timeout_repository_url() {
  printf '%s' "$1" | grep -q "[?&]cacheTimeout="
}

is_skip_hostname_verification_repository_url() {
  printf '%s' "$1" | grep -q "[?&]skipHostnameVerification="
}

is_set_http_scheme_url() {
  printf '%s' "$1" | grep -q "[?&]setHttpScheme="
}

get_image_template_from_url() {
  local IMAGE_TEMPLATE
  IMAGE_TEMPLATE="$(printf '%s' "$1" | grep "[?&]imageTemplate=")"
  IMAGE_TEMPLATE="$(printf '%s' "$IMAGE_TEMPLATE" \
    | sed 's/^.*[?&]imageTemplate=\([^?&]\+\)\([?&].*\)\?$/\1/')"
  IMAGE_TEMPLATE="$(printf '%s' "$IMAGE_TEMPLATE" | urldecode)"
  # shellcheck disable=SC2016
  if printf '%s' "$IMAGE_TEMPLATE" | grep -q '\([`;"]\|\$(\)'
  then
    # shellcheck disable=SC2016
    echo 'Forbidden strings [`|;|"|$(] found in parameter imageTemplate of URL '"$1" >&2
    return 1
  fi
  printf '%s' "$IMAGE_TEMPLATE"
}

get_image_regcred_secret_from_url() {
  local REPOSITORY_CREDENTIAL_SECRET
  REPOSITORY_CREDENTIAL_SECRET="$(printf '%s' "$1" | grep "[?&]imageRegcredSecret=")"
  REPOSITORY_CREDENTIAL_SECRET="$(printf '%s' "$REPOSITORY_CREDENTIAL_SECRET" \
    | sed 's/^.*[?&]imageRegcredSecret=\([^?&]\+\)\([?&].*\)\?$/\1/')"
  REPOSITORY_CREDENTIAL_SECRET="$(printf '%s' "$REPOSITORY_CREDENTIAL_SECRET" | urldecode)"
  printf '%s' "$REPOSITORY_CREDENTIAL_SECRET"
}

get_proxy_from_url() {
  local PROXY_URL
  PROXY_URL="$(printf '%s' "$1" | grep "[?&]proxyUrl=")"
  PROXY_URL="$(printf '%s' "$PROXY_URL" \
    | sed 's/^.*[?&]proxyUrl=\([^?&]\+\)\([?&].*\)\?$/\1/')"
  PROXY_URL="$(printf '%s' "$PROXY_URL" | urldecode)"
  printf '%s' "$PROXY_URL"
}

get_cache_timeout_from_url() {
  local CACHE_TIMEOUT
  CACHE_TIMEOUT="$(printf '%s' "$1" | grep "[?&]cacheTimeout=")"
  CACHE_TIMEOUT="$(printf '%s' "$CACHE_TIMEOUT" \
    | sed 's/^.*[?&]cacheTimeout=\([^?&]\+\)\([?&].*\)\?$/\1/')"
  CACHE_TIMEOUT="$(printf '%s' "$CACHE_TIMEOUT" | urldecode)"
  printf '%s' "$CACHE_TIMEOUT"
}

get_skip_hostname_verification_from_url() {
  local SKIP_HOSTNAME_VERIFICATION
  SKIP_HOSTNAME_VERIFICATION="$(printf '%s' "$1" | grep "[?&]skipHostnameVerification=")"
  SKIP_HOSTNAME_VERIFICATION="$(printf '%s' "$SKIP_HOSTNAME_VERIFICATION" \
    | sed 's/^.*[?&]skipHostnameVerification=\([^?&]\+\)\([?&].*\)\?$/\1/')"
  SKIP_HOSTNAME_VERIFICATION="$(printf '%s' "$SKIP_HOSTNAME_VERIFICATION" | urldecode)"
  printf '%s' "$SKIP_HOSTNAME_VERIFICATION"
}

get_set_http_scheme_from_url() {
  local SET_HTTP_SCHEME
  SET_HTTP_SCHEME="$(printf '%s' "$1" | grep "[?&]setHttpScheme=")"
  SET_HTTP_SCHEME="$(printf '%s' "$SET_HTTP_SCHEME" \
    | sed 's/^.*[?&]setHttpScheme=\([^?&]\+\)\([?&].*\)\?$/\1/')"
  SET_HTTP_SCHEME="$(printf '%s' "$SET_HTTP_SCHEME" | urldecode)"
  printf '%s' "$SET_HTTP_SCHEME"
}

pull_indexes() {
  local INDEX_NAME EXTENSIONS_REPOSITORY_URL
  local INDEX
  for INDEX_NAME in index hashes
  do
    if ! test -s "unwrapped-${INDEX_NAME}.json"
    then
      echo '{}' > "unwrapped-${INDEX_NAME}.json"
    fi
    INDEX=-1
    for EXTENSIONS_REPOSITORY_URL in $(echo "$EXTENSIONS_REPOSITORY_URLS" | tr ',' '\n')
    do
      INDEX="$((INDEX + 1))"
      if ! is_index_cache_expired "$EXTENSIONS_REPOSITORY_URL" "$INDEX_NAME" "$INDEX"
      then
        continue
      fi
      local CURL_EXTRA_OPTS=""
      if is_proxied_repository_url "$EXTENSIONS_REPOSITORY_URL"
      then
        local PROXY_URL
        PROXY_URL="$(get_proxy_from_url "$EXTENSIONS_REPOSITORY_URL")"
        CURL_EXTRA_OPTS="$CURL_EXTRA_OPTS --proxy $PROXY_URL"
      fi
      if is_skip_hostname_verification_repository_url "$EXTENSIONS_REPOSITORY_URL"
      then
        local SKIP_HOSTNAME_VERIFICATION
        SKIP_HOSTNAME_VERIFICATION="$(get_skip_hostname_verification_from_url "$EXTENSIONS_REPOSITORY_URL")"
        if [ "$SKIP_HOSTNAME_VERIFICATION" = true ]
        then
          CURL_EXTRA_OPTS="$CURL_EXTRA_OPTS -k"
        fi
      fi
      curl -f -s -L $CURL_EXTRA_OPTS "${EXTENSIONS_REPOSITORY_URL%%\?*}/${EXTENSION_METADATA_VERSION}/${INDEX_NAME}.json" > "last-${INDEX_NAME}.json"
      jq -s '.[0] as $last | .[1] as $current_unwrapped
        | $last
        | .publishers = (.publishers | map(
          {
            key: .id,
            value: .
          }
          ) | from_entries)
        | .extensions = (.extensions | map(
          {
            key: .name,
            value: (.repository = "'"${EXTENSIONS_REPOSITORY_URL%%\?*}"'")
          }
          ) | from_entries)
        | . as $last_unwrapped
        | $current_unwrapped * $last_unwrapped
        ' "last-${INDEX_NAME}.json" "unwrapped-${INDEX_NAME}.json" > "new-unwrapped-${INDEX_NAME}.json"
      local EXTENSIONS_REPOSITORY_PATH="${EXTENSIONS_REPOSITORY_URL#*://}"
      EXTENSIONS_REPOSITORY_PATH="${EXTENSIONS_REPOSITORY_PATH%%\?*}"
      mkdir -p "$EXTENSIONS_REPOSITORY_PATH/$EXTENSION_METADATA_VERSION/"
      mv "last-${INDEX_NAME}.json" "$EXTENSIONS_REPOSITORY_PATH/$EXTENSION_METADATA_VERSION/${INDEX_NAME}.json"
      mv "new-unwrapped-${INDEX_NAME}.json" "unwrapped-${INDEX_NAME}.json"
      date +%s > "${INDEX_NAME}-${INDEX}.timestamp"
    done
    jq '.extensions = (.extensions | to_entries | map(.value))
        | .publishers = (.publishers | to_entries | map(.value))
        ' "unwrapped-${INDEX_NAME}.json" > "merged-${INDEX_NAME}.json"
    mv "merged-${INDEX_NAME}.json" "${INDEX_NAME}.json"
  done
}

is_any_index_expired() {
  local INDEX_NAME
  for INDEX_NAME in index hashes
  do
    local INDEX=-1
    for EXTENSIONS_REPOSITORY_URL in $(echo "$EXTENSIONS_REPOSITORY_URLS" | tr ',' '\n')
    do
      INDEX="$((INDEX + 1))"
      if is_index_cache_expired "$EXTENSIONS_REPOSITORY_URL" "$INDEX_NAME" "$INDEX"
      then
        return
      fi
    done
  done
  return 1
}

is_index_cache_expired() {
  [ -n "$1" ] && [ -n "$2" ] && [ -n "$3" ] \
    && true || false
  local EXTENSIONS_REPOSITORY_URL="$1"
  local INDEX_NAME="$2"
  local INDEX="$3"
  local CACHE_TIMEOUT=3600

  if is_cache_timeout_repository_url "$EXTENSIONS_REPOSITORY_URL"
  then
    CACHE_TIMEOUT="$(get_cache_timeout_from_url "$EXTENSIONS_REPOSITORY_URL")"
  fi
  ! test -f "${INDEX_NAME}-${INDEX}.timestamp" \
    ||  [ "$(date +%s)" -ge "$(( $(cat "${INDEX_NAME}-${INDEX}.timestamp") + CACHE_TIMEOUT ))" ]
}

update_repositories_credentials() {
  local EXTENSIONS_REPOSITORY_URL SERVICEACCOUNT

  for EXTENSIONS_REPOSITORY_URL in $(echo "$EXTENSIONS_REPOSITORY_URLS" | tr ',' '\n')
  do
    if is_image_repository_with_regcred_secret_url "$EXTENSIONS_REPOSITORY_URL"
    then
      REPOSITORY_CREDENTIAL_SECRET="$(get_image_regcred_secret_from_url "$EXTENSIONS_REPOSITORY_URL")"
      if jq '([] + .imagePullSecrets)|any(.name == "'"$REPOSITORY_CREDENTIAL_SECRET"'")' "$SERVICEACCOUNT_JSON_FILE" | grep -q false
      then
        (
        [ "$EXTENSIONS_CACHE_LOG_LEVEL" = TRACE ] || set +x
        SERVICEACCOUNT="$(jq ".imagePullSecrets = .imagePullSecrets + [{name: \"$REPOSITORY_CREDENTIAL_SECRET\"}]" "$SERVICEACCOUNT_JSON_FILE")"
        printf '%s' "$SERVICEACCOUNT" > "$SERVICEACCOUNT_JSON_FILE"
        )
      fi
    fi
  done
}

get_to_install_extensions() {
  (
  CLUSTER_EXTENSIONS="$(kubectl get sgcluster -A -o json)"
  DISTRIBUTEDLOGS_EXTENSIONS="$(kubectl get sgdistributedlogs -A -o json)"
  printf '%s' "$CLUSTER_EXTENSIONS" | jq '.items[]'
  printf '%s' "$DISTRIBUTEDLOGS_EXTENSIONS" | jq '.items[]'
  ) | jq -r -s '.[]
    | select(has("status") and (.status | has("arch") and has("os")))
    | .status as $status
    | .spec.toInstallPostgresExtensions
    | if . != null then . else [] end
    | .[]
    | .repository + " "
        + .publisher + " "
        + .name + " "
        + .version + " "
        + .postgresVersion
        + " " + (.flavor | if . != null then . else "'"$DEFAULT_FLAVOR"'" end)
        + " " + $status.arch
        + " " + $status.os
        + " " + (.build | if . != null then . else "" end)
        '
  jq -r '
    .extensions[] | . as $extension
    | .versions[] | . as $version
    | .availableFor = (.availableFor
      | sort_by(if .build == null then 0 else (.build | split(".") 
        | (.[0] | tonumber | . * 10000) + (.[1] | split("-")[0] | tonumber)) end)
      | reduce .[] as $availableFor ({};
        . as $result | ($availableFor.postgresVersion | if . != null then . else "any" end)
          + "-" + ($availableFor.arch | if . != null then . else "'"$DEFAULT_BUILD_ARCH"'" end)
          + "-" + ($availableFor.os | if . != null then . else "'"$DEFAULT_BUILD_OS"'" end) | . as $key
        | $result | .[$key] = $availableFor) | to_entries | map(.value))
    | .availableFor[] | . as $availableFor
    | select('"$EXTENSIONS_CACHE_PRELOADED_EXTENSIONS"' | any(. as $test
      | $extension.publisher
        + "/" + ($availableFor.arch | if . != null then . else "'"$DEFAULT_BUILD_ARCH"'" end)
        + "/" + ($availableFor.os | if . != null then . else "'"$DEFAULT_BUILD_OS"'" end)
        + "/" + $extension.name + "-" + $version.version + "-"
        + ($availableFor.flavor | if . != null then . else "'"$DEFAULT_FLAVOR"'" end)
        + $availableFor.postgresVersion
        + ($availableFor.build | if . != null then "-build-" + . else "" end)
      | test($test; "")))
    | $extension.repository + " "
      + $extension.publisher + " "
      + $extension.name + " "
      + $version.version + " "
      + $availableFor.postgresVersion
      + " " + ($availableFor.flavor | if . != null then . else "'"$DEFAULT_FLAVOR"'" end)
      + " " + ($availableFor.arch | if . != null then . else "'"$DEFAULT_BUILD_ARCH"'" end)
      + " " + ($availableFor.os | if . != null then . else "'"$DEFAULT_BUILD_OS"'" end)
      + " " + ($availableFor.build | if . != null then . else "" end)' \
    index.json || echo "Error while applying filter $EXTENSIONS_CACHE_PRELOADED_EXTENSIONS to preload extensions" >&2
}

get_not_found_to_install_extensions() {
  rm -f last_date_time_found
  grep "$NOT_FOUND_URL_REGEXP" /var/log/nginx/access.log \
    | sed -u "s/$NOT_FOUND_URL_REGEXP/\1 \2/" \
    | while read -r DATE_TIME TIME_ZONE NOT_FOUND_URL
      do
        if test -f last_date_time \
          && [ "$DATE_TIME $TIME_ZONE" = "$(cat last_date_time)" ] \
          && ! test -f last_date_time_found
        then
          touch last_date_time_found
          continue
        fi
        try_function get_extension_from_url "$NOT_FOUND_URL"
        if "$RESULT"
        then
          printf '%s %s' "$DATE_TIME" "$TIME_ZONE" > last_date_time
        else
          echo "Warning: error while trying to extract extension from $NOT_FOUND_URL" >&2
        fi
      done
}

get_extension_from_url() {
  [ -n "$1" ] && true || false
  local NOT_FOUND_URL="$1"
  local EXTENSIONS_REPOSITORY_URL NOT_FOUND_URL_PATH EXTENSIONS_REPOSITORY_URL_PATH
  local REPOSITORY PUBLISHER BUILD_ARCH BUILD_OS EXTENSION_PACKAGE EXTENSION_ID_JSON
  EXTENSIONS_REPOSITORY_URL="$(find_repository_base_url "$NOT_FOUND_URL")"
  if is_image_repository_url "$EXTENSIONS_REPOSITORY_URL"
  then
    return
  fi
  NOT_FOUND_URL_PATH="${NOT_FOUND_URL#*://}"
  NOT_FOUND_URL_PATH="${NOT_FOUND_URL_PATH%%\?*}"
  EXTENSIONS_REPOSITORY_URL_PATH="${EXTENSIONS_REPOSITORY_URL%%\?*}"
  EXTENSIONS_REPOSITORY_URL_PATH="${EXTENSIONS_REPOSITORY_URL_PATH#*://}"
  NOT_FOUND_URL_RESOURCE_PATH="${NOT_FOUND_URL_PATH#${EXTENSIONS_REPOSITORY_URL_PATH}}"
  NOT_FOUND_URL_RESOURCE_PATH="${NOT_FOUND_URL_RESOURCE_PATH#/}"
  if [ "$(printf '%s' "$NOT_FOUND_URL_RESOURCE_PATH" | fold -w 1 | grep -c /)" -eq 3 ]
  then
    REPOSITORY="${NOT_FOUND_URL%%$NOT_FOUND_URL_RESOURCE_PATH\?*}"
    PUBLISHER="$(printf '%s' "$NOT_FOUND_URL_RESOURCE_PATH" | cut -d '/' -f 1)"
    BUILD_ARCH="$(printf '%s' "$NOT_FOUND_URL_RESOURCE_PATH" | cut -d '/' -f 2)"
    BUILD_OS="$(printf '%s' "$NOT_FOUND_URL_RESOURCE_PATH" | cut -d '/' -f 3)"
    EXTENSION_PACKAGE="$(printf '%s' "$NOT_FOUND_URL_RESOURCE_PATH" | cut -d '/' -f 4)"
    EXTENSION_PACKAGE="${EXTENSION_PACKAGE%.tar}"
    EXTENSION_ID_JSON="$(printf '%s' "$PUBLISHER/$BUILD_ARCH/$BUILD_OS/$EXTENSION_PACKAGE" | jq -sR .)"
    jq -r '
      .extensions[] | . as $extension
      | .versions[] | . as $version
      | .availableFor | map(. as $availableFor
        | select('"$EXTENSION_ID_JSON"' ==
          $extension.publisher + "/"
            + ($availableFor.arch | if . != null then . else "'"$DEFAULT_BUILD_ARCH"'" end) + "/"
            + ($availableFor.os | if . != null then . else "'"$DEFAULT_BUILD_OS"'" end) + "/"
            + $extension.name + "-" + $version.version + "-"
            + ($availableFor.flavor | if . != null then . else "'"$DEFAULT_FLAVOR"'" end)
            + $availableFor.postgresVersion
            + ($availableFor.build | if . != null then "-build-" + . else "" end))
        | $extension.repository + " "
          + $extension.publisher + " "
          + $extension.name + " "
          + $version.version + " "
          + $availableFor.postgresVersion
          + " " + ($availableFor.flavor | if . != null then . else "'"$DEFAULT_FLAVOR"'" end)
          + " " + ($availableFor.arch | if . != null then . else "'"$DEFAULT_BUILD_ARCH"'" end)
          + " " + ($availableFor.os | if . != null then . else "'"$DEFAULT_BUILD_OS"'" end)
          + " " + ($availableFor.build | if . != null then . else "" end))
      | .[]' \
      index.json
  fi
}

pull_extension() {
  [ -n "$1" ] && [ -n "$2" ] && [ -n "$3" ] && [ -n "$4" ] \
    && [ -n "$5" ] && [ -n "$6" ] && [ -n "$7" ] && [ -n "$8" ] \
    && true || false
  local TO_INSTALL_REPOSITORY="$1"
  local PUBLISHER="$2"
  local NAME="$3"
  local VERSION="$4"
  local POSTGRES_VERSION="$5"
  local FLAVOR="$6"
  local BUILD_ARCH="$7"
  local BUILD_OS="$8"
  local BUILD="$9"

  local REPOSITORY="$TO_INSTALL_REPOSITORY"
  local EXTENSION_PACKAGE
  EXTENSION_PACKAGE="$NAME-$VERSION-$FLAVOR$POSTGRES_VERSION$([ -z "$BUILD" ] || printf '%s' "-build-$BUILD")"
  echo " * Required extension $EXTENSION_PACKAGE"
  EXTENSIONS_REPOSITORY_URL="$(find_repository_base_url "$REPOSITORY")"

  if [ "$ANY_IMAGE_REPOSITORY_URL" != true ] \
    || ! is_image_repository_url "$EXTENSIONS_REPOSITORY_URL"
  then
    download_extension "$EXTENSIONS_REPOSITORY_URL" "$REPOSITORY" "$PUBLISHER" "$BUILD_ARCH" "$BUILD_OS"\
      "$EXTENSION_PACKAGE"
  else
    add_extension_image_to_statefulset "$EXTENSIONS_REPOSITORY_URL" "$REPOSITORY" "$PUBLISHER" "$NAME" \
      "$VERSION" "$POSTGRES_VERSION" "$FLAVOR" "$BUILD_ARCH" "$BUILD_OS" "$BUILD"
  fi
}

find_repository_base_url() {
  [ -n "$1" ] && true || false
  local REPOSITORY="$1"
  local IS_PROXY_URL_AND_SET_HTTP_SCHEME=false

  if is_proxied_repository_url "$REPOSITORY"
  then
    local PROXY_URL
    PROXY_URL="$(get_proxy_from_url "$REPOSITORY")"
    if is_set_http_scheme_url "$PROXY_URL"
    then
      IS_PROXY_URL_AND_SET_HTTP_SCHEME="$(get_set_http_scheme_from_url "$PROXY_URL")"
    fi
  fi
  ! test -f "${INDEX_NAME}-${INDEX}.timestamp" \
    ||  [ "$(date +%s)" -ge "$(( $(cat "${INDEX_NAME}-${INDEX}.timestamp") + CACHE_TIMEOUT ))" ]
  local EXTENSIONS_REPOSITORY_URL
  for EXTENSIONS_REPOSITORY_URL in $(echo "$EXTENSIONS_REPOSITORY_URLS" | tr ',' '\n')
  do
    local TEST_EXTENSIONS_REPOSITORY_URL="$EXTENSIONS_REPOSITORY_URL"
    if [ "$IS_PROXY_URL_AND_SET_HTTP_SCHEME" = true ]
    then
      TEST_EXTENSIONS_REPOSITORY_URL="http://${EXTENSIONS_REPOSITORY_URL#*://}"
    fi
    if printf '%s' "$TEST_EXTENSIONS_REPOSITORY_URL" | grep -q "^${REPOSITORY%%\?*}" \
      || printf '%s' "$REPOSITORY" | grep -q "^${TEST_EXTENSIONS_REPOSITORY_URL%%\?*}"
    then
      printf '%s' "$EXTENSIONS_REPOSITORY_URL"
      return
    fi
  done
  echo "Warning: Could not found repository $REPOSITORY between following repositories:" >&2
  echo >&2
  echo "$EXTENSIONS_REPOSITORY_URLS" | tr ',' '\n' >&2
  echo >&2
  return 1
}

download_extension() {
  [ -n "$1" ] && [ -n "$2" ] && [ -n "$3" ] && [ -n "$4" ] \
    && [ -n "$5" ] && [ -n "$6" ] && true || false
  local EXTENSIONS_REPOSITORY_URL="$1"
  local REPOSITORY="$2"
  local PUBLISHER="$3"
  local BUILD_ARCH="$4"
  local BUILD_OS="$5"
  local EXTENSION_PACKAGE="$6"
  local TEMP="$(shuf -i 0-65535 -n 1)"
  local REPOSITORY_PATH="${REPOSITORY#*://}"

  if ! test -f "$REPOSITORY_PATH/$PUBLISHER/$BUILD_ARCH/$BUILD_OS/$EXTENSION_PACKAGE.tar"
  then
    echo "   + Downloading from $REPOSITORY/$PUBLISHER/$BUILD_ARCH/$BUILD_OS/$EXTENSION_PACKAGE.tar"
    mkdir -p "$REPOSITORY_PATH/$PUBLISHER/$BUILD_ARCH/$BUILD_OS"
    local CURL_EXTRA_OPTS=""
    if is_proxied_repository_url "$EXTENSIONS_REPOSITORY_URL"
    then
      local PROXY_URL
      PROXY_URL="$(get_proxy_from_url "$EXTENSIONS_REPOSITORY_URL")"
      CURL_EXTRA_OPTS="--proxy $PROXY_URL"
    fi
    if is_skip_hostname_verification_repository_url "$EXTENSIONS_REPOSITORY_URL"
    then
      local SKIP_HOSTNAME_VERIFICATION
      SKIP_HOSTNAME_VERIFICATION="$(get_skip_hostname_verification_from_url "$EXTENSIONS_REPOSITORY_URL")"
      if [ "$SKIP_HOSTNAME_VERIFICATION" = true ]
      then
        CURL_EXTRA_OPTS="$CURL_EXTRA_OPTS -k"
      fi
    fi
    curl -f -s -L -I $CURL_EXTRA_OPTS "$REPOSITORY/$PUBLISHER/$BUILD_ARCH/$BUILD_OS/$EXTENSION_PACKAGE.tar" \
      > "$REPOSITORY_PATH/$PUBLISHER/$BUILD_ARCH/$BUILD_OS/$EXTENSION_PACKAGE.headers.$TEMP"
    CONTENT_LENGTH="$(grep -i 'Content-Length' "$REPOSITORY_PATH/$PUBLISHER/$BUILD_ARCH/$BUILD_OS/$EXTENSION_PACKAGE.headers.$TEMP")"
    CONTENT_LENGTH="$(printf '%s' "$CONTENT_LENGTH" | tr -d '[:space:]' | cut -d ':' -f 2)"
    if [ "$CONTENT_LENGTH" -ge 0 ]
    then
      apply_extension_cache_retention_policy "$CONTENT_LENGTH"
    else
      echo "Warning: Can not determine Content-Length for URL $REPOSITORY/$PUBLISHER/$BUILD_ARCH/$BUILD_OS/$EXTENSION_PACKAGE.tar"
    fi
    curl -f -s -L $CURL_EXTRA_OPTS "$REPOSITORY/$PUBLISHER/$BUILD_ARCH/$BUILD_OS/$EXTENSION_PACKAGE.tar" \
      -o "$REPOSITORY_PATH/$PUBLISHER/$BUILD_ARCH/$BUILD_OS/$EXTENSION_PACKAGE.tar.tmp.$TEMP"
    mv "$REPOSITORY_PATH/$PUBLISHER/$BUILD_ARCH/$BUILD_OS/$EXTENSION_PACKAGE.tar.tmp.$TEMP" \
      "$REPOSITORY_PATH/$PUBLISHER/$BUILD_ARCH/$BUILD_OS/$EXTENSION_PACKAGE.tar"
  else
    echo "   . Already downloaded from $REPOSITORY/$PUBLISHER/$BUILD_ARCH/$BUILD_OS/$EXTENSION_PACKAGE.tar"
  fi
}

apply_extension_cache_retention_policy() {
  [ "$1" -ge 0 ] && true || false
  local REQUIRED_BYTES="$1"
  local AVAILABLE_BYTES BLOCK_SIZE REQUIRED_BYTES_WITH_BLOCK_SIZE
  AVAILABLE_BYTES="$(df -B 1 . | tail -n 1 | tr -s '[:space:]' ':' | cut -d : -f 4)"
  BLOCK_SIZE="$(stat -fc '%s' .)"
  REQUIRED_BYTES_WITH_BLOCK_SIZE="$((REQUIRED_BYTES + BLOCK_SIZE))"
  if [ "$AVAILABLE_BYTES" -lt "$REQUIRED_BYTES_WITH_BLOCK_SIZE" ]
  then
    echo "Required $REQUIRED_BYTES_WITH_BLOCK_SIZE bytes but available $AVAILABLE_BYTES, removing files with oldest last access..."
    while true
    do
      if ! ls -R -- * 2>&1 | grep ':$' | sed 's/:$//' \
        | while read -r DIR
          do
            ls -1 "$DIR"
          done \
        | grep -q .
      then
        echo "Warning: Can not free more space on device!"
        break
      fi
      ls -R -- * 2>&1 | grep ':$' | sed 's/:$//' \
        | while read -r DIR
          do
            ls -1 "$DIR" \
              | while read -r FILE
                do
                  ! test -f "$DIR/$FILE" || stat --format '%X:%n' "$DIR/$FILE"
                done
          done \
        | sort -t : -k 1n | grep -v '\.json$' | head -n 1 | cut -d : -f 2 \
        | while read -r FILE
          do
            echo "Removing $FILE of $(stat '%s' "$FILE")"
            rm -f "$FILE"
          done
      AVAILABLE_BYTES="$(df -B 1 . | tail -n 1 | tr '[:space:]' ':' | cut -d : -f 4)"
      if [ "$AVAILABLE_BYTES" -lt "$REQUIRED_BYTES_WITH_BLOCK_SIZE" ]
      then
        break
      fi
    done
    echo "done"
    echo
  fi
}

add_extension_image_to_statefulset() {
  [ -n "$1" ] && [ -n "$2" ] && [ -n "$3" ] && [ -n "$4" ] \
    && [ -n "$5" ] && [ -n "$6" ] && [ -n "$7" ] && [ -n "$8" ] \
    && [ -n "$9" ] && true || false
  local EXTENSIONS_REPOSITORY_URL="$1"
  local REPOSITORY="$2"
  local PUBLISHER="$3"
  local NAME="$4"
  local VERSION="$5"
  local POSTGRES_VERSION="$6"
  local FLAVOR="$7"
  local BUILD_ARCH="$8"
  local BUILD_OS="$9"
  local BUILD="$10"
  local IMAGE_TEMPLATE COMPONENT COMPONENT_VERSION HASH IMAGE_NAME CONTAINER STATEFULSET
  IMAGE_TEMPLATE="$(get_image_template_from_url "$EXTENSIONS_REPOSITORY_URL")"
  COMPONENT="$(jq -r ".extensions[\"$NAME\"]
    | .component" unwrapped-hashes.json)"
  # shellcheck disable=SC2034
  COMPONENT_VERSION="$(jq -r ".extensions[\"$NAME\"]
    | .versions[] | select(.version == \"$VERSION\")
    | .componentVersion | if . != null then . else \"\" end" unwrapped-hashes.json)"
  if [ -z "$COMPONENT_VERSION" ]
  then
    # TODO: this is a mock that have to be fixed
    # in the extensions repository by adding a
    # .extensions[].versions[].componentVersion field
    # in the hashes.json index
    COMPONENT_VERSION="$VERSION"
    case "$COMPONENT" in
      (core|contrib)
        COMPONENT_VERSION="$COMPONENT"
        ;;
      (citus)
        COMPONENT_VERSION="10.1.2"
        ;;
      (healpix)
        # shellcheck disable=SC2034
        COMPONENT_VERSION="1.0"
        ;;
    esac
  fi
  if [ -z "$COMPONENT_VERSION" ]
  then
    echo "   ! Can not retrieve component version for extension $REPOSITORY $PUBLISHER $NAME $VERSION $FLAVOR$POSTGRES_VERSION $BUILD $BUILD_ARCH $BUILD_OS"
    return 1
  fi
  # shellcheck disable=SC2034
  HASH="$(jq -r ".extensions[\"$NAME\"]
    | .versions[] | select(.version == \"$VERSION\")
    | .availableFor[] | select(.postgresVersion == \"$POSTGRES_VERSION\"
      and (.flavor == \"$FLAVOR\" or (.flavor == null and \"$FLAVOR\" == \"$DEFAULT_FLAVOR\"))
      and (.build == \"$BUILD\" or (.build == null and \"$BUILD\" == \"\")))
    | .buildHash" unwrapped-hashes.json)"
  if [ -z "$HASH" ]
  then
    echo "   ! Can not retrieve hash for extension $REPOSITORY $PUBLISHER $NAME $VERSION $FLAVOR$POSTGRES_VERSION $BUILD $BUILD_ARCH $BUILD_OS"
    return 1
  fi
  # shellcheck disable=SC2034
  POSTGRES_EXACT_VERSION="$(jq -r ".extensions[\"plpgsql\"]
    | .versions[]
    | .availableFor[] | select(.postgresVersion == \"$POSTGRES_VERSION\"
      and (.flavor == \"$FLAVOR\" or (.flavor == null and \"$FLAVOR\" == \"$DEFAULT_FLAVOR\"))
      and (.build == \"$BUILD\" or (.build == null and \"$BUILD\" == \"\")))
    | .postgresVersion" unwrapped-hashes.json)"
  if [ -z "$POSTGRES_EXACT_VERSION" ]
  then
    # TODO: this is a mock that have to be fixed
    # in the extensions repository by adding a
    # .extensions[].versions[].availableFor[].postgresExactVersion field
    # in the hashes.json index
    POSTGRES_EXACT_VERSION="$POSTGRES_VERSION"
    case "$POSTGRES_VERSION" in
      (11)
        POSTGRES_EXACT_VERSION="11.13"
        ;;
      (12)
        POSTGRES_EXACT_VERSION="12.8"
        ;;
      (13)
        # shellcheck disable=SC2034
        POSTGRES_EXACT_VERSION="13.4"
        ;;
    esac
  fi
  if [ -z "$POSTGRES_EXACT_VERSION" ]
  then
    echo "   ! Can not retrieve postgres exact version for extension $REPOSITORY $PUBLISHER $NAME $VERSION $FLAVOR$POSTGRES_VERSION $BUILD $BUILD_ARCH $BUILD_OS"
    return 1
  fi
  IMAGE_NAME="$(eval "echo \"$IMAGE_TEMPLATE\"")"
  if jq '.spec.template.spec.containers|any(.image == "'"$IMAGE_NAME"'")' "$STATEFULSET_JSON_FILE" | grep -q false
  then
    echo "   + Add extension image $IMAGE_NAME"
    CONTAINER="$(get_extension_container_as_yaml "$IMAGE_NAME" "$REPOSITORY" "$PUBLISHER" "$BUILD_ARCH" "$BUILD_OS")"
    (
    [ "$EXTENSIONS_CACHE_LOG_LEVEL" = TRACE ] || set +x
    STATEFULSET="$(jq ".spec.template.spec.containers = .spec.template.spec.containers + [$CONTAINER]" "$STATEFULSET_JSON_FILE")"
    printf '%s' "$STATEFULSET" > "$STATEFULSET_JSON_FILE"
    )
  else
    echo "   . Already added image $IMAGE_NAME"
  fi
}

get_extension_container_as_yaml() {
  [ -n "$1" ] && [ -n "$2" ] && [ -n "$3" ] && [ -n "$4" ] \
    && [ -n "$5" ] && true || false
  local IMAGE_NAME="$1"
  local REPOSITORY="$2"
  local PUBLISHER="$3"
  local BUILD_ARCH="$4"
  local BUILD_OS="$5"
  local REPOSITORY_PATH="${REPOSITORY#*://}"
  cat << EOF
{
  name: "extension-$(jq -r '(.spec.template.spec.containers | length) - 2' "$STATEFULSET_JSON_FILE")",
  image: "$IMAGE_NAME",
  env: [{
    name: "EXTENSIONS_CACHE_LOG_LEVEL",
    value: "$EXTENSIONS_CACHE_LOG_LEVEL"
  }],
  command: [ "sh", "-ec", $(cat << INNER_EOF | jq -sR .
cd /opt/app-root/src
sh /usr/local/bin/extensions-cache-conciliator.sh provide '$REPOSITORY_PATH' '$PUBLISHER' '$BUILD_ARCH' '$BUILD_OS'
INNER_EOF
  ) ],
  volumeMounts: [{
    name: "extensions-cache-config",
    mountPath: "/usr/local/bin/extensions-cache-conciliator.sh",
    subPath: "extensions-cache-conciliator.sh",
    readOnly: true
  }, {
    name: "$PERSISTENTVOLUMECLAIM_NAME",
    subPath: "repository",
    mountPath: "/opt/app-root/src",
    readOnly: false
  }]
}
EOF
}

provide() {
  [ -n "$1" ] && [ -n "$2" ] && [ -n "$3" ] && [ -n "$4" ] \
    && true || false
  local REPOSITORY_PATH="$1"
  local PUBLISHER="$2"
  local BUILD_ARCH="$3"
  local BUILD_OS="$4"

  # TODO: This code have to be refactored when generated images will have real realease
  # packages without -dev. This could be achieved by adding an aditional step to the
  # release process where the packages are extracted from the packaged images and
  # re-packaged in a new tick release packaged image.

  mkdir -p "/opt/app-root/src/$REPOSITORY_PATH/$PUBLISHER/$BUILD_ARCH/$BUILD_OS"
  for BASE_EXTENSION_TAR in "/var/lib/postgresql/extensions/$PUBLISHER/$BUILD_ARCH/$BUILD_OS/"*.tar
  do
    BASE_EXTENSION_TAR_NAME="${BASE_EXTENSION_TAR##*/}"
    for EXTENSION_TAR in "$BASE_EXTENSION_TAR" "${BASE_EXTENSION_TAR%-dev.tar}.tar"
    do
      EXTENSION_TAR_NAME="${EXTENSION_TAR##*/}"
      if [ "${BASE_EXTENSION_TAR_NAME%-dev.tar}.tar" = "$EXTENSION_TAR_NAME" ]
      then
        if [ "$BASE_EXTENSION_TAR_NAME" = "${BASE_EXTENSION_TAR_NAME%-dev.tar}.tar" ]
        then
          continue
        fi
        if ! test -f "$EXTENSION_TAR"
        then
          rm -f "${EXTENSION_TAR%.*}.sha256" "${EXTENSION_TAR%.*}.tgz"
          tar xCf "${BASE_EXTENSION_TAR%/*}" "$BASE_EXTENSION_TAR"
          mv "${BASE_EXTENSION_TAR%.*}".sha256 "${EXTENSION_TAR%.*}".sha256
          mv "${BASE_EXTENSION_TAR%.*}".tgz "${EXTENSION_TAR%.*}".tgz
          tar cCf "${BASE_EXTENSION_TAR%/*}" "$EXTENSION_TAR" \
            "${EXTENSION_TAR_NAME%.*}.sha256" "${EXTENSION_TAR_NAME%.*}.tgz"
        fi
        rm -f "${EXTENSION_TAR%.*}.sha256" "${EXTENSION_TAR%.*}.tgz"
      fi
      rm -f "/opt/app-root/src/$REPOSITORY_PATH/$PUBLISHER/$BUILD_ARCH/$BUILD_OS/$EXTENSION_TAR_NAME.tmp"
      ln -s "/proc/$$/root/$EXTENSION_TAR" \
        "/opt/app-root/src/$REPOSITORY_PATH/$PUBLISHER/$BUILD_ARCH/$BUILD_OS/$EXTENSION_TAR_NAME.tmp"
      mv "/opt/app-root/src/$REPOSITORY_PATH/$PUBLISHER/$BUILD_ARCH/$BUILD_OS/$EXTENSION_TAR_NAME.tmp" \
        "/opt/app-root/src/$REPOSITORY_PATH/$PUBLISHER/$BUILD_ARCH/$BUILD_OS/$EXTENSION_TAR_NAME"
    done
  done
  while true
  do
    sleep 300
  done
}

urlencode() {
  sed 's/\(.\)/\1\n/g' \
    | {
      [ "$EXTENSIONS_CACHE_LOG_LEVEL" = TRACE ] || set +x
      NEWLINE="$(printf '\n')"
      while IFS="$NEWLINE" read -r C
      do
        case "$C" in
        [-_.~a-zA-Z0-9]) printf %c "$C" ;;
        "") printf %%0A ;;
        *) printf %%%02X "'$C'" ;;
        esac
      done
      }
}

urldecode() {
  sed 's/\(.\)/\1\n/g' \
    | {
      [ "$EXTENSIONS_CACHE_LOG_LEVEL" = TRACE ] || set +x
      NEWLINE="$(printf '\n')"
      CODE=
      while IFS="$NEWLINE" read -r C
      do
        case "$C" in
        \+)
          if [ -n "$CODE" ]
          then
            >&2 echo "Wrong code $CODE$C"
            exit 1
          fi
          printf ' '
          ;;
        %)
          if [ -n "$CODE" ]
          then
            >&2 echo "Wrong code $CODE$C"
            exit 1
          fi
          CODE='0x'
          ;;
        *)
          if [ -z "$CODE" ]
          then
            printf %c "$C"
          else
            CODE="$CODE$C"
            if [ -z "${CODE#0x??}" ]
            then
              # shellcheck disable=SC2059
              printf "$(printf '\\%03o' "$CODE")"
              CODE=
            fi
          fi
          ;;
        esac
      done
      }
}

try_function() {
  local E_UNSET=true
  if echo "$-" | grep -q e
  then
    E_UNSET=false
  fi
  "$E_UNSET" || set +e
  (set -e; "$@")
  EXIT_CODE="$?"
  "$E_UNSET" || set -e
  RESULT=false
  if [ "$EXIT_CODE" = 0 ]
  then
    RESULT=true
  fi
}

if [ -n "$1" ]
then
  "$@"
fi

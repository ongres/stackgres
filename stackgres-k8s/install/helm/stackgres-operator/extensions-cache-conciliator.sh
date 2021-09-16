#!/bin/sh

set -e

run () {
  # shellcheck disable=SC2153
  [ -n "$EXTENSIONS_REPOSITORY_URLS" ]
  [ -n "$1" ]
  [ -n "$2" ]

  NAMESPACE="$1"
  STATEFULSET_NAME="$2"
  SERVICE_NAME="${3:-$2}"
  SERVICEACCOUNT_NAME="${4:-$2}"
  PERSISTENTVOLUMECLAIM_NAME="${5:-$2}"
  SERVICEACCOUNT_JSON_FILE=serviceaccount.json
  STATEFULSET_JSON_FILE=statefulset.json

  (
  set +e
  while true; do kubectl get sgcluster -A -o name --watch-only; echo; done &
  while true; do kubectl get sgdistributedlogs -A -o name --watch-only; echo; done &
  ) > custom-resources &
  PREVIOUS_RESOURCES_CHANGES="$(wc -l custom-resources | cut -d ' ' -f 1)"

  while true
  do
    set +e
    (
    set -e

    echo "Updating indexes..."
    pull_indexes
    echo "done"
    echo

    if [ "$ALLOW_PULL_EXTENSIONS_FROM_IMAGE_REPOSITORY" = true ]
    then
      echo "Retrieving serviceaccount $NAMESPACE.$SERVICEACCOUNT_NAME"
      SERVICEACCOUNT="$(kubectl get serviceaccount -n "$NAMESPACE" "$SERVICEACCOUNT_NAME" -o json)"
      printf '%s' "$SERVICEACCOUNT" > "$SERVICEACCOUNT_JSON_FILE"
      echo "done"
      echo

      echo "Updating repositories credentials..."
      update_repositories_credentials
      echo "done"
      echo

      echo "Patching serviceaccount $NAMESPACE.$SERVICEACCOUNT_NAME"
      kubectl patch serviceaccount -n "$NAMESPACE" "$SERVICEACCOUNT_NAME" --type merge -p "$(cat "$SERVICEACCOUNT_JSON_FILE")"
      echo "done"
      echo
    fi

    echo "Retrieving statefulset $NAMESPACE.$STATEFULSET_NAME"
    STATEFULSET="$(kubectl get statefulset -n "$NAMESPACE" "$STATEFULSET_NAME" -o json)"
    printf '%s' "$STATEFULSET" > "$STATEFULSET_JSON_FILE"
    echo "done"
    echo

    echo "Updating extensions..."
    TO_INSTALL_EXTENSIONS="$(get_to_install_extensions)"
    TO_INSTALL_EXTENSIONS="$(echo "$TO_INSTALL_EXTENSIONS" | sort | uniq)"
    echo "$TO_INSTALL_EXTENSIONS" \
      | grep -v '^$' \
      | while read -r TO_INSTALL_REPOSITORY PUBLISHER NAME VERSION POSTGRES_VERSION BUILD_ARCH BUILD_OS BUILD
        do
          pull_extension "$TO_INSTALL_REPOSITORY" "$PUBLISHER" "$NAME" \
            "$VERSION" "$POSTGRES_VERSION" "$BUILD_ARCH" "$BUILD_OS" "$BUILD"
        done
    echo "done"
    echo

    echo "Patching statefulset $NAMESPACE.$STATEFULSET_NAME"
    kubectl patch statefulset -n "$NAMESPACE" "$STATEFULSET_NAME" --type merge -p "$(cat "$STATEFULSET_JSON_FILE")"
    echo "done"
    echo
    )
    EXIT_CODE="$?"
    if [ "$EXIT_CODE" = 0 ]
    then
      echo "Waiting for changes..."
      (
      set +x
      while true
      do
        CURRENT_RESOURCES_CHANGES="$(wc -l custom-resources | cut -d ' ' -f 1)"
        if [ "$CURRENT_RESOURCES_CHANGES" != "$PREVIOUS_RESOURCES_CHANGES" ]
        then
          break
        fi
      done
      )
      PREVIOUS_RESOURCES_CHANGES="$(wc -l custom-resources | cut -d ' ' -f 1)"
      echo "...change happened"
    else
      echo
      echo "...an error occurred during reconciliation, retrying in 10 seconds"
      echo
      sleep 10
    fi
  done
}

pull_indexes() {
  local INDEX_NAME EXTENSIONS_REPOSITORY_URL
  local INDEX=0
  for INDEX_NAME in index hashes
  do
    if ! test -s "unwrapped-${INDEX_NAME}.json"
    then
      echo '{}' > "unwrapped-${INDEX_NAME}.json"
    fi
    INDEX=0
    for EXTENSIONS_REPOSITORY_URL in $(echo "$EXTENSIONS_REPOSITORY_URLS" | tr ',' '\n')
    do
      if test -f "${INDEX_NAME}-${INDEX}.timestamp" \
        && [ "$(date +%s)" -lt "$(( $(cat "${INDEX_NAME}-${INDEX}.timestamp") + 3600 ))" ]
      then
        continue
      fi
      INDEX="$((INDEX + 1))"
      curl -f -s -L -k "${EXTENSIONS_REPOSITORY_URL%\?*}/${INDEX_NAME}.json" > "last-${INDEX_NAME}.json"
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
            value: (.repository = "'"http://$SERVICE_NAME.$NAMESPACE?repository=$(
                printf '%s' "${EXTENSIONS_REPOSITORY_URL%\?*}" | urlencode
              )"'")
          }
          ) | from_entries)
        | . as $last_unwrapped
        | $current_unwrapped * $last_unwrapped
        ' "last-${INDEX_NAME}.json" "unwrapped-${INDEX_NAME}.json" > "new-unwrapped-${INDEX_NAME}.json"
      rm "last-${INDEX_NAME}.json"
      mv "new-unwrapped-${INDEX_NAME}.json" "unwrapped-${INDEX_NAME}.json"
      date +%s > "${INDEX_NAME}-${INDEX}.timestamp"
    done
    jq '.extensions = (.extensions | to_entries | map(.value))
        | .publishers = (.publishers | to_entries | map(.value))
        ' "unwrapped-${INDEX_NAME}.json" > "merged-${INDEX_NAME}.json"
    mv "merged-${INDEX_NAME}.json" "${INDEX_NAME}.json"
  done
}

update_repositories_credentials() {
  local EXTENSIONS_REPOSITORY_URL SERVICEACCOUNT

  local INDEX=0
  for EXTENSIONS_REPOSITORY_URL in $(echo "$EXTENSIONS_REPOSITORY_URLS" | tr ',' '\n')
  do
    if is_image_repository_with_regcred_secret_url "$EXTENSIONS_REPOSITORY_URL"
    then
      REPOSITORY_CREDENTIAL_SECRET="$(get_image_regcred_secret_from_url "$EXTENSIONS_REPOSITORY_URL")"
      if jq '([] + .imagePullSecrets)|any(.name == "'"$REPOSITORY_CREDENTIAL_SECRET"'")' "$SERVICEACCOUNT_JSON_FILE" | grep -q false
      then
        SERVICEACCOUNT="$(jq ".imagePullSecrets = .imagePullSecrets + [{name: \"$REPOSITORY_CREDENTIAL_SECRET\"}]" "$SERVICEACCOUNT_JSON_FILE")"
        printf '%s' "$SERVICEACCOUNT" > "$SERVICEACCOUNT_JSON_FILE"
      fi
    fi
    INDEX="$((INDEX + 1))"
  done
}

get_to_install_extensions() {
  (
  CLUSTER_EXTENSIONS="$(kubectl get sgcluster -A -o json)"
  DISTRIBUTEDLOGS_EXTENSIONS="$(kubectl get sgdistributedlogs -A -o json)"
  printf '%s' "$CLUSTER_EXTENSIONS" | jq '.items[]'
  printf '%s' "$DISTRIBUTEDLOGS_EXTENSIONS" | jq '.items[]'
  ) | jq -r -s '.[]
    | .spec.toInstallPostgresExtensions[] 
    | .repository + " "
        + .publisher + " "
        + .name + " "
        + .version + " "
        + .postgresVersion
        + " " + (.arch | if . != null then . else "x86_64" end)
        + " " + (.os | if . != null then . else "linux" end)
        + " " + (.build | if . != null then . else "" end)
        '
}

pull_extension() {
  [ -n "$1" ] && [ -n "$2" ] && [ -n "$3" ] && [ -n "$4" ] \
    && [ -n "$5" ] && [ -n "$6" ] && [ -n "$7" ]
  local TO_INSTALL_REPOSITORY="$1"
  local PUBLISHER="$2"
  local NAME="$3"
  local VERSION="$4"
  local POSTGRES_VERSION="$5"
  local BUILD_ARCH="$6"
  local BUILD_OS="$7"
  local BUILD="$8"
  local REPOSITORY EXTENSION_PACKAGE
  if is_repository_url "$TO_INSTALL_REPOSITORY"
  then
    REPOSITORY="$(get_repository_from_url "$TO_INSTALL_REPOSITORY")"
  else
    REPOSITORY="$TO_INSTALL_REPOSITORY"
  fi
  EXTENSION_PACKAGE="$NAME-$VERSION-pg$POSTGRES_VERSION$([ -z "$BUILD" ] || echo "-build-$BUILD")"
  echo " * Required extension $EXTENSION_PACKAGE"
  local NOT_FOUND=true
  local INDEX=0
  for EXTENSIONS_REPOSITORY_URL in $(echo "$EXTENSIONS_REPOSITORY_URLS" | tr ',' '\n')
  do
    INDEX="$((INDEX + 1))"
    if printf '%s' "$EXTENSIONS_REPOSITORY_URL" | grep "^$REPOSITORY"
    then
      NOT_FOUND=false
      break
    fi
  done
  if "$NOT_FOUND"
  then
    echo "Warning: Could not found repository $REPOSITORY between following repositories:"
    echo
    echo "$EXTENSIONS_REPOSITORY_URLS" | tr ',' '\n'
    echo
    return
  fi

  if [ "$ALLOW_PULL_EXTENSIONS_FROM_IMAGE_REPOSITORY" != true ] \
    || ! is_image_repository_url "$EXTENSIONS_REPOSITORY_URL"
  then
    download_extension "$REPOSITORY" "$PUBLISHER" "$BUILD_ARCH" "$BUILD_OS" "$EXTENSION_PACKAGE"
  else
    add_extension_image_to_statefulset "$REPOSITORY" "$PUBLISHER" "$NAME" \
      "$VERSION" "$POSTGRES_VERSION" "$BUILD_ARCH" "$BUILD_OS" "$BUILD"
  fi
}

download_extension() {
  [ -n "$1" ] && [ -n "$2" ] && [ -n "$3" ] && [ -n "$4" ] && [ -n "$5" ]
  local REPOSITORY="$1"
  local PUBLISHER="$2"
  local BUILD_ARCH="$3"
  local BUILD_OS="$4"
  local EXTENSION_PACKAGE="$5"

  if ! test -f "$PUBLISHER/$BUILD_ARCH/$BUILD_OS/$EXTENSION_PACKAGE.tar"
  then
    echo "   + Downloading from $REPOSITORY/$PUBLISHER/$BUILD_ARCH/$BUILD_OS/$EXTENSION_PACKAGE.tar"
    mkdir -p "$PUBLISHER/$BUILD_ARCH/$BUILD_OS"
    curl -f -s -L -k -I "$REPOSITORY/$PUBLISHER/$BUILD_ARCH/$BUILD_OS/$EXTENSION_PACKAGE.tar" \
      > "$PUBLISHER/$BUILD_ARCH/$BUILD_OS/$EXTENSION_PACKAGE.headers"
    CONTENT_LENGTH="$(grep -i 'Content-Length' "$PUBLISHER/$BUILD_ARCH/$BUILD_OS/$EXTENSION_PACKAGE.headers")"
    CONTENT_LENGTH="$(printf '%s' "$CONTENT_LENGTH" | tr -d '[:space:]' | cut -d ':' -f 2)"
    if [ "$CONTENT_LENGTH" -ge 0 ]
    then
      apply_extension_cache_retention_policy "$CONTENT_LENGTH"
    else
      echo "Warning: Can not determine Content-Length for URL $REPOSITORY/$PUBLISHER/$BUILD_ARCH/$BUILD_OS/$EXTENSION_PACKAGE.tar"
    fi
    curl -f -s -L -k "$REPOSITORY/$PUBLISHER/$BUILD_ARCH/$BUILD_OS/$EXTENSION_PACKAGE.tar" \
      -o "$PUBLISHER/$BUILD_ARCH/$BUILD_OS/$EXTENSION_PACKAGE.tar.tmp"
    mv "$PUBLISHER/$BUILD_ARCH/$BUILD_OS/$EXTENSION_PACKAGE.tar.tmp" \
      "$PUBLISHER/$BUILD_ARCH/$BUILD_OS/$EXTENSION_PACKAGE.tar"
  else
    echo "   . Already downloaded from $REPOSITORY/$PUBLISHER/$BUILD_ARCH/$BUILD_OS/$EXTENSION_PACKAGE.tar"
  fi
}

apply_extension_cache_retention_policy() {
  [ "$1" -ge 0 ]
  local REQUIRED_BYTES="$1"
  local AVAILABLE_BYTES BLOCK_SIZE REQUIRED_BYTES_WITH_BLOCK_SIZE
  AVAILABLE_BYTES="$(df -B 1 . | tail -n 1 | tr '[:space:]' ':' | cut -d : -f 4)"
  BLOCK_SIZE="$(stat -fc '%s' .)"
  REQUIRED_BYTES_WITH_BLOCK_SIZE="$((REQUIRED_BYTES + BLOCK_SIZE))"
  if [ "$AVAILABLE_BYTES" -lt "$REQUIRED_BYTES_WITH_BLOCK_SIZE" ]
  then
    echo "Required $REQUIRED_BYTES_WITH_BLOCK_SIZE bytes but available $AVAILABLE_BYTES, removing files with oldest last access..."
    while true
    do
      if ! ls */*/*/* >/dev/null 2>&1
      then
        echo "Warning: Can not free more space on device!"
        break
      fi
      for FILE in */*/*/*
      do
        stat --format '%X:%n' "$FILE"
      done \
        | sort -t : -k 1n | head -n 1 | cut -d : -f 1 \
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
    && [ -n "$5" ] && [ -n "$6" ] && [ -n "$7" ]
  local REPOSITORY="$1"
  local PUBLISHER="$2"
  local NAME="$3"
  local VERSION="$4"
  local POSTGRES_VERSION="$5"
  local BUILD_ARCH="$6"
  local BUILD_OS="$7"
  local BUILD="$8"
  local IMAGE_TEMPLATE COMPONENT COMPONENT_VERSION HASH IMAGE_NAME CONTAINER STATEFULSET
  IMAGE_TEMPLATE="$(get_image_template_from_url "$EXTENSIONS_REPOSITORY_URL")"
  COMPONENT="$(jq -r ".extensions[\"$NAME\"]
    | .component" unwrapped-hashes.json)"
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
  # shellcheck disable=SC2034
  HASH="$(jq -r ".extensions[\"$NAME\"]
    | .versions[] | select(.version == \"$VERSION\")
    | .availableFor[] | select(.postgresVersion == \"$POSTGRES_VERSION\" and (.build == \"$BUILD\" or (.build == \"\" and \"$BUILD\" == \"\")))
    | .buildHash" unwrapped-hashes.json)"
  IMAGE_NAME="$(eval "echo \"$IMAGE_TEMPLATE\"")"
  if jq '.spec.template.spec.containers|any(.image == "'"$IMAGE_NAME"'")' "$STATEFULSET_JSON_FILE" | grep -q false
  then
    echo "   + Add extension image $IMAGE_NAME"
    CONTAINER="$(get_extension_container_as_yaml "$IMAGE_NAME" "$PUBLISHER" "$BUILD_ARCH" "$BUILD_OS")"
    STATEFULSET="$(jq ".spec.template.spec.containers = .spec.template.spec.containers + [$CONTAINER]" "$STATEFULSET_JSON_FILE")"
    printf '%s' "$STATEFULSET" > "$STATEFULSET_JSON_FILE"
  else
    echo "   . Already added image $IMAGE_NAME"
  fi
}

get_extension_container_as_yaml() {
  [ -n "$1" ] && [ -n "$2" ] && [ -n "$3" ] && [ -n "$4" ]
  local IMAGE_NAME="$1"
  local PUBLISHER="$2"
  local BUILD_ARCH="$3"
  local BUILD_OS="$4"
  cat << EOF
{
  name: "extension-$(jq -r '(.spec.template.spec.containers | length) - 2' "$STATEFULSET_JSON_FILE")",
  image: "$IMAGE_NAME",
  imagePullPolicy: "IfNotPresent",
  securityContext: {
    runAsUser: 1000,
    runAsGroup: 1000
  },
  command: [ "sh", "-exc",
    $(cat << INNER_EOF | jq -sR .
# TODO: This code have to be refactored when generated images will have real realease
# packages without -dev. This could be achieved by adding an aditional step to the
# release process where the packages are extracted from the packaged images and
# re-packaged in a new tick release packaged image.

mkdir -p '/opt/app-root/src/$PUBLISHER/$BUILD_ARCH/$BUILD_OS'
for BASE_EXTENSION_TAR in "/var/lib/postgresql/extensions/$PUBLISHER/$BUILD_ARCH/$BUILD_OS/"*.tar
do
  BASE_EXTENSION_TAR_NAME="\$\${BASE_EXTENSION_TAR##*/}"
  for EXTENSION_TAR in "\$\$BASE_EXTENSION_TAR" "\$\${BASE_EXTENSION_TAR%-dev.tar}.tar"
  do
    EXTENSION_TAR_NAME="\$\${EXTENSION_TAR##*/}"
    if [ "\$\${BASE_EXTENSION_TAR_NAME%-dev.tar}.tar" = "\$\$EXTENSION_TAR_NAME" ]
    then
      if [ "\$\$BASE_EXTENSION_TAR_NAME" = "\$\${BASE_EXTENSION_TAR_NAME%-dev.tar}.tar" ]
      then
        continue
      fi
      if ! test -f "\$\$EXTENSION_TAR"
      then
        rm -f "\$\${EXTENSION_TAR%.*}.sha256" "\$\${EXTENSION_TAR%.*}.tgz"
        tar xCf "\$\${BASE_EXTENSION_TAR%/*}" "\$\$BASE_EXTENSION_TAR"
        mv "\$\${BASE_EXTENSION_TAR%.*}".sha256 "\$\${EXTENSION_TAR%.*}".sha256
        mv "\$\${BASE_EXTENSION_TAR%.*}".tgz "\$\${EXTENSION_TAR%.*}".tgz
        tar cCf "\$\${BASE_EXTENSION_TAR%/*}" "\$\$EXTENSION_TAR" \
          "\$\${EXTENSION_TAR_NAME%.*}.sha256" "\$\${EXTENSION_TAR_NAME%.*}.tgz"
      fi
      rm -f "\$\${EXTENSION_TAR%.*}.sha256" "\$\${EXTENSION_TAR%.*}.tgz"
    fi
    rm -f "/opt/app-root/src/$PUBLISHER/$BUILD_ARCH/$BUILD_OS/\$\$EXTENSION_TAR_NAME.tmp"
    ln -s "/proc/\$\$\$\$/root/\$\$EXTENSION_TAR" \
      "/opt/app-root/src/$PUBLISHER/$BUILD_ARCH/$BUILD_OS/\$\$EXTENSION_TAR_NAME.tmp"
    mv "/opt/app-root/src/$PUBLISHER/$BUILD_ARCH/$BUILD_OS/\$\$EXTENSION_TAR_NAME.tmp" \
      "/opt/app-root/src/$PUBLISHER/$BUILD_ARCH/$BUILD_OS/\$\$EXTENSION_TAR_NAME"
  done
done
while true; do sleep 300; done
INNER_EOF
    ) ],
  volumeMounts: [{
    name: "$PERSISTENTVOLUMECLAIM_NAME",
    subPath: "repository",
    mountPath: "/opt/app-root/src",
    readOnly: false
  }]
}
EOF
}

is_image_repository_url() {
  printf '%s' "$1" | grep -q "[?&]imageTemplate="
}

is_image_repository_with_regcred_secret_url() {
  printf '%s' "$1" | grep "[?&]imageTemplate=" | grep -q "[?&]imageRegcredSecret="
}

is_repository_url() {
  printf '%s' "$1" | grep -q "[?&]repository="
}

get_image_template_from_url() {
  local IMAGE_TEMPLATE
  IMAGE_TEMPLATE="$(printf '%s' "$1" | grep "[?&]imageTemplate=")"
  IMAGE_TEMPLATE="$(printf '%s' "$IMAGE_TEMPLATE" \
    | sed 's/^.*[?&]imageTemplate=\([^?&]\+\)\([?&].*\)\?$/\1/')"
  IMAGE_TEMPLATE="$(printf '%s' "$IMAGE_TEMPLATE" | urldecode)"
  printf '%s' "$IMAGE_TEMPLATE"
}

get_image_regcred_secret_from_url() {
  local REPOSITORY_CREDENTIAL_SECRET
  REPOSITORY_CREDENTIAL_SECRET="$(printf '%s' "$1" | grep "[?&]imageRegcredSecret=")"
  REPOSITORY_CREDENTIAL_SECRET="$(printf '%s' "$REPOSITORY_CREDENTIAL_SECRET" \
    | sed 's/^.*[?&]imageRegcredSecret=\([^?&]\+\)\([?&].*\)\?$/\1/')"
  REPOSITORY_CREDENTIAL_SECRET="$(printf '%s' "$IMAGE_TEMPLATE" | urldecode)"
  printf '%s' "$REPOSITORY_CREDENTIAL_SECRET"
}

get_repository_from_url() {
  local REPOSITORY
  REPOSITORY="$(printf '%s' "$1" | grep "[?&]repository=")"
  REPOSITORY="$(printf '%s' "$REPOSITORY" | sed 's/^.*[?&]repository=\([^?&]\+\)\([?&].*\)\?$/\1/')"
  REPOSITORY="$(printf '%s' "$REPOSITORY" | urldecode)"
  printf '%s' "$REPOSITORY"
}

urlencode() (
  sed 's/\(.\)/\1\n/g' \
    | {
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
)

urldecode() {
  sed 's/\(.\)/\1\n/g' \
    | {
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

if [ -n "$1" ]
then
  "$@"
fi
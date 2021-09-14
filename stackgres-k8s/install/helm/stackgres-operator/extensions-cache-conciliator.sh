#!/bin/sh

set -e

run () {
  [ -n "$EXTENSIONS_REPOSITORY_URLS" ]
  [ -n "$1" ]
  [ -n "$2" ]

  STATEFULSET_NAMESPACE="$1"
  STATEFULSET_NAME="$2"

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
              value: (.repository = "'"http://$STATEFULSET_NAME.$STATEFULSET_NAMESPACE?repository=$(
                  echo "${EXTENSIONS_REPOSITORY_URL%\?*}" | urlencode
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
    echo "done"
    echo

    echo "Retrieving serviceaccount $STATEFULSET_NAMESPACE.$STATEFULSET_NAME"
    SERVICEACCOUNT="$(kubectl get serviceaccount -n "$STATEFULSET_NAMESPACE" "$STATEFULSET_NAME" -o json)"
    printf '%s' "$SERVICEACCOUNT" > serviceaccount.json
    echo "done"
    echo

    echo "Updating repository credentials..."
    INDEX=0
    for EXTENSIONS_REPOSITORY_URL in $(echo "$EXTENSIONS_REPOSITORY_URLS" | tr ',' '\n')
    do
      if printf '%s' "$EXTENSIONS_REPOSITORY_URL" | grep "[?&]imageTemplate=" | grep -q "[?&]imageRegcredSecret="
      then
        REPOSITORY_CREDENTIAL_SECRET="$(printf '%s' "$EXTENSIONS_REPOSITORY_URL" | grep "[?&]imageTemplate=" | grep "[?&]imageRegcredSecret=")"
        REPOSITORY_CREDENTIAL_SECRET="$(printf '%s' "$REPOSITORY_CREDENTIAL_SECRET" | sed 's/^.*[?&]imageRegcredSecret=\([^?&]\+\)\([?&].*\)\?$/\1/')"
        REPOSITORY_CREDENTIAL_SECRET="$(printf '%s' "$REPOSITORY_CREDENTIAL_SECRET" | urldecode)"
        if jq '([] + .imagePullSecrets)|any(.name == "'"$REPOSITORY_CREDENTIAL_SECRET"'")' serviceaccount.json | grep -q false
        then
          SERVICEACCOUNT="$(jq ".imagePullSecrets = .imagePullSecrets + [{name: \"$REPOSITORY_CREDENTIAL_SECRET\"}]" serviceaccount.json)"
          printf '%s' "$SERVICEACCOUNT" > serviceaccount.json
        fi
      fi
      INDEX="$((INDEX + 1))"
    done
    echo "done"
    echo

    echo "Patching serviceaccount $STATEFULSET_NAMESPACE.$STATEFULSET_NAME"
    kubectl patch serviceaccount -n "$STATEFULSET_NAMESPACE" "$STATEFULSET_NAME" --type merge -p "$(cat serviceaccount.json)"
    echo "done"
    echo

    echo "Retrieving statefulset $STATEFULSET_NAMESPACE.$STATEFULSET_NAME"
    STATEFULSET="$(kubectl get statefulset -n "$STATEFULSET_NAMESPACE" "$STATEFULSET_NAME" -o json)"
    printf '%s' "$STATEFULSET" > statefulset.json
    echo "done"
    echo

    echo "Updating extensions..."
    TO_INSTALL_EXTENSIONS="$(
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
            + " " + (if .build != null then .build else "" end)'
    )"
    TO_INSTALL_EXTENSIONS="$(echo "$TO_INSTALL_EXTENSIONS" | sort | uniq)"
    echo "$TO_INSTALL_EXTENSIONS" \
      | grep -v '^$' \
      | while read -r TO_INSTALL_REPOSITORY PUBLISHER NAME VERSION POSTGRES_VERSION BUILD
        do
          if printf '%s' "$TO_INSTALL_REPOSITORY" | grep -q "[?&]repository="
          then
            REPOSITORY="$(printf '%s' "$TO_INSTALL_REPOSITORY" | grep "[?&]repository=")"
            REPOSITORY="$(printf '%s' "$REPOSITORY" | sed 's/^.*[?&]repository=\([^?&]\+\)\([?&].*\)\?$/\1/')"
            REPOSITORY="$(printf '%s' "$REPOSITORY" | urldecode)"
          else
            REPOSITORY="$TO_INSTALL_REPOSITORY"
          fi
          BUILD_SUFFIX="$([ -z "$BUILD" ] || echo "-build-$BUILD")"
          EXTENSION_PACKAGE="$NAME-$VERSION-pg$POSTGRES_VERSION$BUILD_SUFFIX"
          echo " * Required extension $EXTENSION_PACKAGE"
          INDEX=0
          for EXTENSIONS_REPOSITORY_URL in $(echo "$EXTENSIONS_REPOSITORY_URLS" | tr ',' '\n')
          do
            if printf '%s' "$EXTENSIONS_REPOSITORY_URL" \
              | grep "^$REPOSITORY" | grep -q "[?&]imageTemplate="
            then
              IMAGE_TEMPLATE="$(printf '%s' "$EXTENSIONS_REPOSITORY_URL" | grep "[?&]imageTemplate=")"
              IMAGE_TEMPLATE="$(printf '%s' "$IMAGE_TEMPLATE" | sed 's/^.*[?&]imageTemplate=\([^?&]\+\)\([?&].*\)\?$/\1/')"
              IMAGE_TEMPLATE="$(printf '%s' "$IMAGE_TEMPLATE" | urldecode)"
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
                  COMPONENT_VERSION="1.0"
                  ;;
              esac
              HASH="$(jq -r ".extensions[\"$NAME\"]
                | .versions[] | select(.version == \"$VERSION\")
                | .availableFor[] | select(.postgresVersion == \"$POSTGRES_VERSION\" and (.build == \"$BUILD\" or (.build == \"\" and \"$BUILD\" == \"\")))
                | .buildHash" unwrapped-hashes.json)"
              IMAGE_NAME="$(eval "echo \"$IMAGE_TEMPLATE\"")"
              if jq '.spec.template.spec.containers|any(.image == "'"$IMAGE_NAME"'")' statefulset.json | grep -q false
              then
                echo "   + Add extension image $IMAGE_NAME"
                CONTAINER="$(cat << EOF
{
  name: "extension-$(jq -r '(.spec.template.spec.containers | length) - 2' statefulset.json)",
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

mkdir -p '/opt/app-root/src/$PUBLISHER/x86_64/linux'
for BASE_EXTENSION_TAR in "/var/lib/postgresql/extensions/$PUBLISHER/x86_64/linux/"*.tar
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
    rm -f "/opt/app-root/src/$PUBLISHER/x86_64/linux/\$\$EXTENSION_TAR_NAME.tmp"
    ln -s "/proc/\$\$\$\$/root/\$\$EXTENSION_TAR" \
      "/opt/app-root/src/$PUBLISHER/x86_64/linux/\$\$EXTENSION_TAR_NAME.tmp"
    mv "/opt/app-root/src/$PUBLISHER/x86_64/linux/\$\$EXTENSION_TAR_NAME.tmp" \
      "/opt/app-root/src/$PUBLISHER/x86_64/linux/\$\$EXTENSION_TAR_NAME"
  done
done
while true; do sleep 300; done
INNER_EOF
    ) ],
  volumeMounts: [{
    name: "$STATEFULSET_NAME",
    subPath: "repository",
    mountPath: "/opt/app-root/src",
    readOnly: false
  }]
}
EOF
                )"
                STATEFULSET="$(jq ".spec.template.spec.containers = .spec.template.spec.containers + [$CONTAINER]" statefulset.json)"
                printf '%s' "$STATEFULSET" > statefulset.json
              else
                echo "   . Already added image $IMAGE_NAME"
              fi
            else
              if ! test -f "$PUBLISHER/x86_64/linux/$EXTENSION_PACKAGE.tar"
              then
                echo "   + Downloading from $REPOSITORY/$PUBLISHER/x86_64/linux/$EXTENSION_PACKAGE.tar"
                mkdir -p "$PUBLISHER/x86_64/linux"
                curl -f -s -L -k "$REPOSITORY/$PUBLISHER/x86_64/linux/$EXTENSION_PACKAGE.tar" \
                  -o "$PUBLISHER/x86_64/linux/$EXTENSION_PACKAGE.tar.tmp"
                mv "$PUBLISHER/x86_64/linux/$EXTENSION_PACKAGE.tar.tmp" "$PUBLISHER/x86_64/linux/$EXTENSION_PACKAGE.tar"
              else
                echo "   . Already downloaded from $REPOSITORY/$PUBLISHER/x86_64/linux/$EXTENSION_PACKAGE.tar"
              fi
            fi
            INDEX="$((INDEX + 1))"
          done
        done
    echo "done"
    echo

    echo "Patching statefulset $STATEFULSET_NAMESPACE.$STATEFULSET_NAME"
    kubectl patch statefulset -n "$STATEFULSET_NAMESPACE" "$STATEFULSET_NAME" --type merge -p "$(cat statefulset.json)"
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

# urlencode and urldecode credits to https://gist.github.com/1480c1/455c0ec47cd5fd0514231ba865f0fca0

urlencode() (
    local STRING LINES LINENO CURRLINE POS CHARS C
    STRING="${*:-$(
        cat -
        printf x
    )}"
    [ -n "$*" ] || STRING="${STRING%x}"
    # Zero index, + 1 to start from 1 since sed starts from 1
    LINES=$(($(printf %s "$STRING" | wc -l) + 1))
    LINENO=1
    while [ "$LINENO" -le "$LINES" ]; do
        CURRLINE="$(printf %s "$STRING" | sed "${LINENO}q;d")"
        POS=1
        CHARS="$(printf %s "$CURRLINE" | wc -c)"
        while [ "$POS" -le "$CHARS" ]; do
            C="$(printf %s "$CURRLINE" | cut -b$POS)"
            case "$C" in
            [-_.~a-zA-Z0-9]) printf %c "$C" ;;
            *) printf %%%02X "'${C:-\n}'" ;;
            esac
            POS=$((POS + 1))
        done
        [ "$LINENO" -eq "$LINES" ] || printf %%0A
        LINENO="$((LINENO + 1))"
    done
)

urldecode() {
    local STRING LINES LINENO CURRLINE POS CHARS C
    STRING=${*:-$(
        cat -
        printf x
    )}
    [ -n "$*" ] || STRING="${STRING%x}"
    # Zero index, + 1 to start from 1 since sed starts from 1
    LINES=$(($(printf %s "$STRING" | wc -l) + 1))
    LINENO=1
    while [ "$LINENO" -le "$LINES" ]; do
        CURRLINE="$(printf %s "$STRING" | sed "${LINENO}q;d")"
        POS=1
        CHARS=$(printf %s "$CURRLINE" | wc -c)
        while [ "$POS" -le "$CHARS" ]; do
            C="$(printf %s "$CURRLINE" | cut -b$POS)"
            case "$C" in
            \+) printf ' ' ;;
            %)
                # shellcheck disable=SC2059
                printf "$(
                    printf '\\%03o' "$(
                        printf %s "$CURRLINE" | cut -b$POS-$((POS + 2)) | sed s/%/0x/
                    )"
                )"
                POS="$((POS + 2))"
                ;;
            *) printf %c "$C" ;;
            esac
            POS=$((POS + 1))
        done
        [ "$LINENO" -eq "$LINES" ] || printf '\n'
        LINENO="$((LINENO + 1))"
    done
}

run "$@"

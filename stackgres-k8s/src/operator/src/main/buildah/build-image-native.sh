#!/bin/sh

set -e

OPERATOR_IMAGE_NAME="${OPERATOR_IMAGE_NAME:-"stackgres/operator:development"}"
CONTAINER_BASE=$(buildah from "registry.access.redhat.com/ubi8-minimal:8.3-291")
TARGET_OPERATOR_IMAGE_NAME="${TARGET_OPERATOR_IMAGE_NAME:-docker-daemon:$OPERATOR_IMAGE_NAME}"

APP_OPTS="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"

# Include binaries
buildah config --workingdir='/app/' "$CONTAINER_BASE"
buildah run "$CONTAINER_BASE" -- sh -c "$(cat << 'EOF'
  echo 'jboss:x:1000:' >> /etc/group && \
  echo 'jboss:!::' >> /etc/gshadow && \
  echo 'jboss:x:1000:1000::/app:/bin/bash' >> /etc/passwd && \
  echo 'jboss:!!:18655:0:99999:7:::' >> /etc/shadow && \
  echo 'jboss:100000:65536' >> /etc/subgid
EOF
  )"
buildah copy --chown jboss:jboss "$CONTAINER_BASE" 'operator/target/stackgres-operator-runner' '/app/stackgres-operator'
buildah run "$CONTAINER_BASE" -- chmod 775 '/app'

## Run our server and expose the port
buildah config --cmd "./stackgres-operator $APP_OPTS" "$CONTAINER_BASE"
buildah config --user jboss:jboss "$CONTAINER_BASE"
buildah config --env 'HOME=/app' "$CONTAINER_BASE"
buildah config --port 8080 "$CONTAINER_BASE"
buildah config --port 8443 "$CONTAINER_BASE"
buildah config --env LANG="C.UTF-8" --env LC_ALL="C.UTF-8" "$CONTAINER_BASE"

## Commit this container to an image name
buildah commit "$CONTAINER_BASE" "$OPERATOR_IMAGE_NAME"
buildah push -f "${BUILDAH_PUSH_FORMAT:-docker}" "$OPERATOR_IMAGE_NAME" "$TARGET_OPERATOR_IMAGE_NAME"
buildah delete "$CONTAINER_BASE"

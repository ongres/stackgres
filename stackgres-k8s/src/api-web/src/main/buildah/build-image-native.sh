#!/bin/sh

set -e

RESTAPI_IMAGE_NAME="${RESTAPI_IMAGE_NAME:-"stackgres/restapi:development"}"
CONTAINER_BASE=$(buildah from "registry.access.redhat.com/ubi8-minimal:8.0")
TARGET_RESTAPI_IMAGE_NAME="${TARGET_RESTAPI_IMAGE_NAME:-docker-daemon:$RESTAPI_IMAGE_NAME}"

APP_OPTS="-Dquarkus.http.host=0.0.0.0 -Dquarkus.http.port=8080 -Dquarkus.http.ssl-port=8443 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"

# Maintainance tasks
buildah run --net "host" "$CONTAINER_BASE" -- microdnf update -y
buildah run --net "host" "$CONTAINER_BASE" -- microdnf install -y shadow-utils
buildah run "$CONTAINER_BASE" -- useradd stackgres
buildah run --net "host" "$CONTAINER_BASE" -- microdnf remove -y shadow-utils libsemanage
buildah run "$CONTAINER_BASE" -- microdnf clean all

# Include binaries
buildah config --workingdir='/app/' "$CONTAINER_BASE"
buildah copy --chown stackgres:stackgres "$CONTAINER_BASE" 'api-web/target/stackgres-restapi-runner' '/app/stackgres-restapi'
buildah copy "$CONTAINER_BASE" 'api-web/target/libsunec.so' '/app/libsunec.so'
buildah copy "$CONTAINER_BASE" 'api-web/target/cacerts' '/app/cacerts'
buildah run "$CONTAINER_BASE" -- chmod 775 '/app'

## Run our server and expose the port
buildah config --cmd "./stackgres-restapi $APP_OPTS" "$CONTAINER_BASE"
buildah config --port 8080 "$CONTAINER_BASE"
buildah config --user stackgres:stackgres "$CONTAINER_BASE"

## Commit this container to an image name
buildah commit --squash "$CONTAINER_BASE" "$RESTAPI_IMAGE_NAME"
buildah push -f "${BUILDAH_PUSH_FORMAT:-docker}" "$RESTAPI_IMAGE_NAME" "TARGET_RESTAPI_IMAGE_NAME"

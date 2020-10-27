#!/bin/sh

set -e

JOBS_IMAGE_NAME="${JOBS_IMAGE_NAME:-"stackgres/jobs:development"}"
CONTAINER_BASE=$(buildah from "registry.access.redhat.com/ubi8-minimal:8.2")
TARGET_JOBS_IMAGE_NAME="${TARGET_JOBS_IMAGE_NAME:-docker-daemon:$JOBS_IMAGE_NAME}"

APP_OPTS="-Djava.util.logging.manager=org.jboss.logmanager.LogManager"

# Include binaries
buildah config --workingdir='/app/' "$CONTAINER_BASE"
buildah copy --chown nobody:nobody "$CONTAINER_BASE" 'jobs/target/stackgres-jobs-runner' '/app/stackgres-jobs'
buildah run "$CONTAINER_BASE" -- chmod 775 '/app'

## Run our server and expose the port
buildah config --cmd "./stackgres-jobs $APP_OPTS" "$CONTAINER_BASE"
buildah config --port 8080 "$CONTAINER_BASE"
buildah config --port 8443 "$CONTAINER_BASE"
buildah config --user nobody:nobody "$CONTAINER_BASE"
buildah config --env LANG="C.UTF-8" --env LC_ALL="C.UTF-8" "$CONTAINER_BASE"

## Commit this container to an image name
buildah commit "$CONTAINER_BASE" "$JOBS_IMAGE_NAME"
buildah push -f "${BUILDAH_PUSH_FORMAT:-docker}" "$JOBS_IMAGE_NAME" "$TARGET_JOBS_IMAGE_NAME"
buildah delete "$CONTAINER_BASE"

#!/bin/sh

set -e
ADMINUI_IMAGE_NAME="${ADMINUI_IMAGE_NAME:-"stackgres/admin-ui:${IMAGE_TAG%-jvm}"}"
CONTAINER_BASE=$(buildah from "registry.access.redhat.com/ubi8/nginx-118:1-18.1614609200")
TARGET_ADMINUI_IMAGE_NAME="${TARGET_ADMINUI_IMAGE_NAME:-docker-daemon:$ADMINUI_IMAGE_NAME}"

buildah config --user root:root "$CONTAINER_BASE"

# Copying admin static resources to ngnix
buildah copy --chown nginx:nginx "$CONTAINER_BASE" 'admin-ui/target/public' '/opt/app-root/src/admin'

#Expose port and default user
buildah config --port 8080 "$CONTAINER_BASE"
buildah config --user nginx:nginx "$CONTAINER_BASE"

# Commit this container to an image name
buildah commit "$CONTAINER_BASE" "$ADMINUI_IMAGE_NAME"
buildah push -f "${BUILDAH_PUSH_FORMAT:-docker}" "$ADMINUI_IMAGE_NAME" "$TARGET_ADMINUI_IMAGE_NAME"
buildah delete "$CONTAINER_BASE"

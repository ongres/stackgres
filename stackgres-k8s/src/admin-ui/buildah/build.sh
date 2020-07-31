#!/bin/sh

set -e
ADMINUI_IMAGE_NAME="${ADMINUI_IMAGE_NAME:-"stackgres/admin-ui:${IMAGE_TAG%-jvm}"}"
CONTAINER_BASE=$(buildah from "nginx:1.18.0-alpine")
TARGET_ADMINUI_IMAGE_NAME="${TARGET_ADMINUI_IMAGE_NAME:-docker-daemon:$ADMINUI_IMAGE_NAME}"

#Overriding default listen from port 80 to port 8080
buildah run "$CONTAINER_BASE" sed 's/listen       80;/listen       8080;/' -i /etc/nginx/conf.d/default.conf 
buildah run "$CONTAINER_BASE" sed 's/listen  \[::\]:80;/listen  [::]:8080;/' -i /etc/nginx/conf.d/default.conf 

# Copying admin static resources to ngnix
buildah copy --chown nginx:nginx "$CONTAINER_BASE" 'admin-ui/target/public' '/usr/share/nginx/html/admin'

#Expose port and default user
buildah config --port 80 "$CONTAINER_BASE"
buildah config --user nginx:nginx "$CONTAINER_BASE"

# Commit this container to an image name
buildah commit --squash  "$CONTAINER_BASE" "$ADMINUI_IMAGE_NAME"
buildah push -f "${BUILDAH_PUSH_FORMAT:-docker}" "$ADMINUI_IMAGE_NAME" "$TARGET_ADMINUI_IMAGE_NAME"

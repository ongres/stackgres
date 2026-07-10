#!/bin/sh

set -eu

sh "$TEMPLATES_PATH/setup-arbitrary-user.sh"
sh "$TEMPLATES_PATH/relocate-binaries.sh"

# Pre-create the subPath directories declared for the custom persistent volumes so that they
# are owned by the postgres user. If left to kubelet, missing subPath directories are created
# owned by root with permission 0755 making them unwritable by the containers.
for CUSTOM_PERSISTENT_VOLUME_SUB_PATH in ${CUSTOM_PERSISTENT_VOLUME_SUB_PATHS:-}
do
  mkdir -p "$CUSTOM_PERSISTENT_VOLUME_SUB_PATH"
done

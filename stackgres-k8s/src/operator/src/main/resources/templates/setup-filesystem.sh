#!/bin/sh

set -eu

sh "$TEMPLATES_PATH/setup-arbitrary-user.sh"
sh "$TEMPLATES_PATH/relocate-binaries.sh"

#!/bin/sh

set -e

mkdir -p "$PG_DATA_PATH"
chmod -R 700 "$PG_DATA_PATH"

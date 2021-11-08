#!/bin/sh

# shellcheck disable=SC1090
. "$(dirname "$0")/build-functions.sh"

set -e

# shellcheck disable=SC2015
[ -n "$CI_JOB_ID" ] && [ -n "$CI_PROJECT_ID" ] \
  && [ -n "$CI_REGISTRY" ] && [ -n "$CI_REGISTRY_USER" ] && [ -n "$CI_REGISTRY_PASSWORD" ] \
  && true || false

TEMP_DIR="/tmp/$CI_PROJECT_ID"
mkdir -p "$TEMP_DIR"

echo "Copying project files ..."

cp -r . "$TEMP_DIR/stackgres-build-$CI_JOB_ID"

echo "done"

set +e

(
set -e
cd "$TEMP_DIR/stackgres-build-$CI_JOB_ID"

echo

if [ "$1" = build ]
then
  shift

  TO_COPY_FILES=""
  TO_EXTRACT_MODULE_FILES=""
  # shellcheck disable=SC2166
  while [ "x$1" = x--copy ] || [ "x$1" = x--extract ]
  do
    TO_COPY_FILE="$2"
    if [ "x$1" = x--extract ]
    then
      TO_EXTRACT_MODULE_FILES="$TO_EXTRACT_MODULE_FILES $2"
      TO_COPY_FILE="${TO_COPY_FILE#*:}"
    fi
    TO_COPY_FILES="$TO_COPY_FILES $TO_COPY_FILE"
    shift 2
  done

  echo "Building $* ..."

  docker login -u "$CI_REGISTRY_USER" -p "$CI_REGISTRY_PASSWORD" "$CI_REGISTRY"
  sh stackgres-k8s/ci/build/build.sh "$@"

  echo "done"

  if [ -n "$TO_EXTRACT_MODULE_FILES" ]
  then
    echo
    echo "Extracting files ..."

    for MODULE_FILE in $TO_EXTRACT_MODULE_FILES
    do
      sh stackgres-k8s/ci/build/build-functions.sh extract "${MODULE_FILE%:*}" "${MODULE_FILE#*:}"
    done

    echo "done"
  fi

  if [ -n "$TO_COPY_FILES" ]
  then
    echo
    echo "Copying extracted files ..."

    for FILE in $TO_COPY_FILES
    do
      if [ -d "$FILE" ]
      then
        mkdir -p "$CI_PROJECT_DIR/$FILE"
        cp -rf "$FILE/." "$CI_PROJECT_DIR/$FILE"
      elif [ -e "$FILE" ]
      then
        mkdir -p "$CI_PROJECT_DIR/${FILE%/*}"
        cp -f "$FILE" "$CI_PROJECT_DIR/$FILE"
      fi
    done

    echo "done"
  fi
elif [ "$1" = extract ]
then
  shift

  echo "Extracting files from $2 ..."

  sh stackgres-k8s/ci/build/build-functions.sh generate_image_hashes
  sh stackgres-k8s/ci/build/build-functions.sh extract "$@"

  # shellcheck disable=SC2048
  for FILE in $*
  do
    if [ -d "$FILE" ]
    then
      mkdir -p "$CI_PROJECT_DIR/$FILE"
      cp -rf "$FILE/." "$CI_PROJECT_DIR/$FILE"
    elif [ -e "$FILE" ]
    then
      mkdir -p "$CI_PROJECT_DIR/${FILE%/*}"
      cp -f "$FILE" "$CI_PROJECT_DIR/$FILE"
    fi
  done

  echo "done"
fi

echo

)
EXIT_CODE="$?"

echo "Cleaning up ..."

rm -rf "$TEMP_DIR/stackgres-build-$CI_JOB_ID"

echo "done"

exit "$EXIT_CODE"

#!/bin/sh

# shellcheck disable=SC1090
. "$(dirname "$0")/build-functions.sh"

set +e

(
set -e

echo

if [ "$1" = build ]
then
  shift

  TO_EXTRACT_MODULE_FILES=""
  # shellcheck disable=SC2166
  while [ "x$1" = x--extract ]
  do
    TO_EXTRACT_MODULE_FILES="$TO_EXTRACT_MODULE_FILES $2"
    shift 2
  done

  echo "Building $* ..."

  if [ "x$DOCKER_AUTH_CONFIG" != x ]
  then
    mkdir -p $HOME/.docker
    cat "$DOCKER_AUTH_CONFIG" > "$HOME/.docker/config.json"
  fi
  if [ "x$CI_REGISTRY" != x ]
  then
    echo | docker login "$CI_REGISTRY" || \
      docker login -u "$CI_REGISTRY_USER" -p "$CI_REGISTRY_PASSWORD" "$CI_REGISTRY"
  fi
  set +e
  sh stackgres-k8s/ci/build/build.sh "$@"
  EXIT_CODE="$?"

  if [ "$EXIT_CODE" = 0 ]
  then
    echo "done"
  else
    echo "failed"
  fi

  if [ "$EXIT_CODE" = 0 ] && [ -n "$TO_EXTRACT_MODULE_FILES" ]
  then
    echo
    echo "Extracting files ..."

    for MODULE_FILE in $TO_EXTRACT_MODULE_FILES
    do
      sh stackgres-k8s/ci/build/build-functions.sh extract "${MODULE_FILE%:*}" "${MODULE_FILE#*:}"
    done

    echo "done"
  fi

  exit "$EXIT_CODE"
elif [ "$1" = extract ]
then
  shift

  echo "Extracting files from $2 ..."

  if [ "x$DOCKER_AUTH_CONFIG" != x ]
  then
    mkdir -p $HOME/.docker
    cat "$DOCKER_AUTH_CONFIG" > "$HOME/.docker/config.json"
  fi
  if [ "x$CI_REGISTRY" != x ]
  then
    echo | docker login "$CI_REGISTRY" || \
      docker login -u "$CI_REGISTRY_USER" -p "$CI_REGISTRY_PASSWORD" "$CI_REGISTRY"
  fi
  sh stackgres-k8s/ci/build/build-functions.sh generate_image_hashes
  sh stackgres-k8s/ci/build/build-functions.sh extract "$@"

  echo "done"
fi

echo

)
EXIT_CODE="$?"

exit "$EXIT_CODE"

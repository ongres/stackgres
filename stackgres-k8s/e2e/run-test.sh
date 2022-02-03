#!/bin/sh

. "${0%/*}/e2e"

echo "Utils loaded:"
e2e_list_utils | while read UTIL_PATH
  do
    echo " - $UTIL_PATH"
  done

echo "Preparing environment"

echo "Setup versions"
setup_versions
echo "Setup k8s"
setup_k8s
echo "Setup images"
setup_images
echo "Setup cache"
setup_cache
echo "Setup helm"
setup_helm
echo "Setup logs"
setup_logs
echo "Setup operator"
setup_operator

echo "Functional tests results" > "$TARGET_PATH/logs/results.log"

if [ -z "$1" ]
then
  >&2 echo "Must specify a test to run"
  exit 1
fi

SPEC_TO_RUN="${1##*spec/}"

if [ ! -f "$SPEC_PATH/$SPEC_TO_RUN" ]
then
  if [ ! -f "$SPEC_PATH/$E2E_ENV/$SPEC_TO_RUN" ]
  then    
    >&2 echo "Spec $SPEC_PATH/$SPEC_TO_RUN not found"
    exit 1
  else 
    try_function spec "$SPEC_PATH/$E2E_ENV/$SPEC_TO_RUN"

    if [ "$K8S_DELETE" = true ]
    then
      delete_k8s || true
    fi

    if "$RESULT"
    then
      cat "$TARGET_PATH/logs/results.log"
    else
      cat "$TARGET_PATH/logs/results.log"
      exit "$EXIT_CODE"
    fi
  fi
else
  try_function spec "$SPEC_PATH/$SPEC_TO_RUN"

  if [ "$K8S_DELETE" = true ]
  then
    delete_k8s || true
  fi

  if "$RESULT"
  then
    cat "$TARGET_PATH/logs/results.log"
  else
    cat "$TARGET_PATH/logs/results.log"
    exit "$EXIT_CODE"
  fi
fi


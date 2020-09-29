#!/bin/sh

. "$(dirname "$0")/e2e"
echo "Preparing environment"

echo "StackGres version used is $STACKGRES_VERSION"
echo "* StackGres operator image used is $STACKGRES_OPERATOR_IMAGE"
echo "* StackGres restapi image used is $STACKGRES_RESTAPI_IMAGE"
echo "* StackGres admin-ui image used is $STACKGRES_ADMINUI_IMAGE"
echo "Previous StackGres version used is $STACKGRES_PREVIOUS_VERSION"
echo "* Previous StackGres operator image used is $STACKGRES_PREVIOUS_OPERATOR_IMAGE"
echo "* Previous StackGres restapi image used is $STACKGRES_PREVIOUS_RESTAPI_IMAGE"
echo "* Previous StackGres admin-ui image used is $STACKGRES_PREVIOUS_ADMINUI_IMAGE"

setup_images
setup_k8s
setup_cache
setup_helm
setup_operator
setup_logs

echo "Functional tests results" > "$TARGET_PATH/logs/results.log"

if [ -z "$1" ]
then
  >&2 echo "Must specify a test to run"
  exit 1
fi

SPEC_TO_RUN=$(basename "$1")

if [ ! -f "$SPEC_PATH/$SPEC_TO_RUN" ]
then
  if [ ! -f "$SPEC_PATH/$E2E_ENV/$SPEC_TO_RUN" ]
  then    
    >&2 echo "Spec $SPEC_PATH/$SPEC_TO_RUN not found"
    exit 1
  else 
    try_function spec "$SPEC_PATH/$E2E_ENV/$SPEC_TO_RUN"
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
  if "$RESULT"
  then
    cat "$TARGET_PATH/logs/results.log"
  else
    cat "$TARGET_PATH/logs/results.log"
    exit "$EXIT_CODE"
  fi
fi


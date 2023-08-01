#!/bin/sh

set -e

shopt -s expand_aliases 2> /dev/null || true

TEST_SHELL_PATH="$(dirname "$0")"
PROJECT_PATH="$TEST_SHELL_PATH/../../.."
TARGET_PATH="$PROJECT_PATH/target/shell"
SHELL_XTRACE=$(! echo $- | grep -q x || echo " -x")

test -f "$PROJECT_PATH/pom.xml"
mkdir -p "$TARGET_PATH"

TEST_IMAGE_NAMES="$(cat "$TEST_SHELL_PATH/test-images" | tr '\n' ' ')"

run_in_all_containers() {
  (
  set +e
  OK_IMAGE_NAMES=""
  FAIL_IMAGE_NAMES=""
  FAIL=true
  for INDEX in $(seq 1 "$(echo "$TEST_IMAGE_NAMES" | wc -w)")
  do
    local IMAGE_NAME="$(echo "$TEST_IMAGE_NAMES" | tr ' ' '\n' | tail -n +$INDEX | head -n 1)"
    echo
    echo "Run using image $IMAGE_NAME started..."
    echo
    (set -e; run_in_container "$INDEX" "$@")
    local EXIT_CODE="$?"
    if [ "$EXIT_CODE" = 0 ]
    then
      OK_IMAGE_NAMES="$OK_IMAGE_NAMES $IMAGE_NAME"
    else
      FAIL=false
      FAIL_IMAGE_NAMES="$FAIL_IMAGE_NAMES $IMAGE_NAME"
    fi
    if [ -f "$TARGET_PATH/shell-unit-tests-junit-report.xml" ]
    then
      sed 's#<testsuite name="\([^"]\+\)" #<testsuite name="\1 using '"$IMAGE_NAME"'" #' "$TARGET_PATH/shell-unit-tests-junit-report.xml" \
        | sed 's#<testcase classname="\([^"]\+\)" #<testcase classname="\1 using '"$IMAGE_NAME"'" #' \
        > "$TARGET_PATH/shell-unit-tests-junit-report-$INDEX.xml"
    fi
    echo
    echo "Run using image $IMAGE_NAME completed"
    echo
  done

  echo
  echo "Results:"
  echo
  echo "$OK_IMAGE_NAMES" | tr ' ' '\n' | grep -v "^$" | sed 's/^\(.*\)$/OK: \1/'
  echo "$FAIL_IMAGE_NAMES" | tr ' ' '\n' | grep -v "^$" | sed 's/^\(.*\)$/FAIL: \1/'
  echo

  "$FAIL"
  )
}

run_in_container() {
  local IMAGE_NAME="$(echo "$TEST_IMAGE_NAMES" | tr ' ' '\n' | tail -n +$1 | head -n 1)"
  shift
  docker run --rm \
    $([ -z "$SHELL_TEST_TIMEOUT" ] || printf '%s %s' --stop-timeout "$SHELL_TEST_TIMEOUT") \
    -v /etc/passwd:/etc/passwd:ro -v /etc/group:/etc/group:ro \
    -v /etc/shadow:/etc/shadow:ro -v /etc/gshadow:/etc/gshadow:ro \
    -u "$(id -u):$(id -g)" \
    $(id -G | tr ' ' '\n' | sed 's/^\(.*\)$/--group-add \1/') \
    -v "$HOME":"$HOME":rw -e PROMPT_COMMAND= \
    -v /var/run/docker.sock:/var/run/docker.sock \
    -v "$(realpath "$(pwd)/$PROJECT_PATH"):/project" -w /project \
    -e IMAGE_NAME="$IMAGE_NAME" \
    --entrypoint /bin/sh \
    "$IMAGE_NAME" -c "sh $SHELL_XTRACE src/test/shell/shell-unit-tests.sh $*"
}

if [ "$#" = 0 ]
then
  run_in_all_containers all
elif [ "$#" = 1 ]
then
  run_in_container "$1" all
else
  run_in_container "$@"
fi

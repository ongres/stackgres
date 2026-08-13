#!/bin/sh

set -e

shopt -s expand_aliases 2> /dev/null || true

TEST_SHELL_PATH="$(dirname "$0")"
PROJECT_PATH="$TEST_SHELL_PATH/../../.."
TARGET_PATH="$PROJECT_PATH/target/shell"
SHELL_XTRACE=$(! echo $- | grep -q x || echo " -x")

# Container engine used to run the tests, as a command prefix. It defaults to
# docker, see CONTAINER_ENGINE in stackgres-k8s/ci/build.
CONTAINER_ENGINE="${CONTAINER_ENGINE:-docker}"

container_engine_is_podman() {
  case "${CONTAINER_ENGINE%% *}" in
    podman|*/podman) return 0 ;;
  esac
  return 1
}

container_engine_socket_path() {
  local SOCKET_PATH
  if ! container_engine_is_podman
  then
    if [ -S /var/run/docker.sock ]
    then
      printf %s /var/run/docker.sock
    fi
    return
  fi
  for SOCKET_PATH in "${XDG_RUNTIME_DIR:-/run/user/$(id -u)}/podman/podman.sock" \
    /run/podman/podman.sock
  do
    if [ -S "$SOCKET_PATH" ] && [ -w "$SOCKET_PATH" ]
    then
      printf %s "$SOCKET_PATH"
      return
    fi
  done
}

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
  local SOCKET_PATH="$(container_engine_socket_path)"
  # The id has to be given: the test images run as a user of their own, that the
  # /etc/passwd of the host mounted below hides, and the engine fails to resolve
  # it with `unable to find user stackgres: no matching entries in passwd file`.
  # A rootless podman maps the root of the container to the invoking user and
  # every other id to an unrelated subordinate one, so the root of the container
  # is the only id that can read and write the mounted project.
  local RUN_AS_USER="$(id -u):$(id -g)"
  if container_engine_is_podman
  then
    RUN_AS_USER=0:0
  fi
  $CONTAINER_ENGINE run --rm \
    $([ -z "$SHELL_TEST_TIMEOUT" ] || printf '%s %s' --stop-timeout "$SHELL_TEST_TIMEOUT") \
    -v /etc/passwd:/etc/passwd:ro -v /etc/group:/etc/group:ro \
    -v /etc/shadow:/etc/shadow:ro -v /etc/gshadow:/etc/gshadow:ro \
    -u "$RUN_AS_USER" \
    $(container_engine_is_podman || id -G | tr ' ' '\n' | sed 's/^\(.*\)$/--group-add \1/') \
    -v "$HOME":"$HOME":rw -e PROMPT_COMMAND= \
    $([ -z "$SOCKET_PATH" ] || printf '%s %s' -v "$SOCKET_PATH:/var/run/docker.sock") \
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

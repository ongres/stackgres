#!/bin/sh

set -e

IMAGE_NAMES="busybox:1.31.1 bitnami/kubectl:1.19.2 ongres/patroni:v1.6.5-pg12.4-build-5.1"

set -e

shopt -s expand_aliases 2> /dev/null || true

TEST_SHELL_PATH="$(dirname "$0")"
PROJECT_PATH="$TEST_SHELL_PATH/../../.."
TARGET_PATH="$PROJECT_PATH/target/shell"
SHELL_XTRACE=$(! echo $- | grep -q x || echo " -x")

test -f "$PROJECT_PATH/pom.xml"
rm -rf "$TARGET_PATH"
mkdir -p "$TARGET_PATH"

run_in_all_containers() {
  (
  set +e
  OK_IMAGE_NAMES=""
  FAIL_IMAGE_NAMES=""
  FAIL=false
  for INDEX in $(seq 1 "$(echo "$IMAGE_NAMES" | wc -w)")
  do
    local IMAGE_NAME="$(echo "$IMAGE_NAMES" | tr ' ' '\n' | tail -n +$INDEX | head -n 1)"
    echo
    echo "Run using image $IMAGE_NAME started..."
    echo
    (set -e; run_in_container "$INDEX" "$@")
    local EXIT_CODE="$?"
    if [ "$EXIT_CODE" = 0 ]
    then
      OK_IMAGE_NAMES="$OK_IMAGE_NAMES $IMAGE_NAME"
    else
      FAIL=true
      FAIL_IMAGE_NAMES="$FAIL_IMAGE_NAMES $IMAGE_NAME"
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
  local IMAGE_NAME="$(echo "$IMAGE_NAMES" | tr ' ' '\n' | tail -n +$1 | head -n 1)"
  shift
  docker run -ti --rm \
    -v /etc/passwd:/etc/passwd:ro -v /etc/group:/etc/group:ro \
    -v /etc/shadow:/etc/shadow:ro -v /etc/gshadow:/etc/gshadow:ro \
    -u "$(id -u):$(id -g)" \
    $(id -G | tr ' ' '\n' | sed 's/^\(.*\)$/--group-add \1/') \
    -v "$HOME":"$HOME":rw -e PROMPT_COMMAND= \
    -v /var/run/docker.sock:/var/run/docker.sock -v "$(realpath "$(pwd)/$PROJECT_PATH"):/project" -w /project \
    --entrypoint /bin/sh \
    "$IMAGE_NAME" -c "sh $SHELL_XTRACE src/test/shell/test-shell.sh $*"
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

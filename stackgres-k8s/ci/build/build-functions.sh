#!/bin/sh
# shellcheck disable=SC2039
# shellcheck disable=SC2016

# Enable POSIX sh compatibility when running under zsh.
# This script uses only POSIX shell features (plus local variables) and relies on:
#   SH_WORD_SPLIT  - unquoted $VAR undergoes word splitting (for x in $LIST)
#   NO_NOMATCH     - unmatched globs expand to themselves instead of erroring
#   NO_BANG_HIST   - disable ! history expansion (used in .dockerignore: !path)
#   POSIX_BUILTINS - POSIX-compliant builtin behavior
# shellcheck disable=SC2034
if [ -n "$ZSH_VERSION" ]; then
  emulate sh
  setopt SH_WORD_SPLIT NO_NOMATCH NO_BANG_HIST POSIX_BUILTINS
fi

BUILDER_VERSION=1.0.0

set -e

# Command used to build and run the containers. It is used as a command prefix,
# so values with arguments like `podman --remote` are supported. It defaults to
# docker in order to not change the behaviour of the existing runners.
export CONTAINER_ENGINE="${CONTAINER_ENGINE:-docker}"

container_engine_is_podman() {
  case "${CONTAINER_ENGINE%% *}" in
    podman|*/podman) return 0 ;;
  esac
  return 1
}

# The build containers are run as the user that owns the docker socket so that
# the files they create belong to the invoking user.
#
# podman is daemonless and has no socket. A rootless podman maps the root of the
# container to the invoking user and every other id of the container to an
# unrelated subordinate id, so the root of the container is the only id that can
# read and write the mounted project. A rootful podman runs as root anyway.
if [ -z "$BUILD_UID" ]
then
  if container_engine_is_podman
  then
    BUILD_UID=0:0
  else
    BUILD_UID="$(id -u):$(ls -n /var/run/docker.sock | cut -d ' ' -f 4)"
  fi
fi
export BUILD_UID
export DOCKER_CLI_HINTS=false
# podman reads the registries credentials from the file pointed by
# REGISTRY_AUTH_FILE. Point it to the file docker uses so that both engines and
# the code that reads it directly (see list_image_tags) are interchangeable.
export REGISTRY_AUTH_FILE="${REGISTRY_AUTH_FILE:-$HOME/.docker/config.json}"

[ "$DEBUG" != true ] || set -x

die() {
  printf '\n\t%s\n\n' "$1" >&2
  exit 1
}

export LANG=C.UTF-8
export LC_ALL=C.UTF-8

cd "$(dirname "$0")/../../.." || die "Can not change to directory $(dirname "$0")/../../.."

module_image_name() {
  [ "$#" -ge 2 ] || false
  local MODULE="$1"
  local SOURCE_IMAGE_NAME="$2"
  local MODULE_PLATFORM="$3"
  local MODULE_FILTERED_SOURCES
  local MODULE_SOURCES
  local MODULE_PLATFORM_DEPENDENT
  local MODULE_DOCKERFILE
  local MODULE_HASH
  MODULE_SOURCES="$(module_list "$MODULE" sources)"
  MODULE_PLATFORM_DEPENDENT="$(jq -r ".modules[\"$MODULE\"].platform_dependent | . != null and ." stackgres-k8s/ci/build/target/config.json)"
  MODULE_DOCKERFILE="$(jq -r ".modules[\"$MODULE\"].dockerfile.path" stackgres-k8s/ci/build/target/config.json)"
  MODULE_ARTIFACTS="$(module_list "$MODULE" artifacts)"
  {
    echo "${BUILDER_VERSION%.*}"
    echo "$SOURCE_IMAGE_NAME"
    jq -r ".modules[\"$MODULE\"]" stackgres-k8s/ci/build/target/config.json
    eval "cat << EOF
$(jq -r ".modules[\"$MODULE\"].build_env | if . != null then . else {} end
          | to_entries
          | sort_by(.key)
          | map(\"export \" + .key + \"=\\\"\" + .value + \"\\\"\")
          | .[]" stackgres-k8s/ci/build/target/config.json)
EOF
      "
    if [ "$MODULE_DOCKERFILE" != null ]
    then
      path_hash "$MODULE_DOCKERFILE"
      for MODULE_ARTIFACT in $MODULE_ARTIFACTS
      do
        path_hash "$MODULE_ARTIFACT" 2>/dev/null || true
      done
    fi
    eval "set -e; $(
        jq -r ".modules[\"$MODULE\"].filtered_sources | if . == null then [] else . end | .[]" stackgres-k8s/ci/build/target/config.json \
          | while read -r HASH_COMMAND
            do
              cat << EOF
$HASH_COMMAND
EOF
              done
      )"
    for MODULE_SOURCE in $MODULE_SOURCES
    do
      path_hash "$MODULE_SOURCE"
    done
  } > "stackgres-k8s/ci/build/target/$MODULE-hash"
  MODULE_HASH="$(md5sum "stackgres-k8s/ci/build/target/$MODULE-hash" | cut -d ' ' -f 1)"
  local BUILD_REPOSITORY
  BUILD_REPOSITORY="$(jq -r '.build_repository' stackgres-k8s/ci/build/target/config.json)"
  if "$MODULE_PLATFORM_DEPENDENT"
  then
    MODULE_PLATFORM="${MODULE_PLATFORM:-$(get_platform)}"
    TAG_MODULE_PLATFORM="$(printf %s "$MODULE_PLATFORM" | tr '/' '-')"
    if [ "$BUILD_IMAGE_PER_MODULE" = true ]
    then
      printf '%s/%s:hash-%s-%s\n' \
        "$BUILD_REPOSITORY" "$MODULE" "$MODULE_HASH" "$TAG_MODULE_PLATFORM"
    else
      printf '%s:%s-hash-%s-%s\n' \
        "$BUILD_REPOSITORY" "$MODULE" "$MODULE_HASH" "$TAG_MODULE_PLATFORM"
    fi
  else
    if [ "$BUILD_IMAGE_PER_MODULE" = true ]
    then
      printf '%s/%s:hash-%s\n' \
        "$BUILD_REPOSITORY" "$MODULE" "$MODULE_HASH"
    else
      printf '%s:%s-hash-%s\n' \
        "$BUILD_REPOSITORY" "$MODULE" "$MODULE_HASH"
    fi
  fi
}

copy_from_image() {
  [ "$#" -ge 1 ] || false
  local SOURCE_IMAGE_NAME="$1"
  local SOURCE_IMAGE_PLATFORM
  local CONTAINER_ID
  local DEST="${PROJECT_PATH:-$(pwd)}"
  if [ "$SOURCE_IMAGE_NAME" = null ]
  then
    return
  fi
  SOURCE_IMAGE_PLATFORM="$(get_image_platform "$SOURCE_IMAGE_NAME")"
  # Files are copied with `docker create` + `docker cp` instead of running the
  # image, so this never executes a binary from the image. This makes it work
  # regardless of the host architecture (e.g. copying from an arm64 image on an
  # amd64 runner) without requiring qemu/binfmt emulation. `docker cp` (without
  # --archive) writes the files owned by the invoking user, matching the
  # previous `--user $(id -u):$(id -g)` behaviour.
  # shellcheck disable=SC2046
  CONTAINER_ID="$(docker_create --platform "$SOURCE_IMAGE_PLATFORM" \
    $([ "$SKIP_REMOTE_MANIFEST" = true ] || printf %s '--pull always') \
    "$SOURCE_IMAGE_NAME")"
  mkdir -p "$DEST"
  docker_cp "$CONTAINER_ID:/project/." "$DEST"
  docker_rm -fv "$CONTAINER_ID" >/dev/null
}

pre_build_in_container() {
  [ "$#" -ge 2 ] || false
  local MODULE="$1"
  local BUILD_IMAGE_NAME="$2"
  local COMMANDS COMMAND_BUILD_UID
  if [ "$BUILD_SKIP_PRE_BUILD" = true ]
  then
    return
  fi
  COMMANDS="$(jq -r ".modules[\"$MODULE\"].pre_build_commands | if . != null then . | join(\"\n\") else true end" stackgres-k8s/ci/build/target/config.json)"
  COMMAND_BUILD_UID="$(jq -r ".modules[\"$MODULE\"].pre_post_build_uid | if . != null then . else \"\" end" stackgres-k8s/ci/build/target/config.json)"
  run_commands_in_container "$MODULE" "$BUILD_IMAGE_NAME" "${COMMAND_BUILD_UID:-$BUILD_UID}" "$COMMANDS"
}

build_in_container() {
  [ "$#" -ge 2 ] || false
  local MODULE="$1"
  local BUILD_IMAGE_NAME="$2"
  local COMMANDS
  if [ "$BUILD_SKIP_BUILD" = true ]
  then
    return
  fi
  COMMANDS="$(jq -r ".modules[\"$MODULE\"].build_commands | if . != null then . | join(\"\n\") else true end" stackgres-k8s/ci/build/target/config.json)"
  run_commands_in_container "$MODULE" "$BUILD_IMAGE_NAME" "$BUILD_UID" "$COMMANDS"
}

post_build_in_container() {
  [ "$#" -ge 2 ] || false
  local MODULE="$1"
  local BUILD_IMAGE_NAME="$2"
  local COMMANDS COMMAND_BUILD_UID
  if [ "$BUILD_SKIP_POST_BUILD" = true ]
  then
    return
  fi
  COMMANDS="$(jq -r ".modules[\"$MODULE\"].post_build_commands | if . != null then . | join(\"\n\") else true end" stackgres-k8s/ci/build/target/config.json)"
  COMMAND_BUILD_UID="$(jq -r ".modules[\"$MODULE\"].pre_post_build_uid | if . != null then . else \"\" end" stackgres-k8s/ci/build/target/config.json)"
  run_commands_in_container "$MODULE" "$BUILD_IMAGE_NAME" "${COMMAND_BUILD_UID:-$BUILD_UID}" "$COMMANDS"
}

run_commands_in_container() {
  [ "$#" -ge 4 ] || false
  local MODULE="$1"
  local BUILD_IMAGE_NAME="$2"
  local BUILD_UID="$3"
  local COMMANDS="$4"
  local MODULE_PATH
  if [ "$COMMANDS" = true ]
  then
    return
  fi
  MODULE_PATH="$(jq -r ".modules[\"$MODULE\"].path" stackgres-k8s/ci/build/target/config.json)"
  eval "cat << EOF
$(
    jq -r ".modules[\"$MODULE\"].build_env | if . != null then . else {} end
          | to_entries
          | sort_by(.key)
          | map(\"export \" + .key + \"=\\\"\" + .value + \"\\\"\")
          | .[]" stackgres-k8s/ci/build/target/config.json)
EOF
   "  > "stackgres-k8s/ci/build/target/$MODULE-build-env"
  container_engine_serve_socket
  local EXIT_CODE=0
  # shellcheck disable=SC2046
  docker_run -i $(! test -t 1 || printf %s '-t') --rm \
    --platform "$(docker_platform "${BUILD_PLATFORM:-$(get_platform)}")" \
    $([ "$SKIP_REMOTE_MANIFEST" = true ] || printf %s '--pull always') \
    $(container_engine_socket_volume) \
    $(container_engine_testcontainers_env) \
    --volume "${PROJECT_PATH:-$(pwd)}:/project" \
    --workdir /project \
    --user "$BUILD_UID" \
    --env HOME=/tmp \
    --env "PRE_BUILD_COMMANDS=$PRE_BUILD_COMMANDS" \
    --env "BUILD_COMMANDS=$BUILD_COMMANDS" \
    --env "POST_BUILD_COMMANDS=$POST_BUILD_COMMANDS" \
    --env "MODULE_PATH=$MODULE_PATH" \
    --env "SHELL_XTRACE=$([ "$DEBUG" != true ] || printf %s -x)" \
    --entrypoint /bin/sh \
    "$BUILD_IMAGE_NAME" \
    -ec $(echo "$-" | grep -v -q x || printf %s '-x') "
      $(cat "stackgres-k8s/ci/build/target/$MODULE-build-env")
      $COMMANDS" || EXIT_CODE="$?"
  container_engine_stop_socket
  return "$EXIT_CODE"
}

build_module_image() {
  [ "$#" -ge 3 ] || false
  local MODULE="$1"
  local SOURCE_IMAGE_NAME="$2"
  local IMAGE_NAME="$3"
  local BUILD_IMAGE_NAME
  local TARGET_IMAGE_NAME
  local MODULE_PATH
  local MODULE_ARTIFACTS
  local MODULE_DOCKERFILE
  BUILD_IMAGE_NAME="$(jq -r ".modules[\"$MODULE\"].build_image" stackgres-k8s/ci/build/target/config.json)"
  TARGET_IMAGE_NAME="$(jq -r ".modules[\"$MODULE\"].target_image" stackgres-k8s/ci/build/target/config.json)"
  if [ "$TARGET_IMAGE_NAME" = null ]
  then
    TARGET_IMAGE_NAME="$SOURCE_IMAGE_NAME"
  fi
  copy_from_image "$SOURCE_IMAGE_NAME"
  if [ "$BUILD_IMAGE_NAME" != null ]
  then
    pre_build_in_container "$MODULE" "$BUILD_IMAGE_NAME"
    if [ "$?" != 0 ]
    then
      >&2 echo "Pre build step failed for $MODULE"
      return 1
    fi
    build_in_container "$MODULE" "$BUILD_IMAGE_NAME"
    if [ "$?" != 0 ]
    then
      >&2 echo "Build failed for $MODULE"
      return 1
    fi
    post_build_in_container "$MODULE" "$BUILD_IMAGE_NAME"
    if [ "$?" != 0 ]
    then
      >&2 echo "Post build step failed for $MODULE"
      return 1
    fi
  fi
  MODULE_PATH="$(jq -r ".modules[\"$MODULE\"].path" stackgres-k8s/ci/build/target/config.json)"
  MODULE_DOCKERFILE="$(jq -r ".modules[\"$MODULE\"].dockerfile | if . != null then .path else null end" stackgres-k8s/ci/build/target/config.json)"
  MODULE_ARTIFACTS="$(module_list "$MODULE" artifacts)"
  (
  echo '*'
  for MODULE_ARTIFACT in $MODULE_ARTIFACTS
  do
    echo "!$MODULE_ARTIFACT"
  done
  ) > .dockerignore
  if [ "$MODULE_DOCKERFILE" != null ]
  then
  (
    jq -r ".modules[\"$MODULE\"].dockerfile.args
          | if . != null then . else {} end | to_entries
          | map(.key + \" \" + .value + \"\")[]" stackgres-k8s/ci/build/target/config.json \
        | while read -r KEY VALUE
          do
            printf 'ARG %s\n' "$(eval 'cat << EOF
$KEY
EOF')"
          done
    eval "cat '$MODULE_DOCKERFILE'$(
        jq -r ".modules[\"$MODULE\"].dockerfile.seds | if . == null then [] else . end | .[]" stackgres-k8s/ci/build/target/config.json \
          | while read -r SED_EXPRESSION
            do
              cat << EOF | tr -d '\n'
 | sed "$SED_EXPRESSION"
EOF
              done
      )"
    ) > "stackgres-k8s/ci/build/target/Dockerfile.$MODULE"
  else
  (
    cat << 'EOF'
ARG TARGET_IMAGE_NAME

FROM "$TARGET_IMAGE_NAME" as target
  ARG BUILD_UID
  USER $BUILD_UID
  WORKDIR /project
EOF
    for MODULE_ARTIFACT in $MODULE_ARTIFACTS
    do
      echo "  COPY ./$MODULE_ARTIFACT /project/$MODULE_ARTIFACT"
    done
    ) > "stackgres-k8s/ci/build/target/Dockerfile.$MODULE"
  fi
  # shellcheck disable=SC2086
  # shellcheck disable=SC2046
  docker_build $DOCKER_BUILD_OPTS -t "$IMAGE_NAME" \
    $([ "$SKIP_REMOTE_MANIFEST" = true ] || printf %s '--pull') \
    --platform "$(docker_platform "${BUILD_PLATFORM:-$(get_platform)}")" \
    --build-arg "BUILD_UID=${BUILD_UID%:*}" \
    --build-arg "TARGET_IMAGE_NAME=$TARGET_IMAGE_NAME" \
    $(jq -r ".modules[\"$MODULE\"].dockerfile.args
        | if . != null then . else {} end | to_entries
        | map(.key + \" \" + .value + \"\")[]" stackgres-k8s/ci/build/target/config.json \
      | while read -r KEY VALUE
        do
          printf ' %s %s=%s' '--build-arg' "$KEY" "$(eval "printf %s \"$VALUE\"")"
        done) \
    -f "stackgres-k8s/ci/build/target/Dockerfile.$MODULE" .
}

module_list() {
  [ "$#" -ge 2 ] || false
  local MODULE="$1"
  local MODULE_FILES_PATH="$2"
  local MODULE_FILES
  MODULE_FILES="$(
    jq -r ".modules[\"$MODULE\"][\"$MODULE_FILES_PATH\"] | if . != null then if (.|type) == \"array\" then . else (to_entries | map(.value)) end else [] end | sort | .[]" stackgres-k8s/ci/build/target/config.json
    )"
  printf %s "$MODULE_FILES"
}

init_hash() {
  (
  set +e
  (
  set -e
  if [ -d stackgres-k8s/ci/build/target/.git ]
  then
    if ! git --git-dir stackgres-k8s/ci/build/target/.git reset > /dev/null
    then
      rm -rf stackgres-k8s/ci/build/target/.git
    fi
  fi
  if ! [ -d stackgres-k8s/ci/build/target/.git ]
  then
    if [ -d "$PROJECT_PATH"/.git ]
    then
      tar cf - -C "$PROJECT_PATH" .git | tar xf - -C stackgres-k8s/ci/build/target
    else
      git --git-dir stackgres-k8s/ci/build/target/.git -c init.defaultBranch=main init > /dev/null
    fi
  fi
  if git --git-dir stackgres-k8s/ci/build/target/.git status --porcelain 2>&1 | grep -q .
  then
    git --git-dir stackgres-k8s/ci/build/target/.git add . > /dev/null
    git --git-dir stackgres-k8s/ci/build/target/.git \
      -c user.name=ci -c user.email= commit -q -m "build hash" --no-gpg-sign > /dev/null
  fi
  )
  EXIT_CODE="$?"
  if [ "$EXIT_CODE" != 0 ]
  then
    rm -rf stackgres-k8s/ci/build/target/.git
    exit "$EXIT_CODE"
  fi
  )
}

module_type() {
  [ "$#" -ge 1 ] || false
  local MODULE="$1"
  local MODULE_TYPE
  MODULE_TYPE="$(jq -r ".modules | select(has(\"$MODULE\"))[\"$MODULE\"] | select(has(\"type\")).type" stackgres-k8s/ci/build/target/config.json)"
  [ -n "$MODULE_TYPE" ] || die "Module $MODULE is not defined or has no type in stackgres-k8s/ci/build/config.yml" 1
  printf %s "$MODULE_TYPE"
}

source_image_name() {
  [ "$#" -ge 1 ] || false
  local MODULE="$1"
  local MODULE_PLATFORM="$2"
  local SOURCE_MODULE
  local SOURCE_IMAGE_NAME
  SOURCE_MODULE="$(jq -r ".stages[] | select(has(\"$MODULE\"))[\"$MODULE\"]" stackgres-k8s/ci/build/target/config.json)"
  [ -n "$SOURCE_MODULE" ] || die "Module $MODULE has no stage defined in stackgres-k8s/ci/build/config.yml" 1
  if [ "$SOURCE_MODULE" = null ]
  then
    SOURCE_IMAGE_NAME="$(jq -r ".target_image" stackgres-k8s/ci/build/target/config.json)"
  else
    SOURCE_IMAGE_NAME="$(image_name "$SOURCE_MODULE" "$MODULE_PLATFORM")"
  fi
  printf '%s\n' "$SOURCE_IMAGE_NAME"
}

is_source_for_any_module() {
  [ "$#" -ge 1 ] || return 0
  local MODULE="$1"
  local HAS_TARGET_MODULE
  HAS_TARGET_MODULE="$(jq -r ".stages | any(to_entries | any(.value == \"$MODULE\"))" stackgres-k8s/ci/build/target/config.json)"
  [ "$HAS_TARGET_MODULE" = true ]
}

image_name() {
  [ "$#" -ge 1 ] || false
  local BUILD_HASH
  local MODULE="$1"
  local MODULE_PLATFORM="$2"
  local IMAGE_NAME
  local MODULE_PLATFORM_DEPENDENT
  local TAG_MODULE_PLATFORM
  BUILD_HASH="$(cat stackgres-k8s/ci/build/target/build_hash)"
  MODULE_PLATFORM_DEPENDENT="$(jq -r ".modules[\"$MODULE\"].platform_dependent | . != null and ." stackgres-k8s/ci/build/target/config.json)"
  if [ "$MODULE_PLATFORM_DEPENDENT" = true ]
  then
    TAG_MODULE_PLATFORM="-$(printf %s "${MODULE_PLATFORM:-$(get_platform)}" | tr '/' '-')"
  else
    TAG_MODULE_PLATFORM=
  fi
  IMAGE_NAME="$(grep "^$MODULE=.*$TAG_MODULE_PLATFORM$" "stackgres-k8s/ci/build/target/image-hashes.$BUILD_HASH")" \
    || die "Unable to retrieve hash for module $MODULE in stackgres-k8s/ci/build/target/image-hashes.$BUILD_HASH" 1
  IMAGE_NAME="$(printf %s "$IMAGE_NAME"| cut -d = -f 2-)"
  [ -n "$IMAGE_NAME" ] \
    || die "Unable to retrieve hash for module $MODULE in stackgres-k8s/ci/build/target/image-hashes.$BUILD_HASH" 1
  printf '%s\n' "$IMAGE_NAME"
}

build_image() {
  [ "$#" -ge 1 ] || false
  local BUILD_HASH
  local MODULE="$1"
  local MODULE_TYPE
  local IMAGE_NAME
  local SOURCE_IMAGE_NAME
  BUILD_HASH="$(cat stackgres-k8s/ci/build/target/build_hash)"
  MODULE_TYPE="$(module_type "$MODULE")"
  SOURCE_IMAGE_NAME="$(source_image_name "$MODULE" "$BUILD_PLATFORM")"
  IMAGE_NAME="$(image_name "$MODULE" "$BUILD_PLATFORM")"
  echo
  echo "--------------------------------------------------------------------------------------------------------------------------------"
  echo
  echo "Image $IMAGE_NAME"
  echo "Source image $SOURCE_IMAGE_NAME"
  echo
  local BUILD_SKIPPED=false
  if {
      [ "$DO_BUILD" != true ] \
        && ! printf " $DO_BUILD_MODULES " | grep -qF " $MODULE " \
        && grep -q "^$IMAGE_NAME=" "stackgres-k8s/ci/build/target/image-digests.$BUILD_HASH"
    }
  then
    if is_source_for_any_module "$MODULE"
    then
      echo "Already exists on remote repository. Just extracting..."
      copy_from_image "$IMAGE_NAME"
    else
      echo "Already exists on remote repository."
    fi
    BUILD_SKIPPED=true
  else
    if {
        [ "$DO_BUILD" != true ] \
          && ! printf " $DO_BUILD_MODULES " | grep -qF " $MODULE " \
          && docker_inspect "$IMAGE_NAME" >/dev/null 2>&1
      }
    then
      if is_source_for_any_module "$MODULE"
      then
        echo "Already exists locally. Just extracting ..."
        copy_from_image "$IMAGE_NAME"
      else
        echo "Already exists locally."
      fi
      BUILD_SKIPPED=true
    else
      echo "Building $MODULE ..."
      build_module_image "$MODULE" "$SOURCE_IMAGE_NAME" "$IMAGE_NAME"
    fi
    if [ "$SKIP_PUSH" != true ] || [ "$DO_PUSH" = true ]
    then
      push_build_image "$IMAGE_NAME"
    fi
  fi
  if [ "$BUILD_SKIPPED" = true ] && [ "$DO_PUSH" = true ]
  then
    push_build_image "$IMAGE_NAME"
  fi
  echo
  echo "--------------------------------------------------------------------------------------------------------------------------------"
  echo
}

push_build_image() {
  local IMAGE_NAME="$1"
  docker_push --platform "$(docker_platform "${BUILD_PLATFORM:-$(get_platform)}")" "$IMAGE_NAME"
}

extract_all() {
  while [ "$#" -ge 1 ]
  do
    local MODULE="$1"
    shift
    IMAGE_NAME="$(image_name "$MODULE")"
    copy_from_image "$IMAGE_NAME"
  done
}

extract() {
  [ "$#" -ge 2 ] || false
  local MODULE="$1"
  shift
  IMAGE_NAME="$(image_name "$MODULE")"
  extract_from_image "$IMAGE_NAME" "$@"
}

extract_from_image() {
  [ "$#" -ge 2 ] || false
  local IMAGE_NAME="$1"
  shift
  local IMAGE_PLATFORM
  local CONTAINER_ID
  local WORKDIR
  local DEST="${PROJECT_PATH:-$(pwd)}"
  local DEST_PARENT
  local FILE
  local SRC
  IMAGE_PLATFORM="$(get_image_platform "$IMAGE_NAME")"
  # Files are extracted with `docker create` + `docker cp` instead of running
  # the image, so extraction never executes a binary from the image. This makes
  # it work regardless of the host architecture (e.g. extracting from an arm64
  # image on an amd64 runner) without requiring qemu/binfmt emulation.
  # `docker cp` (without --archive) writes the files owned by the invoking user,
  # matching the previous `--user $(id -u):$(id -g)` behaviour.
  CONTAINER_ID="$(docker_create --platform "$IMAGE_PLATFORM" \
    $([ "$SKIP_REMOTE_MANIFEST" = true ] || printf %s '--pull always') \
    "$IMAGE_NAME")"
  # Relative artifact paths were resolved against the image WORKDIR by the old
  # in-container `cp`; `docker cp` resolves them against `/`, so prefix WORKDIR.
  WORKDIR="$(docker_inspect "$CONTAINER_ID" --format '{{.Config.WorkingDir}}' 2>/dev/null || true)"
  WORKDIR="${WORKDIR:-/}"
  for FILE in "$@"
  do
    case "$FILE" in
      /*) SRC="$FILE" ;;
      *)  SRC="${WORKDIR%/}/$FILE" ;;
    esac
    case "$FILE" in
      */*) DEST_PARENT="$DEST/${FILE%/*}" ;;
      *)   DEST_PARENT="$DEST" ;;
    esac
    # Copying the path into its parent keeps the name it has in the image and
    # needs to know nothing about what it is: a file lands beside whatever is
    # already there and the contents of a directory are merged into the one of
    # the same name. Asking for `<path>/.` instead tells the engines apart
    # rather than the kind of path, since docker fails with `not a directory`
    # on a file, which is how this used to be told, while podman copies the
    # file into the directory made to receive it and leaves a directory where
    # the file was meant to be.
    mkdir -p "$DEST_PARENT"
    # silently skipping the paths that do not exist in the image
    docker_cp "$CONTAINER_ID:$SRC" "$DEST_PARENT" 2>/dev/null || true
  done
  docker_rm -fv "$CONTAINER_ID" >/dev/null
}

generate_image_hashes() {
  local MODULE
  local MODULE_TYPE
  local MODULE_PLATFORMS
  local MODULE_PLATFORM
  local SOURCE_IMAGE_NAME
  local IMAGE_NAME
  local MODULES
  local PROJECT_HASH_PATH
  local BUILD_HASH

  mkdir -p stackgres-k8s/ci/build/target

  init_config

  init_hash

  BUILD_HASH="$(echo "$*" | md5sum | cut -d ' ' -f 1)"
  printf %s "$BUILD_HASH" > stackgres-k8s/ci/build/target/build_hash
  PROJECT_HASH_PATH="stackgres-k8s/ci/build/target/project_hash.$BUILD_HASH"
  if ! test -f "$PROJECT_HASH_PATH" \
    || [ "$(cat "$PROJECT_HASH_PATH")" != "$(project_hash)" ]
  then
    cat << EOF > "stackgres-k8s/ci/build/target/junit-build.hashes.xml.$BUILD_HASH"
<?xml version="1.0" encoding="UTF-8"?>
<testsuites>
  <testsuite name="build hashes">
EOF

    rm -f "stackgres-k8s/ci/build/target/all-images.$BUILD_HASH"
    rm -f "stackgres-k8s/ci/build/target/image-hashes.$BUILD_HASH"
    rm -f "stackgres-k8s/ci/build/target/"*"-image-hashes.$BUILD_HASH"
    if [ "$#" -gt 0 ]
    then
      local MODULES_FILTER=" $* "
      MODULES="$(jq -r '.modules | to_entries[] | .key' stackgres-k8s/ci/build/target/config.json \
        | while read MODULE
          do
            if printf "$MODULES_FILTER" | grep -q " $MODULE "
            then
              echo "$MODULE"
            fi
          done)"
    else
      MODULES="$(jq -r '.modules | to_entries[] | .key' stackgres-k8s/ci/build/target/config.json)"
    fi
    for MODULE in $MODULES
    do
      generate_image_hash "$MODULE"
    done

    rm -rf "stackgres-k8s/ci/build/target/image-type-hashes.$BUILD_HASH"
    for MODULE_TYPE_IMAGE_HASHES in "stackgres-k8s/ci/build/target/"*"-image-hashes.$BUILD_HASH"
    do
      local MODULE_TYPE="${MODULE_TYPE_IMAGE_HASHES##*/}"
      MODULE_TYPE="${MODULE_TYPE%-image-hashes.*}"
      local MODULE_TYPE_HASH
      MODULE_TYPE_HASH="$(md5sum "$MODULE_TYPE_IMAGE_HASHES" | cut -d ' ' -f 1 | tr -d '\n')"
      printf '%s=%s\n' "$MODULE_TYPE" "$MODULE_TYPE_HASH" >> "stackgres-k8s/ci/build/target/image-type-hashes.$BUILD_HASH"
      cat << EOF >> "stackgres-k8s/ci/build/target/junit-build.hashes.xml.$BUILD_HASH"
    <testcase classname="module type $MODULE_TYPE" name="$MODULE_TYPE_HASH" />
EOF
    done

    cat << EOF >> "stackgres-k8s/ci/build/target/junit-build.hashes.xml.$BUILD_HASH"
  </testsuite>
</testsuites>
EOF

    project_hash > "$PROJECT_HASH_PATH"
  fi
}

init_config() {
  local CONFIG_HASH

  mkdir -p stackgres-k8s/ci/build/target

  CONFIG_HASH="$(md5sum stackgres-k8s/ci/build/config.yml | cut -d ' ' -f 1)"
  if ! test -f stackgres-k8s/ci/build/target/config.json \
    || ! test -f stackgres-k8s/ci/build/target/config.yml.md5 \
    || [ "$(printf %s "$CONFIG_HASH")" != "$(cat stackgres-k8s/ci/build/target/config.yml.md5)" ]
  then
    yq . stackgres-k8s/ci/build/config.yml > stackgres-k8s/ci/build/target/config.json
    printf %s "$CONFIG_HASH" > stackgres-k8s/ci/build/target/config.yml.md5
  fi
}

init_config() {
  local CONFIG_HASH

  mkdir -p stackgres-k8s/ci/build/target

  CONFIG_HASH="$(md5sum stackgres-k8s/ci/build/config.yml | cut -d ' ' -f 1)"
  if ! test -f stackgres-k8s/ci/build/target/config.json \
    || ! test -f stackgres-k8s/ci/build/target/config.yml.md5 \
    || [ "$(printf %s "$CONFIG_HASH")" != "$(cat stackgres-k8s/ci/build/target/config.yml.md5)" ]
  then
    yq . stackgres-k8s/ci/build/config.yml > stackgres-k8s/ci/build/target/config.json
    printf %s "$CONFIG_HASH" > stackgres-k8s/ci/build/target/config.yml.md5
  fi
}

generate_image_hash() {
  local BUILD_HASH
  local MODULE="$1"
  BUILD_HASH="$(cat stackgres-k8s/ci/build/target/build_hash)"
  MODULE_TYPE="$(module_type "$MODULE")"
  MODULE_PLATFORMS="$(jq -r "
      (.modules[\"$MODULE\"].platform_dependent | . != null and .) as \$module_platform_dependent
      | .platforms | if . != null and \$module_platform_dependent then . else [\"$(get_platform)\"] end
      | join(\" \")" \
    stackgres-k8s/ci/build/target/config.json)"
  for MODULE_PLATFORM in $MODULE_PLATFORMS
  do
    SOURCE_IMAGE_NAME="$(source_image_name "$MODULE" "$MODULE_PLATFORM")"
    IMAGE_NAME="$(module_image_name "$MODULE" "$SOURCE_IMAGE_NAME" "$MODULE_PLATFORM")"
    flock "stackgres-k8s/ci/build/target/junit-build.hashes.xml.$BUILD_HASH" \
      cat << EOF >> "stackgres-k8s/ci/build/target/junit-build.hashes.xml.$BUILD_HASH"
    <testcase classname="module $MODULE" name="${IMAGE_NAME##*:hash-}" />
EOF
    flock "stackgres-k8s/ci/build/target/$MODULE_TYPE-image-hashes.$BUILD_HASH" \
      printf '%s\n' "$IMAGE_NAME" >> "stackgres-k8s/ci/build/target/$MODULE_TYPE-image-hashes.$BUILD_HASH"
    flock "stackgres-k8s/ci/build/target/image-hashes.$BUILD_HASH" \
      printf '%s=%s\n' "$MODULE" "$IMAGE_NAME" >> "stackgres-k8s/ci/build/target/image-hashes.$BUILD_HASH"
    if [ "$SOURCE_IMAGE_NAME" != null ]
    then
      flock "stackgres-k8s/ci/build/target/all-images.$BUILD_HASH" \
        printf '%s\n' "$SOURCE_IMAGE_NAME" >> "stackgres-k8s/ci/build/target/all-images.$BUILD_HASH"
    fi
    flock "stackgres-k8s/ci/build/target/all-images.$BUILD_HASH" \
      printf '%s\n' "$IMAGE_NAME" >> "stackgres-k8s/ci/build/target/all-images.$BUILD_HASH"
  done
}

show_image_hashes() {
  local BUILD_HASH
  BUILD_HASH="$(cat stackgres-k8s/ci/build/target/build_hash)"
  echo "Calculated image hashes:"

  while IFS='=' read -r MODULE IMAGE_NAME
  do
    printf ' - %s => %s\n' "$MODULE" "$IMAGE_NAME"
  done < "stackgres-k8s/ci/build/target/image-hashes.$BUILD_HASH"

  echo "done"

  echo

  echo "Calculated image type hashes:"

  while IFS='=' read -r MODULE_TYPE MODULE_TYPE_HASH
  do
    printf ' - %s => %s\n' "$MODULE_TYPE" "$MODULE_TYPE_HASH"
  done < "stackgres-k8s/ci/build/target/image-type-hashes.$BUILD_HASH"

  echo "done"

  echo
}

find_image_digests() {
  (! ls stackgres-k8s/ci/build/target/image-digests.* > /dev/null 2>&1 \
    || rm -rf stackgres-k8s/ci/build/target/image-digests.*)
  if [ "$BUILD_IMAGE_PER_MODULE" != true ] && [ "$SKIP_REMOTE_MANIFEST" != true ]
  then
    # Single-repo format: list all tags at once, then match locally
    local BUILD_REPOSITORY
    BUILD_REPOSITORY="$(jq -r '.build_repository' stackgres-k8s/ci/build/target/config.json)"
    list_image_tags "$BUILD_REPOSITORY" \
      > "stackgres-k8s/ci/build/target/registry-tags"
    sort "$1" | uniq \
      | grep "^${BUILD_REPOSITORY}:" \
      | grep -v "@sha256:" \
      | xargs -I @ -P 16 sh $(! echo $- | grep -q x || printf %s "-x") \
        -c 'IMAGE_NAME="@"
            if grep -q "^${IMAGE_NAME##*:}$" "stackgres-k8s/ci/build/target/registry-tags" 2>/dev/null
            then
              printf '\''%s=%s\n'\'' "$IMAGE_NAME" "$IMAGE_NAME" \
                > "stackgres-k8s/ci/build/target/image-digests.${IMAGE_NAME##*/}"
            fi'
    sort "$1" | uniq \
      | grep -v "^${BUILD_REPOSITORY}:" \
      | grep -v "@sha256:" \
      | xargs -I @ -P 16 sh $(! echo $- | grep -q x || printf %s "-x") \
        stackgres-k8s/ci/build/build-functions.sh find_image_digest @
    sort "$1" | uniq \
      | grep -v "^${BUILD_REPOSITORY}:" \
      | grep "@sha256:" \
      | xargs -I @ -P 16 sh $(! echo $- | grep -q x || printf %s "-x") \
        -c 'IMAGE_NAME="@"; printf '%s=%s' "$IMAGE_NAME" "${IMAGE_NAME#*@}" > "stackgres-k8s/ci/build/target/image-digests.${IMAGE_NAME##*/}"'
  else
    # Legacy multi-repo or skip-remote: per-image parallel lookup
    sort "$1" | uniq \
      | grep -v "@sha256:" \
      | xargs -I @ -P 16 sh $(! echo $- | grep -q x || printf %s "-x") \
        stackgres-k8s/ci/build/build-functions.sh find_image_digest @
    sort "$1" | uniq \
      | grep "@sha256:" \
      | xargs -I @ -P 16 sh $(! echo $- | grep -q x || printf %s "-x") \
        -c 'IMAGE_NAME="@"; printf '%s=%s' "$IMAGE_NAME" "${IMAGE_NAME#*@}" > "stackgres-k8s/ci/build/target/image-digests.${IMAGE_NAME##*/}"'
  fi
  (! ls stackgres-k8s/ci/build/target/image-digests.* > /dev/null 2>&1 \
    || cat stackgres-k8s/ci/build/target/image-digests.*)
}

list_image_tags() {
  local REPO="$1"
  local REGISTRY="${REPO%%/*}"
  local REPO_PATH="${REPO#*/}"
  local AUTH AUTH_OPTS RESPONSE AUTH_HEADER REALM SERVICE SCOPE TOKEN

  # Extract basic auth from Docker config
  AUTH="$(jq -r ".auths[\"$REGISTRY\"].auth // empty" "$HOME/.docker/config.json" 2>/dev/null)"
  AUTH_OPTS=""
  if [ -n "$AUTH" ]; then
    AUTH_OPTS="-u $(printf %s "$AUTH" | base64 -d)"
  fi

  local TAGS_URL="https://$REGISTRY/v2/$REPO_PATH/tags/list"

  # Try direct/basic auth first
  # shellcheck disable=SC2086
  local NO_AUTH=false
  if RESPONSE="$(curl -sf $AUTH_OPTS "$TAGS_URL?n=10000" 2>/dev/null)"
  then
    NO_AUTH=true
  fi

  if [ "$NO_AUTH" = false ]; then
    # Token-based auth: parse WWW-Authenticate from 401
    # shellcheck disable=SC2086
    AUTH_HEADER="$(curl -si $AUTH_OPTS "$TAGS_URL?n=10000" 2>/dev/null \
      | grep -i '^www-authenticate:' | head -1)"
    REALM="$(printf %s "$AUTH_HEADER" | sed 's/.*realm="\([^"]*\)".*/\1/')"
    SERVICE="$(printf %s "$AUTH_HEADER" | sed 's/.*service="\([^"]*\)".*/\1/')"
    SCOPE="$(printf %s "$AUTH_HEADER" | sed 's/.*scope="\([^"]*\)".*/\1/')"

    if [ -z "$REALM" ]; then
      return 1
    fi

    # shellcheck disable=SC2086
    TOKEN="$(curl -sf $AUTH_OPTS \
      "${REALM}?service=${SERVICE}&scope=${SCOPE}" 2>/dev/null \
      | jq -r '.token // .access_token // empty')"
  fi

  local LAST
  if [ -n "$TOKEN" ]; then
    RESPONSE=""
    LAST=""
  else
    if printf %s "$RESPONSE" | jq -r '.tags // [] | length' | grep -qxF 0; then
      return
    fi
    LAST="$(printf %s "$RESPONSE" | jq -r '.tags // [] | . as $tags | .[($tags|length - 1)]')"
  fi
  while true; do
    printf %s "$RESPONSE" | jq -r '.tags // [] | .[]' 2>/dev/null
    if [ -n "$TOKEN" ]; then
      if [ -n "$LAST" ]; then
        RESPONSE="$(curl -sf -H "Authorization: Bearer $TOKEN" "$TAGS_URL?n=10000&last=$LAST" 2>/dev/null)"
      else
        RESPONSE="$(curl -sf -H "Authorization: Bearer $TOKEN" "$TAGS_URL?n=10000" 2>/dev/null)"
      fi
    else
      if [ -n "$LAST" ]; then
        RESPONSE="$(curl -sf $AUTH_OPTS "$TAGS_URL?n=10000&last=$LAST" 2>/dev/null)"
      else
        RESPONSE="$(curl -sf $AUTH_OPTS "$TAGS_URL?n=10000" 2>/dev/null)"
      fi
    fi
    printf %s "$RESPONSE" | jq -r '.tags // [] | .[]' 2>/dev/null
    if printf %s "$RESPONSE" | jq -r '.tags // [] | length' | grep -qxF 0; then
      return
    fi
    LAST="$(printf %s "$RESPONSE" | jq -r '.tags // [] | . as $tags | .[($tags|length - 1)]')"
  done
}

find_image_digest() {
  local IMAGE_NAME="$1"
  local IMAGE_DIGEST
  if retrieve_image_manifest "$IMAGE_NAME" >/dev/null 2>&1
  then
    if ! IMAGE_DIGEST="$(jq -r \
      '. as $manifest | if length == 0 then halt_error else . end | $manifest[0].RepoDigests | map(split(":")|last) | if length == 0 then halt_error else . end | sort | first' \
      "stackgres-k8s/ci/build/target/manifest.local.${IMAGE_NAME##*/}" \
      2>/dev/null)"
    then
      IMAGE_DIGEST="$(jq -r 'if (.|type) == "array" then . else [.] end | map(.Descriptor.digest) | sort | first' \
        "stackgres-k8s/ci/build/target/manifest.${IMAGE_NAME##*/}")"
    fi
    printf '%s=%s\n' "$IMAGE_NAME" "$IMAGE_DIGEST" \
      > "stackgres-k8s/ci/build/target/image-digests.${IMAGE_NAME##*/}"
  fi
}

get_image_platform() {
  local IMAGE_NAME="$1"
  local IMAGE_MEDIA_TYPE
  local IMAGE_PLATFORM
  retrieve_image_manifest "$IMAGE_NAME" > /dev/null
  if IMAGE_PLATFORM="$(jq -r \
    '. as $manifest | if length == 0 then halt_error else . end | $manifest[0].Os + "/" + $manifest[0].Architecture' \
    "stackgres-k8s/ci/build/target/manifest.local.${IMAGE_NAME##*/}" \
    2>/dev/null)"
  then
    printf %s "$IMAGE_PLATFORM"
    return
  fi
  IMAGE_MEDIA_TYPE="$(jq -r '. | type' \
    "stackgres-k8s/ci/build/target/manifest.${IMAGE_NAME##*/}")"
  if [ "$IMAGE_MEDIA_TYPE" = "array" ]
  then
    docker_buildx_inspect --bootstrap | grep Platforms | cut -d : -f 2 | tr -d ' ' | tr ',' '\n' \
      | while read -r IMAGE_PLATFORM
        do
          if jq -r '.[]|.Descriptor.platform.os + "/" + .Descriptor.platform.architecture' \
            "stackgres-k8s/ci/build/target/manifest.${IMAGE_NAME##*/}" | grep -qxF "$IMAGE_PLATFORM"
          then
            printf %s "$IMAGE_PLATFORM"
            break
          fi
        done
  else
    jq -r '.Descriptor.platform.os + "/" + .Descriptor.platform.architecture' \
      "stackgres-k8s/ci/build/target/manifest.${IMAGE_NAME##*/}"
  fi
}

retrieve_image_manifest() {
  local IMAGE_NAME="$1"
  if ! jq -r \
    '. as $manifest | if length == 0 then halt_error else . end | $manifest[0].RepoDigests | map(split(":")|last) | if length == 0 then halt_error else . end | sort | first' \
    "stackgres-k8s/ci/build/target/manifest.local.${IMAGE_NAME##*/}" \
    >/dev/null 2>&1
  then
    if [ "$SKIP_REMOTE_MANIFEST" = true ]
    then
      docker_inspect "$IMAGE_NAME" \
        > "stackgres-k8s/ci/build/target/manifest.local.${IMAGE_NAME##*/}"
      local EXIT_CODE="$?"
      if [ "$EXIT_CODE" != 0 ]
      then
        return "$EXIT_CODE"
      fi
      if ! jq -r \
        '. as $manifest | if length == 0 then halt_error else . end | $manifest[0].RepoDigests | map(split(":")|last) | if length == 0 then halt_error else . end | sort | first' \
        "stackgres-k8s/ci/build/target/manifest.local.${IMAGE_NAME##*/}" \
        >/dev/null 2>&1
      then
        echo "Using a local image registry to calculate digest for $IMAGE_NAME" >&2
        if container_engine_is_podman
        then
          # podman runs the containers in the network namespace of the host, so
          # the published ports are ignored and can not be read back from the
          # container. Pick the port up front and let the registry bind to it.
          REGISTRY_PORT="$(get_free_port)"
          REGISTRY_CONTAINER_ID="$(docker_run -d --stop-timeout 300 \
            -e "REGISTRY_HTTP_ADDR=:$REGISTRY_PORT" docker.io/library/registry:2)"
        else
          REGISTRY_CONTAINER_ID="$(docker_run -d -p 5000 --stop-timeout 300 docker.io/library/registry:2)"
          REGISTRY_PORT="$(docker_inspect "$REGISTRY_CONTAINER_ID" | jq '.[0].NetworkSettings.Ports["5000/tcp"][0].HostPort' -r)"
        fi
        REGISTRY_IMAGE_NAME="localhost:$REGISTRY_PORT/$(printf %s "${IMAGE_NAME%:*}" | tr '/:' '_'):${IMAGE_NAME##*:}"
        docker_tag "$IMAGE_NAME" "$REGISTRY_IMAGE_NAME"
        docker_inspect "$REGISTRY_IMAGE_NAME" \
          > "stackgres-k8s/ci/build/target/manifest.local.${IMAGE_NAME##*/}"
        REGISTRY_IMAGE_PLATFORM="$(jq -r \
            '. as $manifest | if length == 0 then halt_error else . end | $manifest[0].Os + "/" + $manifest[0].Architecture' \
            "stackgres-k8s/ci/build/target/manifest.local.${IMAGE_NAME##*/}" \
            2>/dev/null)"
        # The local registry is plain HTTP, podman refuses that unless told to
        # shellcheck disable=SC2046
        docker_push $(! container_engine_is_podman || printf %s '--tls-verify=false') \
          --platform "$REGISTRY_IMAGE_PLATFORM" "$REGISTRY_IMAGE_NAME"
        docker_inspect "$REGISTRY_IMAGE_NAME" \
          > "stackgres-k8s/ci/build/target/manifest.local.${IMAGE_NAME##*/}"
        docker_rm -fv "$REGISTRY_CONTAINER_ID"
        docker_rmi "$REGISTRY_IMAGE_NAME"
        jq -r \
          '. as $manifest | if length == 0 then halt_error else . end | $manifest[0].RepoDigests | map(split(":")|last) | if length == 0 then halt_error else . end | sort | first' \
          "stackgres-k8s/ci/build/target/manifest.local.${IMAGE_NAME##*/}" \
          >/dev/null 2>&1
      fi
    elif ! docker_inspect "$IMAGE_NAME" \
      > "stackgres-k8s/ci/build/target/manifest.local.${IMAGE_NAME##*/}" \
      2>/dev/null \
      || ! jq -r \
      '. as $manifest | if length == 0 then halt_error else . end | $manifest[0].RepoDigests | map(split(":")|last) | if length == 0 then halt_error else . end | sort | first' \
      "stackgres-k8s/ci/build/target/manifest.local.${IMAGE_NAME##*/}" \
      >/dev/null 2>&1
    then
      if ! [ -s "stackgres-k8s/ci/build/target/manifest.${IMAGE_NAME##*/}" ]
      then
        docker_manifest_inspect -v "$IMAGE_NAME" \
          > "stackgres-k8s/ci/build/target/manifest.${IMAGE_NAME##*/}"
      fi
    fi
  fi
}

get_platform() {
  printf '%s/%s' "$(uname | tr '[:upper:]' '[:lower:]')" "$(uname -m)"
}

get_platform_tag_suffix() {
  get_platform | tr '/' '-'
}

get_module_hash() {
  local MODULE="$1"
  local MODULE_PLATFORM="$2"
  local IMAGE_HASHES_FILE="stackgres-k8s/ci/build/target/image-hashes.$(cat stackgres-k8s/ci/build/target/build_hash)"
  local MODULE_PLATFORM_DEPENDENT
  local TAG_MODULE_PLATFORM
  if ! grep -q "^${MODULE}=" "$IMAGE_HASHES_FILE"
  then
    >&2 echo "Module $MODULE not found"
    return 1
  fi
  MODULE_PLATFORM_DEPENDENT="$(jq -r ".modules[\"$MODULE\"].platform_dependent | . != null and ." stackgres-k8s/ci/build/target/config.json)"
  if [ "$MODULE_PLATFORM_DEPENDENT" = true ]
  then
    TAG_MODULE_PLATFORM="-$(printf %s "${MODULE_PLATFORM:-$(get_platform)}" | tr '/' '-')"
  else
    TAG_MODULE_PLATFORM=
  fi
  sed -n "s/^${MODULE}=.*[:-]hash-\([^:]\+\)$TAG_MODULE_PLATFORM$/\1/p" "$IMAGE_HASHES_FILE"
}

project_hash() {
  git --git-dir "stackgres-k8s/ci/build/target/.git" rev-parse HEAD:
  env -0 | sort -z | md5sum
}

path_hash() {
  [ "$#" -ge 1 ] || false
  local FILE="$1"
  git --git-dir "stackgres-k8s/ci/build/target/.git" rev-parse HEAD:"$FILE"
}

# Every invocation of the container engine goes through the functions below.
# CONTAINER_ENGINE is expanded unquoted on purpose: it is a command prefix, so
# values with arguments like `podman --remote` are supported.

# shellcheck disable=SC2086
docker_inspect() {
  $CONTAINER_ENGINE inspect "$@"
}

# shellcheck disable=SC2086
docker_images() {
  $CONTAINER_ENGINE images "$@"
}

# shellcheck disable=SC2086
docker_rmi() {
  $CONTAINER_ENGINE rmi "$@"
}

# shellcheck disable=SC2086
docker_run() {
  $CONTAINER_ENGINE run "$@"
}

# shellcheck disable=SC2086
docker_create() {
  $CONTAINER_ENGINE create "$@"
}

# shellcheck disable=SC2086
docker_cp() {
  $CONTAINER_ENGINE cp "$@"
}

# shellcheck disable=SC2086
docker_build() {
  $CONTAINER_ENGINE build "$@"
}

# shellcheck disable=SC2086
docker_login() {
  if container_engine_is_podman && [ "$#" = 1 ]
  then
    # `docker login <registry>` succeeds without asking anything when it already
    # has credentials for it, which is how the pipeline tests them before
    # falling back to a login with a user and a password. podman prompts instead
    # and then fails with `reading password: inappropriate ioctl for device`,
    # since a job has no terminal, so ask it for the stored login instead.
    $CONTAINER_ENGINE login --get-login "$1" > /dev/null
    return
  fi
  $CONTAINER_ENGINE login "$@"
}

# shellcheck disable=SC2086
docker_pull() {
  $CONTAINER_ENGINE pull "$@"
}

# shellcheck disable=SC2086
docker_save() {
  $CONTAINER_ENGINE save "$@"
}

docker_push() {
  if container_engine_is_podman
  then
    # podman push has no --platform option since a local podman image is always
    # single arch. Drop any --platform <value> or --platform=<value> the caller
    # passed by rotating the arguments.
    local COUNT="$#"
    local INDEX=0
    local SKIP=false
    local ARG
    while [ "$INDEX" -lt "$COUNT" ]
    do
      INDEX="$((INDEX + 1))"
      ARG="$1"
      shift
      if [ "$SKIP" = true ]
      then
        SKIP=false
        continue
      fi
      case "$ARG" in
        --platform) SKIP=true; continue ;;
        --platform=*) continue ;;
      esac
      set -- "$@" "$ARG"
    done
    # shellcheck disable=SC2086
    $CONTAINER_ENGINE push "$@"
  else
    docker push --platform=linux/"$(uname -m | grep -qxF aarch64 && printf arm64 || printf amd64)" "$@"
  fi
}

docker_platform() {
  local PLATFORM="$1"
  printf "${PLATFORM%/*}/$(printf %s "${PLATFORM#*/}" | grep -qxF aarch64 && printf arm64 || printf amd64)"
}

docker_engine_platform() {
  if container_engine_is_podman
  then
    # podman version does not expose the server architecture
    # shellcheck disable=SC2086
    $CONTAINER_ENGINE info --format '{{.Version.OsArch}}'
  else
    docker version --format '{{ .Server.Os }}/{{ .Server.Arch }}'
  fi
}

# shellcheck disable=SC2086
docker_tag() {
  $CONTAINER_ENGINE tag "$@"
}

# shellcheck disable=SC2086
docker_rm() {
  $CONTAINER_ENGINE rm "$@"
}

# podman only inspects the manifest of an image that is a manifest list, and
# fails on any other with `Treating single images as manifest lists is not
# implemented`, which is what most of the images of this build are. Ask the
# registry through skopeo instead and give back the shape docker gives with -v:
# an object per image, and an array of them when the image is a manifest list.
docker_manifest_inspect() {
  if ! container_engine_is_podman
  then
    # shellcheck disable=SC2086
    $CONTAINER_ENGINE manifest inspect "$@"
    return
  fi
  local IMAGE_NAME=
  local ARG
  for ARG
  do
    case "$ARG" in
      (-*) ;;
      (*) IMAGE_NAME="$ARG" ;;
    esac
  done
  local MANIFEST
  MANIFEST="$(skopeo inspect --raw "docker://$IMAGE_NAME")"
  if printf %s "$MANIFEST" | jq -e 'has("manifests")' > /dev/null
  then
    printf %s "$MANIFEST" | jq --arg ref "$IMAGE_NAME" \
      '[ .manifests[] | { Ref: ($ref + "@" + .digest),
        Descriptor: { mediaType, digest, size, platform } } ]'
  else
    # The platform of a single image is in its configuration, that only the
    # inspect without --raw retrieves.
    local IMAGE_DIGEST_AND_PLATFORM
    IMAGE_DIGEST_AND_PLATFORM="$(skopeo inspect \
      --format '{{ .Digest }} {{ .Os }} {{ .Architecture }}' "docker://$IMAGE_NAME")"
    printf %s "$MANIFEST" | jq --arg ref "$IMAGE_NAME" \
      --argjson size "$(printf %s "$MANIFEST" | wc -c | tr -d ' ')" \
      --arg digest "$(printf %s "$IMAGE_DIGEST_AND_PLATFORM" | cut -d ' ' -f 1)" \
      --arg os "$(printf %s "$IMAGE_DIGEST_AND_PLATFORM" | cut -d ' ' -f 2)" \
      --arg architecture "$(printf %s "$IMAGE_DIGEST_AND_PLATFORM" | cut -d ' ' -f 3)" \
      '{ Ref: $ref, Descriptor: { mediaType: .mediaType, digest: $digest,
        size: $size, platform: { architecture: $architecture, os: $os } } }'
  fi
}

# shellcheck disable=SC2086
docker_manifest_create() {
  # podman keeps the manifest lists in the image store, so an image named like
  # the list to create, like the placeholder the pipeline pushes to create the
  # tag before assembling the list, makes it fail with `that name is already in
  # use`. docker keeps them in a store of its own and never sees the conflict.
  # `podman manifest create --replace` would do, but it only exists since
  # podman 5, so remove the name instead, which is what the callers mean by
  # removing the manifest before creating it.
  if container_engine_is_podman
  then
    local MANIFEST_NAME=
    local ARG
    for ARG
    do
      case "$ARG" in
        (-*) ;;
        (*) MANIFEST_NAME="$ARG"; break ;;
      esac
    done
    if $CONTAINER_ENGINE image exists "$MANIFEST_NAME" 2> /dev/null
    then
      $CONTAINER_ENGINE rmi "$MANIFEST_NAME" > /dev/null
    fi
  fi
  $CONTAINER_ENGINE manifest create "$@"
}

# shellcheck disable=SC2086
docker_manifest_push() {
  $CONTAINER_ENGINE manifest push "$@"
}

# shellcheck disable=SC2086
docker_manifest_rm() {
  $CONTAINER_ENGINE manifest rm "$@"
}

docker_buildx_inspect() {
  if container_engine_is_podman
  then
    # podman has no buildx. Without emulation it can only build for the host
    # platform, that is exactly what the caller of this function needs to know.
    printf 'Platforms: %s\n' "$(docker_engine_platform)"
  else
    docker buildx inspect "$@"
  fi
}

# The socket of the engine of this host, that a build container has to be given
# access to: the Java tests start containers through the Testcontainers of the
# Dev Services of Quarkus. docker always has one, podman only has one while
# something is serving its docker compatible API.
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
  # A host can have both, the socket of the rootless podman of each user and the
  # one of the rootful podman, and only the one this user can write to belongs
  # to the podman this script drives.
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

# podman is daemonless, so its docker compatible API only exists while a service
# is serving it. The user unit that does so is not enabled by default and a job
# pod has no systemd at all, so serve one for as long as the build container
# needs it instead of requiring it to be set up beforehand.
CONTAINER_ENGINE_SOCKET_PATH=
CONTAINER_ENGINE_SOCKET_SERVICE_PID=
container_engine_serve_socket() {
  CONTAINER_ENGINE_SOCKET_PATH="$(container_engine_socket_path)"
  if [ -n "$CONTAINER_ENGINE_SOCKET_PATH" ] || ! container_engine_is_podman
  then
    return
  fi
  CONTAINER_ENGINE_SOCKET_PATH="${TMPDIR:-/tmp}/stackgres-build-engine-$$.sock"
  $CONTAINER_ENGINE system service --time=0 "unix://$CONTAINER_ENGINE_SOCKET_PATH" &
  CONTAINER_ENGINE_SOCKET_SERVICE_PID="$!"
  local ATTEMPT=0
  while [ ! -S "$CONTAINER_ENGINE_SOCKET_PATH" ] && [ "$ATTEMPT" -lt 100 ]
  do
    ATTEMPT="$((ATTEMPT + 1))"
    sleep 0.1
  done
  if [ ! -S "$CONTAINER_ENGINE_SOCKET_PATH" ]
  then
    >&2 echo "Could not serve the API of the container engine with \`$CONTAINER_ENGINE system service\`," \
      "that the Java tests need in order to start containers"
    container_engine_stop_socket
    return 1
  fi
}

container_engine_stop_socket() {
  if [ -n "$CONTAINER_ENGINE_SOCKET_SERVICE_PID" ]
  then
    kill "$CONTAINER_ENGINE_SOCKET_SERVICE_PID" 2> /dev/null || true
    wait "$CONTAINER_ENGINE_SOCKET_SERVICE_PID" 2> /dev/null || true
    rm -f "$CONTAINER_ENGINE_SOCKET_PATH"
    CONTAINER_ENGINE_SOCKET_SERVICE_PID=
    CONTAINER_ENGINE_SOCKET_PATH=
  fi
}

container_engine_socket_volume() {
  local SOCKET_PATH="${CONTAINER_ENGINE_SOCKET_PATH:-$(container_engine_socket_path)}"
  if [ -n "$SOCKET_PATH" ]
  then
    # Always mounted where docker has it, so that anything looking for the engine
    # from inside a build container keeps finding it where it expects to.
    printf %s "--volume $SOCKET_PATH:/var/run/docker.sock"
  fi
}

# Testcontainers starts its containers on the engine of this host and not inside
# the build container, so they are siblings of it and it has to be told:
#
# * the path the socket has out here, to be able to mount it into its reaper;
# * the host to reach the ports they publish, since those are published out here
#   and not on the loopback of the build container, where Testcontainers looks
#   by default. podman resolves host.containers.internal to this host in every
#   container it runs, which is what makes them reachable;
# * to not run the reaper at all, since it would need a privileged container
#   with a rootless podman and the build container is thrown away at the end of
#   the build anyway.
container_engine_testcontainers_env() {
  local SOCKET_PATH
  if ! container_engine_is_podman
  then
    return
  fi
  SOCKET_PATH="${CONTAINER_ENGINE_SOCKET_PATH:-$(container_engine_socket_path)}"
  printf '%s' '--env DOCKER_HOST=unix:///var/run/docker.sock'
  if [ -n "$SOCKET_PATH" ]
  then
    printf ' --env TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=%s' "$SOCKET_PATH"
  fi
  printf ' %s' '--env TESTCONTAINERS_HOST_OVERRIDE=host.containers.internal'
  printf ' %s' '--env TESTCONTAINERS_RYUK_DISABLED=true'
}

get_free_port() {
  local PORT
  local ATTEMPT=0
  local EXIT_CODE
  while [ "$ATTEMPT" -lt 100 ]
  do
    ATTEMPT="$((ATTEMPT + 1))"
    PORT="$(( 32768 + ( ( $$ + ATTEMPT * 7919 ) % 28000 ) ))"
    EXIT_CODE=0
    curl -s -o /dev/null --max-time 1 "http://localhost:$PORT/" || EXIT_CODE="$?"
    # 7 is the curl exit code for a refused connection
    if [ "$EXIT_CODE" = 7 ]
    then
      printf %s "$PORT"
      return
    fi
  done
  >&2 echo "Could not find a free port"
  return 1
}

if [ "$(basename "$0")" = "build-functions.sh" ] && [ "$#" -ge 1 ]
then
  "$@"
fi

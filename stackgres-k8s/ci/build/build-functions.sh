#!/bin/sh
# shellcheck disable=SC2039
# shellcheck disable=SC2016

BUILDER_VERSION=1.0.0

[ "$DEBUG" != true ] || set -x

message_and_exit() {
  printf '\n\t%s\n\n' "$1"
  exit "$2"
}

command -v xargs > /dev/null || message_and_exit 'The program `xargs` is required to be in PATH' 8
command -v ls > /dev/null || message_and_exit 'The program `ls` is required to be in PATH' 8
command -v jq > /dev/null || message_and_exit 'The program `jq` is required to be in PATH' 8
command -v yq > /dev/null || message_and_exit 'The program `yq` (https://kislyuk.github.io/yq/) is required to be in PATH' 8
command -v git > /dev/null || message_and_exit 'The program `git` is required to be in PATH' 8
command -v md5sum > /dev/null || message_and_exit 'The program `md5sum` is required to be in PATH' 8
command -v find > /dev/null || message_and_exit 'The program `find` is required to be in PATH' 8
command -v java > /dev/null || message_and_exit 'The program `java` is required to be in PATH' 8
command -v docker > /dev/null || message_and_exit 'The program `docker` is required to be in PATH' 8
docker manifest > /dev/null 2>&1 || message_and_exit '`docker manifest` is not working' 9

export LC_ALL=C

cd "$(dirname "$0")/../../.." || message_and_exit "Can not change to directory $(dirname "$0")/../../.."

module_image_name() {
  [ "$#" -ge 2 ] || false
  local MODULE="$1"
  local SOURCE_IMAGE_NAME="$2"
  local MODULE_SOURCES
  local MODULE_DOCKERFILE
  local MODULE_HASH
  MODULE_SOURCES="$(module_list_of_files "$MODULE" sources)"
  MODULE_DOCKERFILE="$(jq -r ".modules[\"$MODULE\"].dockerfile.path" stackgres-k8s/ci/build/target/config.json)"
  MODULE_HASH="$( (
    echo "${BUILDER_VERSION%.*}"
    echo "$SOURCE_IMAGE_NAME"
    jq -r ".modules[\"$MODULE\"]" stackgres-k8s/ci/build/target/config.json
    if [ "$MODULE_DOCKERFILE" != null ]
    then
      file_hash "$MODULE_DOCKERFILE"
    fi
    for MODULE_SOURCE in $MODULE_SOURCES
    do
      file_hash "$MODULE_SOURCE"
    done
    ) | md5sum | cut -d ' ' -f 1)"
  printf 'registry.gitlab.com/ongresinc/stackgres/build/%s:hash-%s\n' "$MODULE" "$MODULE_HASH"
}

copy_from_image() {
  [ "$#" -ge 1 ] || false
  local SOURCE_IMAGE_NAME="$1"
  if [ "$SOURCE_IMAGE_NAME" = null ]
  then
    return
  fi
  # shellcheck disable=SC2046
  docker_run -i $(! test -t 1 || printf '%s' '-t') --rm \
    --volume "$(pwd):/project-target" \
    --user "$UID" \
    "$SOURCE_IMAGE_NAME" \
    sh -ec $(echo "$-" | grep -v -q x || printf '%s' '-x') \
      'cp -rd /project/. /project-target/.'
}

pre_build_in_container() {
  [ "$#" -ge 2 ] || false
  local MODULE="$1"
  local BUILD_IMAGE_NAME="$2"
  local COMMANDS
  if [ "$BUILD_SKIP_PRE_BUILD" = true ]
  then
    return
  fi
  COMMANDS="$(jq -r ".modules[\"$MODULE\"].pre_build_commands | if . != null then . | join(\"\n\") else true end" stackgres-k8s/ci/build/target/config.json)"
  run_commands_in_container "$MODULE" "$BUILD_IMAGE_NAME" "0" "$COMMANDS"
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
  run_commands_in_container "$MODULE" "$BUILD_IMAGE_NAME" "$UID" "$COMMANDS"
}

post_build_in_container() {
  [ "$#" -ge 2 ] || false
  local MODULE="$1"
  local BUILD_IMAGE_NAME="$2"
  local COMMANDS
  if [ "$BUILD_SKIP_POST_BUILD" = true ]
  then
    return
  fi
  COMMANDS="$(jq -r ".modules[\"$MODULE\"].post_build_commands | if . != null then . | join(\"\n\") else true end" stackgres-k8s/ci/build/target/config.json)"
  run_commands_in_container "$MODULE" "$BUILD_IMAGE_NAME" "0" "$COMMANDS"
}

run_commands_in_container() {
  [ "$#" -ge 4 ] || false
  local MODULE="$1"
  local BUILD_IMAGE_NAME="$2"
  local UID="$3"
  local COMMANDS="$4"
  local MODULE_PATH
  MODULE_PATH="$(jq -r ".modules[\"$MODULE\"].path" stackgres-k8s/ci/build/target/config.json)"
  # shellcheck disable=SC2046
  docker_run -i $(! test -t 1 || printf '%s' '-t') --rm \
    --volume "$(pwd):/project" \
    --workdir /project \
    --user "$UID" \
    --env "PRE_BUILD_COMMANDS=$PRE_BUILD_COMMANDS" \
    --env "BUILD_COMMANDS=$BUILD_COMMANDS" \
    --env "POST_BUILD_COMMANDS=$POST_BUILD_COMMANDS" \
    --env "MODULE_PATH=$MODULE_PATH" \
    --env "SHELL_XTRACE=$([ "$DEBUG" != true ] || printf '%s' -x)" \
    "$BUILD_IMAGE_NAME" \
    sh -ec $(echo "$-" | grep -v -q x || printf '%s' '-x') \
      "$(jq -r ".modules[\"$MODULE\"].build_env
          | to_entries
          | map(\"export \" + .key + \"='\" + .value + \"'\")
          | .[]" \
        stackgres-k8s/ci/build/target/config.json)
      sh -ec $(echo "$-" | grep -v -q x || printf '%s' '-x') '$COMMANDS'"
}

build_module_image() {
  [ "$#" -ge 3 ] || false
  local MODULE="$1"
  local SOURCE_IMAGE_NAME="$2"
  local IMAGE_NAME="$3"
  local BUILD_IMAGE_NAME
  local TARGET_IMAGE_NAME
  local MODULE_PATH
  local MODULE_SOURCES
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
    build_in_container "$MODULE" "$BUILD_IMAGE_NAME"
    post_build_in_container "$MODULE" "$BUILD_IMAGE_NAME"
  fi
  MODULE_PATH="$(jq -r ".modules[\"$MODULE\"].path" stackgres-k8s/ci/build/target/config.json)"
  MODULE_DOCKERFILE="$(jq -r ".modules[\"$MODULE\"].dockerfile.path" stackgres-k8s/ci/build/target/config.json)"
  MODULE_ARTIFACTS="$(module_list_of_files "$MODULE" artifacts)"
  MODULE_SOURCES="$(module_list_of_files "$MODULE" sources)"
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
            printf 'ARG %s\n' "$KEY"
          done
    eval "cat '$MODULE_DOCKERFILE'$(
        jq -r ".modules[\"$MODULE\"].dockerfile.seds[]" stackgres-k8s/ci/build/target/config.json \
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
  ARG UID
  USER $UID
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
    --build-arg "UID=$UID" \
    --build-arg "TARGET_IMAGE_NAME=$TARGET_IMAGE_NAME" \
    $(jq -r ".modules[\"$MODULE\"].dockerfile.args
        | if . != null then . else {} end | to_entries
        | map(.key + \" \" + .value + \"\")[]" stackgres-k8s/ci/build/target/config.json \
      | while read -r KEY VALUE
        do
          printf '%s %s=%s' '--build-arg' "$KEY" "$(eval "printf '%s' \"$VALUE\"")"
        done) \
    -f "stackgres-k8s/ci/build/target/Dockerfile.$MODULE" .
}

module_list_of_files() {
  [ "$#" -ge 2 ] || false
  local MODULE="$1"
  local MODULE_FILES_PATH="$2"
  local MODULE_FILES
  MODULE_FILES="$(
    jq -r ".modules[\"$MODULE\"][\"$MODULE_FILES_PATH\"] | if . != null then .[] else [][] end" stackgres-k8s/ci/build/target/config.json
    )"
  printf '%s' "$MODULE_FILES"
}

project_hash() {
  git rev-parse HEAD
}

file_hash() {
  [ "$#" -ge 1 ] || false
  local MODULE_FILE="$1"
  git rev-parse HEAD:"$MODULE_FILE"
}

docker_inspect() {
  (
  set -x
  docker inspect "$@"
  )
}

docker_images() {
  (
  set -x
  docker images "$@"
  )
}

docker_rmi() {
  (
  set -x
  docker rmi "$@"
  )
}

docker_run() {
  (
  set -x
  docker run "$@"
  )
}

docker_build() {
  (
  set -x
  docker build "$@"
  )
}

docker_push() {
  (
  set -x
  docker push "$@"
  )
}

retrieve_image_hash_script() {
  cat << EOF
IMAGE_NAME='$1'
IMAGE_DIGEST="\$(docker manifest inspect "\$IMAGE_NAME" 2>/dev/null || true)"
if [ -n "\$IMAGE_DIGEST" ]
then
  printf '%s=%s\n' "\$IMAGE_NAME" "\$(printf '%s' "\$IMAGE_DIGEST" | jq -r '.config.digest')" > "stackgres-k8s/ci/build/target/image-digests.\${IMAGE_NAME##*/}"
fi
EOF
}

set_module_functions() {
  [ "$#" -ge 1 ] || false
  local MODULE="$1"
  MODULE_TYPE="$(jq -r ".modules | select(has(\"$MODULE\"))[\"$MODULE\"] | select(has(\"type\")).type" stackgres-k8s/ci/build/target/config.json)"
  [ -n "$MODULE_TYPE" ] || message_and_exit "Module $MODULE is not defined or has no type in stackgres-k8s/ci/build/config.yml" 1
}

source_image_name() {
  [ "$#" -ge 1 ] || false
  local MODULE="$1"
  local SOURCE_MODULE
  local SOURCE_IMAGE_NAME
  SOURCE_MODULE="$(jq -r ".stages[] | select(has(\"$MODULE\"))[\"$MODULE\"]" stackgres-k8s/ci/build/target/config.json)"
  [ -n "$SOURCE_MODULE" ] || message_and_exit "Module $MODULE has no stage defined in stackgres-k8s/ci/build/config.yml" 1
  if [ "$SOURCE_MODULE" = null ]
  then
    SOURCE_IMAGE_NAME="$(jq -r ".target_image" stackgres-k8s/ci/build/target/config.json)"
  else
    SOURCE_IMAGE_NAME="$(image_name "$SOURCE_MODULE")"
  fi
  printf '%s\n' "$SOURCE_IMAGE_NAME"
}

image_name() {
  [ "$#" -ge 1 ] || false
  local MODULE="$1"
  local IMAGE_NAME
  IMAGE_NAME="$(grep "^$MODULE=" stackgres-k8s/ci/build/target/image-hashes)" \
    || message_and_exit "Unable to retrieve hash for module $MODULE in stackgres-k8s/ci/build/target/image-hashes" 1
  IMAGE_NAME="$(printf '%s' "$IMAGE_NAME"| cut -d = -f 2-)"
  [ -n "$IMAGE_NAME" ] \
    || message_and_exit "Unable to retrieve hash for module $MODULE in stackgres-k8s/ci/build/target/image-hashes" 1
  printf '%s\n' "$IMAGE_NAME"
}

build_image() {
  [ "$#" -ge 1 ] || false
  local MODULE="$1"
  local IMAGE_NAME
  local SOURCE_IMAGE_NAME
  set_module_functions "$MODULE"
  SOURCE_IMAGE_NAME="$(source_image_name "$MODULE")"
  IMAGE_NAME="$(image_name "$MODULE")"
  echo
  echo "--------------------------------------------------------------------------------------------------------------------------------"
  echo
  echo "Image $IMAGE_NAME"
  echo "Source image $SOURCE_IMAGE_NAME"
  echo
  if [ "$DO_BUILD" != true ] && grep -q "^$IMAGE_NAME=" stackgres-k8s/ci/build/target/image-digests
  then
    echo "Already exists. Skipping ..."
  else
    if [ "$DO_BUILD" != true ] && docker_inspect "$IMAGE_NAME" >/dev/null 2>&1
    then
      echo "Already exists locally. Skipping build ..."
    else
      echo "Building $MODULE ..."
      build_module_image "$MODULE" "$SOURCE_IMAGE_NAME" "$IMAGE_NAME"
    fi
    if [ "$SKIP_PUSH" != true ]
    then
      docker_push "$IMAGE_NAME"
    fi
  fi
  echo
  echo "--------------------------------------------------------------------------------------------------------------------------------"
  echo
}

generate_image_hashes() {
  local MODULE
  local SOURCE_IMAGE_NAME
  local IMAGE_NAME

  UID="$(id -u)"

  mkdir -p stackgres-k8s/ci/build/target

  yq . stackgres-k8s/ci/build/config.yml > stackgres-k8s/ci/build/target/config.json

  if [ -n "$(git status --porcelain)" ] && [ "$BUILD_SKIP_GIT_STATUS_CHECK" != true ]
  then
    echo >&2
    echo "FATAL: You have uncommited changes that may affect hash calculation" >&2
    echo >&2
    echo "Set BUILD_SKIP_GIT_STATUS_CHECK to true to skip this check." >&2
    echo >&2
    return 1
  fi

  if test -f stackgres-k8s/ci/build/target/project_hash \
    && [ "$(cat stackgres-k8s/ci/build/target/project_hash)" = "$(project_hash)" ]
  then
    echo "Calculated image hashes..."

    while IFS='=' read -r MODULE IMAGE_NAME
    do
      printf ' - %s => %s\n' "$MODULE" "$IMAGE_NAME"
    done < stackgres-k8s/ci/build/target/image-hashes

    echo "done"

    echo

    echo "Calculated image type hashes ..."

    for MODULE_TYPE_IMAGE_HASHES in stackgres-k8s/ci/build/target/*-image-hashes
    do
      local MODULE_TYPE="${MODULE_TYPE_IMAGE_HASHES##*/}"
      MODULE_TYPE="${MODULE_TYPE%-image-hashes}"
      local MODULE_TYPE_HASH
      MODULE_TYPE_HASH="$(md5sum "$MODULE_TYPE_IMAGE_HASHES" | cut -d ' ' -f 1 | tr -d '\n')"
      printf " - %s hash => %s\n" "$MODULE_TYPE" "$MODULE_TYPE_HASH"
    done

    echo "done"

    echo

    return
  fi

  echo "Calculating image hashes ..."

  cat << EOF > stackgres-k8s/ci/build/target/junit-build.hashes.xml
<?xml version="1.0" encoding="UTF-8"?>
<testsuites>
  <testsuite name="build hashes">
EOF

  rm -f stackgres-k8s/ci/build/target/all-images
  rm -f stackgres-k8s/ci/build/target/image-hashes
  rm -f stackgres-k8s/ci/build/target/*-image-hashes
  for MODULE in $(jq -r '.modules | to_entries[] | .key' stackgres-k8s/ci/build/target/config.json)
  do
    set_module_functions "$MODULE"
    SOURCE_IMAGE_NAME="$(source_image_name "$MODULE")"
    IMAGE_NAME="$(module_image_name "$MODULE" "$SOURCE_IMAGE_NAME")"
    cat << EOF >> stackgres-k8s/ci/build/target/junit-build.hashes.xml
    <testcase classname="module $MODULE" name="${IMAGE_NAME##*:hash-}" />
EOF
    printf '%s\n' "$IMAGE_NAME" > "stackgres-k8s/ci/build/target/image-hashes.$MODULE"
    printf '%s\n' "$IMAGE_NAME" >> "stackgres-k8s/ci/build/target/$MODULE_TYPE-image-hashes"
    printf '%s=%s\n' "$MODULE" "$IMAGE_NAME" >> stackgres-k8s/ci/build/target/image-hashes
    printf '%s\n' "$SOURCE_IMAGE_NAME" >> stackgres-k8s/ci/build/target/all-images
    printf '%s\n' "$IMAGE_NAME" >> stackgres-k8s/ci/build/target/all-images
  done
  while IFS='=' read -r MODULE IMAGE_NAME
  do
    printf ' - %s => %s\n' "$MODULE" "$IMAGE_NAME"
  done < stackgres-k8s/ci/build/target/image-hashes

  echo "done"

  echo

  echo "Calculating image type hashes ..."

  for MODULE_TYPE_IMAGE_HASHES in stackgres-k8s/ci/build/target/*-image-hashes
  do
    local MODULE_TYPE="${MODULE_TYPE_IMAGE_HASHES##*/}"
    MODULE_TYPE="${MODULE_TYPE%-image-hashes}"
    local MODULE_TYPE_HASH
    MODULE_TYPE_HASH="$(md5sum "$MODULE_TYPE_IMAGE_HASHES" | cut -d ' ' -f 1 | tr -d '\n')"
    printf " - %s hash => %s\n" "$MODULE_TYPE" "$MODULE_TYPE_HASH"
    cat << EOF >> stackgres-k8s/ci/build/target/junit-build.hashes.xml
    <testcase classname="module type $MODULE_TYPE" name="$MODULE_TYPE_HASH" />
EOF
  done

  echo "done"

  echo

  cat << EOF >> stackgres-k8s/ci/build/target/junit-build.hashes.xml
  </testsuite>
</testsuites>
EOF

  project_hash > stackgres-k8s/ci/build/target/project_hash
}

retrieve_image_digests() {
  echo "Retrieving image digests ..."
  sort stackgres-k8s/ci/build/target/all-images | uniq \
    | xargs -I @ -P 16 sh -c \
      "$(
      echo "$-" | grep -v -q x || printf 'set -x\n'
      retrieve_image_hash_script @
      )"
  (! ls stackgres-k8s/ci/build/target/image-digests.* > /dev/null 2>&1 \
    || cat stackgres-k8s/ci/build/target/image-digests.*) \
    > stackgres-k8s/ci/build/target/image-digests
  sort stackgres-k8s/ci/build/target/all-images | uniq \
    | while read -r IMAGE_NAME
      do
        printf ' - %s => %s\n' "$IMAGE_NAME" "$( (grep "^$IMAGE_NAME=" stackgres-k8s/ci/build/target/image-digests || echo '=<not found>') | cut -d = -f 2-)"
      done
  echo "done"

  echo
}

extract() {
  [ "$#" -ge 2 ] || false
  local MODULE="$1"
  shift
  set_module_functions "$MODULE"
  IMAGE_NAME="$(image_name "$MODULE")"
  extract_from_image "$IMAGE_NAME" "$@"
}

extract_from_image() {
  [ "$#" -ge 2 ] || false
  local IMAGE_NAME="$1"
  shift
  docker_run --rm --entrypoint /bin/sh -u "$(id -u)" -v "$(pwd):/out" "$IMAGE_NAME" \
    -c "$(cat << EOF
for FILE in $*
do
  if [ -d "\$FILE" ]
  then
    mkdir -p "/out/\$FILE"
    cp -rf "\$FILE/." "/out/\$FILE"
  elif [ -e "\$FILE" ]
  then
    mkdir -p "/out/\${FILE%/*}"
    cp -f "\$FILE" "/out/\$FILE"
  fi
done
EOF
      )"
}

if [ "$(basename "$0")" = "build-functions.sh" ] && [ "$#" -ge 1 ]
then
  "$@"
fi

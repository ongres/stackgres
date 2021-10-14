#!/bin/sh

[ "$DEBUG" != true ] || set -x

message_and_exit() {
  echo "\n\t$1\n\n"
  exit $2
}

command -v xargs > /dev/null || message_and_exit 'The program `xargs` is required to be in PATH' 8
command -v ls > /dev/null || message_and_exit 'The program `ls` is required to be in PATH' 8
command -v jq > /dev/null || message_and_exit 'The program `jq` is required to be in PATH' 8
command -v yq > /dev/null || message_and_exit 'The program `yq` (https://kislyuk.github.io/yq/) is required to be in PATH' 8
command -v md5sum > /dev/null || message_and_exit 'The program `md5sum` is required to be in PATH' 8
command -v find > /dev/null || message_and_exit 'The program `find` is required to be in PATH' 8
command -v java > /dev/null || message_and_exit 'The program `java` is required to be in PATH' 8
command -v docker > /dev/null || message_and_exit 'The program `docker` is required to be in PATH' 8
docker manifest > /dev/null 2>&1 || message_and_exit '`docker manifest` is not working' 9

export LC_ALL=C

cd "$(dirname "$0")/../../.."

#
# Java helper functions
#

java_module_image_name() {
  [ -n "$1" -a -n "$2" ]
  local MODULE="$1"
  local SOURCE_IMAGE_NAME="$2"
  local MODULE_PATH
  local MODULE_SOURCES
  local MODULE_HASH
  MODULE_PATH="$(jq -r ".modules[\"$MODULE\"].path" stackgres-k8s/ci/build/target/config.json)"
  MODULE_SOURCES="$(module_list_of_files "$MODULE" sources "[ \"$MODULE_PATH/pom.xml\", \"$MODULE_PATH/src\" ]")"
  MODULE_ARTIFACTS="$(module_list_of_files "$MODULE" artifacts)"
  for MODULE_SOURCE in $MODULE_SOURCES
  do
    [ -f "$MODULE_SOURCE" ] || [ -d "$MODULE_SOURCE" ] \
      || message_and_exit "$MODULE_SOURCE not found or type not supported for module $MODULE" 1
  done
  MODULE_HASH="$( (
    echo "$SOURCE_IMAGE_NAME"
    cat stackgres-k8s/ci/build/Dockerfile-java
    jq -r ".modules[\"$MODULE\"]" stackgres-k8s/ci/build/target/config.json
    jq -r '.maven_opts' stackgres-k8s/ci/build/target/config.json
    jq -r '.maven_cli_opts' stackgres-k8s/ci/build/target/config.json
    cat_files $MODULE_SOURCES
    echo "$MODULE_ARTIFACTS"
    ) | md5sum | cut -d ' ' -f 1)"
  printf 'registry.gitlab.com/ongresinc/stackgres/build/%s:hash-%s\n' "$MODULE" "$MODULE_HASH"
}

build_java_image() {
  [ -n "$1" -a -n "$2" -a -n "$3" ]
  local MODULE="$1"
  local SOURCE_IMAGE_NAME="$2"
  local IMAGE_NAME="$3"
  local MODULE_PATH
  local PRE_BUILD_COMMANDS
  local POST_BUILD_COMMANDS
  local MAVEN_OPTS
  local MAVEN_CLI_OPTS
  MODULE_PATH="$(jq -r ".modules[\"$MODULE\"].path" stackgres-k8s/ci/build/target/config.json)"
  PRE_BUILD_COMMANDS="$(jq -r ".modules[\"$MODULE\"].pre_build_commands | if . != null then . | join(\"\n\") else true end" stackgres-k8s/ci/build/target/config.json)"
  POST_BUILD_COMMANDS="$(jq -r ".modules[\"$MODULE\"].post_build_commands | if . != null then . | join(\"\n\") else true end" stackgres-k8s/ci/build/target/config.json)"
  MAVEN_OPTS="$(jq -r '.maven_opts' stackgres-k8s/ci/build/target/config.json)"
  MAVEN_CLI_OPTS="$(jq -r '.maven_cli_opts' stackgres-k8s/ci/build/target/config.json)"
  MODULE_ARTIFACTS="$(module_list_of_files "$MODULE" artifacts)"
  (
  cat stackgres-k8s/ci/build/Dockerfile-java
  for MODULE_ARTIFACT in $MODULE_ARTIFACTS
  do
    echo "  COPY --from=build /project/$MODULE_ARTIFACT /project/$MODULE_ARTIFACT"
  done
  ) > "stackgres-k8s/ci/build/target/Dockerfile.$MODULE"
  docker_build $DOCKER_BUILD_OPTS -t "$IMAGE_NAME" \
    --build-arg "UID=$UID" \
    --build-arg "IMAGE_NAME=$IMAGE_NAME" \
    --build-arg "SOURCE_IMAGE_NAME=$SOURCE_IMAGE_NAME" \
    --build-arg "PRE_BUILD_COMMANDS=$PRE_BUILD_COMMANDS" \
    --build-arg "POST_BUILD_COMMANDS=$POST_BUILD_COMMANDS" \
    --build-arg "MODULE_PATH=$MODULE_PATH" \
    --build-arg "MAVEN_OPTS=$MAVEN_OPTS" \
    --build-arg "MAVEN_CLI_OPTS=$MAVEN_CLI_OPTS" \
    --build-arg "MODULE_PACKAGE_PATH=io/stackgres" \
    --build-arg "SHELL_XTRACE=$([ "$DEBUG" != true ] || printf '%s' -x)" \
    -f "stackgres-k8s/ci/build/target/Dockerfile.$MODULE" .
}

#
# Web helper functions
#

web_module_image_name() {
  [ -n "$1" -a -n "$2" ]
  local MODULE="$1"
  local SOURCE_IMAGE_NAME="$2"
  local MODULE_PATH
  local MODULE_SOURCES
  local MODULE_HASH
  MODULE_PATH="$(jq -r ".modules[\"$MODULE\"].path" stackgres-k8s/ci/build/target/config.json)"
  MODULE_SOURCES="$(module_list_of_files "$MODULE" sources)"
  MODULE_ARTIFACTS="$(module_list_of_files "$MODULE" artifacts)"
  for MODULE_SOURCE in $MODULE_SOURCES
  do
    [ -f "$MODULE_SOURCE" ] || [ -d "$MODULE_SOURCE" ] \
      || message_and_exit "$MODULE_SOURCE not found or type not supported for module $MODULE" 1
  done
  MODULE_HASH="$( (
    echo "$SOURCE_IMAGE_NAME"
    cat stackgres-k8s/ci/build/Dockerfile-web
    jq -r ".modules[\"$MODULE\"]" stackgres-k8s/ci/build/target/config.json
    grep '<artifactId>stackgres-parent</artifactId>' "stackgres-k8s/src/pom.xml" -A 2 -B 2 \
      | sed -n 's/^.*<version>\([^<]\+\)<\/version>.*$/\1/p'
    cat_files $MODULE_SOURCES
    echo "$MODULE_ARTIFACTS"
    ) | md5sum | cut -d ' ' -f 1)"
  printf 'registry.gitlab.com/ongresinc/stackgres/build/%s:hash-%s' "$MODULE" "$MODULE_HASH"
}

build_web_image() {
  [ -n "$1" -a -n "$2" -a -n "$3" ]
  local MODULE="$1"
  local SOURCE_IMAGE_NAME="$2"
  local IMAGE_NAME="$3"
  local MODULE_PATH
  local BUILD_COMMANDS
  MODULE_PATH="$(jq -r ".modules[\"$MODULE\"].path" stackgres-k8s/ci/build/target/config.json)"
  BUILD_COMMANDS="$(jq -r ".modules[\"$MODULE\"].build_commands | join(\"\n\")" stackgres-k8s/ci/build/target/config.json)"
  MODULE_ARTIFACTS="$(module_list_of_files "$MODULE" artifacts)"
  (
  cat stackgres-k8s/ci/build/Dockerfile-web
  for MODULE_ARTIFACT in $MODULE_ARTIFACTS
  do
    echo "  COPY --from=build /project/$MODULE_ARTIFACT /project/$MODULE_ARTIFACT"
  done
  ) > "stackgres-k8s/ci/build/target/Dockerfile.$MODULE"
  docker_build $DOCKER_BUILD_OPTS -t "$IMAGE_NAME" \
    --build-arg "UID=$UID" \
    --build-arg "SOURCE_IMAGE_NAME=$SOURCE_IMAGE_NAME" \
    --build-arg "IMAGE_NAME=$IMAGE_NAME" \
    --build-arg "BUILD_COMMANDS=$BUILD_COMMANDS" \
    --build-arg "MODULE_PATH=$MODULE_PATH" \
    --build-arg "SHELL_XTRACE=$([ "$DEBUG" != true ] || printf '%s' -x)" \
    -f "stackgres-k8s/ci/build/target/Dockerfile.$MODULE" .
}

#
# JVM image helper functions
#

jvm_image_module_image_name() {
  [ -n "$1" -a -n "$2" ]
  local MODULE="$1"
  local SOURCE_IMAGE_NAME="$2"
  local MODULE_NAME
  local MODULE_PATH
  local MODULE_HASH
  MODULE_NAME="$(jq -r ".modules[\"$MODULE\"].name" stackgres-k8s/ci/build/target/config.json)"
  MODULE_PATH="$(jq -r ".modules[\"$MODULE\"].path" stackgres-k8s/ci/build/target/config.json)"
  MODULE_HASH="$( (
    echo "$SOURCE_IMAGE_NAME"
    cat stackgres-k8s/ci/build/Dockerfile-jvm-image
    cat "$MODULE_PATH/src/main/docker/Dockerfile.jvm"
    cat "$MODULE_PATH/src/main/docker/stackgres-$MODULE_NAME.jvm.sh"
    ) | md5sum | cut -d ' ' -f 1)"
  printf 'registry.gitlab.com/ongresinc/stackgres/build/%s:hash-%s\n' "$MODULE" "$MODULE_HASH"
}

build_jvm_image_image() {
  [ -n "$1" -a -n "$2" -a -n "$3" ]
  local MODULE="$1"
  local SOURCE_IMAGE_NAME="$2"
  local IMAGE_NAME="$3"
  local MODULE_PATH
  local BASE_IMAGE
  MODULE_PATH="$(jq -r ".modules[\"$MODULE\"].path" stackgres-k8s/ci/build/target/config.json)"
  BASE_IMAGE="$(jq -r ".base_jvm_image" stackgres-k8s/ci/build/target/config.json)"
  (
  cat stackgres-k8s/ci/build/Dockerfile-jvm-image
  sed "s#^\( *COPY\) \+'#\1 --from=source '/project/$MODULE_PATH/#" \
    "$MODULE_PATH/src/main/docker/Dockerfile.jvm"
  ) > "stackgres-k8s/ci/build/target/Dockerfile.$MODULE"
  pwd
  docker_build $DOCKER_BUILD_OPTS -t "$IMAGE_NAME" \
    --build-arg "UID=$UID" \
    --build-arg "SOURCE_IMAGE_NAME=$SOURCE_IMAGE_NAME" \
    --build-arg "BASE_IMAGE=$BASE_IMAGE" \
    --build-arg "MODULE_PATH=$MODULE_PATH" \
    -f "stackgres-k8s/ci/build/target/Dockerfile.$MODULE" .
}

#
# Image helper functions
#

image_module_image_name() {
  [ -n "$1" -a -n "$2" ]
  local MODULE="$1"
  local SOURCE_IMAGE_NAME="$2"
  local MODULE_PATH
  local MODULE_HASH
  MODULE_PATH="$(jq -r ".modules[\"$MODULE\"].path" stackgres-k8s/ci/build/target/config.json)"
  MODULE_HASH="$( (
    echo "$SOURCE_IMAGE_NAME"
    cat stackgres-k8s/ci/build/Dockerfile-image
    cat "$MODULE_PATH/docker/Dockerfile"
    ) | md5sum | cut -d ' ' -f 1)"
  printf 'registry.gitlab.com/ongresinc/stackgres/build/%s:hash-%s\n' "$MODULE" "$MODULE_HASH"
}

build_image_image() {
  [ -n "$1" -a -n "$2" -a -n "$3" ]
  local MODULE="$1"
  local SOURCE_IMAGE_NAME="$2"
  local IMAGE_NAME="$3"
  local MODULE_PATH
  local BASE_IMAGE
  MODULE_PATH="$(jq -r ".modules[\"$MODULE\"].path" stackgres-k8s/ci/build/target/config.json)"
  BASE_IMAGE="$(jq -r ".modules[\"$MODULE\"].base_image" stackgres-k8s/ci/build/target/config.json)"
  (
  cat stackgres-k8s/ci/build/Dockerfile-image
  sed "s#^\( *COPY\) \+'#\1 --from=source '/project/$MODULE_PATH/#" \
    "$MODULE_PATH/docker/Dockerfile"
  ) > "stackgres-k8s/ci/build/target/Dockerfile.$MODULE"
  docker_build $DOCKER_BUILD_OPTS -t "$IMAGE_NAME" \
    --build-arg "UID=$UID" \
    --build-arg "SOURCE_IMAGE_NAME=$SOURCE_IMAGE_NAME" \
    --build-arg "BASE_IMAGE=$BASE_IMAGE" \
    --build-arg "MODULE_PATH=$MODULE_PATH" \
    -f "stackgres-k8s/ci/build/target/Dockerfile.$MODULE" .
}

#
# Native helper functions
#

native_module_image_name() {
  [ -n "$1" -a -n "$2" ]
  local MODULE="$1"
  local SOURCE_IMAGE_NAME="$2"
  local MODULE_NAME
  local MODULE_HASH
  MODULE_NAME="$(jq -r ".modules[\"$MODULE\"].name" stackgres-k8s/ci/build/target/config.json)"
  MODULE_HASH="$( (
    echo "$SOURCE_IMAGE_NAME"
    cat stackgres-k8s/ci/build/Dockerfile-native
    jq -r ".modules[\"$MODULE\"]" stackgres-k8s/ci/build/target/config.json
    jq -r '.maven_opts' stackgres-k8s/ci/build/target/config.json
    jq -r '.maven_cli_opts' stackgres-k8s/ci/build/target/config.json
    ) | md5sum | cut -d ' ' -f 1)"
  printf 'registry.gitlab.com/ongresinc/stackgres/build/%s:hash-%s\n' "$MODULE" "$MODULE_HASH"
}

build_native_image() {
  [ -n "$1" -a -n "$2" -a -n "$3" ]
  local MODULE="$1"
  local SOURCE_IMAGE_NAME="$2"
  local IMAGE_NAME="$3"
  local MODULE_NAME
  local MODULE_PATH
  local PRE_BUILD_COMMANDS
  local POST_BUILD_COMMANDS
  local MAVEN_OPTS
  local MAVEN_CLI_OPTS
  MODULE_NAME="$(jq -r ".modules[\"$MODULE\"].name" stackgres-k8s/ci/build/target/config.json)"
  MODULE_PATH="$(jq -r ".modules[\"$MODULE\"].path" stackgres-k8s/ci/build/target/config.json)"
  PRE_BUILD_COMMANDS="$(jq -r ".modules[\"$MODULE\"].pre_build_commands | if . != null then . | join(\"\n\") else true end" stackgres-k8s/ci/build/target/config.json)"
  POST_BUILD_COMMANDS="$(jq -r ".modules[\"$MODULE\"].post_build_commands | if . != null then . | join(\"\n\") else true end" stackgres-k8s/ci/build/target/config.json)"
  MAVEN_OPTS="$(jq -r '.maven_opts' stackgres-k8s/ci/build/target/config.json)"
  MAVEN_CLI_OPTS="$(jq -r '.maven_cli_opts' stackgres-k8s/ci/build/target/config.json)"
  MODULE_ARTIFACTS="$(module_list_of_files "$MODULE" artifacts)"
  (
  cat stackgres-k8s/ci/build/Dockerfile-native
  for MODULE_ARTIFACT in $MODULE_ARTIFACTS
  do
    echo "  COPY --from=build /project/$MODULE_ARTIFACT /project/$MODULE_ARTIFACT"
  done
  ) > "stackgres-k8s/ci/build/target/Dockerfile.$MODULE"
  docker_build $DOCKER_BUILD_OPTS -t "$IMAGE_NAME" \
    --build-arg "UID=$UID" \
    --build-arg "IMAGE_NAME=$IMAGE_NAME" \
    --build-arg "SOURCE_IMAGE_NAME=$SOURCE_IMAGE_NAME" \
    --build-arg "PRE_BUILD_COMMANDS=$PRE_BUILD_COMMANDS" \
    --build-arg "POST_BUILD_COMMANDS=$POST_BUILD_COMMANDS" \
    --build-arg "MODULE_PATH=$MODULE_PATH" \
    --build-arg "MODULE_NAME=$MODULE_NAME" \
    --build-arg "MAVEN_OPTS=$MAVEN_OPTS" \
    --build-arg "MAVEN_CLI_OPTS=$MAVEN_CLI_OPTS" \
    --build-arg "SHELL_XTRACE=$([ "$DEBUG" != true ] || printf '%s' -x)" \
    -f "stackgres-k8s/ci/build/target/Dockerfile.$MODULE" .
}

#
# Native image helper functions
#

native_image_module_image_name() {
  [ -n "$1" -a -n "$2" ]
  local MODULE="$1"
  local SOURCE_IMAGE_NAME="$2"
  local MODULE_NAME
  local MODULE_PATH
  local MODULE_HASH
  MODULE_NAME="$(jq -r ".modules[\"$MODULE\"].name" stackgres-k8s/ci/build/target/config.json)"
  MODULE_PATH="$(jq -r ".modules[\"$MODULE\"].path" stackgres-k8s/ci/build/target/config.json)"
  MODULE_HASH="$( (
    echo "$SOURCE_IMAGE_NAME"
    cat "$MODULE_PATH/src/main/docker/Dockerfile.native"
    cat stackgres-k8s/ci/build/Dockerfile-native-image
    cat "$MODULE_PATH/src/main/docker/stackgres-$MODULE_NAME.native.sh"
    ) | md5sum | cut -d ' ' -f 1)"
  printf 'registry.gitlab.com/ongresinc/stackgres/build/%s:hash-%s\n' "$MODULE" "$MODULE_HASH"
}

build_native_image_image() {
  [ -n "$1" -a -n "$2" -a -n "$3" ]
  local MODULE="$1"
  local SOURCE_IMAGE_NAME="$2"
  local IMAGE_NAME="$3"
  local MODULE_PATH
  local BASE_IMAGE
  MODULE_PATH="$(jq -r ".modules[\"$MODULE\"].path" stackgres-k8s/ci/build/target/config.json)"
  BASE_IMAGE="$(jq -r ".base_native_image" stackgres-k8s/ci/build/target/config.json)"
  (
  cat stackgres-k8s/ci/build/Dockerfile-native-image
  sed "s#^\( *COPY\) \+'#\1 --from=source '/project/$MODULE_PATH/#" \
    "$MODULE_PATH/src/main/docker/Dockerfile.native"
  ) > "stackgres-k8s/ci/build/target/Dockerfile.$MODULE"
  docker_build $DOCKER_BUILD_OPTS -t "$IMAGE_NAME" \
    --build-arg "UID=$UID" \
    --build-arg "SOURCE_IMAGE_NAME=$SOURCE_IMAGE_NAME" \
    --build-arg "BASE_IMAGE=$BASE_IMAGE" \
    --build-arg "MODULE_PATH=$MODULE_PATH" \
    -f "stackgres-k8s/ci/build/target/Dockerfile.$MODULE" .
}

#
# Resource helper functions
#

resource_module_image_name() {
  [ -n "$1" -a -n "$2" ]
  local MODULE="$1"
  local SOURCE_IMAGE_NAME="$2"
  local MODULE_SOURCES
  local MODULE_HASH
  MODULE_SOURCES="$(module_list_of_files "$MODULE" sources)"
  MODULE_ARTIFACTS="$(module_list_of_files "$MODULE" artifacts)"
  for MODULE_SOURCE in $MODULE_SOURCES
  do
    [ -f "$MODULE_SOURCE" ] || [ -d "$MODULE_SOURCE" ] \
      || message_and_exit "$MODULE_SOURCE not found or type not supported for module $MODULE" 1
  done
  MODULE_HASH="$( (
    echo "$SOURCE_IMAGE_NAME"
    cat stackgres-k8s/ci/build/Dockerfile-resource
    cat_files $MODULE_SOURCES
    echo "$MODULE_ARTIFACTS"
    ) | md5sum | cut -d ' ' -f 1)"
  printf 'registry.gitlab.com/ongresinc/stackgres/build/%s:hash-%s' "$MODULE" "$MODULE_HASH"
}

build_resource_image() {
  [ -n "$1" -a -n "$2" -a -n "$3" ]
  local MODULE="$1"
  local SOURCE_IMAGE_NAME="$2"
  local IMAGE_NAME="$3"
  local BUILD_IMAGE_NAME
  local BUILD_COMMANDS
  local MODULE_TARGET
  BUILD_IMAGE_NAME="$(jq -r ". as \$doc | .modules[\"$MODULE\"].build_image | if . != null then . else \$doc.base_image end" stackgres-k8s/ci/build/target/config.json)"
  BUILD_COMMANDS="$(jq -r ".modules[\"$MODULE\"].build_commands | join(\"\n\")" stackgres-k8s/ci/build/target/config.json)"
  MODULE_TARGET="$(jq -r ".modules[\"$MODULE\"].target" stackgres-k8s/ci/build/target/config.json)"
  MODULE_ARTIFACTS="$(module_list_of_files "$MODULE" artifacts)"
  (
  cat stackgres-k8s/ci/build/Dockerfile-resource
  for MODULE_ARTIFACT in $MODULE_ARTIFACTS
  do
    echo "  COPY --from=build /project/$MODULE_ARTIFACT /project/$MODULE_ARTIFACT"
  done
  ) > "stackgres-k8s/ci/build/target/Dockerfile.$MODULE"
  docker_build $DOCKER_BUILD_OPTS -t "$IMAGE_NAME" \
    --build-arg "UID=$UID" \
    --build-arg "IMAGE_NAME=$IMAGE_NAME" \
    --build-arg "SOURCE_IMAGE_NAME=$SOURCE_IMAGE_NAME" \
    --build-arg "BUILD_IMAGE_NAME=$BUILD_IMAGE_NAME" \
    --build-arg "BUILD_COMMANDS=$BUILD_COMMANDS" \
    --build-arg "MODULE_TARGET=$MODULE_TARGET" \
    --build-arg "SHELL_XTRACE=$([ "$DEBUG" != true ] || printf '%s' -x)" \
    -f "stackgres-k8s/ci/build/target/Dockerfile.$MODULE" .
}

#
# Other helper functions
#

module_list_of_files() {
  [ -n "$1" -a -n "$2" ]
  local MODULE="$1"
  local MODULE_FILES_PATH="$2"
  local MODULE_FILES
  MODULE_FILES="$(
    if [ -n "$3" ]
    then
      jq -r ".modules[\"$MODULE\"][\"$MODULE_FILES_PATH\"] | if . != null then . else $3 end | .[]" stackgres-k8s/ci/build/target/config.json
    else
      jq -r ".modules[\"$MODULE\"][\"$MODULE_FILES_PATH\"][]" stackgres-k8s/ci/build/target/config.json
    fi
    )"
  printf '%s' "$MODULE_FILES"
}

cat_files() {
  local MODULE_FILE
  for MODULE_FILE in "$@"
  do
    if [ -f "$MODULE_FILE" ]
    then
      cat "$MODULE_FILE"
    elif [ -d "$MODULE_FILE" ]
    then
      find "$MODULE_FILE" -type f | sort | xargs -I @ cat '@'
    fi
  done
}

docker_build() {
  (
  set -x
  docker build "$@"
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
  [ -n "$1" ]
  local MODULE="$1"
  MODULE_TYPE="$(jq -r ".modules | select(has(\"$MODULE\"))[\"$MODULE\"] | select(has(\"type\")).type" stackgres-k8s/ci/build/target/config.json)"
  [ -n "$MODULE_TYPE" ] || message_and_exit "Module $MODULE is not defined or has no type in stackgres-k8s/ci/build/config.yml" 1
  MODULE_TYPE_PREFIX="$(printf '%s' "$MODULE_TYPE" | tr '-' '_')"
  MODULE_IMAGE_NAME_FUNCTION="${MODULE_TYPE_PREFIX}_module_image_name"
  MODULE_BUILD_FUNCTION="build_${MODULE_TYPE_PREFIX}_image"
}

source_image_name() {
  [ -n "$1" ]
  local MODULE="$1"
  local SOURCE_MODULE
  local SOURCE_IMAGE_NAME
  SOURCE_MODULE="$(jq -r ".stages[] | select(has(\"$MODULE\"))[\"$MODULE\"]" stackgres-k8s/ci/build/target/config.json)"
  [ -n "$SOURCE_MODULE" ] || message_and_exit "Module $MODULE has no stage defined in stackgres-k8s/ci/build/config.yml" 1
  if [ "$SOURCE_MODULE" = null ]
  then
    SOURCE_IMAGE_NAME="$(jq -r ".base_image" stackgres-k8s/ci/build/target/config.json)"
  else
    SOURCE_IMAGE_NAME="$(image_name "$SOURCE_MODULE")"
  fi
  printf '%s\n' "$SOURCE_IMAGE_NAME"
}

image_name() {
  [ -n "$1" ]
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
  [ -n "$1" ]
  local MODULE="$1"
  local IMAGE_NAME
  local SOURCE_IMAGE_NAME
  local BUILD_IMAGE_ID
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
    if [ "$DO_BUILD" != true ] && docker inspect "$IMAGE_NAME" >/dev/null 2>&1
    then
      echo "Already exists locally. Skipping build ..."
    else
      echo "Building $MODULE ..."
      docker images --filter "label=build-of=$IMAGE_NAME" --format '{{.ID}}' \
        | while read ID
          do
            docker rmi "$ID"
          done
      set +e
      (
      set -e
      "$MODULE_BUILD_FUNCTION" "$MODULE" "$SOURCE_IMAGE_NAME" "$IMAGE_NAME"
      )
      BUILD_EXIT_CODE="$?"
      set -e
      MODULE_CACHE="$(module_list_of_files "$MODULE" cache "[]")"
      if [ -n "$MODULE_CACHE" ]
      then
        BUILD_IMAGE_ID="$(docker images --filter "label=build-of=$IMAGE_NAME" --format '{{.ID}}')"
        extract_from_image "$BUILD_IMAGE_ID" $MODULE_CACHE
      fi
      if [ "$BUILD_EXIT_CODE" != 0 ]
      then
        return "$BUILD_EXIT_CODE"
      fi
    fi
    if [ "$SKIP_PUSH" != true ]
    then
      docker push "$IMAGE_NAME"
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
    IMAGE_NAME="$("$MODULE_IMAGE_NAME_FUNCTION" "$MODULE" "$SOURCE_IMAGE_NAME")"
    cat << EOF >> stackgres-k8s/ci/build/target/junit-build.hashes.xml
    <testcase classname="module $MODULE" name="${IMAGE_NAME##*:hash-}" />
EOF
    printf '%s\n' "$IMAGE_NAME" > "stackgres-k8s/ci/build/target/image-hashes.$MODULE"
    printf '%s\n' "$IMAGE_NAME" >> "stackgres-k8s/ci/build/target/$MODULE_TYPE-image-hashes"
    printf '%s=%s\n' "$MODULE" "$IMAGE_NAME" >> stackgres-k8s/ci/build/target/image-hashes
    printf '%s\n' "$SOURCE_IMAGE_NAME" >> stackgres-k8s/ci/build/target/all-images
    printf '%s\n' "$IMAGE_NAME" >> stackgres-k8s/ci/build/target/all-images
  done
  cat stackgres-k8s/ci/build/target/image-hashes \
    | while IFS== read MODULE IMAGE_NAME
      do
        printf ' - %s => %s\n' "$MODULE" "$IMAGE_NAME"
      done
  echo "done"

  echo

  echo "Calculating image type hashes ..."

  for MODULE_TYPE_IMAGE_HASHES in stackgres-k8s/ci/build/target/*-image-hashes
  do
    local MODULE_TYPE="${MODULE_TYPE_IMAGE_HASHES##*/}"
    MODULE_TYPE="${MODULE_TYPE%-image-hashes}"
    local MODULE_TYPE_HASH="$(md5sum "$MODULE_TYPE_IMAGE_HASHES" | cut -d ' ' -f 1 | tr -d '\n')"
    printf " - %s hash => %s\n" "$MODULE_TYPE" "$MODULE_TYPE_HASH"
    cat << EOF >> stackgres-k8s/ci/build/target/junit-build.hashes.xml
    <testcase classname="module type $MODULE_TYPE" name="$MODULE_TYPE_HASH" />
EOF
  done

  echo "done"

  cat << EOF >> stackgres-k8s/ci/build/target/junit-build.hashes.xml
  </testsuite>
</testsuites>
EOF

  echo
}

retrieve_image_digests() {
  echo "Retrieving image digests ..."
  cat stackgres-k8s/ci/build/target/all-images | sort | uniq \
    | xargs -I @ -P 16 sh -c $(echo "$-" | grep -v -q x || printf '%s' '-x') \
      "$(retrieve_image_hash_script @)"
  (! ls stackgres-k8s/ci/build/target/image-digests.* > /dev/null 2>&1 \
    || cat stackgres-k8s/ci/build/target/image-digests.*) \
    > stackgres-k8s/ci/build/target/image-digests
  cat stackgres-k8s/ci/build/target/all-images | sort | uniq \
    | while read IMAGE_NAME
      do
        printf ' - %s => %s\n' "$IMAGE_NAME" "$( (grep "^$IMAGE_NAME=" stackgres-k8s/ci/build/target/image-digests || echo '=<not found>') | cut -d = -f 2-)"
      done
  echo "done"

  echo
}

extract() {
  [ -n "$1" -a -n "$2" ]
  local MODULE="$1"
  shift
  set_module_functions "$MODULE"
  IMAGE_NAME="$(image_name "$MODULE")"
  extract_from_image "$IMAGE_NAME" "$@"
}

extract_from_image() {
  [ -n "$1" -a -n "$2" ]
  local IMAGE_NAME="$1"
  shift
  docker run --rm --entrypoint /bin/sh -u "$(id -u)" -v "$(pwd):/out" "$IMAGE_NAME" \
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

if [ "$(basename "$0")" = "build-functions.sh" ] && [ -n "$1" ]
then
  "$@"
fi

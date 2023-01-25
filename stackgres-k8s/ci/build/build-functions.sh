#!/bin/sh
# shellcheck disable=SC2039
# shellcheck disable=SC2016

BUILDER_VERSION=1.0.0

[ "$DEBUG" != true ] || set -x

message_and_exit() {
  printf '\n\t%s\n\n' "$1" >&2
  exit "$2"
}

export LANG=C.UTF-8
export LC_ALL=C.UTF-8

cd "$(dirname "$0")/../../.." || message_and_exit "Can not change to directory $(dirname "$0")/../../.."

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
  if "$MODULE_PLATFORM_DEPENDENT"
  then
    printf 'registry.gitlab.com/ongresinc/stackgres/build/%s:hash-%s-%s\n' \
      "$MODULE" "$MODULE_HASH" "${MODULE_PLATFORM:-$(get_platform)}"
  else
    printf 'registry.gitlab.com/ongresinc/stackgres/build/%s:hash-%s\n' \
      "$MODULE" "$MODULE_HASH"
  fi
}

copy_from_image() {
  [ "$#" -ge 1 ] || false
  local SOURCE_IMAGE_NAME="$1"
  local SOURCE_IMAGE_PLATFORM
  if [ "$SOURCE_IMAGE_NAME" = null ]
  then
    return
  fi
  SOURCE_IMAGE_PLATFORM="$(get_image_platform "$SOURCE_IMAGE_NAME")"
  # shellcheck disable=SC2046
  docker_run -i $(! test -t 1 || printf '%s' '-t') --rm \
    --platform "$SOURCE_IMAGE_PLATFORM" \
    --volume "$(pwd):/project-target" \
    --user "$BUILD_UID" \
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
  run_commands_in_container "$MODULE" "$BUILD_IMAGE_NAME" "$BUILD_UID" "$COMMANDS"
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
          | map(\"export \" + .key + \"=\\\"\" + .value + \"\\\"\")
          | .[]" stackgres-k8s/ci/build/target/config.json)
EOF
   "  > "stackgres-k8s/ci/build/target/$MODULE-build-env"
  # shellcheck disable=SC2046
  docker_run -i $(! test -t 1 || printf '%s' '-t') --rm \
    --volume "/var/run/docker.sock:/var/run/docker.sock" \
    --volume "$(pwd):/project" \
    --workdir /project \
    --user "$BUILD_UID" \
    --env "PRE_BUILD_COMMANDS=$PRE_BUILD_COMMANDS" \
    --env "BUILD_COMMANDS=$BUILD_COMMANDS" \
    --env "POST_BUILD_COMMANDS=$POST_BUILD_COMMANDS" \
    --env "MODULE_PATH=$MODULE_PATH" \
    --env "SHELL_XTRACE=$([ "$DEBUG" != true ] || printf '%s' -x)" \
    --entrypoint /bin/sh \
    "$BUILD_IMAGE_NAME" \
    -ec $(echo "$-" | grep -v -q x || printf '%s' '-x') "
      $(cat "stackgres-k8s/ci/build/target/$MODULE-build-env")
      $COMMANDS"
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
    build_in_container "$MODULE" "$BUILD_IMAGE_NAME"
    post_build_in_container "$MODULE" "$BUILD_IMAGE_NAME"
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
    --build-arg "BUILD_UID=$BUILD_UID" \
    --build-arg "TARGET_IMAGE_NAME=$TARGET_IMAGE_NAME" \
    $(jq -r ".modules[\"$MODULE\"].dockerfile.args
        | if . != null then . else {} end | to_entries
        | map(.key + \" \" + .value + \"\")[]" stackgres-k8s/ci/build/target/config.json \
      | while read -r KEY VALUE
        do
          printf ' %s %s=%s' '--build-arg' "$KEY" "$(eval "printf '%s' \"$VALUE\"")"
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
  printf '%s' "$MODULE_FILES"
}

init_hash() {
  if [ -d stackgres-k8s/ci/build/target/.git ] \
    && git --git-dir stackgres-k8s/ci/build/target/.git status --porcelain | grep -q .
  then
    rm -rf stackgres-k8s/ci/build/target/.git
  fi
  if ! [ -d stackgres-k8s/ci/build/target/.git ]
  then
    (
    set +e
    (
    set -e
    git --git-dir stackgres-k8s/ci/build/target/.git init -q
    git --git-dir stackgres-k8s/ci/build/target/.git add .
    git --git-dir stackgres-k8s/ci/build/target/.git \
      -c user.name=ci -c user.email= commit -q -m "build hash" --no-gpg-sign
    )
    EXIT_CODE="$?"
    if [ "$EXIT_CODE" != 0 ]
    then
      rm -rf stackgres-k8s/ci/build/target/.git
    fi
    exit "$EXIT_CODE"
    )
  fi
}

module_type() {
  [ "$#" -ge 1 ] || false
  local MODULE="$1"
  local MODULE_TYPE
  MODULE_TYPE="$(jq -r ".modules | select(has(\"$MODULE\"))[\"$MODULE\"] | select(has(\"type\")).type" stackgres-k8s/ci/build/target/config.json)"
  [ -n "$MODULE_TYPE" ] || message_and_exit "Module $MODULE is not defined or has no type in stackgres-k8s/ci/build/config.yml" 1
  printf '%s' "$MODULE_TYPE"
}

source_image_name() {
  [ "$#" -ge 1 ] || false
  local MODULE="$1"
  local MODULE_PLATFORM="$2"
  local SOURCE_MODULE
  local SOURCE_IMAGE_NAME
  SOURCE_MODULE="$(jq -r ".stages[] | select(has(\"$MODULE\"))[\"$MODULE\"]" stackgres-k8s/ci/build/target/config.json)"
  [ -n "$SOURCE_MODULE" ] || message_and_exit "Module $MODULE has no stage defined in stackgres-k8s/ci/build/config.yml" 1
  if [ "$SOURCE_MODULE" = null ]
  then
    SOURCE_IMAGE_NAME="$(jq -r ".target_image" stackgres-k8s/ci/build/target/config.json)"
  else
    SOURCE_IMAGE_NAME="$(image_name "$SOURCE_MODULE" "$MODULE_PLATFORM")"
  fi
  printf '%s\n' "$SOURCE_IMAGE_NAME"
}

image_name() {
  [ "$#" -ge 1 ] || false
  local MODULE="$1"
  local MODULE_PLATFORM="$2"
  local IMAGE_NAME
  local MODULE_PLATFORM_DEPENDENT
  MODULE_PLATFORM_DEPENDENT="$(jq -r ".modules[\"$MODULE\"].platform_dependent | . != null and ." stackgres-k8s/ci/build/target/config.json)"
  if [ "$MODULE_PLATFORM_DEPENDENT" = true ]
  then
    MODULE_PLATFORM="${MODULE_PLATFORM:-$(get_platform)}"
  else
    MODULE_PLATFORM=
  fi
  IMAGE_NAME="$(grep "^$MODULE=.*$MODULE_PLATFORM$" stackgres-k8s/ci/build/target/image-hashes)" \
    || message_and_exit "Unable to retrieve hash for module $MODULE in stackgres-k8s/ci/build/target/image-hashes" 1
  IMAGE_NAME="$(printf '%s' "$IMAGE_NAME"| cut -d = -f 2-)"
  [ -n "$IMAGE_NAME" ] \
    || message_and_exit "Unable to retrieve hash for module $MODULE in stackgres-k8s/ci/build/target/image-hashes" 1
  printf '%s\n' "$IMAGE_NAME"
}

build_image() {
  [ "$#" -ge 1 ] || false
  local MODULE="$1"
  local MODULE_TYPE
  local IMAGE_NAME
  local SOURCE_IMAGE_NAME
  MODULE_TYPE="$(module_type "$MODULE")"
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

extract() {
  [ "$#" -ge 2 ] || false
  local MODULE="$1"
  shift
  MODULE_TYPE="$(module_type "$MODULE")"
  IMAGE_NAME="$(image_name "$MODULE")"
  extract_from_image "$IMAGE_NAME" "$@"
}

extract_from_image() {
  [ "$#" -ge 2 ] || false
  local IMAGE_NAME="$1"
  shift
  local IMAGE_PLATFORM
  IMAGE_PLATFORM="$(get_image_platform "$IMAGE_NAME")"
  docker_run --rm --entrypoint /bin/sh --platform "$IMAGE_PLATFORM" \
    -u "$(id -u)" -v "$(pwd):/out" "$IMAGE_NAME" \
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

generate_image_hashes() {
  local MODULE
  local MODULE_TYPE
  local MODULE_PLATFORMS
  local MODULE_PLATFORM
  local SOURCE_IMAGE_NAME
  local IMAGE_NAME

  BUILD_UID="${BUILD_ID:-$(id -u)}"

  mkdir -p stackgres-k8s/ci/build/target

  yq . stackgres-k8s/ci/build/config.yml > stackgres-k8s/ci/build/target/config.json

  init_hash

  if ! test -f stackgres-k8s/ci/build/target/project_hash \
    || [ "$(cat stackgres-k8s/ci/build/target/project_hash)" != "$(project_hash)" ]
  then
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
        cat << EOF >> stackgres-k8s/ci/build/target/junit-build.hashes.xml
    <testcase classname="module $MODULE" name="${IMAGE_NAME##*:hash-}" />
EOF
        printf '%s\n' "$IMAGE_NAME" >> "stackgres-k8s/ci/build/target/$MODULE_TYPE-image-hashes"
        printf '%s=%s\n' "$MODULE" "$IMAGE_NAME" >> stackgres-k8s/ci/build/target/image-hashes
        if [ "$SOURCE_IMAGE_NAME" != null ]
        then
          printf '%s\n' "$SOURCE_IMAGE_NAME" >> stackgres-k8s/ci/build/target/all-images
        fi
        printf '%s\n' "$IMAGE_NAME" >> stackgres-k8s/ci/build/target/all-images
      done
    done

    rm -rf stackgres-k8s/ci/build/target/image-type-hashes
    for MODULE_TYPE_IMAGE_HASHES in stackgres-k8s/ci/build/target/*-image-hashes
    do
      local MODULE_TYPE="${MODULE_TYPE_IMAGE_HASHES##*/}"
      MODULE_TYPE="${MODULE_TYPE%-image-hashes}"
      local MODULE_TYPE_HASH
      MODULE_TYPE_HASH="$(md5sum "$MODULE_TYPE_IMAGE_HASHES" | cut -d ' ' -f 1 | tr -d '\n')"
      printf '%s=%s\n' "$MODULE_TYPE" "$MODULE_TYPE_HASH" >> stackgres-k8s/ci/build/target/image-type-hashes
      cat << EOF >> stackgres-k8s/ci/build/target/junit-build.hashes.xml
    <testcase classname="module type $MODULE_TYPE" name="$MODULE_TYPE_HASH" />
EOF
    done

    cat << EOF >> stackgres-k8s/ci/build/target/junit-build.hashes.xml
  </testsuite>
</testsuites>
EOF

    project_hash > stackgres-k8s/ci/build/target/project_hash
  fi
}

show_image_hashes() {
  echo "Calculated image hashes:"

  while IFS='=' read -r MODULE IMAGE_NAME
  do
    printf ' - %s => %s\n' "$MODULE" "$IMAGE_NAME"
  done < stackgres-k8s/ci/build/target/image-hashes

  echo "done"

  echo

  echo "Calculated image type hashes:"

  while IFS='=' read -r MODULE_TYPE MODULE_TYPE_HASH
  do
    printf ' - %s => %s\n' "$MODULE_TYPE" "$MODULE_TYPE_HASH"
  done < stackgres-k8s/ci/build/target/image-type-hashes

  echo "done"

  echo
}

find_image_digests() {
  sort "$1" | uniq \
    | xargs -I @ -P 16 sh stackgres-k8s/ci/build/build-functions.sh find_image_digest @
  (! ls stackgres-k8s/ci/build/target/image-digests.* > /dev/null 2>&1 \
    || cat stackgres-k8s/ci/build/target/image-digests.*)
}

find_image_digest() {
  local IMAGE_NAME="$1"
  if download_image_manifest "$IMAGE_NAME" >/dev/null 2>&1
  then
    printf '%s=%s\n' "$IMAGE_NAME" "$(
      jq -r 'if (.|type) == "array" then .[] else . end | .Descriptor.digest' \
        "stackgres-k8s/ci/build/target/manifest.${IMAGE_NAME##*/}" \
      | tr '\n' ',' | sed 's/,\+$//')" \
      > "stackgres-k8s/ci/build/target/image-digests.${IMAGE_NAME##*/}"
  fi
}

get_image_platform() {
  local IMAGE_NAME="$1"
  local IMAGE_MEDIA_TYPE
  local IMAGE_PLATFORM
  download_image_manifest "$IMAGE_NAME"
  IMAGE_MEDIA_TYPE="$(jq -r '. | type' \
    "stackgres-k8s/ci/build/target/manifest.${IMAGE_NAME##*/}")"
  if [ "$IMAGE_MEDIA_TYPE" = "array" ]
  then
    docker_buildx_inspect --bootstrap | grep Platforms | cut -d : -f 2 | tr -d ' ' | tr ',' '\n' \
      | while read -r PLATFORM
        do
          if jq -r '.[]|.Descriptor.platform.os + "/" + .Descriptor.platform.architecture' \
            "stackgres-k8s/ci/build/target/manifest.${IMAGE_NAME##*/}" | grep -qxF "$PLATFORM"
          then
            printf '%s' "$PLATFORM"
            break
          fi
        done
  else
    jq -r '.Descriptor.platform.os + "/" + .Descriptor.platform.architecture' \
      "stackgres-k8s/ci/build/target/manifest.${IMAGE_NAME##*/}"
  fi
}

download_image_manifest() {
  local IMAGE_NAME="$1"
  if ! [ -s "stackgres-k8s/ci/build/target/manifest.${IMAGE_NAME##*/}" ]
  then
    docker_manifest_inspect -v "$IMAGE_NAME" \
      > "stackgres-k8s/ci/build/target/manifest.${IMAGE_NAME##*/}"
  fi
}

get_platform() {
  printf '%s-%s' "$(uname | tr '[:upper:]' '[:lower:]')" "$(uname -m)"
}

project_hash() {
  git --git-dir stackgres-k8s/ci/build/target/.git rev-parse HEAD:
}

path_hash() {
  [ "$#" -ge 1 ] || false
  local FILE="$1"
  git --git-dir stackgres-k8s/ci/build/target/.git rev-parse HEAD:"$FILE"
}

docker_inspect() {
  docker inspect "$@"
}

docker_images() {
  docker images "$@"
}

docker_rmi() {
  docker rmi "$@"
}

docker_run() {
  docker run "$@"
}

docker_build() {
  docker build "$@"
}

docker_push() {
  docker push "$@"
}

docker_manifest_inspect() {
  docker manifest inspect "$@"
}

docker_buildx_inspect() {
  docker buildx inspect "$@"
}

if [ "$(basename "$0")" = "build-functions.sh" ] && [ "$#" -ge 1 ]
then
  "$@"
fi

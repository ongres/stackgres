#!/bin/sh
# shellcheck disable=SC2039
# shellcheck disable=SC2016

BUILDER_VERSION=1.0.0

set -e

export BUILD_UID="${BUILD_UID:-$(id -u):$(ls -n /var/run/docker.sock | cut -d ' ' -f 4)}"

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
    MODULE_PLATFORM="${MODULE_PLATFORM:-$(get_platform)}"
    TAG_MODULE_PLATFORM="$(printf %s "$MODULE_PLATFORM" | tr '/' '-')"
    printf 'registry.gitlab.com/ongresinc/stackgres/build/%s:hash-%s-%s\n' \
      "$MODULE" "$MODULE_HASH" "$TAG_MODULE_PLATFORM"
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
  docker_run -i $(! test -t 1 || printf %s '-t') --rm \
    $([ "$SKIP_REMOTE_MANIFEST" = true ] || printf %s '--pull always') \
    --platform "$SOURCE_IMAGE_PLATFORM" \
    --volume "${PROJECT_PATH:-$(pwd)}:/project-target" \
    --user "$(id -u):$(id -g)" \
    --env HOME=/tmp \
    "$SOURCE_IMAGE_NAME" \
    sh -ec $(echo "$-" | grep -v -q x || printf %s '-x') \
      'cp -a /project/. /project-target/.'
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
          | map(\"export \" + .key + \"=\\\"\" + .value + \"\\\"\")
          | .[]" stackgres-k8s/ci/build/target/config.json)
EOF
   "  > "stackgres-k8s/ci/build/target/$MODULE-build-env"
  # shellcheck disable=SC2046
  docker_run -i $(! test -t 1 || printf %s '-t') --rm \
    --platform "${BUILD_PLATFORM:-$(get_platform)}" \
    $([ "$SKIP_REMOTE_MANIFEST" = true ] || printf %s '--pull always') \
    --volume "/var/run/docker.sock:/var/run/docker.sock" \
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
    --platform "${BUILD_PLATFORM:-$(get_platform)}" \
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
      git --git-dir stackgres-k8s/ci/build/target/.git init > /dev/null
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

image_name() {
  [ "$#" -ge 1 ] || false
  local BUILD_HASH
  local MODULE="$1"
  local MODULE_PLATFORM="$2"
  local IMAGE_NAME
  local MODULE_PLATFORM_DEPENDENT
  BUILD_HASH="$(cat stackgres-k8s/ci/build/target/build_hash)"
  MODULE_PLATFORM_DEPENDENT="$(jq -r ".modules[\"$MODULE\"].platform_dependent | . != null and ." stackgres-k8s/ci/build/target/config.json)"
  if [ "$MODULE_PLATFORM_DEPENDENT" = true ]
  then
    MODULE_PLATFORM="${MODULE_PLATFORM:-$(get_platform)}"
  else
    MODULE_PLATFORM=
  fi
  TAG_MODULE_PLATFORM="$(printf %s "$MODULE_PLATFORM" | tr '/' '-')"
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
  if {
      [ "$DO_BUILD" != true ] \
        && ! printf " $DO_BUILD_MODULES " | grep -qF " $MODULE " \
        && grep -q "^$IMAGE_NAME=" "stackgres-k8s/ci/build/target/image-digests.$BUILD_HASH"
    }
  then
    echo "Already exists on remote repository. Just extracting..."
    copy_from_image "$SOURCE_IMAGE_NAME"
  else
    if {
        [ "$DO_BUILD" != true ] \
          && ! printf " $DO_BUILD_MODULES " | grep -qF " $MODULE " \
          && docker_inspect "$IMAGE_NAME" >/dev/null 2>&1
      }
    then
      echo "Already exists locally. Just extracting ..."
      copy_from_image "$SOURCE_IMAGE_NAME"
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
  IMAGE_PLATFORM="$(get_image_platform "$IMAGE_NAME")"
  docker_run --rm --entrypoint /bin/sh --platform "$IMAGE_PLATFORM" \
    $([ "$SKIP_REMOTE_MANIFEST" = true ] || printf %s '--pull always') \
    --user "$(id -u):$(id -g)" \
    --env HOME=/tmp \
    -v "${PROJECT_PATH:-$(pwd)}:/out" \
    "$IMAGE_NAME" \
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
  sort "$1" | uniq \
    | xargs -I @ -P 16 sh $(! echo $- | grep -q x || printf %s "-x") \
      stackgres-k8s/ci/build/build-functions.sh find_image_digest @
  (! ls stackgres-k8s/ci/build/target/image-digests.* > /dev/null 2>&1 \
    || cat stackgres-k8s/ci/build/target/image-digests.*)
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
    '. as $manifest | if length == 0 then halt_error else . end | $manifest[0].Architecture' \
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
        REGISTRY_CONTAINER_ID="$(docker_run -d -p 5000 --stop-timeout 300 registry:2)"
        REGISTRY_PORT="$(docker_inspect "$REGISTRY_CONTAINER_ID" | jq '.[0].NetworkSettings.Ports["5000/tcp"][0].HostPort' -r)"
        REGISTRY_IMAGE_NAME="localhost:$REGISTRY_PORT/$(printf %s "${IMAGE_NAME%:*}" | tr '/:' '_'):${IMAGE_NAME##*:}"
        docker_tag "$IMAGE_NAME" "$REGISTRY_IMAGE_NAME"
        docker_push "$REGISTRY_IMAGE_NAME"
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

project_hash() {
  git --git-dir "stackgres-k8s/ci/build/target/.git" rev-parse HEAD:
}

path_hash() {
  [ "$#" -ge 1 ] || false
  local FILE="$1"
  git --git-dir "stackgres-k8s/ci/build/target/.git" rev-parse HEAD:"$FILE"
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

docker_tag() {
  docker tag "$@"
}

docker_rm() {
  docker rm "$@"
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

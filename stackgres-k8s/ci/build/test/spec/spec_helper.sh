# shellcheck shell=sh

# Fixtures directory
FIXTURES_DIR="${SHELLSPEC_SPECDIR}/fixtures"

# Will be set per-test to a temp directory
TEST_PROJECT_DIR=""
BUILD_DIR=""
TARGET_DIR=""

setup_test_project() {
  TEST_PROJECT_DIR="$(mktemp -d)"
  BUILD_DIR="$TEST_PROJECT_DIR/stackgres-k8s/ci/build"
  TARGET_DIR="$BUILD_DIR/target"
  mkdir -p "$TARGET_DIR"
  mkdir -p "$TEST_PROJECT_DIR/test/module-a/src"
  mkdir -p "$TEST_PROJECT_DIR/test/module-a/out"
  mkdir -p "$TEST_PROJECT_DIR/test/module-b/src"
  mkdir -p "$TEST_PROJECT_DIR/test/module-b/out"
  mkdir -p "$TEST_PROJECT_DIR/test/module-c/src"
  mkdir -p "$TEST_PROJECT_DIR/test/module-c/out"
  mkdir -p "$TEST_PROJECT_DIR/test/platform-mod/src"
  mkdir -p "$TEST_PROJECT_DIR/test/platform-mod/out"
  mkdir -p "$TEST_PROJECT_DIR/test/noplatform-mod/src"
  mkdir -p "$TEST_PROJECT_DIR/test/noplatform-mod/out"
  mkdir -p "$TEST_PROJECT_DIR/test/filtered-mod/src"
  mkdir -p "$TEST_PROJECT_DIR/test/filtered-mod/out"
  mkdir -p "$TEST_PROJECT_DIR/test/custom-df-mod/src"
  mkdir -p "$TEST_PROJECT_DIR/test/custom-df-mod/out"
  mkdir -p "$TEST_PROJECT_DIR/test/default-df-mod/src"
  mkdir -p "$TEST_PROJECT_DIR/test/default-df-mod/out/file1"
  mkdir -p "$TEST_PROJECT_DIR/test/default-df-mod/out/file2"
  mkdir -p "$TEST_PROJECT_DIR/test/spec/fixtures"
  echo "module-a content" > "$TEST_PROJECT_DIR/test/module-a/src/file.txt"
  echo "module-b content" > "$TEST_PROJECT_DIR/test/module-b/src/file.txt"
  echo "module-c content" > "$TEST_PROJECT_DIR/test/module-c/src/file.txt"
  echo "platform-mod content" > "$TEST_PROJECT_DIR/test/platform-mod/src/file.txt"
  echo "noplatform-mod content" > "$TEST_PROJECT_DIR/test/noplatform-mod/src/file.txt"
  echo "filtered-mod content" > "$TEST_PROJECT_DIR/test/filtered-mod/src/file.txt"
  echo "custom-df-mod content" > "$TEST_PROJECT_DIR/test/custom-df-mod/src/file.txt"
  echo "default-df-mod content" > "$TEST_PROJECT_DIR/test/default-df-mod/src/file.txt"
  cp "$FIXTURES_DIR/mock-dockerfile" "$TEST_PROJECT_DIR/test/spec/fixtures/mock-dockerfile"
}

cleanup_test_project() {
  if [ -n "$TEST_PROJECT_DIR" ] && [ -d "$TEST_PROJECT_DIR" ]; then
    rm -rf "$TEST_PROJECT_DIR"
  fi
}

# Load a fixture config into the test project's target dir
load_fixture_config() {
  local CONFIG_NAME="$1"
  cp "$FIXTURES_DIR/$CONFIG_NAME.yml" "$BUILD_DIR/config.yml"
  yq . "$BUILD_DIR/config.yml" > "$TARGET_DIR/config.json"
  md5sum "$BUILD_DIR/config.yml" | cut -d ' ' -f 1 > "$TARGET_DIR/config.yml.md5"
}

# Real path to build-functions.sh in the actual repo
REAL_BUILD_FUNCTIONS="${SHELLSPEC_SPECDIR}/../../build-functions.sh"
REAL_BUILD_SH="${SHELLSPEC_SPECDIR}/../../build.sh"

# Source build-functions.sh with the problematic lines neutralized.
# - Line 9: BUILD_UID default uses docker.sock (we pre-set BUILD_UID)
# - Line 21: cd to project root (we cd ourselves)
# - Lines 839-842: dispatch block (not needed when sourcing)
source_build_functions() {
  cd "$TEST_PROJECT_DIR" || return 1

  export BUILD_UID="1000:1000"
  export LANG=C.UTF-8
  export LC_ALL=C.UTF-8

  # shellcheck disable=SC1090
  eval "$(sed \
    -e '/^export BUILD_UID=.*docker\.sock/s/^/#TEST_SKIP# /' \
    -e '/^cd "\$(dirname/s/^/#TEST_SKIP# /' \
    -e '/^if \[ "\$(basename "\$0")" = "build-functions.sh" \]/,/^fi$/s/^/#TEST_SKIP# /' \
    "$REAL_BUILD_FUNCTIONS")"
}

# Mock docker commands - these record calls and return success
DOCKER_CALL_LOG=""

mock_docker_commands() {
  DOCKER_CALL_LOG="$TEST_PROJECT_DIR/docker_calls.log"
  : > "$DOCKER_CALL_LOG"

  docker_run() {
    echo "docker_run $*" >> "$DOCKER_CALL_LOG"
    return 0
  }

  docker_create() {
    echo "docker_create $*" >> "$DOCKER_CALL_LOG"
    echo "test-container-id"
    return 0
  }

  docker_cp() {
    echo "docker_cp $*" >> "$DOCKER_CALL_LOG"
    return 0
  }

  docker_build() {
    echo "docker_build $*" >> "$DOCKER_CALL_LOG"
    return 0
  }

  docker_push() {
    echo "docker_push $*" >> "$DOCKER_CALL_LOG"
    return 0
  }

  docker_inspect() {
    echo "docker_inspect $*" >> "$DOCKER_CALL_LOG"
    return 1  # Default: image not found
  }

  docker_tag() {
    echo "docker_tag $*" >> "$DOCKER_CALL_LOG"
    return 0
  }

  docker_rm() {
    echo "docker_rm $*" >> "$DOCKER_CALL_LOG"
    return 0
  }

  docker_rmi() {
    echo "docker_rmi $*" >> "$DOCKER_CALL_LOG"
    return 0
  }

  docker_images() {
    echo "docker_images $*" >> "$DOCKER_CALL_LOG"
    return 0
  }

  docker_manifest_inspect() {
    echo "docker_manifest_inspect $*" >> "$DOCKER_CALL_LOG"
    return 1  # Default: manifest not found
  }

  docker_buildx_inspect() {
    echo "docker_buildx_inspect $*" >> "$DOCKER_CALL_LOG"
    echo "Platforms: linux/amd64,linux/arm64"
    return 0
  }
}

# Mock docker_inspect to simulate image found locally
mock_docker_inspect_found() {
  docker_inspect() {
    echo "docker_inspect $*" >> "$DOCKER_CALL_LOG"
    printf '[{"RepoDigests":["test@sha256:abc123"],"Architecture":"amd64"}]'
    return 0
  }
}

# Mock docker_inspect to simulate image not found
mock_docker_inspect_not_found() {
  docker_inspect() {
    echo "docker_inspect $*" >> "$DOCKER_CALL_LOG"
    return 1
  }
}

# Count calls to a docker command in the log
count_docker_calls() {
  local CMD="$1"
  grep -c "^${CMD} " "$DOCKER_CALL_LOG" 2>/dev/null || echo 0
}

# Check if a docker command was called with specific args
docker_called_with() {
  local PATTERN="$1"
  grep -q "$PATTERN" "$DOCKER_CALL_LOG" 2>/dev/null
}

# Override flock to be a no-op passthrough
flock() {
  shift  # skip the lock file
  "$@"
}

# Override get_platform for deterministic tests
mock_get_platform() {
  get_platform() {
    printf 'linux/x86_64'
  }
}

# Initialize git in the test project for hash-related tests
init_test_git() {
  (
    cd "$TEST_PROJECT_DIR" || return 1
    git -c init.defaultBranch=main init -q .
    git add .
    git -c user.name=test -c user.email=test@test commit -q -m "init" --no-gpg-sign
  )
}

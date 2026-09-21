# shellcheck shell=sh

Describe "build_image cache logic"
  BUILD_HASH=""

  setup() {
    setup_test_project
    source_build_functions
    mock_docker_commands
    mock_get_platform
    load_fixture_config config-minimal
    init_test_git
    init_hash

    BUILD_HASH="$(echo "module-a module-b module-c" | md5sum | cut -d ' ' -f 1)"
    printf %s "$BUILD_HASH" > "$TARGET_DIR/build_hash"
    cat > "$TARGET_DIR/image-hashes.$BUILD_HASH" << EOF
module-a=registry.gitlab.com/ongresinc/stackgres/build/module-a:hash-aaa111
module-b=registry.gitlab.com/ongresinc/stackgres/build/module-b:hash-bbb222
module-c=registry.gitlab.com/ongresinc/stackgres/build/module-c:hash-ccc333
EOF
    : > "$TARGET_DIR/image-digests.$BUILD_HASH"

    # Track calls to build_module_image
    BUILD_MODULE_IMAGE_CALLED=false
    build_module_image() {
      BUILD_MODULE_IMAGE_CALLED=true
    }

    # Mock copy_from_image
    copy_from_image() { :; }
  }

  cleanup() {
    cleanup_test_project
  }

  Before 'setup'
  After 'cleanup'

  Describe "remote cache hit"
    It "skips build when image exists in registry"
      echo "registry.gitlab.com/ongresinc/stackgres/build/module-a:hash-aaa111=sha256:abc" \
        >> "$TARGET_DIR/image-digests.$BUILD_HASH"
      When call build_image module-a
      The variable BUILD_MODULE_IMAGE_CALLED should equal false
      The output should include "Already exists on remote"
    End
  End

  Describe "local cache hit"
    It "skips build when image exists locally"
      mock_docker_inspect_found
      When call build_image module-a
      The variable BUILD_MODULE_IMAGE_CALLED should equal false
      The output should include "Already exists locally"
    End
  End

  Describe "cache miss"
    It "calls build_module_image when image not found anywhere"
      mock_docker_inspect_not_found
      When call build_image module-a
      The variable BUILD_MODULE_IMAGE_CALLED should equal true
      The output should include "Building module-a"
    End

    It "calls docker_push after build"
      mock_docker_inspect_not_found
      SKIP_PUSH=""
      build_image module-a >/dev/null 2>&1
      When call grep -c "docker_push" "$DOCKER_CALL_LOG"
      The output should equal "1"
    End
  End

  Describe "DO_BUILD=true"
    It "forces rebuild despite remote cache hit"
      echo "registry.gitlab.com/ongresinc/stackgres/build/module-a:hash-aaa111=sha256:abc" \
        >> "$TARGET_DIR/image-digests.$BUILD_HASH"
      DO_BUILD=true
      When call build_image module-a
      The variable BUILD_MODULE_IMAGE_CALLED should equal true
      The output should include "Building module-a"
    End
  End

  Describe "DO_BUILD_MODULES"
    It "forces rebuild for specified module"
      echo "registry.gitlab.com/ongresinc/stackgres/build/module-a:hash-aaa111=sha256:abc" \
        >> "$TARGET_DIR/image-digests.$BUILD_HASH"
      DO_BUILD_MODULES="module-a"
      When call build_image module-a
      The variable BUILD_MODULE_IMAGE_CALLED should equal true
      The output should include "Building module-a"
    End
  End

  Describe "SKIP_PUSH=true"
    It "skips docker_push after build"
      mock_docker_inspect_not_found
      SKIP_PUSH=true
      build_image module-a >/dev/null 2>&1
      When call grep "docker_push" "$DOCKER_CALL_LOG"
      The status should not equal 0
    End
  End
End

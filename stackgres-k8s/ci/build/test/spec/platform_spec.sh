# shellcheck shell=sh

Describe "platform handling"
  setup() {
    setup_test_project
    source_build_functions
    mock_docker_commands
  }

  cleanup() {
    cleanup_test_project
  }

  Before 'setup'
  After 'cleanup'

  Describe "get_platform"
    It "returns os/arch format"
      When call get_platform
      The output should include "/"
    End

    It "returns lowercase os"
      When call get_platform
      The output should start with "linux/"
    End
  End

  Describe "get_platform_tag_suffix"
    It "replaces / with -"
      mock_get_platform
      When call get_platform_tag_suffix
      The output should equal "linux-x86_64"
    End
  End

  Describe "get_image_platform"
    # A locally built image that was never pushed has no RepoDigests, so the
    # manifest path can not complete for it and would ask the registry for a tag
    # that is not there. The platform is in the local docker inspect output.
    It "resolves the platform of an image that is only local, with no registry"
      docker_inspect() {
        echo "docker_inspect $*" >> "$DOCKER_CALL_LOG"
        printf '[{"RepoDigests":[],"Os":"linux","Architecture":"amd64"}]'
        return 0
      }
      When call get_image_platform registry.example.com/build:mod-hash-local
      The status should be success
      The output should equal "linux/amd64"
      The contents of file "$DOCKER_CALL_LOG" should not include "docker_manifest_inspect"
    End

    It "falls back to the manifest of the registry when the image is not local"
      docker_manifest_inspect() {
        echo "docker_manifest_inspect $*" >> "$DOCKER_CALL_LOG"
        printf '{"Descriptor":{"platform":{"os":"linux","architecture":"arm64"}}}'
        return 0
      }
      When call get_image_platform registry.example.com/build:mod-hash-remote
      The status should be success
      The output should equal "linux/arm64"
      The contents of file "$DOCKER_CALL_LOG" should include "docker_manifest_inspect"
    End
  End

  Describe "platform-dependent module hash generation"
    setup_platform() {
      setup_test_project
      source_build_functions
      mock_docker_commands
      mock_get_platform
      load_fixture_config config-platform
      init_test_git
      init_hash

      BUILD_HASH="$(echo "platform-mod noplatform-mod" | md5sum | cut -d ' ' -f 1)"
      printf %s "$BUILD_HASH" > "$TARGET_DIR/build_hash"
      : > "$TARGET_DIR/image-hashes.$BUILD_HASH"
      : > "$TARGET_DIR/all-images.$BUILD_HASH"
      : > "$TARGET_DIR/junit-build.hashes.xml.$BUILD_HASH"
    }

    Before 'setup_platform'
    After 'cleanup'

    It "generates per-platform hash for platform-dependent module"
      When call generate_image_hash platform-mod
      The path "$TARGET_DIR/image-hashes.$BUILD_HASH" should be file
      The contents of file "$TARGET_DIR/image-hashes.$BUILD_HASH" should include "linux-x86_64"
      The contents of file "$TARGET_DIR/image-hashes.$BUILD_HASH" should include "linux-aarch64"
    End
  End

  Describe "BUILD_PLATFORM override"
    It "overrides detected platform in get_platform usage"
      BUILD_PLATFORM="linux/aarch64"
      # BUILD_PLATFORM is used directly in run_commands_in_container and build_module_image
      # rather than calling get_platform. Verify the env var is respected.
      When call echo "${BUILD_PLATFORM:-$(get_platform)}"
      The output should equal "linux/aarch64"
    End
  End
End

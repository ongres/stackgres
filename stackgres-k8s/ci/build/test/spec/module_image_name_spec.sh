# shellcheck shell=sh

Describe "module_image_name (hash computation)"
  setup() {
    setup_test_project
    source_build_functions
    mock_docker_commands
    mock_get_platform
    load_fixture_config config-minimal
    init_test_git
    init_hash
  }

  cleanup() {
    cleanup_test_project
  }

  Before 'setup'
  After 'cleanup'

  Describe "hash determinism"
    It "produces same hash on repeated calls"
      HASH1="$(module_image_name module-a busybox:latest)"
      When call module_image_name module-a "busybox:latest"
      The output should equal "$HASH1"
    End
  End

  Describe "hash includes BUILDER_VERSION"
    It "changes hash when BUILDER_VERSION changes"
      HASH_ORIGINAL="$(module_image_name module-a busybox:latest)"
      BUILDER_VERSION=2.0.0
      When call module_image_name module-a "busybox:latest"
      The output should not equal "$HASH_ORIGINAL"
    End
  End

  Describe "hash includes source image name"
    It "changes hash for different parent image"
      HASH1="$(module_image_name module-a busybox:latest)"
      When call module_image_name module-a "alpine:latest"
      The output should not equal "$HASH1"
    End
  End

  Describe "hash includes module config"
    It "changes hash when module config differs"
      HASH1="$(module_image_name module-a busybox:latest)"
      # Modify config to change the module
      jq '.modules["module-a"].build_commands = ["echo changed"]' \
        "$TARGET_DIR/config.json" > "$TARGET_DIR/config.json.tmp"
      mv "$TARGET_DIR/config.json.tmp" "$TARGET_DIR/config.json"
      When call module_image_name module-a "busybox:latest"
      The output should not equal "$HASH1"
    End
  End

  Describe "hash includes evaluated build_env"
    It "changes hash when build_env changes"
      HASH1="$(module_image_name module-a busybox:latest)"
      jq '.modules["module-a"].build_env = {"MY_VAR": "new-value"}' \
        "$TARGET_DIR/config.json" > "$TARGET_DIR/config.json.tmp"
      mv "$TARGET_DIR/config.json.tmp" "$TARGET_DIR/config.json"
      When call module_image_name module-a "busybox:latest"
      The output should not equal "$HASH1"
    End
  End

  Describe "hash includes source path content"
    It "changes hash when source file changes"
      HASH1="$(module_image_name module-a busybox:latest)"
      echo "modified content" > "$TEST_PROJECT_DIR/test/module-a/src/file.txt"
      (cd "$TEST_PROJECT_DIR" && \
        git --git-dir "$TARGET_DIR/.git" add . && \
        git --git-dir "$TARGET_DIR/.git" -c user.name=ci -c user.email= commit -q -m "change" --no-gpg-sign) 2>/dev/null
      When call module_image_name module-a "busybox:latest"
      The output should not equal "$HASH1"
    End
  End

  Describe "hash includes filtered_sources output"
    setup_filtered() {
      setup_test_project
      source_build_functions
      mock_docker_commands
      mock_get_platform
      load_fixture_config config-filtered-sources
      init_test_git
      init_hash
    }

    Before 'setup_filtered'
    After 'cleanup'

    It "includes filtered_sources command output in hash"
      module_image_name filtered-mod "busybox:latest" >/dev/null
      When call cat "$TARGET_DIR/filtered-mod-hash"
      The output should include "filtered-output-1"
      The output should include "filtered-output-2"
    End
  End

  Describe "platform-dependent tag"
    setup_platform() {
      setup_test_project
      source_build_functions
      mock_docker_commands
      mock_get_platform
      load_fixture_config config-platform
      init_test_git
      init_hash
    }

    Before 'setup_platform'
    After 'cleanup'

    It "appends platform suffix for platform-dependent module"
      When call module_image_name platform-mod "busybox:latest" "linux/x86_64"
      The output should include "linux-x86_64"
    End

    It "has no platform suffix for non-platform-dependent module"
      When call module_image_name noplatform-mod "busybox:latest"
      The output should not include "linux-x86_64"
      The output should not include "linux-aarch64"
    End
  End
End

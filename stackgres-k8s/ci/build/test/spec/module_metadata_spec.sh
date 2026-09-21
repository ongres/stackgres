# shellcheck shell=sh

Describe "module metadata"
  setup() {
    setup_test_project
    source_build_functions
    mock_docker_commands
    load_fixture_config config-minimal
  }

  cleanup() {
    cleanup_test_project
  }

  Before 'setup'
  After 'cleanup'

  Describe "module_list"
    It "returns sorted source paths for array sources"
      When call module_list module-a sources
      The output should include "test/module-a/src"
    End

    It "returns artifact paths"
      When call module_list module-a artifacts
      The output should include "test/module-a/out"
    End

    It "returns empty for missing field"
      When call module_list module-a cache
      The output should equal ""
    End
  End

  Describe "module_list with map-style artifacts"
    setup_map() {
      setup_test_project
      source_build_functions
      mock_docker_commands
      # Create a config with map-style artifacts
      cat > "$BUILD_DIR/config.yml" << 'YAMLEOF'
target_image: busybox:latest
platforms:
  - linux/x86_64
modules:
  map-mod:
    type: test
    path: test/module-a
    target_image: busybox:latest
    build_image: alpine:latest
    sources:
      - test/module-a/src
    artifacts:
      0: test/module-a/out/first
      1: test/module-a/out/second
      999: test/module-a/out/last
stages:
  - map-mod: null
YAMLEOF
      yq . "$BUILD_DIR/config.yml" > "$TARGET_DIR/config.json"
    }

    Before 'setup_map'
    After 'cleanup'

    It "handles map-style artifacts with numbered keys"
      When call module_list map-mod artifacts
      The output should include "test/module-a/out/first"
      The output should include "test/module-a/out/second"
      The output should include "test/module-a/out/last"
    End
  End

  Describe "module_type"
    It "returns correct type for module-a"
      When call module_type module-a
      The output should equal "test"
    End

    It "returns correct type for module-c"
      When call module_type module-c
      The output should equal "test-leaf"
    End

    It "errors on undefined module"
      When run module_type nonexistent-module
      The status should not equal 0
      The error should include "not defined"
    End
  End
End

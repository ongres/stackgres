# shellcheck shell=sh

Describe "init_config"
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

  Describe "when no cache exists"
    It "converts YAML to JSON"
      cp "$FIXTURES_DIR/config-minimal.yml" "$BUILD_DIR/config.yml"
      When call init_config
      The path "$TARGET_DIR/config.json" should be file
      The path "$TARGET_DIR/config.yml.md5" should be file
      The contents of file "$TARGET_DIR/config.json" should include '"module-a"'
    End
  End

  Describe "when config has not changed"
    It "skips conversion when MD5 matches"
      load_fixture_config config-minimal
      # Record the modification time of config.json
      cp "$TARGET_DIR/config.json" "$TARGET_DIR/config.json.backup"
      When call init_config
      # config.json should still exist and be unchanged
      The path "$TARGET_DIR/config.json" should be file
      The contents of file "$TARGET_DIR/config.json" should equal "$(cat "$TARGET_DIR/config.json.backup")"
    End
  End

  Describe "when config changes"
    It "regenerates JSON when config.yml changes"
      load_fixture_config config-minimal
      # Modify the config
      echo "# changed" >> "$BUILD_DIR/config.yml"
      When call init_config
      The path "$TARGET_DIR/config.json" should be file
      # MD5 should be updated
      The contents of file "$TARGET_DIR/config.yml.md5" should not equal ""
    End
  End

  Describe "when target directory is missing"
    It "creates target directory"
      cp "$FIXTURES_DIR/config-minimal.yml" "$BUILD_DIR/config.yml"
      rm -rf "$TARGET_DIR"
      When call init_config
      The path "$TARGET_DIR" should be directory
      The path "$TARGET_DIR/config.json" should be file
    End
  End
End

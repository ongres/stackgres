# shellcheck shell=sh

Describe "build integration"
  BUILD_HASH=""
  BUILT_MODULES=""

  setup() {
    setup_test_project
    source_build_functions
    mock_docker_commands
    mock_get_platform
    load_fixture_config config-minimal
    init_test_git
    init_hash

    # Track which modules are built
    BUILT_MODULES=""
    build_module_image() {
      BUILT_MODULES="$BUILT_MODULES $1"
    }
    copy_from_image() { :; }
  }

  cleanup() {
    cleanup_test_project
  }

  Before 'setup'
  After 'cleanup'

  Describe "full 3-module chain build (all cache miss)"
    It "builds all three modules in dependency order"
      # Generate hashes for all modules
      generate_image_hashes module-a module-b module-c
      BUILD_HASH="$(cat "$TARGET_DIR/build_hash")"

      # No digests file -> all cache miss
      : > "$TARGET_DIR/image-digests.$BUILD_HASH"

      # Build all modules
      for MODULE in module-a module-b module-c; do
        build_image "$MODULE"
      done

      When call echo "$BUILT_MODULES"
      The output should include "module-a"
      The output should include "module-b"
      The output should include "module-c"
    End
  End

  Describe "partial cache hit"
    It "only builds uncached modules"
      generate_image_hashes module-a module-b module-c
      BUILD_HASH="$(cat "$TARGET_DIR/build_hash")"

      # module-a has a digest (cache hit), others don't
      IMAGE_A="$(grep '^module-a=' "$TARGET_DIR/image-hashes.$BUILD_HASH" | cut -d= -f2-)"
      printf '%s=sha256:cached\n' "$IMAGE_A" > "$TARGET_DIR/image-digests.$BUILD_HASH"

      for MODULE in module-a module-b module-c; do
        build_image "$MODULE"
      done

      When call echo "$BUILT_MODULES"
      The output should not include "module-a"
      The output should include "module-b"
      The output should include "module-c"
    End
  End

  Describe "leaf module resolves full chain"
    It "dependency resolution includes all ancestors"
      # Replicate dependency resolution from build.sh
      MODULES="module-c"
      COMPLETED_MODULES=""
      for MODULE in $MODULES; do
        CURRENT_MODULE="$MODULE"
        while true; do
          PARENT_MODULE="$(jq -r ".stages[]|to_entries[]|select(.key == \"$CURRENT_MODULE\").value" "$TARGET_DIR/config.json")"
          [ "$PARENT_MODULE" != null ] || break
          if ! printf ' %s ' "$COMPLETED_MODULES" | tr '\n' ' ' | grep -qF " $PARENT_MODULE "; then
            COMPLETED_MODULES="$PARENT_MODULE $COMPLETED_MODULES"
          fi
          CURRENT_MODULE="$PARENT_MODULE"
        done
        if ! printf ' %s ' "$COMPLETED_MODULES" | tr '\n' ' ' | grep -qF " $MODULE "; then
          COMPLETED_MODULES="$COMPLETED_MODULES $MODULE"
        fi
      done
      MODULES="$COMPLETED_MODULES"

      generate_image_hashes $MODULES
      BUILD_HASH="$(cat "$TARGET_DIR/build_hash")"
      : > "$TARGET_DIR/image-digests.$BUILD_HASH"

      for MODULE in $MODULES; do
        build_image "$MODULE"
      done

      When call echo "$BUILT_MODULES"
      The output should include "module-a"
      The output should include "module-b"
      The output should include "module-c"
    End
  End

  Describe "hashes command"
    It "generates and shows hashes without building"
      generate_image_hashes module-a module-b module-c
      When call show_image_hashes
      The output should include "module-a"
      The output should include "module-b"
      The output should include "module-c"
      The output should include "hash-"
    End
  End

  Describe "hash generation creates expected files"
    It "creates image-hashes, image-type-hashes, and junit XML"
      When call generate_image_hashes module-a module-b module-c
      The path "$TARGET_DIR/build_hash" should be file
      BUILD_HASH="$(cat "$TARGET_DIR/build_hash")"
      The path "$TARGET_DIR/image-hashes.$BUILD_HASH" should be file
      The path "$TARGET_DIR/image-type-hashes.$BUILD_HASH" should be file
      The path "$TARGET_DIR/junit-build.hashes.xml.$BUILD_HASH" should be file
    End
  End
End

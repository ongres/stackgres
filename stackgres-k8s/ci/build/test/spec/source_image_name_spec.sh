# shellcheck shell=sh

Describe "source_image_name"
  setup() {
    setup_test_project
    source_build_functions
    mock_docker_commands
    mock_get_platform
    load_fixture_config config-minimal

    # Set up image hashes file so image_name() can work
    BUILD_HASH="$(echo "module-a module-b module-c" | md5sum | cut -d ' ' -f 1)"
    printf %s "$BUILD_HASH" > "$TARGET_DIR/build_hash"
    cat > "$TARGET_DIR/image-hashes.$BUILD_HASH" << EOF
module-a=registry.gitlab.com/ongresinc/stackgres/build/module-a:hash-aaa111
module-b=registry.gitlab.com/ongresinc/stackgres/build/module-b:hash-bbb222
module-c=registry.gitlab.com/ongresinc/stackgres/build/module-c:hash-ccc333
EOF
  }

  cleanup() {
    cleanup_test_project
  }

  Before 'setup'
  After 'cleanup'

  It "returns global target_image for root module (null parent)"
    When call source_image_name module-a
    The output should equal "busybox:latest"
  End

  It "returns parent module image name for dependent module"
    When call source_image_name module-b
    The output should equal "registry.gitlab.com/ongresinc/stackgres/build/module-a:hash-aaa111"
  End

  It "returns grandparent chain correctly"
    When call source_image_name module-c
    The output should equal "registry.gitlab.com/ongresinc/stackgres/build/module-b:hash-bbb222"
  End
End

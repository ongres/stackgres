# shellcheck shell=sh

Describe "Dockerfile generation"
  Describe "default Dockerfile (no custom dockerfile.path)"
    setup() {
      setup_test_project
      source_build_functions
      mock_docker_commands
      mock_get_platform
      load_fixture_config config-dockerfile

      # Mock build phases so build_module_image only generates the Dockerfile
      copy_from_image() { :; }
      pre_build_in_container() { :; }
      build_in_container() { :; }
      post_build_in_container() { :; }
      docker_build() {
        echo "docker_build $*" >> "$DOCKER_CALL_LOG"
        return 0
      }
    }

    cleanup() {
      cleanup_test_project
    }

    Before 'setup'
    After 'cleanup'

    It "generates Dockerfile from target_image and artifact COPYs"
      When call build_module_image default-df-mod "busybox:latest" "test-image:hash-123"
      The path "$TARGET_DIR/Dockerfile.default-df-mod" should be file
      The contents of file "$TARGET_DIR/Dockerfile.default-df-mod" should include 'FROM "$TARGET_IMAGE_NAME"'
      The contents of file "$TARGET_DIR/Dockerfile.default-df-mod" should include "COPY ./test/default-df-mod/out/file1"
      The contents of file "$TARGET_DIR/Dockerfile.default-df-mod" should include "COPY ./test/default-df-mod/out/file2"
    End

    It "generates .dockerignore with artifact exclusions"
      build_module_image default-df-mod "busybox:latest" "test-image:hash-123" 2>/dev/null
      When call cat .dockerignore
      The first line of output should equal "*"
      The output should include "!test/default-df-mod/out/file1"
      The output should include "!test/default-df-mod/out/file2"
    End
  End

  Describe "custom Dockerfile"
    setup_custom() {
      setup_test_project
      source_build_functions
      mock_docker_commands
      mock_get_platform
      load_fixture_config config-dockerfile

      copy_from_image() { :; }
      pre_build_in_container() { :; }
      build_in_container() { :; }
      post_build_in_container() { :; }
      docker_build() {
        echo "docker_build $*" >> "$DOCKER_CALL_LOG"
        return 0
      }
    }

    cleanup() {
      cleanup_test_project
    }

    Before 'setup_custom'
    After 'cleanup'

    It "uses custom Dockerfile when dockerfile.path is set"
      When call build_module_image custom-df-mod "busybox:latest" "test-image:hash-456"
      The path "$TARGET_DIR/Dockerfile.custom-df-mod" should be file
      The contents of file "$TARGET_DIR/Dockerfile.custom-df-mod" should include "WORKDIR /project"
    End

    It "applies dockerfile.seds transformations"
      build_module_image custom-df-mod "busybox:latest" "test-image:hash-456" 2>/dev/null
      When call cat "$TARGET_DIR/Dockerfile.custom-df-mod"
      The output should include "REPLACED"
      The output should not include "PLACEHOLDER"
    End

    It "adds dockerfile.args as ARG lines"
      build_module_image custom-df-mod "busybox:latest" "test-image:hash-456" 2>/dev/null
      When call cat "$TARGET_DIR/Dockerfile.custom-df-mod"
      The output should include "ARG MY_ARG"
      The output should include "ARG VERSION"
    End
  End
End

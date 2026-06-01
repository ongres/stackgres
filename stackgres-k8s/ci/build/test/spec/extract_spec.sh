# shellcheck shell=sh

Describe "extract_from_image"
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

  It "extracts artifacts with docker create and docker cp without executing the image"
    When call extract_from_image registry.example.com/build:mod-hash-abc some/path/templates
    The status should be success
    # A container is created from the image but never run, so no binary from the
    # image is executed and extraction works across architectures.
    The contents of file "$DOCKER_CALL_LOG" should include "docker_create"
    The contents of file "$DOCKER_CALL_LOG" should include "registry.example.com/build:mod-hash-abc"
    The contents of file "$DOCKER_CALL_LOG" should not include "docker_run"
    The contents of file "$DOCKER_CALL_LOG" should not include "--entrypoint /bin/sh"
    # The requested artifact is copied out of the created container
    The contents of file "$DOCKER_CALL_LOG" should include "docker_cp test-container-id:"
    The contents of file "$DOCKER_CALL_LOG" should include "some/path/templates"
    # The created container is removed afterwards
    The contents of file "$DOCKER_CALL_LOG" should include "docker_rm -fv test-container-id"
  End

  It "pulls the image unless remote manifest is skipped"
    When call extract_from_image registry.example.com/build:mod-hash-abc some/path/templates
    The status should be success
    The contents of file "$DOCKER_CALL_LOG" should include "--pull always"
  End

  It "does not pull when SKIP_REMOTE_MANIFEST is true"
    SKIP_REMOTE_MANIFEST=true
    When call extract_from_image registry.example.com/build:mod-hash-abc some/path/templates
    The status should be success
    The contents of file "$DOCKER_CALL_LOG" should not include "--pull always"
  End
End

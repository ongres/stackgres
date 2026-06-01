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

  It "extracts artifacts by running the image and copying to the mounted volume"
    When call extract_from_image registry.example.com/build:mod-hash-abc some/path/templates
    The status should be success
    # The image is run with /bin/sh as entrypoint to copy artifacts out
    The contents of file "$DOCKER_CALL_LOG" should include "docker_run"
    The contents of file "$DOCKER_CALL_LOG" should include "--entrypoint /bin/sh"
    # The project directory is bind mounted at /out as the copy destination
    The contents of file "$DOCKER_CALL_LOG" should include ":/out"
    # The requested artifact is copied from the running image
    The contents of file "$DOCKER_CALL_LOG" should include "registry.example.com/build:mod-hash-abc"
    The contents of file "$DOCKER_CALL_LOG" should include "for FILE in some/path/templates"
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

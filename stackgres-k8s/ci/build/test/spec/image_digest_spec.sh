# shellcheck shell=sh

Describe "image digest lookup"
  setup() {
    setup_test_project
    source_build_functions
    mock_docker_commands
    mock_get_platform
    SKIP_REMOTE_MANIFEST=true
  }

  cleanup() {
    cleanup_test_project
  }

  Before 'setup'
  After 'cleanup'

  Describe "find_image_digest"
    It "writes digest file on successful manifest retrieval"
      docker_inspect() {
        printf '[{"RepoDigests":["test@sha256:abc123def456"],"Architecture":"amd64"}]'
        return 0
      }
      find_image_digest "registry.gitlab.com/ongresinc/stackgres/build/test-mod:hash-123"
      # find_image_digest writes to image-digests.${IMAGE_NAME##*/} = image-digests.test-mod:hash-123
      When call cat "$TARGET_DIR/image-digests.test-mod:hash-123"
      The output should include "abc123def456"
    End

    It "writes nothing on failed manifest retrieval"
      docker_inspect() { return 1; }
      docker_manifest_inspect() { return 1; }
      find_image_digest "registry.gitlab.com/ongresinc/stackgres/build/test-mod:hash-999" 2>/dev/null || true
      When call test -f "$TARGET_DIR/image-digests.test-mod:hash-999"
      The status should not equal 0
    End
  End

  Describe "find_image_digests deduplication"
    It "deduplicates images via sort|uniq"
      cat > "$TEST_PROJECT_DIR/test-images.txt" << 'EOF'
registry.gitlab.com/ongresinc/stackgres/build/mod-a:hash-aaa
registry.gitlab.com/ongresinc/stackgres/build/mod-a:hash-aaa
registry.gitlab.com/ongresinc/stackgres/build/mod-b:hash-bbb
EOF
      UNIQUE_COUNT="$(sort "$TEST_PROJECT_DIR/test-images.txt" | uniq | wc -l | tr -d ' ')"
      When call echo "$UNIQUE_COUNT"
      The output should equal "2"
    End
  End

  Describe "retrieve_image_manifest"
    It "uses local docker inspect when SKIP_REMOTE_MANIFEST=true"
      SKIP_REMOTE_MANIFEST=true
      DOCKER_CALL_LOG="$TEST_PROJECT_DIR/docker_calls.log"
      : > "$DOCKER_CALL_LOG"
      docker_inspect() {
        echo "docker_inspect $*" >> "$DOCKER_CALL_LOG"
        printf '[{"RepoDigests":["test@sha256:localdigest"],"Architecture":"amd64"}]'
        return 0
      }
      docker_manifest_inspect() {
        echo "docker_manifest_inspect $*" >> "$DOCKER_CALL_LOG"
        return 0
      }
      retrieve_image_manifest "registry.gitlab.com/ongresinc/stackgres/build/test:hash-loc" >/dev/null 2>&1
      When run grep "docker_manifest_inspect" "$DOCKER_CALL_LOG"
      The status should not equal 0
    End

    It "calls docker_inspect first"
      SKIP_REMOTE_MANIFEST=true
      DOCKER_CALL_LOG="$TEST_PROJECT_DIR/docker_calls.log"
      : > "$DOCKER_CALL_LOG"
      docker_inspect() {
        echo "docker_inspect $*" >> "$DOCKER_CALL_LOG"
        printf '[{"RepoDigests":["test@sha256:localonly"],"Architecture":"amd64"}]'
        return 0
      }
      retrieve_image_manifest "registry.gitlab.com/ongresinc/stackgres/build/test:hash-skip" >/dev/null 2>&1
      When call grep -c "docker_inspect" "$DOCKER_CALL_LOG"
      The output should equal "1"
    End
  End
End

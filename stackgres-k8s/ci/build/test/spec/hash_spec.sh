# shellcheck shell=sh

Describe "git-based hashing"
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

  Describe "init_hash"
    It "creates git repo in target if none exists"
      When call init_hash
      The path "$TARGET_DIR/.git" should be directory
    End

    It "reuses existing valid git repo"
      init_test_git  # Creates .git in project root
      # Now init_hash should work with the existing repo
      When call init_hash
      The path "$TARGET_DIR/.git" should be directory
    End

    It "recovers from corrupted git repo"
      mkdir -p "$TARGET_DIR/.git"
      echo "corrupted" > "$TARGET_DIR/.git/HEAD"
      When call init_hash
      The path "$TARGET_DIR/.git" should be directory
      # Should have been re-created
      The contents of file "$TARGET_DIR/.git/HEAD" should not equal "corrupted"
      The stderr should be defined  # git may emit error messages during recovery
    End
  End

  Describe "path_hash"
    setup_with_git() {
      setup_test_project
      source_build_functions
      mock_docker_commands
      init_test_git
      init_hash
    }

    Before 'setup_with_git'
    After 'cleanup'

    It "returns deterministic hash for file content"
      HASH1="$(path_hash test/module-a/src/file.txt)"
      When call path_hash test/module-a/src/file.txt
      The output should equal "$HASH1"
    End

    It "changes when file content changes"
      HASH_BEFORE="$(path_hash test/module-a/src/file.txt)"
      echo "changed content" > "$TEST_PROJECT_DIR/test/module-a/src/file.txt"
      (cd "$TEST_PROJECT_DIR" && git --git-dir "$TARGET_DIR/.git" add . && \
        git --git-dir "$TARGET_DIR/.git" -c user.name=ci -c user.email= commit -q -m "change" --no-gpg-sign) 2>/dev/null
      When call path_hash test/module-a/src/file.txt
      The output should not equal "$HASH_BEFORE"
    End
  End

  Describe "project_hash"
    setup_with_git() {
      setup_test_project
      source_build_functions
      mock_docker_commands
      init_test_git
      init_hash
    }

    Before 'setup_with_git'
    After 'cleanup'

    It "includes HEAD tree hash and environment"
      When call project_hash
      The output should not be blank
      The lines of output should equal 2
    End
  End
End

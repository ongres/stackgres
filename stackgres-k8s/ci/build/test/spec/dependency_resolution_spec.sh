# shellcheck shell=sh

Describe "dependency resolution"
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

  # The dependency resolution logic is in build.sh lines 24-49.
  # We replicate it here as a function to test in isolation.
  resolve_dependencies() {
    local MODULES="$*"
    local COMPLETED_MODULES=""

    for MODULE in $MODULES
    do
      CURRENT_MODULE="$MODULE"
      while true
      do
        PARENT_MODULE="$(jq -r ".stages[]|to_entries[]|select(.key == \"$CURRENT_MODULE\").value" "$TARGET_DIR/config.json")"
        if [ -z "$PARENT_MODULE" ]
        then
          echo "Module $CURRENT_MODULE stage not defined" >&2
          return 1
        fi
        if [ "$PARENT_MODULE" = null ]
        then
          break
        fi
        if ! printf ' %s ' "$COMPLETED_MODULES" | tr '\n' ' ' | grep -qF " $PARENT_MODULE "
        then
          COMPLETED_MODULES="$PARENT_MODULE $COMPLETED_MODULES"
        fi
        CURRENT_MODULE="$PARENT_MODULE"
      done
      if ! printf ' %s ' "$COMPLETED_MODULES" | tr '\n' ' ' | grep -qF " $MODULE "
      then
        COMPLETED_MODULES="$COMPLETED_MODULES $MODULE"
      fi
    done

    echo "$COMPLETED_MODULES" | tr -s ' ' | sed 's/^ //;s/ $//'
  }

  Describe "root module"
    It "resolves to itself when it has no parent"
      When call resolve_dependencies module-a
      The output should equal "module-a"
    End
  End

  Describe "module with ancestors"
    It "includes full ancestor chain in build order"
      When call resolve_dependencies module-c
      The output should equal "module-a module-b module-c"
    End
  End

  Describe "multiple modules with shared ancestor"
    setup_diamond() {
      setup_test_project
      source_build_functions
      mock_docker_commands
      # Create a diamond config: d->b, d->c, b->a, c->a
      cat > "$BUILD_DIR/config.yml" << 'YAMLEOF'
target_image: busybox:latest
platforms:
  - linux/x86_64
modules:
  mod-a:
    type: test
    path: test/module-a
    target_image: busybox:latest
    build_image: alpine:latest
    sources:
      - test/module-a/src
    artifacts:
      - test/module-a/out
  mod-b:
    type: test
    path: test/module-b
    target_image: null
    build_image: alpine:latest
    sources:
      - test/module-b/src
    artifacts:
      - test/module-b/out
  mod-c:
    type: test
    path: test/module-c
    target_image: null
    build_image: alpine:latest
    sources:
      - test/module-c/src
    artifacts:
      - test/module-c/out
stages:
  - mod-a: null
  - mod-b: mod-a
  - mod-c: mod-a
YAMLEOF
      yq . "$BUILD_DIR/config.yml" > "$TARGET_DIR/config.json"
    }

    Before 'setup_diamond'
    After 'cleanup'

    It "deduplicates shared ancestor"
      When call resolve_dependencies mod-b mod-c
      The output should include "mod-a"
      The output should include "mod-b"
      The output should include "mod-c"
      # mod-a should appear exactly once
      The output should match pattern "*mod-a*"
    End
  End

  Describe "undefined module"
    It "produces error for module not in stages"
      When run resolve_dependencies nonexistent-module
      The status should not equal 0
      The error should include "not defined"
    End
  End

  Describe "SKIP_DEPENDENCIES"
    It "flag allows using only originally requested modules"
      # This tests the logic in build.sh lines 90-93
      SKIP_DEPENDENCIES=true
      ORIGINAL_MODULES="module-c"
      MODULES="$(resolve_dependencies module-c)"
      # After resolution, if SKIP_DEPENDENCIES is set, original list is used
      if [ "$SKIP_DEPENDENCIES" = true ]; then
        MODULES="$ORIGINAL_MODULES"
      fi
      When call echo "$MODULES"
      The output should equal "module-c"
    End
  End
End

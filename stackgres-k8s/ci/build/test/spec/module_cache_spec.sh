# shellcheck shell=sh

Describe "module cache volumes"
  setup() {
    setup_test_project
    source_build_functions
    mock_docker_commands
    mock_get_platform
    BUILD_CACHE_PATH="$TEST_PROJECT_DIR/cache"
  }

  cleanup() {
    cleanup_test_project
  }

  Before 'setup'
  After 'cleanup'

  Describe "module_cache_volume_opts"
    # The cache of a module is a list, so each entry needs its own option, and
    # its own directory under BUILD_CACHE_PATH: mounting BUILD_CACHE_PATH itself
    # at every entry would give a module its cache under the name of another.
    It "emits one option per cache entry, each with its own directory"
      BUILD_CACHE_PATH=/cache
      When call module_cache_volume_opts .m2/ stackgres-k8s/src/admin-ui/node_modules
      The status should be success
      The output should equal "--volume /cache/.m2:/project/.m2:rw --volume /cache/stackgres-k8s/src/admin-ui/node_modules:/project/stackgres-k8s/src/admin-ui/node_modules:rw"
    End

    It "emits nothing for a module that declares no cache"
      When call module_cache_volume_opts
      The status should be success
      The output should equal ""
    End
  End

  Describe "prepare_module_cache"
    # A bind mount of a path that does not exist yet is created by the daemon
    # and owned by root, which the build user could not write into.
    It "creates the cache directories before docker mounts them"
      prepare_module_cache .m2/ node_modules
      When call test -d "$BUILD_CACHE_PATH/.m2" -a -d "$BUILD_CACHE_PATH/node_modules"
      The status should be success
    End
  End

  Describe "run_commands_in_container"
    write_config_with_cache() {
      cat > "$TARGET_DIR/config.json" << 'EOF'
{ "modules": { "cache-mod": {
  "path": ".",
  "cache": [ ".m2/", "node_modules" ],
  "build_env": {} } } }
EOF
    }

    # A cache read as a whole JSON array used to reach docker as the pretty
    # printed array, whose words made docker take one of them as the image:
    # "docker: invalid reference format".
    It "passes a cache list as well formed options"
      write_config_with_cache
      When call run_commands_in_container cache-mod registry.example.com/build:mod-hash-abc 1000:1000 'echo ok'
      The status should be success
      The contents of file "$DOCKER_CALL_LOG" should include "--volume $BUILD_CACHE_PATH/.m2:/project/.m2:rw"
      The contents of file "$DOCKER_CALL_LOG" should include "--volume $BUILD_CACHE_PATH/node_modules:/project/node_modules:rw"
      # No leftovers of the JSON array, and the image is still the image
      The contents of file "$DOCKER_CALL_LOG" should not include "["
      The contents of file "$DOCKER_CALL_LOG" should include "/bin/sh registry.example.com/build:mod-hash-abc -ec"
    End
  End
End

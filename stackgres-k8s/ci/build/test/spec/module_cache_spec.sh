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

  Describe "module_cache_artifact_paths"
    write_config_with_artifacts() {
      cat > "$TARGET_DIR/config.json" << 'EOF'
{ "modules": {
  "producer": { "cache": [ ".m2/" ],
    "artifacts": [ "src/producer/target", ".m2/repository/io/stackgres/producer" ] },
  "other": { "cache": [ ".m2/" ],
    "artifacts": [ ".m2/repository/io/stackgres/other/" ] },
  "consumer": { "cache": [ ".m2/" ], "artifacts": [ "src/consumer/target/runner" ] } } }
EOF
    }

    # The artifacts of every module count, not only the ones of the module being
    # built: a module that produces nothing under the cache still has to read
    # there what its source module installed.
    It "lists the artifact paths of the whole build that fall under the cache path"
      write_config_with_artifacts
      When call module_cache_artifact_paths .m2/
      The status should be success
      The line 1 of output should equal ".m2/repository/io/stackgres/other"
      The line 2 of output should equal ".m2/repository/io/stackgres/producer"
      The lines of output should equal 2
    End

    It "lists nothing for a cache path no artifact falls under"
      write_config_with_artifacts
      When call module_cache_artifact_paths src/producer/target
      The status should be success
      The output should equal ""
    End
  End

  Describe "module_cache_volume_opts with artifacts under the cache"
    write_config_with_artifacts() {
      cat > "$TARGET_DIR/config.json" << 'EOF'
{ "modules": { "producer": { "cache": [ ".m2/" ],
    "artifacts": [ ".m2/repository/io/stackgres/producer" ] } } }
EOF
    }

    # The cache mount would otherwise swallow what the module installs, which the
    # image of the module is then built from: the docker build context is the
    # project directory, so the artifact has to resolve there.
    It "mounts the artifact paths back from the project directory"
      write_config_with_artifacts
      BUILD_CACHE_PATH=/cache
      PROJECT_PATH=/builds/stackgres
      When call module_cache_volume_opts .m2/
      The status should be success
      The output should equal "--volume /cache/.m2:/project/.m2:rw --volume /builds/stackgres/.m2/repository/io/stackgres/producer:/project/.m2/repository/io/stackgres/producer:rw"
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

    # Both ends of the artifact mount: the project side is what gets mounted, and
    # the cache side is the mount point. Without the second one the daemon creates
    # the directories leading to it, owned by root, inside a cache the build user
    # then can not write into.
    It "creates both ends of the mount of an artifact under the cache"
      cat > "$TARGET_DIR/config.json" << 'EOF'
{ "modules": { "producer": { "cache": [ ".m2/" ],
    "artifacts": [ ".m2/repository/io/stackgres/producer" ] } } }
EOF
      PROJECT_PATH="$TEST_PROJECT_DIR/project"
      prepare_module_cache .m2/
      When call test -d "$PROJECT_PATH/.m2/repository/io/stackgres/producer" \
        -a -d "$BUILD_CACHE_PATH/.m2/repository/io/stackgres/producer"
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

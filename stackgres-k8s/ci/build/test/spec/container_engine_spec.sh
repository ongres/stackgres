# shellcheck shell=sh

Describe "container engine"
  setup() {
    setup_test_project
    mock_container_engine
    unset CONTAINER_ENGINE
    source_build_functions
  }

  cleanup() {
    cleanup_test_project
  }

  Before 'setup'
  After 'cleanup'

  Describe "engine selection"
    It "defaults to docker"
      When call docker_images
      The contents of file "$ENGINE_CALL_LOG" should include "docker images"
    End

    It "uses podman when CONTAINER_ENGINE is set to podman"
      CONTAINER_ENGINE=podman
      When call docker_images
      The contents of file "$ENGINE_CALL_LOG" should include "podman images"
      The contents of file "$ENGINE_CALL_LOG" should not include "docker images"
    End

    It "supports an engine with arguments"
      CONTAINER_ENGINE="podman --remote"
      When call docker_inspect some-image
      The contents of file "$ENGINE_CALL_LOG" should include "podman --remote inspect some-image"
    End
  End

  Describe "container_engine_is_podman"
    Parameters
      podman success
      "podman --remote" success
      /usr/bin/podman success
      docker failure
      "docker --context foo" failure
    End

    It "detects podman in $1"
      CONTAINER_ENGINE="$1"
      When call container_engine_is_podman
      The status should be "$2"
    End
  End

  Describe "docker_push"
    It "appends the host platform with docker"
      When call docker_push some-image
      The contents of file "$ENGINE_CALL_LOG" should include "docker push --platform=linux/"
      The contents of file "$ENGINE_CALL_LOG" should not include "--remove-signatures"
    End

    It "drops the signatures with podman"
      CONTAINER_ENGINE=podman
      When call docker_push some-image
      The contents of file "$ENGINE_CALL_LOG" should include "podman push --remove-signatures some-image"
    End

    It "drops --platform <value> with podman"
      CONTAINER_ENGINE=podman
      When call docker_push --platform linux/arm64 some-image
      The contents of file "$ENGINE_CALL_LOG" should include "podman push --remove-signatures some-image"
      The contents of file "$ENGINE_CALL_LOG" should not include "--platform"
    End

    It "drops --platform=<value> with podman"
      CONTAINER_ENGINE=podman
      When call docker_push --platform=linux/arm64 some-image
      The contents of file "$ENGINE_CALL_LOG" should include "podman push --remove-signatures some-image"
      The contents of file "$ENGINE_CALL_LOG" should not include "--platform"
    End

    It "keeps the other options and their order with podman"
      CONTAINER_ENGINE=podman
      When call docker_push --tls-verify=false --platform linux/arm64 some-image
      The contents of file "$ENGINE_CALL_LOG" should include "podman push --remove-signatures --tls-verify=false some-image"
    End
  End

  Describe "docker_buildx_inspect"
    It "delegates to buildx with docker"
      When call docker_buildx_inspect --bootstrap
      The contents of file "$ENGINE_CALL_LOG" should include "docker buildx inspect --bootstrap"
    End

    It "emulates the Platforms line with podman"
      CONTAINER_ENGINE=podman
      When call docker_buildx_inspect --bootstrap
      The output should equal "Platforms: linux/amd64"
      The contents of file "$ENGINE_CALL_LOG" should not include "buildx"
    End
  End

  Describe "docker_engine_platform"
    It "reads the server platform with docker"
      When call docker_engine_platform
      The output should equal "linux/amd64"
      The contents of file "$ENGINE_CALL_LOG" should include "docker version --format"
    End

    It "reads the platform from info with podman"
      CONTAINER_ENGINE=podman
      When call docker_engine_platform
      The output should equal "linux/amd64"
      The contents of file "$ENGINE_CALL_LOG" should include "podman info --format"
    End
  End

  Describe "docker_manifest_inspect"
    It "delegates to the engine with docker"
      When call docker_manifest_inspect -v some-image
      The contents of file "$ENGINE_CALL_LOG" should include "docker manifest inspect -v some-image"
    End

    It "describes a single image through skopeo with podman"
      CONTAINER_ENGINE=podman
      mock_skopeo
      When call docker_manifest_inspect -v some-image
      The output should include '"Ref": "some-image"'
      The output should include '"digest": "sha256:aaaa"'
      The output should include '"architecture": "amd64"'
      The output should include '"size": 76'
      The contents of file "$ENGINE_CALL_LOG" should not include "podman manifest"
    End

    It "describes a manifest list as the array docker gives with podman"
      CONTAINER_ENGINE=podman
      mock_skopeo
      When call docker_manifest_inspect -v some-image-list
      The output should include '"Ref": "some-image-list@sha256:bbbb"'
      The output should include '"digest": "sha256:cccc"'
      The output should include '"architecture": "arm64"'
      The contents of file "$ENGINE_CALL_LOG" should not include "podman manifest"
    End
  End

  Describe "docker_login"
    It "tests the stored credentials with docker"
      When call docker_login some-registry
      The contents of file "$ENGINE_CALL_LOG" should include "docker login some-registry"
    End

    It "asks podman for the stored login instead of letting it prompt"
      CONTAINER_ENGINE=podman
      When call docker_login some-registry
      The contents of file "$ENGINE_CALL_LOG" should include "podman login --get-login some-registry"
    End

    It "delegates a login with credentials to podman"
      CONTAINER_ENGINE=podman
      When call docker_login -u some-user -p some-password some-registry
      The contents of file "$ENGINE_CALL_LOG" should include "podman login -u some-user -p some-password some-registry"
      The contents of file "$ENGINE_CALL_LOG" should not include "--get-login"
    End
  End

  Describe "docker_manifest_create"
    It "frees the name of the list from a local image with podman"
      CONTAINER_ENGINE=podman
      When call docker_manifest_create some-image some-image-amd64
      The contents of file "$ENGINE_CALL_LOG" should include "podman rmi some-image"
      The contents of file "$ENGINE_CALL_LOG" should include "podman manifest create some-image some-image-amd64"
    End

    It "skips the options when looking for the name of the list"
      CONTAINER_ENGINE=podman
      When call docker_manifest_create --amend some-image some-image-amd64
      The contents of file "$ENGINE_CALL_LOG" should include "podman rmi some-image"
      The contents of file "$ENGINE_CALL_LOG" should not include "podman rmi --amend"
    End

    It "does not remove anything with docker"
      When call docker_manifest_create some-image some-image-amd64
      The contents of file "$ENGINE_CALL_LOG" should not include "rmi"
    End
  End

  Describe "manifest shims"
    It "creates a manifest through the engine"
      CONTAINER_ENGINE=podman
      When call docker_manifest_create some-image some-image-amd64
      The contents of file "$ENGINE_CALL_LOG" should include "podman manifest create some-image some-image-amd64"
    End

    It "pushes a manifest through the engine"
      When call docker_manifest_push some-image
      The contents of file "$ENGINE_CALL_LOG" should include "docker manifest push some-image"
    End
  End

  Describe "BUILD_UID"
    build_uid() {
      # BUILD_UID is only computed when unset, so re-source with it cleared
      unset BUILD_UID
      # shellcheck disable=SC1090
      eval "$(sed -n '/^if \[ -z "\$BUILD_UID" \]/,/^export BUILD_UID$/p' \
        "$SHELLSPEC_SPECDIR/../../build-functions.sh")"
      printf %s "$BUILD_UID"
    }

    It "is the root of the container with podman"
      CONTAINER_ENGINE=podman
      When call build_uid
      The output should equal "0:0"
    End

    It "is the invoking user and the docker socket group with docker"
      When call build_uid
      The output should start with "$(id -u):"
    End
  End

  Describe "container_engine_socket_volume"
    It "mounts the socket of docker where docker has it"
      CONTAINER_ENGINE_SOCKET_PATH=/var/run/docker.sock
      When call container_engine_socket_volume
      The output should equal "--volume /var/run/docker.sock:/var/run/docker.sock"
    End

    It "mounts the socket of podman where docker has it, so nothing has to know"
      CONTAINER_ENGINE=podman
      CONTAINER_ENGINE_SOCKET_PATH=/run/user/1000/podman/podman.sock
      When call container_engine_socket_volume
      The output should equal "--volume /run/user/1000/podman/podman.sock:/var/run/docker.sock"
    End

    It "mounts nothing when there is no socket to mount"
      CONTAINER_ENGINE=podman
      XDG_RUNTIME_DIR="$TEST_PROJECT_DIR"
      container_engine_socket_path() { printf ''; }
      When call container_engine_socket_volume
      The output should equal ""
    End
  End

  Describe "container_engine_testcontainers_env"
    It "does not set anything with docker"
      When call container_engine_testcontainers_env
      The output should equal ""
    End

    It "points Testcontainers at the socket and disables the reaper with podman"
      CONTAINER_ENGINE=podman
      CONTAINER_ENGINE_SOCKET_PATH=/run/user/1000/podman/podman.sock
      When call container_engine_testcontainers_env
      The output should include "--env DOCKER_HOST=unix:///var/run/docker.sock"
      The output should include "--env TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/run/user/1000/podman/podman.sock"
      The output should include "--env TESTCONTAINERS_HOST_OVERRIDE=host.containers.internal"
      The output should include "--env TESTCONTAINERS_RYUK_DISABLED=true"
    End
  End

  Describe "get_free_port"
    It "returns a port in the ephemeral range"
      When call get_free_port
      The output should match pattern "[0-9]*"
      The status should be success
    End
  End
End

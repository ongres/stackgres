#!/bin/sh

. "$(dirname "$0")/e2e"

setup_images

export E2E_DOCKER_IMAGE="${E2E_DOCKER_IMAGE:-$(grep -n '</\?properties>' "$STACKGRES_PATH/src/pom.xml" | cut -d : -f 1 | tr '\n' ':' \
  | xargs -I % sh -c 'head -n "$(echo % | cut -d : -f 2)" '"$STACKGRES_PATH/src/pom.xml"' | tail -n "$(( $(echo % | cut -d : -f 2) - $(echo % | cut -d : -f 1) - 1 ))"' \
  | grep -o '<it.image>\([^<]\+\)</it.image>' | tr -d ' ' | tr '<>' '  ' | cut -d ' ' -f 3)}"
export E2E_CONTAINER_NAME="${E2E_CONTAINER_NAME:-stackgres-e2e}"

if ! docker inspect "$E2E_DOCKER_IMAGE" > /dev/null 2>&1
then
  docker build -f "$STACKGRES_PATH/src/operator/src/test/docker/Dockerfile.it" -t "$E2E_DOCKER_IMAGE" .
fi

ENV_PATH="$TARGET_PATH/envs.$$"
export E2E_BUILD_IMAGES=false
export K8S_FROM_DIND=true
env | grep '^\(E2E_\|K8S_\|KIND_\|JAVA_\)' > "$ENV_PATH"
if docker ps -a | grep -q "\s$E2E_CONTAINER_NAME$"
then
  docker rm -fv "$E2E_CONTAINER_NAME"
fi
docker run "$([ -t 1 ] && echo '-i' || echo '-it')" \
  --rm --name "$E2E_CONTAINER_NAME" \
  -v /etc/passwd:/etc/passwd:ro -v /etc/group:/etc/group:ro \
  -v /etc/shadow:/etc/shadow:ro -v /etc/gshadow:/etc/gshadow:ro \
  -u "$(id -u):$(id -g)" \
  $(groups | tr ' ' '\n' | grep -vF "$(cat /etc/group | grep ":$(id -g):" | cut -d : -f 1)" \
    | xargs -r -n 1 -I % sh -c 'cat /etc/group | grep "^%:" | cut -d : -f 3' | xargs -r -n 1 echo " --group-add ") \
  -v "$HOME":"$HOME":rw -e PROMPT_COMMAND= \
  -v /var/run/docker.sock:/var/run/docker.sock -v "$PROJECT_PATH:/stackgres" -w /stackgres \
  --env-file "$ENV_PATH" "$E2E_DOCKER_IMAGE" "$@"

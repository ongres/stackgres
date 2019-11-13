#!/bin/sh

set -e

IMAGE_NAME="${IMAGE_NAME:-"stackgres/operator:development-jvm"}"
CONTAINER_BASE=$(buildah from "azul/zulu-openjdk-alpine:8-jre")

JAVA_OPTS="-Djava.net.preferIPv4Stack=true -XX:MaxRAMPercentage=85.0"
APP_OPTS="-Dquarkus.http.host=0.0.0.0 -Dquarkus.http.port=8080 -Dquarkus.http.ssl-port=8443 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"

if [ "$DEBUG_OPERATOR" = true ]
then
  JAVA_OPTS="$JAVA_OPTS -agentlib:jdwp=transport=dt_socket,server=y,address=8000,suspend=$([ "$DEBUG_OPERATOR_SUSPEND" = true ] && echo y || echo n)"
fi

# Include binaries
buildah config --workingdir='/app/' "$CONTAINER_BASE"
buildah copy --chown nobody:nobody "$CONTAINER_BASE" 'operator/target/stackgres-operator-runner.jar' '/app/stackgres-operator.jar'
buildah copy --chown nobody:nobody "$CONTAINER_BASE" 'operator/target/lib/*' '/app/lib/'
buildah run "$CONTAINER_BASE" -- chmod 775 '/app'

## Run our server and expose the port
buildah config --cmd "java $JAVA_OPTS -jar /app/stackgres-operator.jar $APP_OPTS" "$CONTAINER_BASE"
buildah config --port 8080 "$CONTAINER_BASE"
buildah config --port 8443 "$CONTAINER_BASE"
buildah config --user nobody:nobody "$CONTAINER_BASE"

## Commit this container to an image name
buildah commit --squash "$CONTAINER_BASE" "$IMAGE_NAME"
buildah push "$IMAGE_NAME" docker-daemon:$IMAGE_NAME

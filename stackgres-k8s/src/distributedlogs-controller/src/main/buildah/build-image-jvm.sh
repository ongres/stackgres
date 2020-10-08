#!/bin/sh

set -e

DISTRIBUTEDLOGS_CONTROLLER_IMAGE_NAME="${DISTRIBUTEDLOGS_CONTROLLER_IMAGE_NAME:-"stackgres/distributedlogs-controller:development-jvm"}"
CONTAINER_BASE=$(buildah from "azul/zulu-openjdk-alpine:11.0.8-jre-headless")
TARGET_DISTRIBUTEDLOGS_CONTROLLER_IMAGE_NAME="${TARGET_DISTRIBUTEDLOGS_CONTROLLER_IMAGE_NAME:-docker-daemon:$DISTRIBUTEDLOGS_CONTROLLER_IMAGE_NAME}"

# Include binaries
buildah config --workingdir='/app/' "$CONTAINER_BASE"
buildah copy --chown nobody:nobody "$CONTAINER_BASE" 'distributedlogs-controller/target/stackgres-distributedlogs-controller-runner.jar' '/app/stackgres-distributedlogs-controller.jar'
buildah copy --chown nobody:nobody "$CONTAINER_BASE" 'distributedlogs-controller/target/lib/*' '/app/lib/'
cat << 'EOF' > distributedlogs-controller/target/stackgres-distributedlogs-controller.sh
#!/bin/sh

JAVA_OPTS="${JAVA_OPTS:-"-Djava.net.preferIPv4Stack=true -Djava.awt.headless=true -XX:MaxRAMPercentage=75.0"}"
APP_OPTS="${APP_OPTS:-"-Dquarkus.http.host=0.0.0.0 -Dquarkus.http.port=8080 -Dquarkus.http.ssl-port=8443 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"}"
if [ "$DEBUG_DISTRIBUTEDLOGS_CONTROLLER" = true ]
then
  set -x
  JAVA_OPTS="$JAVA_OPTS -agentlib:jdwp=transport=dt_socket,server=y,address=8000,suspend=$([ "$DEBUG_DISTRIBUTEDLOGS_CONTROLLER_SUSPEND" = true ] && echo y || echo n)"
fi
if [ -n "$DISTRIBUTEDLOGS_CONTROLLER_LOG_LEVEL" ]
then
  JAVA_OPTS="$JAVA_OPTS -Dquarkus.log.level=$DISTRIBUTEDLOGS_CONTROLLER_LOG_LEVEL"
fi
if [ "$DISTRIBUTEDLOGS_CONTROLLER_SHOW_STACK_TRACES" = true ]
then
  JAVA_OPTS="$JAVA_OPTS -Dquarkus.log.console.format=%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c{4.}] (%t) %s%e%n"
fi
JAVA_JAR="-jar /app/stackgres-distributedlogs-controller.jar"
exec java $JAVA_OPTS $JAVA_JAR $APP_OPTS
EOF
buildah copy --chown nobody:nobody "$CONTAINER_BASE" 'distributedlogs-controller/target/stackgres-distributedlogs-controller.sh' '/app/'
buildah run "$CONTAINER_BASE" -- chmod 775 '/app'

## Run our server and expose the port
buildah config --cmd 'sh /app/stackgres-distributedlogs-controller.sh' "$CONTAINER_BASE"
buildah config --port 8080 "$CONTAINER_BASE"
buildah config --port 8443 "$CONTAINER_BASE"
buildah config --user nobody:nobody "$CONTAINER_BASE"

## Commit this container to an image name
buildah commit "$CONTAINER_BASE" "$DISTRIBUTEDLOGS_CONTROLLER_IMAGE_NAME"
buildah push -f "${BUILDAH_PUSH_FORMAT:-docker}" "$DISTRIBUTEDLOGS_CONTROLLER_IMAGE_NAME" "$TARGET_DISTRIBUTEDLOGS_CONTROLLER_IMAGE_NAME"
buildah delete "$CONTAINER_BASE"

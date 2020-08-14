#!/bin/sh

set -e

OPERATOR_IMAGE_NAME="${OPERATOR_IMAGE_NAME:-"stackgres/operator:development-jvm"}"
CONTAINER_BASE=$(buildah from "azul/zulu-openjdk-alpine:11.0.8-jre-headless")
TARGET_OPERATOR_IMAGE_NAME="${TARGET_OPERATOR_IMAGE_NAME:-docker-daemon:$OPERATOR_IMAGE_NAME}"

# Include binaries
buildah config --workingdir='/app/' "$CONTAINER_BASE"
buildah copy --chown nobody:nobody "$CONTAINER_BASE" 'operator/target/stackgres-operator-runner.jar' '/app/stackgres-operator.jar'
buildah copy --chown nobody:nobody "$CONTAINER_BASE" 'operator/target/lib/*' '/app/lib/'
cat << 'EOF' > operator/target/stackgres-operator.sh
#!/bin/sh

JAVA_OPTS="${JAVA_OPTS:-"-Djava.net.preferIPv4Stack=true -Djava.awt.headless=true -XX:MaxRAMPercentage=75.0"}"
APP_OPTS="${APP_OPTS:-"-Dquarkus.http.host=0.0.0.0 -Dquarkus.http.port=8080 -Dquarkus.http.ssl-port=8443 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"}"
if [ "$DEBUG_OPERATOR" = true ]
then
  set -x
  JAVA_OPTS="$JAVA_OPTS -agentlib:jdwp=transport=dt_socket,server=y,address=8000,suspend=$([ "$DEBUG_OPERATOR_SUSPEND" = true ] && echo y || echo n)"
fi
if [ -n "$OPERATOR_LOG_LEVEL" ]
then
  JAVA_OPTS="$JAVA_OPTS -Dquarkus.log.level=$OPERATOR_LOG_LEVEL"
fi
if [ "$OPERATOR_SHOW_STACK_TRACES" = true ]
then
  JAVA_OPTS="$JAVA_OPTS -Dquarkus.log.console.format=%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c{4.}] (%t) %s%e%n"
fi
JAVA_JAR="-jar /app/stackgres-operator.jar"
exec java $JAVA_OPTS $JAVA_JAR $APP_OPTS
EOF
buildah copy --chown nobody:nobody "$CONTAINER_BASE" 'operator/target/stackgres-operator.sh' '/app/'
buildah run "$CONTAINER_BASE" -- chmod 775 '/app'

## Run our server and expose the port
buildah config --cmd 'sh /app/stackgres-operator.sh' "$CONTAINER_BASE"
buildah config --port 8080 "$CONTAINER_BASE"
buildah config --port 8443 "$CONTAINER_BASE"
buildah config --user nobody:nobody "$CONTAINER_BASE"

## Commit this container to an image name
buildah commit "$CONTAINER_BASE" "$OPERATOR_IMAGE_NAME"
buildah push -f "${BUILDAH_PUSH_FORMAT:-docker}" "$OPERATOR_IMAGE_NAME" "$TARGET_OPERATOR_IMAGE_NAME"
buildah delete "$CONTAINER_BASE"

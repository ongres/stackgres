#!/bin/sh

set -e

RESTAPI_IMAGE_NAME="${RESTAPI_IMAGE_NAME:-"stackgres/restapi:development-jvm"}"
CONTAINER_BASE=$(buildah from "registry.access.redhat.com/ubi8/openjdk-11:1.3-10")
TARGET_RESTAPI_IMAGE_NAME="${TARGET_RESTAPI_IMAGE_NAME:-docker-daemon:$RESTAPI_IMAGE_NAME}"

# Include binaries
buildah config --user root:root "$CONTAINER_BASE"
buildah config --workingdir='/app/' "$CONTAINER_BASE"
buildah copy --chown jboss:jboss "$CONTAINER_BASE" 'api-web/target/stackgres-restapi-runner.jar' '/app/stackgres-restapi.jar'
buildah copy --chown jboss:jboss "$CONTAINER_BASE" 'api-web/target/lib/*' '/app/lib/'
cat << 'EOF' > api-web/target/stackgres-restapi.sh
#!/bin/sh

JAVA_OPTS="${JAVA_OPTS:-"-Djava.net.preferIPv4Stack=true -Djava.awt.headless=true -XX:MaxRAMPercentage=75.0"}"
APP_OPTS="${APP_OPTS:-"-Dquarkus.http.host=0.0.0.0 -Dquarkus.http.port=8080 -Dquarkus.http.ssl-port=8443 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"}"
if [ "$DEBUG_RESTAPI" = true ]
then
  set -x
  JAVA_OPTS="$JAVA_OPTS -agentlib:jdwp=transport=dt_socket,server=y,address=8000,suspend=$([ "$DEBUG_RESTAPI_SUSPEND" = true ] && echo y || echo n)"
fi
if [ -n "$RESTAPI_LOG_LEVEL" ]
then
  JAVA_OPTS="$JAVA_OPTS -Dquarkus.log.level=$RESTAPI_LOG_LEVEL"
fi
if [ "$RESTAPI_SHOW_STACK_TRACES" = true ]
then
  JAVA_OPTS="$JAVA_OPTS -Dquarkus.log.console.format=%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c{4.}] (%t) %s%e%n"
fi
JAVA_JAR="-jar /app/stackgres-restapi.jar"
exec java $JAVA_OPTS $JAVA_JAR $APP_OPTS
EOF
buildah copy --chown jboss:jboss "$CONTAINER_BASE" 'api-web/target/stackgres-restapi.sh' '/app/'
buildah run "$CONTAINER_BASE" chmod 775 '/home/jboss' -R
buildah run "$CONTAINER_BASE" chmod 775 '/app' -R
buildah run "$CONTAINER_BASE" chmod 775 '/app/stackgres-restapi.sh'

## Run our server and expose the port
buildah config --cmd 'sh /app/stackgres-restapi.sh' "$CONTAINER_BASE"
buildah config --user jboss:jboss "$CONTAINER_BASE"
buildah config --env 'HOME=/app' "$CONTAINER_BASE"
buildah config --port 8080 "$CONTAINER_BASE"
buildah config --port 8443 "$CONTAINER_BASE"
buildah config --env LANG="C.UTF-8" --env LC_ALL="C.UTF-8" "$CONTAINER_BASE"

## Commit this container to an image name
buildah commit "$CONTAINER_BASE" "$RESTAPI_IMAGE_NAME"
buildah push -f "${BUILDAH_PUSH_FORMAT:-docker}" "$RESTAPI_IMAGE_NAME" "$TARGET_RESTAPI_IMAGE_NAME"
buildah delete "$CONTAINER_BASE"

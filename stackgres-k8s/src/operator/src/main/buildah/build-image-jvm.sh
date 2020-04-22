#!/bin/sh

set -e

IMAGE_NAME="${IMAGE_NAME:-"stackgres/operator:development-jvm"}"
CONTAINER_BASE=$(buildah from "azul/zulu-openjdk-alpine:8u242-jre")

# Include binaries
buildah config --workingdir='/app/' "$CONTAINER_BASE"
buildah copy --chown nobody:nobody "$CONTAINER_BASE" 'operator/target/stackgres-operator-runner.jar' '/app/stackgres-operator.jar'
buildah copy --chown nobody:nobody "$CONTAINER_BASE" 'operator/target/lib/*' '/app/lib/'
cat << 'EOF' > operator/target/stackgres-operator.sh
#!/bin/sh

JAVA_OPTS="${JAVA_OPTS:-"-Djava.net.preferIPv4Stack=true -XX:MaxRAMPercentage=85.0"}"
APP_OPTS="${APP_OPTS:-"-Dquarkus.http.host=0.0.0.0 -Dquarkus.http.port=8080 -Dquarkus.http.ssl-port=8443 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"}"
if [ "$DEBUG_OPERATOR" = true ]
then
  set -x
  JAVA_OPTS="$JAVA_OPTS -agentlib:jdwp=transport=dt_socket,server=y,address=8000,suspend=$([ "$DEBUG_OPERATOR_SUSPEND" = true ] && echo y || echo n)"
fi
if [ ! -z "$OPERATOR_LOG_LEVEL" ]
then
  JAVA_OPTS="$JAVA_OPTS -Dquarkus.log.level=$OPERATOR_LOG_LEVEL"
fi
if [ "$OPERATOR_SHOW_STACK_TRACES" = true ]
then
  JAVA_OPTS="$JAVA_OPTS -Dquarkus.log.console.format=%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c{4.}] (%t) %s%e%n"
fi
JAVA_JAR="-jar /app/stackgres-operator.jar"
if [ "$OPERATOR_UNCOMPRESSED" = true ]
then
  (
  mkdir -p /tmp/stackgres-operator
  cd /tmp/stackgres-operator
  unzip /app/stackgres-operator.jar
  cp -a /app/stackgres-operator.jar .
  cp -a /app/lib lib
  )
  JAVA_OPTS="$JAVA_OPTS -cp /tmp/stackgres-operator:/tmp/stackgres-operator/stackgres-operator.jar io.quarkus.runner.GeneratedMain"
  JAVA_JAR=""
  set -x
  > /tmp/inotifyd.log
  inotifyd - $(find /tmp/stackgres-operator -type d|sed 's/$/:cDnd/') >> /tmp/inotifyd.log &
  java $JAVA_OPTS $JAVA_JAR $APP_OPTS &
  PID=$!
  TIME="$(date +%s)"
  tail -f /tmp/inotifyd.log | while IFS="$(echo " "|tr " " "\n")" read line
  do
    if [ "$(date +%s)" -lt "$((TIME + 3))" ]
    then
      continue
    fi
    kill "$PID"
    wait "$PID"
    java $JAVA_OPTS $JAVA_JAR $APP_OPTS &
    PID=$!
    TIME="$(date +%s)"
  done
  exit
fi

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
buildah commit --squash "$CONTAINER_BASE" "$IMAGE_NAME"
buildah push "$IMAGE_NAME" docker-daemon:$IMAGE_NAME

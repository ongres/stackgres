#!/bin/sh

APP_PATH="${APP_PATH:-/app}"
if [ "$DEBUG_CLUSTER_CONTROLLER" = true ]
then
  set -x
  DEBUG_JAVA_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,address=8000,suspend=$([ "$DEBUG_CLUSTER_CONTROLLER_SUSPEND" = true ] && echo y || echo n)"
fi
if [ -n "$CLUSTER_CONTROLLER_LOG_LEVEL" ]
then
  APP_OPTS="$APP_OPTS -Dquarkus.log.level=$CLUSTER_CONTROLLER_LOG_LEVEL"
fi
if [ "$CLUSTER_CONTROLLER_SHOW_STACK_TRACES" = true ]
then
  APP_OPTS="$APP_OPTS -Dquarkus.log.console.format=%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c{4.}] (%t) %s%e%n"
fi
if [ "$JAVA_CDS_GENERATION" = true ]
then
  export KUBERNETES_MASTER=240.0.0.1
  java \
    -XX:ArchiveClassesAtExit="$APP_PATH"/quarkus-run.jsa \
    -XX:MaxRAMPercentage=75.0 \
    -Djava.net.preferIPv4Stack=true \
    -Djava.awt.headless=true \
    -Djava.util.logging.manager=org.jboss.logmanager.LogManager \
    $JAVA_OPTS $DEBUG_JAVA_OPTS -jar "$APP_PATH"/quarkus-run.jar \
    -Dquarkus.http.host=0.0.0.0 \
    $APP_OPTS &
  PID=$!
  until curl -s localhost:8080/q/health/ready
  do
    sleep 1
  done
  kill "$PID"
  wait "$PID" || true
  exit
fi
exec java \
  -XX:SharedArchiveFile="$APP_PATH"/quarkus-run.jsa \
  -XX:MaxRAMPercentage=75.0 \
  $(
    if [ "$MEMORY_REQUEST" -gt 0 ]
    then
      printf ' -Xmx%s' "$(( $MEMORY_REQUEST - 1048575 ))"
    fi
  ) \
  -Djava.net.preferIPv4Stack=true \
  -Djava.awt.headless=true \
  -Djava.util.logging.manager=org.jboss.logmanager.LogManager \
  $JAVA_OPTS $DEBUG_JAVA_OPTS -jar "$APP_PATH"/quarkus-run.jar \
  -Dquarkus.http.host=0.0.0.0 \
  $APP_OPTS

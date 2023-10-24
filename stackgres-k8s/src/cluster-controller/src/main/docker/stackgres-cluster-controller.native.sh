#!/bin/sh

if [ "$DEBUG_CLUSTER_CONTROLLER" = true ]
then
  set -x
fi
if [ -n "$CLUSTER_CONTROLLER_LOG_LEVEL" ]
then
  APP_OPTS="$APP_OPTS -Dquarkus.log.level=$CLUSTER_CONTROLLER_LOG_LEVEL"
fi
if [ "$CLUSTER_CONTROLLER_SHOW_STACK_TRACES" = true ]
then
  APP_OPTS="$APP_OPTS -Dquarkus.log.console.format=%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c{4.}] (%t) %s%e%n"
fi
exec /app/stackgres-cluster-controller \
  $(
    if [ "$MEMORY_REQUEST" -gt 0 ]
    then
      printf ' -Xmx%s' "$(( $MEMORY_REQUEST - 1048575 ))"
    fi
  ) \
  -Dquarkus.http.host=0.0.0.0 \
  -Djava.util.logging.manager=org.jboss.logmanager.LogManager \
  $APP_OPTS

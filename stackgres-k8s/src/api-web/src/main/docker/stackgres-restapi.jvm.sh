#!/bin/sh

if [ "$DEBUG_RESTAPI" = true ]
then
  set -x
  DEBUG_JAVA_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,address=8000,suspend=$([ "$DEBUG_RESTAPI_SUSPEND" = true ] && echo y || echo n)"
fi
if [ -n "$RESTAPI_LOG_LEVEL" ]
then
  APP_OPTS="$APP_OPTS -Dquarkus.log.level=$RESTAPI_LOG_LEVEL"
fi
if [ "$RESTAPI_SHOW_STACK_TRACES" = true ]
then
  APP_OPTS="$APP_OPTS -Dquarkus.log.console.format=%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c{4.}] (%t) %s%e%n"
fi
exec java \
  -XX:MaxRAMPercentage=75.0 \
  -Djava.net.preferIPv4Stack=true \
  -Djava.awt.headless=true \
  -Djava.util.logging.manager=org.jboss.logmanager.LogManager \
  $JAVA_OPTS $DEBUG_JAVA_OPTS -jar /app/quarkus-run.jar \
  -Dquarkus.http.host=0.0.0.0 \
  $APP_OPTS

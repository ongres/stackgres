#!/bin/sh

if [ "$DEBUG_OPERATOR" = true ]
then
  set -x
fi
if [ -n "$OPERATOR_LOG_LEVEL" ]
then
  APP_OPTS="$APP_OPTS -Dquarkus.log.level=$OPERATOR_LOG_LEVEL"
fi
if [ "$OPERATOR_SHOW_STACK_TRACES" = true ]
then
  APP_OPTS="$APP_OPTS -Dquarkus.log.console.format=%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c{4.}] (%t) %s%e%n"
fi
exec /app/stackgres-operator \
  -Dquarkus.http.host=0.0.0.0 \
  -Dquarkus.http.port="${OPERATOR_HTTP_PORT:-8080}" \
  -Dquarkus.http.ssl-port="${OPERATOR_HTTPS_PORT:-8443}" \
  -Djava.util.logging.manager=org.jboss.logmanager.LogManager \
  $(
    [ -z "$OPERATOR_CERT_FILE" ] || printf ' %s' "-Dquarkus.http.ssl.certificate.files=$OPERATOR_CERT_FILE"
    [ -z "$OPERATOR_KEY_FILE" ] || printf ' %s' "-Dquarkus.http.ssl.certificate.key-files=$OPERATOR_KEY_FILE"
  ) \
  $APP_OPTS

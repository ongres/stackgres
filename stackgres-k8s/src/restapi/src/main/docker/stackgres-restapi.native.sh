#!/bin/sh

if [ "$DEBUG_RESTAPI" = true ]
then
  set -x
fi
if [ -n "$RESTAPI_CERT_FILE" ]
then
  APP_OPTS="$APP_OPTS -Dquarkus.http.ssl.certificate.files=$RESTAPI_CERT_FILE"
fi
if [ -n "$RESTAPI_KEY_FILE" ]
then
  APP_OPTS="$APP_OPTS -Dquarkus.http.ssl.certificate.key-files=$RESTAPI_KEY_FILE"
fi
if [ -n "$RESTAPI_JWT_PUBLIC_RSA_FILE" ]
then
  APP_OPTS="$APP_OPTS -Dmp.jwt.verify.publickey.location=$RESTAPI_JWT_PUBLIC_RSA_FILE"
fi
if [ -n "$RESTAPI_JWT_PRIVATE_RSA_FILE" ]
then
  APP_OPTS="$APP_OPTS -Dsmallrye.jwt.sign.key.location=$RESTAPI_JWT_PRIVATE_RSA_FILE"
fi
if [ -n "$RESTAPI_LOG_LEVEL" ]
then
  APP_OPTS="$APP_OPTS -Dquarkus.log.level=$RESTAPI_LOG_LEVEL"
fi
if [ "$RESTAPI_SHOW_STACK_TRACES" = true ]
then
  APP_OPTS="$APP_OPTS -Dquarkus.log.console.format=%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c{4.}] (%t) %s%e%n"
fi
exec /app/stackgres-restapi \
  -Dquarkus.http.host=0.0.0.0 \
  -Djava.util.logging.manager=org.jboss.logmanager.LogManager \
  $APP_OPTS

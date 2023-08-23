#!/bin/sh

if [ "$SHOW_DEBUG" = true ]
then
  set -x
fi

if [ "$SHOW_DEBUG" = true ]
then
  cat << EOF
GRAFANA_EMBEDDED=$GRAFANA_EMBEDDED
GRAFANA_URL_PATH=$GRAFANA_URL_PATH
GRAFANA_SCHEMA=$GRAFANA_SCHEMA
GRAFANA_WEB_HOST=$GRAFANA_WEB_HOST
GRAFANA_TOKEN=$GRAFANA_TOKEN"
EOF
fi
envsubst '
  $GRAFANA_EMBEDDED
  $GRAFANA_URL_PATH
  $GRAFANA_SCHEMA
  $GRAFANA_WEB_HOST
  $GRAFANA_TOKEN' \
  < /etc/nginx/template.d/stackgres-restapi.template \
  > /etc/nginx/conf.d/stackgres-restapi.conf
if [ "$SHOW_DEBUG" = true ]
then
  cat /etc/nginx/conf.d/stackgres-restapi.conf
fi
exec nginx -g 'daemon off;'

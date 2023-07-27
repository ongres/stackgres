#!/bin/sh

GRAFANA_CREDENTIALS="$GRAFANA_USER:$GRAFANA_PASSWORD"
if [ -z "$GRAFANA_CREDENTIALS" ]
then
  >&2 echo "Grafana credentials are empty"
  exit 1
fi
GRAFANA_HOST="$GRAFANA_SCHEMA://$GRAFANA_WEB_HOST"

curl_grafana_api() {
  curl -svk -H "Accept: application/json" -H "Content-Type: application/json" --user "$GRAFANA_CREDENTIALS" --fail "$@"
}

GRAFANA_PROMETHEUS_DATASOURCE_NAME="$GRAFANA_DATASOURCE_NAME"
GRAFANA_DASHBOARD_URLS="["
for DASHBOARD in $DASHBOARD_LIST
do
  cat << EOF > /tmp/grafana-dashboard-import.json
{
  "dashboard": $(sed "s/\${DS_PROMETHEUS}/$GRAFANA_PROMETHEUS_DATASOURCE_NAME/g" /etc/grafana/"$DASHBOARD" | jq .),
  "overwrite": true,
  "inputs": [{
    "name": "DS_PROMETHEUS",
    "type": "datasource",
    "pluginId": "prometheus",
    "value": "$GRAFANA_PROMETHEUS_DATASOURCE_NAME"
  }],
  "folderId": null
}
EOF
  GRAFANA_DASHBOARD_URL="$(curl_grafana_api -d "@/tmp/grafana-dashboard-import.json" "$GRAFANA_HOST/api/dashboards/db" | jq -M -r .url)"
  [ -n "$GRAFANA_DASHBOARD_URLS" ]
  [ "$GRAFANA_DASHBOARD_URL" != null ]
  GRAFANA_DASHBOARD_URL="${DASHBOARD%.json}:$GRAFANA_HOST/${GRAFANA_DASHBOARD_URL#*/}"
  if [ "${GRAFANA_DASHBOARD_URLS%\"}" = "$GRAFANA_DASHBOARD_URLS" ]
  then
    GRAFANA_DASHBOARD_URLS="$GRAFANA_DASHBOARD_URLS\"$GRAFANA_DASHBOARD_URL\""
  else
    GRAFANA_DASHBOARD_URLS="$GRAFANA_DASHBOARD_URLS,\"$GRAFANA_DASHBOARD_URL\""
  fi
done
GRAFANA_DASHBOARD_URLS="$GRAFANA_DASHBOARD_URLS]"
GRAFANA_API_KEY_ID="$(curl_grafana_api "$GRAFANA_HOST/api/auth/keys" | jq -r '.[]|select(.name == "stackgres")|.id|select(.!=null)')"
[ -z "$GRAFANA_API_KEY_ID" ] || curl_grafana_api -X DELETE "$GRAFANA_HOST/api/auth/keys/$GRAFANA_API_KEY_ID" > /dev/null
GRAFANA_API_KEY_TOKEN="$(curl_grafana_api -d '{"name":"stackgres", "role": "Viewer"}' "$GRAFANA_HOST/api/auth/keys" | jq -r .key)"
[ -n "$GRAFANA_API_KEY_TOKEN" ]
until kubectl get sgconfig -n "$OPERATOR_NAMESPACE" "$OPERATOR_NAME" -o json \
  | jq ".
    | .status.grafana.urls = $(printf %s "$GRAFANA_DASHBOARD_URLS" | jq -R . | jq -s .)
    | .status.grafana.token = \"$GRAFANA_API_KEY_TOKEN\"
    | .status.grafana.configHash = \"$GRAFANA_CONFIG_HASH\"" \
  | kubectl replace --raw /apis/stackgres.io/v1/namespaces/"$OPERATOR_NAMESPACE"/sgconfigs/"$OPERATOR_NAME"/status -f -
do
  sleep 2
done

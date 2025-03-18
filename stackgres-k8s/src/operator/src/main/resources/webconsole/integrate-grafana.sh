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
GRAFANA_DASHBOARD_URLS=""
for DASHBOARD in $GRAFANA_DASHBOARD_LIST
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
  if [ "x$GRAFANA_DASHBOARD_URL" = x ] \
    || [ "x$GRAFANA_DASHBOARD_URL" = xnull ]
  then
    echo "Can not retrieve imported grafana dashboard URL for dasboard $DASHBOARD"
    exit 1
  fi
  GRAFANA_DASHBOARD_URL="${DASHBOARD%.json}:$GRAFANA_HOST/${GRAFANA_DASHBOARD_URL#*/}"
  if [ "x$GRAFANA_DASHBOARD_URLS" = x ]
  then
    GRAFANA_DASHBOARD_URLS="$GRAFANA_DASHBOARD_URL"
  else
    GRAFANA_DASHBOARD_URLS="$GRAFANA_DASHBOARD_URLS $GRAFANA_DASHBOARD_URL"
  fi
done
if curl_grafana_api "$GRAFANA_HOST/api/serviceaccounts/search?query=stackgres" > /dev/null 2>&1
then
  GRAFANA_API_SA_ID="$(curl_grafana_api "$GRAFANA_HOST/api/serviceaccounts/search?query=stackgres")"
  GRAFANA_API_SA_ID="$(printf %s "$GRAFANA_API_SA_ID" | jq -r '.serviceAccounts[]|select(.name == "stackgres")|.id|select(.!=null)')"
  [ "x$GRAFANA_API_SA_ID" = x ] || curl_grafana_api -X DELETE "$GRAFANA_HOST/api/serviceaccounts/$GRAFANA_API_SA_ID"
  GRAFANA_API_SA_ID="$(curl_grafana_api -d '{"name":"stackgres", "role": "Viewer", "isDisable": false}' "$GRAFANA_HOST/api/serviceaccounts" | jq -r .id)"
  GRAFANA_TOKEN="$(curl_grafana_api -d '{"name":"stackgres"}' "$GRAFANA_HOST/api/serviceaccounts/$GRAFANA_API_SA_ID/tokens")"
  GRAFANA_TOKEN="$(printf %s "$GRAFANA_TOKEN" | jq -r .key)"
else
  GRAFANA_API_KEY_ID="$(curl_grafana_api "$GRAFANA_HOST/api/auth/keys")"
  GRAFANA_API_KEY_ID="$(printf %s "$GRAFANA_API_KEY_ID" | jq -r '.[]|select(.name == "stackgres")|.id|select(.!=null)')"
  [ "x$GRAFANA_API_KEY_ID" = x ] || curl_grafana_api -X DELETE "$GRAFANA_HOST/api/auth/keys/$GRAFANA_API_KEY_ID"
  GRAFANA_TOKEN="$(curl_grafana_api -d '{"name":"stackgres", "role": "Viewer"}' "$GRAFANA_HOST/api/auth/keys")"
  GRAFANA_TOKEN="$(printf %s "$GRAFANA_TOKEN" | jq -r .key)"
fi
if [ "x$GRAFANA_TOKEN" = x ]
then
  echo "Can not retrieve grafana token"
  exit 1
fi
until kubectl get sgconfig -n "$SGCONFIG_NAMESPACE" "$OPERATOR_NAME" -o json \
  | jq ".
    | .status.grafana.urls = $(printf %s "$GRAFANA_DASHBOARD_URLS" | tr ' ' '\n' | jq -R . | jq -s .)
    | .status.grafana.token = \"$GRAFANA_TOKEN\"
    | .status.grafana.configHash = \"$GRAFANA_CONFIG_HASH\"" \
  | kubectl replace --raw /apis/stackgres.io/v1/namespaces/"$SGCONFIG_NAMESPACE"/sgconfigs/"$OPERATOR_NAME"/status -f -
do
  sleep 2
done

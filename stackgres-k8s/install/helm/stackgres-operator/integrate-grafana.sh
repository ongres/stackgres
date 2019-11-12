#!/bin/sh

set -e

grafana_host=http://localhost:3000
grafana_credentials=admin:prom-operator
grafana_prometheus_datasource_name=Prometheus
curl_grafana_api() {
  curl -sk -H "Accept: application/json" -H "Content-Type: application/json" -u "$grafana_credentials" "$@"
}
dashboard_id=9628
dashboard_json="$(cat << EOF
{
  "dashboard": $(curl_grafana_api "$grafana_host/api/gnet/dashboards/$dashboard_id" | jq .json),
  "overwrite": true,
  "inputs": [{
    "name": "DS_PROMETHEUS",
    "type": "datasource",
    "pluginId": "prometheus",
    "value": "$grafana_prometheus_datasource_name"
  }]
}
EOF
)"
grafana_dashboard_url="$(curl_grafana_api -X POST -d "$dashboard_json" "$grafana_host/api/dashboards/import" | jq -r .importedUrl)"
grafana_api_key_id="$(curl_grafana_api -X GET "$grafana_host/api/auth/keys" | jq -r 'map(select(.name = "stackgres")|.id)|first')"
curl_grafana_api -X DELETE "$grafana_host/api/auth/keys/$grafana_api_key_id" > /dev/null
grafana_api_key_token="$(curl_grafana_api -X POST -d '{"name":"stackgres", "role": "Viewer"}' "$grafana_host/api/auth/keys" | jq -r .key)"
echo "$grafana_dashboard_url"
echo "$grafana_api_key_token"

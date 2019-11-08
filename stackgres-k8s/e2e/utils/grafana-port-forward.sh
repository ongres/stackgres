GRAFANA_POD_ID=$(kubectl get pods --all-namespaces | grep grafana |  grep -P '\w+-\w+-\w+-(\w+)' -o)
GRAFANA_NAME_SPACE=$(kubectl get pods --all-namespaces | grep grafana | cut -d ' ' -f 1)
kubectl port-forward $GRAFANA_POD_ID -n $GRAFANA_NAME_SPACE 3000

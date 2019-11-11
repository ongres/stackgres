helm delete stackgres-operator --purge || true
helm delete stackgres --purge || true
helm template ../stackgres/operator/install/kubernetes/chart/stackgres-operator/ | kubectl delete --ignore-not-found -f -
helm template ../stackgres/operator/install/kubernetes/chart/stackgres-cluster/ | kubectl delete --ignore-not-found -f -

---
title: "GKE"
weight: 1
---

# Monitoring with Stackdriver

When creating a kubernetes cluster with Google Kubernetes Engine you may specify to enable
 Stackdriver Kubernetes monitoring and logging by specifying the `--enable-stackdriver-kubernetes`
 parameter on the gcloud CLI.

[Project prometheus-to-sd](https://github.com/GoogleCloudPlatform/k8s-stackdriver/tree/master/prometheus-to-sd)
 allow to scrape metrics from prometheus exporters and send them to Stackdriver. Every pod created
 by StackGres have to be monitored by a prometheus-to-sd's `monitor` executable.
 Currently we do not create a sidecar container to send metrics to Stackdriver, if you feel this
 should be included in StackGres please [open an issue](https://gitlab.com/ongresinc/stackgres/issues/new).

As a workaround here is a script that allow to reconcile pods for an entire StackGres cluster:

```
CLUSTER=simple
NAMESPACE=default
kubectl delete pod -n "$NAMESPACE" -l "app=StackGres-to-Stackdrirver,cluster=$CLUSTER"
kubectl get pod -n "$NAMESPACE" -l app=StackGres,cluster=true -o name | cut -d / -f 2 | xargs -r -n 1 -I % sh -ec "
cat << EOF | kubectl create -n $NAMESPACE -f -
$(cat << EOF
---
apiVersion: v1
kind: Pod
metadata:
  name: %-prometheus-to-sd
  labels:
    app: StackGres-to-Stackdrirver
    cluster: $CLUSTER
spec:
  hostNetwork: true
  containers:
  - name: prometheus-to-sd
    image: gcr.io/google-containers/prometheus-to-sd:v0.9.0
    ports:
      - name: profiler
        containerPort: 6060
    command:
      - /monitor
      - --stackdriver-prefix=custom.googleapis.com
      - --source=stackgres-$NAMESPACE-$CLUSTER:http://$CLUSTER-prometheus-postgres-exporter:9187/metrics
      - --pod-id=%
      - --namespace-id=$NAMESPACE
    env:
      - name: POD_NAME
        valueFrom:
          fieldRef:
            fieldPath: metadata.name
      - name: POD_NAMESPACE
        valueFrom:
          fieldRef:
            fieldPath: metadata.namespace
EOF
)
EOF
"
```
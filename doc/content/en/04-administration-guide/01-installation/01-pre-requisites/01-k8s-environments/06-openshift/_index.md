---
title: OpenShift
weight: 6
url: /install/prerequisites/k8s/openshift
description: Red Hat OpenShift is a unified platform to build, modernize, and deploy applications at scale.
showToc: true
---

[OpenShift Container Platform](https://docs.openshift.com/container-platform/latest/getting_started/openshift-overview.html) is a Kubernetes environment for managing the lifecycle of container-based applications and their dependencies on various computing platforms, such as bare metal, virtualized, on-premise, and in cloud.

> StackGres is actively tested with OpenShift Container Platform (see the [tested versions page]({{% relref "01-introduction/07-tested-environments" %}})), if you find any problem, please [open an issue](https://gitlab.com/ongresinc/stackgres/-/issues/new).

StackGres support OpenShift Container Platform from version {{% openshift-min-version %}} up to version {{% openshift-max-version %}}.

## Monitoring on Red Hat OpenShift 4.6+

Starting from Red Hat OpenShift 4.6 the prometheus operator is installed by default in the cluster. You will have to configure to [enable the monitoring for user-defined projects](https://docs.openshift.com/container-platform/4.6/monitoring/enabling-monitoring-for-user-defined-projects.html). This can be achieved by creating (or editing if already exist) the cluster-monitoring-config ConfigMap (in namespace openshift-monitoring) and setting the parameter `enableUserWorkload` to `true` in the key `config.yaml` as in the following example:

```yaml
cat << EOF | kubectl create -f -
apiVersion: v1
kind: ConfigMap
metadata:
  name: cluster-monitoring-config
  namespace: openshift-monitoring
data:
  config.yaml: |
    enableUserWorkload: true
EOF
```

> If the ConfigMap already exists edit it in order to change the `enableUserWorkload` field to `true` in case it is not already set so

You will also have to install Grafana Operator separately by creating a Namespace, an OperatorGroup and a Subscription:

```yaml
cat << EOF | kubectl create -f -
apiVersion: v1
kind: Namespace
metadata:
  name: grafana
---
apiVersion: operators.coreos.com/v1
kind: OperatorGroup
metadata:
  name: grafana
  namespace: grafana
spec:
  targetNamespaces:
  - grafana
---
apiVersion: operators.coreos.com/v1alpha1
kind: Subscription
metadata:
  name: grafana-operator
  namespace: grafana
spec:
  channel: v4
  name: grafana-operator
  source: community-operators
  sourceNamespace: openshift-marketplace
  startingCSV: grafana-operator.v4.10.0
  installPlanApproval: Automatic
EOF
```

Also, create a Grafana instance and configure it in order to use the OpenShift user workload Prometheus for monitoring:

```yaml
cat << EOF | kubectl create -f -
apiVersion: integreatly.org/v1alpha1
kind: Grafana
metadata:
  name: grafana
  namespace: grafana
EOF
```

```
# Wait for Grafana resources to be created
until kubectl get grafana -n grafana -o json | jq '.items[]|.status.message == "success" and .status.phase == "reconciling"' | grep -q true; do sleep 1; done
oc adm policy add-cluster-role-to-user cluster-monitoring-view -n grafana -z grafana-serviceaccount
BEARER_TOKEN="$(oc create token grafana-serviceaccount --duration=8760h -n grafana)"
```

```yaml
cat << EOF | kubectl create -f -
apiVersion: integreatly.org/v1alpha1
kind: GrafanaDataSource
metadata:
  name: prometheus-grafanadatasource
  namespace: grafana
spec:
  datasources:
    - access: proxy
      editable: true
      isDefault: true
      jsonData:
        httpHeaderName1: 'Authorization'
        timeInterval: 5s
        tlsSkipVerify: true
      name: Prometheus
      secureJsonData:
        httpHeaderValue1: 'Bearer ${BEARER_TOKEN}'
      type: prometheus
      url: 'https://thanos-querier.openshift-monitoring.svc.cluster.local:9091'
  name: prometheus-grafanadatasource.yaml
EOF
```

## Local Path Provisioner on Red Hat OpenShift 4.6+

When installing the Local Path Provisioner on OpenShift 4.x you have to create some extra configurations. In particular, since the OpenShift nodes filesystem are enforced with [SELinux](https://es.wikipedia.org/wiki/SELinux) you have to create a special SELinux context for the base folder used by the Local Path Provisioner. This can be achieved by creating a [MachineConfig](https://docs.openshift.com/container-platform/4.6/post_installation_configuration/machine-configuration-tasks.html):

```yaml
cat << 'EOF' | kubectl create -f -
apiVersion: machineconfiguration.openshift.io/v1
kind: MachineConfig
metadata:
  name: 50-master-local-path-provisioner
  labels:
    machineconfiguration.openshift.io/role: master
spec:
  config:
    ignition:
      version: 2.2.0
    systemd:
      units:
        - name: local-path-provisioner.service
          enabled: true
          contents: |
            [Unit]
            Description=Create and set SELinux contenxt on local-path-provisioner directory
            Before=kubelet.service
            [Service]
            ExecStartPre=/bin/mkdir -p /opt/local-path-provisioner
            ExecStart=/usr/bin/chcon -Rt container_file_t /opt/local-path-provisioner
            [Install]
            WantedBy=multi-user.target
EOF
```

> After applying this command the cluster nodes will be restarted for the configuration to be applied.

The Local Path Provisioner ServiceAccount also requires some privileges that may be granted using the following commands:

```
oc adm policy add-scc-to-user privileged -n local-path-storage -z local-path-provisioner-service-account
oc adm policy add-scc-to-user hostmount-anyuid -n local-path-storage -z local-path-provisioner-service-account
```


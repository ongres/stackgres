---
title: SGConfig
weight: 12
url: /reference/crd/sgconfig
description: Details about SGConfig
showToc: true
---

___

**Kind:** SGConfig

**listKind:** SGConfigList

**plural:** sgconfigs

**singular:** sgconfig
___

StackGres Operator configuration is stored in `SGConfig` Custom Resource.

> **WARNING**: Creating more than one SGConfig is forbidden in order to avoid misbehaviours. The single SGConfig should be created automatically during installation.

**Example:**

```yaml
apiVersion: stackgres.io/v1
kind: SGConfig
metadata:
  name: stackgres-operator
spec:
  # Default values copied from <project_dir>/helm-charts/stackgres-operator/values.yaml
  containerRegistry: quay.io
  imagePullPolicy: IfNotPresent
  operator:
    image:
      pullPolicy: IfNotPresent
  restapi:
    name: stackgres-restapi
    image:
      pullPolicy: IfNotPresent
  adminui:
    name: stackgres-adminui
    image:
      pullPolicy: IfNotPresent
    service:
      exposeHTTP: false
      type: ClusterIP
  jobs:
    name: stackgres-jobs
    image:
      pullPolicy: IfNotPresent
  authentication:
    type: jwt
    user: admin
  extensions:
    repositoryUrls:
    - https://extensions.stackgres.io/postgres/repository
  grafana:
    autoEmbed: false
    datasourceName: Prometheus
    password: prom-operator
    schema: http
    user: admin
  prometheus:
    allowAutobind: true
```

See also [StackGres Installation section]({{%  relref "04-administration-guide/01-installation" %}}).



SGConfig stores the configuration of the StackGres Operator

> **WARNING**: Creating more than one SGConfig is forbidden.
 The single SGConfig should be created automatically during installation.
 More SGConfig may exists only when allowedNamespaces or allowedNamespaceLabelSelector is used.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr><tr>
      <td><b>apiVersion</b></td>
      <td>✓</td>
      <td></td>
      <td></td>
      <td>string</td>
      <td>stackgres.io/v1</td>
      </tr>
      <tr>
      <td><b>kind</b></td>
      <td>✓</td>
      <td></td>
      <td></td>
      <td>string</td>
      <td>SGConfig</td>
      </tr>
      <tr>
      <td><b><a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.27/#objectmeta-v1-meta">metadata</a></b></td>
      <td>✓</td>
      <td>✓</td>
      <td></td>
      <td>object</td>
      <td>Refer to the Kubernetes API documentation for the fields of the `metadata` field.</td>
      </tr>
      <tr>
        <td><b><a href="#sgconfigspec">spec</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Spec defines the desired state of SGConfig<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgconfigstatus">status</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Status defines the observed state of SGConfig<br/>
        </td>
      </tr></tbody>
</table>

## SGConfig.spec
<sup><sup>[↩ Parent](#sgconfig)</sup></sup>

Spec defines the desired state of SGConfig

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgconfigspecadminui">adminui</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Section to configure Web Console container<br/>
        </td>
      </tr>
      <tr>
        <td><b>allowImpersonationForRestApi</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          When set to `true` the cluster role for impersonation will be created even if `disableClusterRole` is set to `true`.

It is `false` by default.

> This value can only be set in operator helm chart.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>allowedNamespaceLabelSelector</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]string</td>
        <td>
          Section to configure namespaces that the operator is allowed to use. If allowedNamespaces is defined it will be used instead. If empty all namespaces will be allowed (default).

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#labelselector-v1-meta

> This value can only be set in operator helm chart.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>allowedNamespaces</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          Section to configure allowed namespaces that the operator is allowed to use. If empty all namespaces will be allowed (default).

> This value can only be set in operator helm chart or with the environment variable `ALLOWED_NAMESPACES`.
>   It is set by OLM when [scoping the operator](https://olm.operatorframework.io/docs/advanced-tasks/operator-scoping-with-operatorgroups/).
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgconfigspecauthentication">authentication</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Section to configure Web Console authentication<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgconfigspeccert">cert</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Section to configure the Operator, REST API and Web Console certificates and JWT RSA key-pair.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgconfigspeccollector">collector</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Section to configure OpenTelemetry Collector

By default a single instance of OpenTelemetry Collector will receive metrics
 from all monitored Pods and will then exports those metrics to
 a configured target (by default will expose a Prometheus exporter).

See receivers section to scale this architecture to a set of OpenTelemetry Collectors.
<br/>
          <br/>
            <i>Default</i>: map[config:map[exporters:map[otlp:map[endpoint:stackgres-collector:4317 tls:map[ca_file:/etc/operator/certs/tls.crt]] prometheus:map[enable_open_metrics:false endpoint:0.0.0.0:9464 metric_expiration:180m resource_to_telemetry_conversion:map[enabled:false] send_timestamps:true tls:map[ca_file:/etc/operator/certs/tls.crt cert_file:/etc/operator/certs/tls.crt key_file:/etc/operator/certs/tls.key reload_interval:10m]]] receivers:map[otlp:map[protocols:map[grpc:map[endpoint:0.0.0.0:4317 tls:map[ca_file:/etc/operator/certs/tls.crt cert_file:/etc/operator/certs/tls.crt key_file:/etc/operator/certs/tls.key]]]]] service:map[pipelines:map[metrics:map[exporters:[prometheus] receivers:[prometheus]]]]] ports:[map[containerPort:9464 name:prom-http protocol:TCP]] prometheusOperator:map[allowDiscovery:true] service:map[spec:map[ports:[map[name:prom-http port:9464 protocol:TCP targetPort:prom-http]] type:ClusterIP]]]<br/>
        </td>
      </tr>
      <tr>
        <td><b>containerRegistry</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The container registry host (and port) where the images will be pulled from.

> This value can only be set in operator helm chart or with the environment variable `SG_CONTAINER_REGISTRY`.
<br/>
          <br/>
            <i>Default</i>: quay.io<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgconfigspecdeploy">deploy</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Section to configure deployment aspects.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgconfigspecdeveloper">developer</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Section to configure developer options.

Following options are for developers only, but can also be useful in some cases ;)
<br/>
        </td>
      </tr>
      <tr>
        <td><b>disableClusterRole</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          When set to `true` the creation of the operator ClusterRole and ClusterRoleBinding is disabled.
 Also, when `true`, some features that rely on unnamespaced resources premissions will be disabled:

* Creation and upgrade of CustomResourceDefinitions
* Set CA bundle for Webhooks
* Check existence of CustomResourceDefinition when listing custom resources
* Validation of StorageClass
* REST API endpoint `can-i/{verb}/{resource}` and `can-i` will always return the full list of permissions for any resource and verb since they rely on creation of subjectaccessreviews unnamespaced resource that requires a cluster role.
* Other REST API endpoints will not work since they rely on impersonation that requires a cluster role.
  This point in particular breaks the Web Console completely. You may still enable this specific cluster role with `.allowImpersonationForRestApi`.
  If you do not need the Web Console you may still disable it completely by setting `.deploy.restapi` to `false`.

When set to `true` and `allowedNamespaces` is not set or is empty then `allowedNamespaces` will be considered set and containing only the namespace of the operator.

It is `false` by default.

> This value can only be set in operator helm chart.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>disableCrdsAndWebhooksUpdate</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          When set to `true` the cluster role to update or patch CRDs will be disabled.

It is `false` by default.

> This value can only be set in operator helm chart.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgconfigspecextensions">extensions</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Section to configure extensions<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgconfigspecgrafana">grafana</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Section to configure Grafana integration<br/>
        </td>
      </tr>
      <tr>
        <td><b>imagePullPolicy</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Image pull policy used for images loaded by the Operator<br/>
          <br/>
            <i>Default</i>: IfNotPresent<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgconfigspecimagepullsecretsindex">imagePullSecrets</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          The list of references to secrets in the same namespace where a ServiceAccount is created by the operator to use for pulling any images in pods that reference such ServiceAccount. ImagePullSecrets are distinct from Secrets because Secrets can be mounted in the pod, but ImagePullSecrets are only accessed by the kubelet. More info: https://kubernetes.io/docs/concepts/containers/images/#specifying-imagepullsecrets-on-a-pod
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgconfigspecjobs">jobs</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Section to configure Operator Installation Jobs<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgconfigspecoperator">operator</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Section to configure Operator Pod<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgconfigspecprometheus">prometheus</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          **Deprecated** this section has been replaced by `.spec.collector.prometheusOperator`.

Section to configure Prometheus integration.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgconfigspecrbac">rbac</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Section to configure RBAC for Web Console admin user<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgconfigspecrestapi">restapi</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Section to configure REST API Pod<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgconfigspecserviceaccount">serviceAccount</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Section to configure Operator Installation ServiceAccount<br/>
        </td>
      </tr>
      <tr>
        <td><b>sgConfigNamespace</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          When set will indicate the namespace where the SGConfig used by the operator will be created.

By default the SGConfig will be created in the same namespace as the operator.

> This value can only be set in operator helm chart.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgconfigspecshardingsphere">shardingSphere</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Section to configure integration with ShardingSphere operator<br/>
        </td>
      </tr></tbody>
</table>

### SGConfig.spec.adminui
<sup><sup>[↩ Parent](#sgconfigspec)</sup></sup>

Section to configure Web Console container

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgconfigspecadminuiimage">image</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Section to configure Web Console image<br/>
        </td>
      </tr>
      <tr>
        <td><b>resources</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Web Console resources. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#resourcerequirements-v1-core<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgconfigspecadminuiservice">service</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Section to configure Web Console service.<br/>
        </td>
      </tr></tbody>
</table>

#### SGConfig.spec.adminui.image
<sup><sup>[↩ Parent](#sgconfigspecadminui)</sup></sup>

Section to configure Web Console image

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Web Console image name<br/>
          <br/>
            <i>Default</i>: stackgres/admin-ui<br/>
        </td>
      </tr>
      <tr>
        <td><b>pullPolicy</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Web Console image pull policy<br/>
          <br/>
            <i>Default</i>: IfNotPresent<br/>
        </td>
      </tr>
      <tr>
        <td><b>tag</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Web Console image tag<br/>
        </td>
      </tr></tbody>
</table>

#### SGConfig.spec.adminui.service
<sup><sup>[↩ Parent](#sgconfigspecadminui)</sup></sup>

Section to configure Web Console service.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>exposeHTTP</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          When set to `true` the HTTP port will be exposed in the Web Console Service<br/>
          <br/>
            <i>Default</i>: false<br/>
        </td>
      </tr>
      <tr>
        <td><b>loadBalancerIP</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          LoadBalancer will get created with the IP specified in
this field. This feature depends on whether the underlying cloud-provider supports specifying
the loadBalancerIP when a load balancer is created. This field will be ignored if the
cloud-provider does not support the feature.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>loadBalancerSourceRanges</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          If specified and supported by the platform,
this will restrict traffic through the cloud-provider load-balancer will be restricted to the
specified client IPs. This field will be ignored if the cloud-provider does not support the
feature.
More info: https://kubernetes.io/docs/tasks/access-application-cluster/configure-cloud-provider-firewall/
<br/>
        </td>
      </tr>
      <tr>
        <td><b>nodePort</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          The HTTPS port used to expose the Service on Kubernetes nodes<br/>
        </td>
      </tr>
      <tr>
        <td><b>nodePortHTTP</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          The HTTP port used to expose the Service on Kubernetes nodes<br/>
        </td>
      </tr>
      <tr>
        <td><b>type</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The type used for the service of the UI:
* Set to LoadBalancer to create a load balancer (if supported by the kubernetes cluster)
  to allow connect from Internet to the UI. Note that enabling this feature will probably incurr in
  some fee that depend on the host of the kubernetes cluster (for example this is true for EKS, GKE
  and AKS).
* Set to NodePort to expose admin UI from kubernetes nodes.
<br/>
          <br/>
            <i>Default</i>: ClusterIP<br/>
        </td>
      </tr></tbody>
</table>

### SGConfig.spec.authentication
<sup><sup>[↩ Parent](#sgconfigspec)</sup></sup>

Section to configure Web Console authentication

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>createAdminSecret</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          When `true` will create the secret used to store the admin user credentials to access the UI.
<br/>
          <br/>
            <i>Default</i>: true<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgconfigspecauthenticationoidc">oidc</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Section to configure Web Console OIDC authentication<br/>
        </td>
      </tr>
      <tr>
        <td><b>password</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The admin password that will be created for the Web Console.

If not specified a random password will be generated.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgconfigspecauthenticationsecretref">secretRef</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Allow to specify a reference to a Secret with the admin user credentials for the Web Console.

In order to assign properly permissions. Make sure the `user` field match the value of the `k8sUsername` key in the referenced Secret.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>type</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Specify the authentication mechanism to use. By default is `jwt`, see https://stackgres.io/doc/latest/api/rbac#local-secret-mechanism.
 If set to `oidc` then see https://stackgres.io/doc/latest/api/rbac/#openid-connect-provider-mechanism.
<br/>
          <br/>
            <i>Default</i>: jwt<br/>
        </td>
      </tr>
      <tr>
        <td><b>user</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The admin username that will be created for the Web Console

Operator bundle installation can not change the default value of this field.
<br/>
          <br/>
            <i>Default</i>: admin<br/>
        </td>
      </tr></tbody>
</table>

#### SGConfig.spec.authentication.oidc
<sup><sup>[↩ Parent](#sgconfigspecauthentication)</sup></sup>

Section to configure Web Console OIDC authentication

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>authServerUrl</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          <br/>
        </td>
      </tr>
      <tr>
        <td><b>clientId</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          <br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgconfigspecauthenticationoidcclientidsecretref">clientIdSecretRef</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          <br/>
        </td>
      </tr>
      <tr>
        <td><b>credentialsSecret</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          <br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgconfigspecauthenticationoidccredentialssecretsecretref">credentialsSecretSecretRef</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          <br/>
        </td>
      </tr>
      <tr>
        <td><b>tlsVerification</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Can be one of `required`, `certificate-validation` or `none`<br/>
        </td>
      </tr></tbody>
</table>

##### SGConfig.spec.authentication.oidc.clientIdSecretRef
<sup><sup>[↩ Parent](#sgconfigspecauthenticationoidc)</sup></sup>



<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          <br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          <br/>
        </td>
      </tr></tbody>
</table>

##### SGConfig.spec.authentication.oidc.credentialsSecretSecretRef
<sup><sup>[↩ Parent](#sgconfigspecauthenticationoidc)</sup></sup>



<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          <br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          <br/>
        </td>
      </tr></tbody>
</table>

#### SGConfig.spec.authentication.secretRef
<sup><sup>[↩ Parent](#sgconfigspecauthentication)</sup></sup>

Allow to specify a reference to a Secret with the admin user credentials for the Web Console.

In order to assign properly permissions. Make sure the `user` field match the value of the `k8sUsername` key in the referenced Secret.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The name of the Secret.<br/>
        </td>
      </tr></tbody>
</table>

### SGConfig.spec.cert
<sup><sup>[↩ Parent](#sgconfigspec)</sup></sup>

Section to configure the Operator, REST API and Web Console certificates and JWT RSA key-pair.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>autoapprove</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          If set to `true` the CertificateSigningRequest used to generate the certificate used by
 Webhooks will be approved by the Operator Installation Job.
<br/>
          <br/>
            <i>Default</i>: true<br/>
        </td>
      </tr>
      <tr>
        <td><b>certDuration</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          The duration in days of the generated certificate for the Operator after which it will expire and be regenerated.
If not specified it will be set to 730 (2 years) by default.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgconfigspeccertcertmanager">certManager</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Section to configure cert-manager integration to generate Operator certificates<br/>
        </td>
      </tr>
      <tr>
        <td><b>collectorCertDuration</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          The duration in days of the generated certificate for the OpenTelemetry Collector after which it will expire and be regenerated.
If not specified it will be set to 730 (2 years) by default.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>collectorSecretName</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The Secret name with the OpenTelemetry Collector certificate
 of type kubernetes.io/tls. See https://kubernetes.io/docs/concepts/configuration/secret/#tls-secrets
<br/>
        </td>
      </tr>
      <tr>
        <td><b>createForCollector</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          When set to `true` the OpenTelemetry Collector certificate will be created.<br/>
          <br/>
            <i>Default</i>: true<br/>
        </td>
      </tr>
      <tr>
        <td><b>createForOperator</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          When set to `true` the Operator certificate will be created.<br/>
          <br/>
            <i>Default</i>: true<br/>
        </td>
      </tr>
      <tr>
        <td><b>createForWebApi</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          When set to `true` the Web Console / REST API certificate will be created.<br/>
          <br/>
            <i>Default</i>: true<br/>
        </td>
      </tr>
      <tr>
        <td><b>regenerateCert</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          When set to `true` the Operator certificates will be regenerated if `createForOperator` is set to `true`, and the certificate is expired or invalid.
<br/>
          <br/>
            <i>Default</i>: true<br/>
        </td>
      </tr>
      <tr>
        <td><b>regenerateCollectorCert</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          When set to `true` the OpenTelemetry Collector certificates will be regenerated if `createForCollector` is set to `true`, and the certificate is expired or invalid.
<br/>
          <br/>
            <i>Default</i>: true<br/>
        </td>
      </tr>
      <tr>
        <td><b>regenerateWebCert</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          When set to `true` the Web Console / REST API certificates will be regenerated if `createForWebApi` is set to `true`, and the certificate is expired or invalid.
<br/>
          <br/>
            <i>Default</i>: true<br/>
        </td>
      </tr>
      <tr>
        <td><b>regenerateWebRsa</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          When set to `true` the Web Console / REST API RSA key pair will be regenerated if `createForWebApi` is set to `true`, and the certificate is expired or invalid.
<br/>
          <br/>
            <i>Default</i>: true<br/>
        </td>
      </tr>
      <tr>
        <td><b>secretName</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The Secret name with the Operator Webhooks certificate issued by the Kubernetes cluster CA
 of type kubernetes.io/tls. See https://kubernetes.io/docs/concepts/configuration/secret/#tls-secrets
<br/>
        </td>
      </tr>
      <tr>
        <td><b>webCertDuration</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          The duration in days of the generated certificate for the Web Console / REST API after which it will expire and be regenerated.
If not specified it will be set to 730 (2 years) by default.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>webRsaDuration</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          The duration in days of the generated RSA key pair for the Web Console / REST API after which it will expire and be regenerated.
If not specified it will be set to 730 (2 years) by default.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>webSecretName</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The Secret name with the Web Console / REST API certificate
 of type kubernetes.io/tls. See https://kubernetes.io/docs/concepts/configuration/secret/#tls-secrets
<br/>
        </td>
      </tr></tbody>
</table>

#### SGConfig.spec.cert.certManager
<sup><sup>[↩ Parent](#sgconfigspeccert)</sup></sup>

Section to configure cert-manager integration to generate Operator certificates

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>autoConfigure</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          When set to `true` then Issuer and Certificate for Operator, Web Console / REST API and OpenTelemetry Collector
 Pods will be generated
<br/>
          <br/>
            <i>Default</i>: false<br/>
        </td>
      </tr>
      <tr>
        <td><b>duration</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The requested duration (i.e. lifetime) of the Certificates. See https://cert-manager.io/docs/reference/api-docs/#cert-manager.io%2fv1<br/>
          <br/>
            <i>Default</i>: 2160h<br/>
        </td>
      </tr>
      <tr>
        <td><b>encoding</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The private key cryptography standards (PKCS) encoding for this certificate’s private key to be encoded in. See https://cert-manager.io/docs/reference/api-docs/#cert-manager.io/v1.CertificatePrivateKey<br/>
          <br/>
            <i>Default</i>: PKCS1<br/>
        </td>
      </tr>
      <tr>
        <td><b>renewBefore</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          How long before the currently issued certificate’s expiry cert-manager should renew the certificate. See https://cert-manager.io/docs/reference/api-docs/#cert-manager.io%2fv1<br/>
          <br/>
            <i>Default</i>: 360h<br/>
        </td>
      </tr>
      <tr>
        <td><b>size</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Size is the key bit size of the corresponding private key for this certificate. See https://cert-manager.io/docs/reference/api-docs/#cert-manager.io/v1.CertificatePrivateKey<br/>
          <br/>
            <i>Default</i>: 2048<br/>
        </td>
      </tr></tbody>
</table>

### SGConfig.spec.collector
<sup><sup>[↩ Parent](#sgconfigspec)</sup></sup>

Section to configure OpenTelemetry Collector

By default a single instance of OpenTelemetry Collector will receive metrics
 from all monitored Pods and will then exports those metrics to
 a configured target (by default will expose a Prometheus exporter).

See receivers section to scale this architecture to a set of OpenTelemetry Collectors.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>affinity</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          OpenTelemetry Collector Pod affinity. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#affinity-v1-core<br/>
        </td>
      </tr>
      <tr>
        <td><b>annotations</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          OpenTelemetry Collector Pod annotations<br/>
        </td>
      </tr>
      <tr>
        <td><b>config</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Section to configure OpenTelemetry Collector Configuration. See https://opentelemetry.io/docs/collector/configuration<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          OpenTelemetry Collector Deploymnet/Deamonset base name<br/>
          <br/>
            <i>Default</i>: stackgres-collector<br/>
        </td>
      </tr>
      <tr>
        <td><b>nodeSelector</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          OpenTelemetry Collector Pod node selector<br/>
        </td>
      </tr>
      <tr>
        <td><b>ports</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          Section to configure OpenTelemetry Collector ports. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#containerport-v1-core<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgconfigspeccollectorprometheusoperator">prometheusOperator</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Section to configure OpenTelemetry Collector integration with Prometheus Operator.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgconfigspeccollectorreceivers">receivers</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          This section allow to configure a variable number of OpenTelemetry Collector
 receivers (by default equals to the number of Pod with metrics enabled)
 that will scrape the metrics separately and send them to a defined number
 of OpenTelemetry Collector exporters (by default 1) that exports those metrics
 to one or more configured targets (by default will expose a Prometheus exporter).
<br/>
        </td>
      </tr>
      <tr>
        <td><b>resources</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          OpenTelemetry Collector Pod resources. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#resourcerequirements-v1-core<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgconfigspeccollectorservice">service</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Section to configure OpenTelemetry Collector Service<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgconfigspeccollectorserviceaccount">serviceAccount</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Section to configure OpenTelemetry Collector ServiceAccount<br/>
        </td>
      </tr>
      <tr>
        <td><b>tolerations</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          OpenTelemetry Collector Pod tolerations. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#toleration-v1-core<br/>
        </td>
      </tr>
      <tr>
        <td><b>volumeMounts</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          Section to configure OpenTelemetry Collector Volume Mounts. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#volumemount-v1-core<br/>
        </td>
      </tr>
      <tr>
        <td><b>volumes</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          Section to configure OpenTelemetry Collector Volumes. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#volume-v1-core<br/>
        </td>
      </tr></tbody>
</table>

#### SGConfig.spec.collector.prometheusOperator
<sup><sup>[↩ Parent](#sgconfigspeccollector)</sup></sup>

Section to configure OpenTelemetry Collector integration with Prometheus Operator.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>allowDiscovery</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          If set to false or monitors is set automatic bind to Prometheus
 created using the [Prometheus Operator](https://github.com/prometheus-operator/prometheus-operator) will be disabled.

If disabled the cluster will not be binded to Prometheus automatically and will require manual configuration.

Will be ignored if monitors is set.
<br/>
          <br/>
            <i>Default</i>: true<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgconfigspeccollectorprometheusoperatormonitorsindex">monitors</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          Optional section to configure PodMonitors for specific Prometheus instances

*WARNING*: resources created by this integration that does set
 the metadata namespace to the same as the operator will not
 be removed when removing the helm chart. Changing the namespace
 may require configure the Prometheus CR properly in order to
 discover PodMonitor in such namespace.
<br/>
        </td>
      </tr></tbody>
</table>

##### SGConfig.spec.collector.prometheusOperator.monitors[index]
<sup><sup>[↩ Parent](#sgconfigspeccollectorprometheusoperator)</sup></sup>

Section to configure a PodMonitor for a specific Prometheus instance that will scrape from the collector Pod pointing by default to the prometheus exporter

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgconfigspeccollectorprometheusoperatormonitorsindexmetadata">metadata</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Section to overwrite some PodMonitor metadata<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The name of the Prometheus resource that will scrape from the collector Pod pointing by default to the prometheus exporter<br/>
        </td>
      </tr>
      <tr>
        <td><b>namespace</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The namespace of the Prometheus resource that will scrape from the collector Pod pointing by default to the prometheus exporter<br/>
        </td>
      </tr>
      <tr>
        <td><b>spec</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          The PodMonitor spec that will be overwritten by the operator. See https://github.com/prometheus-operator/prometheus-operator/blob/main/Documentation/api.md#monitoring.coreos.com/v1.PodMonitorSpec<br/>
        </td>
      </tr></tbody>
</table>

##### SGConfig.spec.collector.prometheusOperator.monitors[index].metadata
<sup><sup>[↩ Parent](#sgconfigspeccollectorprometheusoperatormonitorsindex)</sup></sup>

Section to overwrite some PodMonitor metadata

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>annotations</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          The labels to set for the PodMonitor<br/>
        </td>
      </tr>
      <tr>
        <td><b>labels</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          The labels to set for the PodMonitor<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The name of the PodMonitor<br/>
        </td>
      </tr>
      <tr>
        <td><b>namespace</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The namespace of the PodMonitor. Changing the namespace may require configure the Prometheus CR properly in order to discover PodMonitor in such namespace.<br/>
        </td>
      </tr>
      <tr>
        <td><b>ownerReferences</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          The ownerReferences to set for the PodMonitor in order to be garbage collected by the specified object.<br/>
        </td>
      </tr></tbody>
</table>

#### SGConfig.spec.collector.receivers
<sup><sup>[↩ Parent](#sgconfigspeccollector)</sup></sup>

This section allow to configure a variable number of OpenTelemetry Collector
 receivers (by default equals to the number of Pod with metrics enabled)
 that will scrape the metrics separately and send them to a defined number
 of OpenTelemetry Collector exporters (by default 1) that exports those metrics
 to one or more configured targets (by default will expose a Prometheus exporter).


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgconfigspeccollectorreceiversdeploymentsindex">deployments</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          A set of separate Deployments of 1 instance each that allow to set the OpenTelemetry Collectors receivers to a specified number of instances.

When not set the number of Deployment of OpenTelemetry Collectors receivers will match the number of instances of all the existing SGClusters
 that has the field `.spec.configurations.observability.enableMetrics` set to `true`. Also, when not set, each Deployment will include a pod
 affinity rule matching any of the SGClusters Pods set defined below. This will allow to create an OpenTelemetry Collector receiver instance
 dedicated to each SGCluster Pod running in the same Node.

Each Deployment will use a configuration for the OpenTelemetry Collector that will scrape from a set of SGClusters Pods that has the field
 `.spec.configurations.observability.enableMetrics` set to `true`. The set of Pods of each of those OpenTelemetry Collector configuration
 will be a partition of the list of SGClusters Pods that has the field `.spec.configurations.observability.enableMetrics` set to `true`
 ordered by the field `Pod.metadata.creationTimestamp` (from the oldest to the newest) and ordered crescently alphabetically by the fields
 `Pod.metadata.namespace` and `Pod.metadata.name`.

If is possible to override (even partially) the list of SGCluster Pods using the `sgClusters` section.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>enabled</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          When set to `true` it enables the creation of a set of OpenTelemetry Collectors receivers
 that will be scraping from the SGCluster Pods and allow to scale the observability
 architecture and a set of OpenTelemetry Collectors exporters that exports those metrics
 to one or more configured targets.
<br/>
          <br/>
            <i>Default</i>: false<br/>
        </td>
      </tr>
      <tr>
        <td><b>exporters</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          When receivers are enabled indicates the number of OpenTelemetry Collectors exporters that
 exports metrics to one or more configured targets.
<br/>
          <br/>
            <i>Default</i>: 1<br/>
        </td>
      </tr></tbody>
</table>

##### SGConfig.spec.collector.receivers.deployments[index]
<sup><sup>[↩ Parent](#sgconfigspeccollectorreceivers)</sup></sup>



<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>affinity</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          OpenTelemetry Collector Pod affinity. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#affinity-v1-core<br/>
        </td>
      </tr>
      <tr>
        <td><b>annotations</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          OpenTelemetry Collector Pod annotations<br/>
        </td>
      </tr>
      <tr>
        <td><b>nodeSelector</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          OpenTelemetry Collector Pod node selector<br/>
        </td>
      </tr>
      <tr>
        <td><b>resources</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          OpenTelemetry Collector Pod resources. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#resourcerequirements-v1-core<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgconfigspeccollectorreceiversdeploymentsindexsgclustersindex">sgClusters</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          List of SGCluster Pods to scrape from this Deployment's Pod that will be included to the OpenTelemetry Collector
 configuration alongside the SGCluster Pods assigned as described in `SGConfig.spec.collector.receivers.deployments`.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>tolerations</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          OpenTelemetry Collector Pod tolerations. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#toleration-v1-core<br/>
        </td>
      </tr></tbody>
</table>

##### SGConfig.spec.collector.receivers.deployments[index].sgClusters[index]
<sup><sup>[↩ Parent](#sgconfigspeccollectorreceiversdeploymentsindex)</sup></sup>



<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>indexes</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]integer</td>
        <td>
          The indexes of the SGCluster's Pods that will be included to the OpenTelemetry Collector configuration alongside
 the SGCluster Pods assigned as described in `SGConfig.spec.collector.receivers.deployments`.

If not specified all the SGCluster's Pods will be included.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The name of the SGCluster<br/>
        </td>
      </tr>
      <tr>
        <td><b>namespace</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The namespace of the SGCluster<br/>
        </td>
      </tr></tbody>
</table>

#### SGConfig.spec.collector.service
<sup><sup>[↩ Parent](#sgconfigspeccollector)</sup></sup>

Section to configure OpenTelemetry Collector Service

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>annotations</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          OpenTelemetry Collector Service annotations<br/>
        </td>
      </tr>
      <tr>
        <td><b>spec</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Section to configure OpenTelemetry Collector Service specs. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#servicespec-v1-core<br/>
        </td>
      </tr></tbody>
</table>

#### SGConfig.spec.collector.serviceAccount
<sup><sup>[↩ Parent](#sgconfigspeccollector)</sup></sup>

Section to configure OpenTelemetry Collector ServiceAccount

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>annotations</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          OpenTelemetry Collector ServiceAccount annotations<br/>
        </td>
      </tr>
      <tr>
        <td><b>repoCredentials</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          Repositories credentials Secret names<br/>
        </td>
      </tr></tbody>
</table>

### SGConfig.spec.deploy
<sup><sup>[↩ Parent](#sgconfigspec)</sup></sup>

Section to configure deployment aspects.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>collector</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          When set to `true` the OpenTelemetry Collector will be deployed.<br/>
          <br/>
            <i>Default</i>: true<br/>
        </td>
      </tr>
      <tr>
        <td><b>operator</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          When set to `true` the Operator will be deployed.<br/>
          <br/>
            <i>Default</i>: true<br/>
        </td>
      </tr>
      <tr>
        <td><b>restapi</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          When set to `true` the Web Console / REST API will be deployed.<br/>
          <br/>
            <i>Default</i>: true<br/>
        </td>
      </tr></tbody>
</table>

### SGConfig.spec.developer
<sup><sup>[↩ Parent](#sgconfigspec)</sup></sup>

Section to configure developer options.

Following options are for developers only, but can also be useful in some cases ;)


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>allowPullExtensionsFromImageRepository</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          If set to `true` and `extensions.cache.enabled` is also `true`
 it will try to download extensions from images (experimental)
<br/>
          <br/>
            <i>Default</i>: false<br/>
        </td>
      </tr>
      <tr>
        <td><b>disableArbitraryUser</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          It set to `true` disable arbitrary user that is set for OpenShift clusters
<br/>
          <br/>
            <i>Default</i>: false<br/>
        </td>
      </tr>
      <tr>
        <td><b>enableJvmDebug</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          Only work with JVM version and allow connect
 on port 8000 of operator Pod with jdb or similar
<br/>
          <br/>
            <i>Default</i>: false<br/>
        </td>
      </tr>
      <tr>
        <td><b>enableJvmDebugSuspend</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          Only work with JVM version and if `enableJvmDebug` is `true`
 suspend the JVM until a debugger session is started
<br/>
          <br/>
            <i>Default</i>: false<br/>
        </td>
      </tr>
      <tr>
        <td><b>externalOperatorIp</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Set the external Operator IP<br/>
        </td>
      </tr>
      <tr>
        <td><b>externalOperatorPort</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Set the external Operator port<br/>
        </td>
      </tr>
      <tr>
        <td><b>externalRestApiIp</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Set the external REST API IP<br/>
        </td>
      </tr>
      <tr>
        <td><b>externalRestApiPort</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Set the external REST API port<br/>
        </td>
      </tr>
      <tr>
        <td><b>logLevel</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Set `quarkus.log.level`. See https://quarkus.io/guides/logging#root-logger-configuration<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgconfigspecdeveloperpatches">patches</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Section to define patches for some StackGres Pods
<br/>
        </td>
      </tr>
      <tr>
        <td><b>showDebug</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          If set to `true` add extra debug to any script controlled by the reconciliation cycle of the operator configuration<br/>
          <br/>
            <i>Default</i>: false<br/>
        </td>
      </tr>
      <tr>
        <td><b>showStackTraces</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          Set `quarkus.log.console.format` to `%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c{4.}] (%t) %s%e%n`. See https://quarkus.io/guides/logging#logging-format<br/>
          <br/>
            <i>Default</i>: false<br/>
        </td>
      </tr>
      <tr>
        <td><b>useJvmImages</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          The operator will use JVM version of the images
<br/>
          <br/>
            <i>Default</i>: false<br/>
        </td>
      </tr>
      <tr>
        <td><b>version</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Set the operator version (used for testing)<br/>
        </td>
      </tr></tbody>
</table>

#### SGConfig.spec.developer.patches
<sup><sup>[↩ Parent](#sgconfigspecdeveloper)</sup></sup>

Section to define patches for some StackGres Pods


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgconfigspecdeveloperpatchesadminui">adminui</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Section to define volumes to be used by the adminui container
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgconfigspecdeveloperpatchesclustercontroller">clusterController</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Section to define volumes to be used by the cluster controller container
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgconfigspecdeveloperpatchesjobs">jobs</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Section to define volumes to be used by the jobs container
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgconfigspecdeveloperpatchesoperator">operator</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Section to define volumes to be used by the operator container
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgconfigspecdeveloperpatchesrestapi">restapi</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Section to define volumes to be used by the restapi container
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgconfigspecdeveloperpatchesstream">stream</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Section to define volumes to be used by the stream container
<br/>
        </td>
      </tr></tbody>
</table>

##### SGConfig.spec.developer.patches.adminui
<sup><sup>[↩ Parent](#sgconfigspecdeveloperpatches)</sup></sup>

Section to define volumes to be used by the adminui container


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>volumeMounts</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          Pod's container volume mounts. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#volumemount-v1-core<br/>
        </td>
      </tr>
      <tr>
        <td><b>volumes</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          Pod volumes. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#volume-v1-core<br/>
        </td>
      </tr></tbody>
</table>

##### SGConfig.spec.developer.patches.clusterController
<sup><sup>[↩ Parent](#sgconfigspecdeveloperpatches)</sup></sup>

Section to define volumes to be used by the cluster controller container


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>volumeMounts</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          Pod's container volume mounts. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#volumemount-v1-core<br/>
        </td>
      </tr>
      <tr>
        <td><b>volumes</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          Pod volumes. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#volume-v1-core<br/>
        </td>
      </tr></tbody>
</table>

##### SGConfig.spec.developer.patches.jobs
<sup><sup>[↩ Parent](#sgconfigspecdeveloperpatches)</sup></sup>

Section to define volumes to be used by the jobs container


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>volumeMounts</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          Pod's container volume mounts. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#volumemount-v1-core<br/>
        </td>
      </tr>
      <tr>
        <td><b>volumes</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          Pod volumes. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#volume-v1-core<br/>
        </td>
      </tr></tbody>
</table>

##### SGConfig.spec.developer.patches.operator
<sup><sup>[↩ Parent](#sgconfigspecdeveloperpatches)</sup></sup>

Section to define volumes to be used by the operator container


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>volumeMounts</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          Pod's container volume mounts. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#volumemount-v1-core<br/>
        </td>
      </tr>
      <tr>
        <td><b>volumes</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          Pod volumes. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#volume-v1-core<br/>
        </td>
      </tr></tbody>
</table>

##### SGConfig.spec.developer.patches.restapi
<sup><sup>[↩ Parent](#sgconfigspecdeveloperpatches)</sup></sup>

Section to define volumes to be used by the restapi container


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>volumeMounts</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          Pod's container volume mounts. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#volumemount-v1-core<br/>
        </td>
      </tr>
      <tr>
        <td><b>volumes</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          Pod volumes. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#volume-v1-core<br/>
        </td>
      </tr></tbody>
</table>

##### SGConfig.spec.developer.patches.stream
<sup><sup>[↩ Parent](#sgconfigspecdeveloperpatches)</sup></sup>

Section to define volumes to be used by the stream container


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>volumeMounts</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          Pod's container volume mounts. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#volumemount-v1-core<br/>
        </td>
      </tr>
      <tr>
        <td><b>volumes</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          Pod volumes. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#volume-v1-core<br/>
        </td>
      </tr></tbody>
</table>

### SGConfig.spec.extensions
<sup><sup>[↩ Parent](#sgconfigspec)</sup></sup>

Section to configure extensions

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgconfigspecextensionscache">cache</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Section to configure extensions cache (experimental).

This feature is in beta and may cause failures, please use with caution and report any
 error to https://gitlab.com/ongresinc/stackgres/-/issues/new
<br/>
        </td>
      </tr>
      <tr>
        <td><b>repositoryUrls</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          A list of extensions repository URLs used to retrieve extensions

To set a proxy for extensions repository add parameter proxyUrl to the URL:
    `https://extensions.stackgres.io/postgres/repository?proxyUrl=<proxy scheme>%3A%2F%2F<proxy host>[%3A<proxy port>]` (URL encoded)

Other URL parameters are:

* `skipHostnameVerification`: set it to `true` in order to use a server or a proxy with a self signed certificate
* `retry`: set it to `<max retriex>[:<sleep before next retry>]` in order to retry a request on failure
* `setHttpScheme`: set it to `true` in order to force using HTTP scheme
<br/>
          <br/>
            <i>Default</i>: [https://extensions.stackgres.io/postgres/repository]<br/>
        </td>
      </tr></tbody>
</table>

#### SGConfig.spec.extensions.cache
<sup><sup>[↩ Parent](#sgconfigspecextensions)</sup></sup>

Section to configure extensions cache (experimental).

This feature is in beta and may cause failures, please use with caution and report any
 error to https://gitlab.com/ongresinc/stackgres/-/issues/new


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>enabled</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          When set to `true` enable the extensions cache.

This feature is in beta and may cause failures, please use with caution and report any
 error to https://gitlab.com/ongresinc/stackgres/-/issues/new
<br/>
          <br/>
            <i>Default</i>: false<br/>
        </td>
      </tr>
      <tr>
        <td><b>hostPath</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          If set, will use a host path volume with the specified path for the extensions cache
 instead of a PersistentVolume
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgconfigspecextensionscachepersistentvolume">persistentVolume</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Section to configure the extensions cache PersistentVolume<br/>
        </td>
      </tr>
      <tr>
        <td><b>preloadedExtensions</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          An array of extensions pattern used to pre-loaded estensions into the extensions cache<br/>
          <br/>
            <i>Default</i>: [x86_64/linux/timescaledb-1\.7\.4-pg12]<br/>
        </td>
      </tr></tbody>
</table>

##### SGConfig.spec.extensions.cache.persistentVolume
<sup><sup>[↩ Parent](#sgconfigspecextensionscache)</sup></sup>

Section to configure the extensions cache PersistentVolume

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>size</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The PersistentVolume size for the extensions cache

Only use whole numbers (e.g. not 1e6) and K/Ki/M/Mi/G/Gi as units
<br/>
          <br/>
            <i>Default</i>: 1Gi<br/>
        </td>
      </tr>
      <tr>
        <td><b>storageClass</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          If defined set storage class
If set to "-" (equivalent to storageClass: "" in a PV spec) disables
 dynamic provisioning
If undefined (the default) or set to null, no storageClass spec is
 set, choosing the default provisioner.  (gp2 on AWS, standard on
 GKE, AWS & OpenStack)
<br/>
        </td>
      </tr></tbody>
</table>

### SGConfig.spec.grafana
<sup><sup>[↩ Parent](#sgconfigspec)</sup></sup>

Section to configure Grafana integration

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>autoEmbed</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          When set to `true` embed automatically Grafana into the Web Console by creating the
 StackGres dashboard and the read-only role used to read it from the Web Console 
<br/>
          <br/>
            <i>Default</i>: false<br/>
        </td>
      </tr>
      <tr>
        <td><b>dashboardConfigMap</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The ConfigMap name with the dashboard JSON in the key `grafana-dashboard.json`
 that will be created in Grafana. If not set the default
<br/>
        </td>
      </tr>
      <tr>
        <td><b>dashboardId</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The dashboard id that will be create in Grafana
 (see https://grafana.com/grafana/dashboards). By default 9628. (used to embed automatically
 Grafana)

Manual Steps:
 
Create grafana dashboard for postgres exporter and copy/paste share URL:
- Grafana > Create > Import > Grafana.com Dashboard 9628
Copy/paste grafana dashboard URL for postgres exporter:
- Grafana > Dashboard > Manage > Select postgres exporter dashboard > Copy URL
<br/>
        </td>
      </tr>
      <tr>
        <td><b>datasourceName</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The datasource name used to create the StackGres Dashboard into Grafana<br/>
          <br/>
            <i>Default</i>: Prometheus<br/>
        </td>
      </tr>
      <tr>
        <td><b>password</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The password to access Grafana. By default prom-operator (the default in for
 kube-prometheus-stack helm chart). (used to embed automatically Grafana)
<br/>
          <br/>
            <i>Default</i>: prom-operator<br/>
        </td>
      </tr>
      <tr>
        <td><b>schema</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The schema to access Grafana. By default http. (used to embed manually and
 automatically grafana)
<br/>
          <br/>
            <i>Default</i>: http<br/>
        </td>
      </tr>
      <tr>
        <td><b>secretName</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The name of secret with credentials to access Grafana. (used to embed
 automatically Grafana, alternative to use `user` and `password`)
<br/>
        </td>
      </tr>
      <tr>
        <td><b>secretNamespace</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The namespace of secret with credentials to access Grafana. (used to
 embed automatically Grafana, alternative to use `user` and `password`)
<br/>
        </td>
      </tr>
      <tr>
        <td><b>secretPasswordKey</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The key of secret with password used to access Grafana. (used to
 embed automatically Grafana, alternative to use `user` and `password`)
<br/>
        </td>
      </tr>
      <tr>
        <td><b>secretUserKey</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The key of secret with username used to access Grafana. (used to embed
 automatically Grafana, alternative to use `user` and `password`)
<br/>
        </td>
      </tr>
      <tr>
        <td><b>token</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The Grafana API token to access the PostgreSQL dashboard created
 in Grafana (used to embed manually Grafana)

Manual Steps:
 
Create and copy/paste grafana API token:
- Grafana > Configuration > API Keys > Add API key (for viewer) > Copy key value
<br/>
        </td>
      </tr>
      <tr>
        <td><b>url</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The URL of the PostgreSQL dashboard created in Grafana (used to embed manually
 Grafana)
<br/>
        </td>
      </tr>
      <tr>
        <td><b>user</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The username to access Grafana. By default admin. (used to embed automatically
 Grafana)
<br/>
          <br/>
            <i>Default</i>: admin<br/>
        </td>
      </tr>
      <tr>
        <td><b>webHost</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The service host name to access grafana (used to embed manually and
 automatically Grafana). 
The parameter value should point to the grafana service following the 
 [DNS reference](https://kubernetes.io/docs/concepts/services-networking/dns-pod-service/) `svc_name.namespace`
<br/>
        </td>
      </tr></tbody>
</table>

### SGConfig.spec.imagePullSecrets[index]
<sup><sup>[↩ Parent](#sgconfigspec)</sup></sup>

A reference to a secrets in the same namespace where a ServiceAccount is created by the operator to use for pulling any images in pods that reference such ServiceAccount.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The name of the referenced Secret.<br/>
        </td>
      </tr></tbody>
</table>

### SGConfig.spec.jobs
<sup><sup>[↩ Parent](#sgconfigspec)</sup></sup>

Section to configure Operator Installation Jobs

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>affinity</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Operator Installation Jobs affinity. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#affinity-v1-core<br/>
        </td>
      </tr>
      <tr>
        <td><b>annotations</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Operator Installation Jobs annotations<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgconfigspecjobsimage">image</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Section to configure Operator Installation Jobs image<br/>
        </td>
      </tr>
      <tr>
        <td><b>nodeSelector</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Operator Installation Jobs node selector<br/>
        </td>
      </tr>
      <tr>
        <td><b>resources</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Operator Installation Jobs resources. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#resourcerequirements-v1-core<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgconfigspecjobsserviceaccount">serviceAccount</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Section to configure Jobs ServiceAccount<br/>
        </td>
      </tr>
      <tr>
        <td><b>tolerations</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          Operator Installation Jobs tolerations. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#toleration-v1-core<br/>
        </td>
      </tr></tbody>
</table>

#### SGConfig.spec.jobs.image
<sup><sup>[↩ Parent](#sgconfigspecjobs)</sup></sup>

Section to configure Operator Installation Jobs image

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Operator Installation Jobs image name<br/>
          <br/>
            <i>Default</i>: stackgres/jobs<br/>
        </td>
      </tr>
      <tr>
        <td><b>pullPolicy</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Operator Installation Jobs image pull policy<br/>
          <br/>
            <i>Default</i>: IfNotPresent<br/>
        </td>
      </tr>
      <tr>
        <td><b>tag</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Operator Installation Jobs image tag<br/>
        </td>
      </tr></tbody>
</table>

#### SGConfig.spec.jobs.serviceAccount
<sup><sup>[↩ Parent](#sgconfigspecjobs)</sup></sup>

Section to configure Jobs ServiceAccount

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>annotations</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Jobs ServiceAccount annotations<br/>
        </td>
      </tr>
      <tr>
        <td><b>repoCredentials</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          Repositories credentials Secret names<br/>
        </td>
      </tr></tbody>
</table>

### SGConfig.spec.operator
<sup><sup>[↩ Parent](#sgconfigspec)</sup></sup>

Section to configure Operator Pod

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>affinity</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Operator Pod affinity. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#affinity-v1-core

> This value can only be set in operator helm chart.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>annotations</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Operator Pod annotations<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgconfigspecoperatorimage">image</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Section to configure Operator image<br/>
        </td>
      </tr>
      <tr>
        <td><b>nodeSelector</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Operator Pod node selector

> This value can only be set in operator helm chart.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>resources</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Operator Pod resources. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#resourcerequirements-v1-core

> This value can only be set in operator helm chart.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgconfigspecoperatorservice">service</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Section to configure Operator Service<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgconfigspecoperatorserviceaccount">serviceAccount</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Section to configure Operator ServiceAccount<br/>
        </td>
      </tr>
      <tr>
        <td><b>tolerations</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          Operator Pod tolerations. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#toleration-v1-core

> This value can only be set in operator helm chart.
<br/>
        </td>
      </tr></tbody>
</table>

#### SGConfig.spec.operator.image
<sup><sup>[↩ Parent](#sgconfigspecoperator)</sup></sup>

Section to configure Operator image

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Operator image name

> This value can only be set in operator helm chart.
<br/>
          <br/>
            <i>Default</i>: stackgres/operator<br/>
        </td>
      </tr>
      <tr>
        <td><b>pullPolicy</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Operator image pull policy

> This value can only be set in operator helm chart.
<br/>
          <br/>
            <i>Default</i>: IfNotPresent<br/>
        </td>
      </tr>
      <tr>
        <td><b>tag</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Operator image tag

> This value can only be set in operator helm chart.
<br/>
        </td>
      </tr></tbody>
</table>

#### SGConfig.spec.operator.service
<sup><sup>[↩ Parent](#sgconfigspecoperator)</sup></sup>

Section to configure Operator Service

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>annotations</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Section to configure Operator Service annotations

> This value can only be set in operator helm chart.
<br/>
        </td>
      </tr></tbody>
</table>

#### SGConfig.spec.operator.serviceAccount
<sup><sup>[↩ Parent](#sgconfigspecoperator)</sup></sup>

Section to configure Operator ServiceAccount

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>annotations</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Section to configure Operator ServiceAccount annotations

> This value can only be set in operator helm chart.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>repoCredentials</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          Repositories credentials Secret names

> This value can only be set in operator helm chart.
<br/>
        </td>
      </tr></tbody>
</table>

### SGConfig.spec.prometheus
<sup><sup>[↩ Parent](#sgconfigspec)</sup></sup>

**Deprecated** this section has been replaced by `.spec.collector.prometheusOperator`.

Section to configure Prometheus integration.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>allowAutobind</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          **Deprecated** this field has been replaced by `.spec.collector.prometheusOperator.allowDiscovery`.

If set to false disable automatic bind to Prometheus
  created using the [Prometheus Operator](https://github.com/prometheus-operator/prometheus-operator).
If disabled the cluster will not be binded to Prometheus automatically and will require manual
  intervention by the Kubernetes cluster administrator.
<br/>
          <br/>
            <i>Default</i>: true<br/>
        </td>
      </tr></tbody>
</table>

### SGConfig.spec.rbac
<sup><sup>[↩ Parent](#sgconfigspec)</sup></sup>

Section to configure RBAC for Web Console admin user

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>create</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          When set to `true` the admin user is assigned the `cluster-admin` ClusterRole by creating
 ClusterRoleBinding.
<br/>
          <br/>
            <i>Default</i>: true<br/>
        </td>
      </tr></tbody>
</table>

### SGConfig.spec.restapi
<sup><sup>[↩ Parent](#sgconfigspec)</sup></sup>

Section to configure REST API Pod

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>affinity</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          REST API Pod affinity. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#affinity-v1-core<br/>
        </td>
      </tr>
      <tr>
        <td><b>annotations</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          REST API Pod annotations<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgconfigspecrestapiimage">image</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Section to configure REST API image<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          REST API Deployment name<br/>
          <br/>
            <i>Default</i>: stackgres-restapi<br/>
        </td>
      </tr>
      <tr>
        <td><b>nodeSelector</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          REST API Pod node selector<br/>
        </td>
      </tr>
      <tr>
        <td><b>resources</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          REST API Pod resources. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#resourcerequirements-v1-core<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgconfigspecrestapiservice">service</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Section to configure REST API Service<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgconfigspecrestapiserviceaccount">serviceAccount</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Section to configure REST API ServiceAccount<br/>
        </td>
      </tr>
      <tr>
        <td><b>tolerations</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          REST API Pod tolerations. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#toleration-v1-core<br/>
        </td>
      </tr></tbody>
</table>

#### SGConfig.spec.restapi.image
<sup><sup>[↩ Parent](#sgconfigspecrestapi)</sup></sup>

Section to configure REST API image

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          REST API image name<br/>
          <br/>
            <i>Default</i>: stackgres/restapi<br/>
        </td>
      </tr>
      <tr>
        <td><b>pullPolicy</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          REST API image pull policy<br/>
          <br/>
            <i>Default</i>: IfNotPresent<br/>
        </td>
      </tr>
      <tr>
        <td><b>tag</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          REST API image tag<br/>
        </td>
      </tr></tbody>
</table>

#### SGConfig.spec.restapi.service
<sup><sup>[↩ Parent](#sgconfigspecrestapi)</sup></sup>

Section to configure REST API Service

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>annotations</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          REST API Service annotations<br/>
        </td>
      </tr></tbody>
</table>

#### SGConfig.spec.restapi.serviceAccount
<sup><sup>[↩ Parent](#sgconfigspecrestapi)</sup></sup>

Section to configure REST API ServiceAccount

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>annotations</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          REST API ServiceAccount annotations<br/>
        </td>
      </tr>
      <tr>
        <td><b>repoCredentials</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          Repositories credentials Secret names<br/>
        </td>
      </tr></tbody>
</table>

### SGConfig.spec.serviceAccount
<sup><sup>[↩ Parent](#sgconfigspec)</sup></sup>

Section to configure Operator Installation ServiceAccount

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>annotations</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Section to configure Installation ServiceAccount annotations<br/>
        </td>
      </tr>
      <tr>
        <td><b>create</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          If `true` the Operator Installation ServiceAccount will be created

> This value can only be set in operator helm chart.
<br/>
          <br/>
            <i>Default</i>: true<br/>
        </td>
      </tr>
      <tr>
        <td><b>repoCredentials</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          Repositories credentials Secret names

> This value can only be set in operator helm chart.
<br/>
        </td>
      </tr></tbody>
</table>

### SGConfig.spec.shardingSphere
<sup><sup>[↩ Parent](#sgconfigspec)</sup></sup>

Section to configure integration with ShardingSphere operator

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgconfigspecshardingsphereserviceaccount">serviceAccount</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Section to configure ServiceAccount used by ShardingSphere operator.

You may configure a specific value for a sharded cluster under section
 `SGShardedCluster.speccoordinator.configurations.shardingSphere.serviceAccount`.
<br/>
        </td>
      </tr></tbody>
</table>

#### SGConfig.spec.shardingSphere.serviceAccount
<sup><sup>[↩ Parent](#sgconfigspecshardingsphere)</sup></sup>

Section to configure ServiceAccount used by ShardingSphere operator.

You may configure a specific value for a sharded cluster under section
 `SGShardedCluster.speccoordinator.configurations.shardingSphere.serviceAccount`.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The name of the ServiceAccount used by ShardingSphere operator<br/>
        </td>
      </tr>
      <tr>
        <td><b>namespace</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The namespace of the ServiceAccount used by ShardingSphere operator<br/>
        </td>
      </tr></tbody>
</table>

## SGConfig.status
<sup><sup>[↩ Parent](#sgconfig)</sup></sup>

Status defines the observed state of SGConfig

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgconfigstatusconditionsindex">conditions</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          <br/>
        </td>
      </tr>
      <tr>
        <td><b>existingCrUpdatedToVersion</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Indicate the version to which existing CRs have been updated to<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgconfigstatusgrafana">grafana</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          <br/>
        </td>
      </tr>
      <tr>
        <td><b>removeOldOperatorBundleResources</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          Indicate when the old operator bundle resources has been removed<br/>
        </td>
      </tr>
      <tr>
        <td><b>version</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Latest version of the operator used to check for updates<br/>
        </td>
      </tr></tbody>
</table>

### SGConfig.status.conditions[index]
<sup><sup>[↩ Parent](#sgconfigstatus)</sup></sup>



<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>lastTransitionTime</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Last time the condition transitioned from one status to another.<br/>
        </td>
      </tr>
      <tr>
        <td><b>message</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          A human readable message indicating details about the transition.<br/>
        </td>
      </tr>
      <tr>
        <td><b>reason</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The reason for the condition's last transition.<br/>
        </td>
      </tr>
      <tr>
        <td><b>status</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Status of the condition, one of True, False, Unknown.<br/>
        </td>
      </tr>
      <tr>
        <td><b>type</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Type of deployment condition.<br/>
        </td>
      </tr></tbody>
</table>

### SGConfig.status.grafana
<sup><sup>[↩ Parent](#sgconfigstatus)</sup></sup>



<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>configHash</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Grafana configuration hash<br/>
        </td>
      </tr>
      <tr>
        <td><b>token</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Grafana Token that allow to access dashboards<br/>
        </td>
      </tr>
      <tr>
        <td><b>urls</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          Grafana URLs to StackGres dashboards<br/>
        </td>
      </tr></tbody>
</table>

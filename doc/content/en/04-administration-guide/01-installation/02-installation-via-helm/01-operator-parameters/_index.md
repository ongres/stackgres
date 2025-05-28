---
title: Operator Parameters
weight: 1
url: /install/helm/parameters
aliases: [ /install/operator/parameters ]
description: Details about cluster parameters that can be used with Helm to set up the operator.
showToc: true
---

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Key</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th><div style="width:5rem">Default</div></th>
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
			<td id="adminui--image--name">adminui.image.name</td>
			<td>string</td>
			<td>`"stackgres/admin-ui"`</td>
			<td>Web Console image name</td>
      </tr>
      <tr>
			<td id="adminui--image--pullPolicy">adminui.image.pullPolicy</td>
			<td>string</td>
			<td>`"IfNotPresent"`</td>
			<td>Web Console image pull policy</td>
      </tr>
      <tr>
			<td id="adminui--image--tag">adminui.image.tag</td>
			<td>string</td>
			<td>`"1.16.3"`</td>
			<td>Web Console image tag</td>
      </tr>
      <tr>
			<td id="adminui--resources">adminui.resources</td>
			<td>object</td>
			<td>`{}`</td>
			<td>Web Console resources. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.27/#resourcerequirements-v1-core</td>
      </tr>
      <tr>
			<td id="adminui--service--exposeHTTP">adminui.service.exposeHTTP</td>
			<td>bool</td>
			<td>`false`</td>
			<td>When set to `true` the HTTP port will be exposed in the Web Console Service</td>
      </tr>
      <tr>
			<td id="adminui--service--loadBalancerIP">adminui.service.loadBalancerIP</td>
			<td>string</td>
			<td>`nil`</td>
			<td>LoadBalancer will get created with the IP specified in   this field. This feature depends on whether the underlying cloud-provider supports specifying   the loadBalancerIP when a load balancer is created. This field will be ignored if the   cloud-provider does not support the feature.</td>
      </tr>
      <tr>
			<td id="adminui--service--loadBalancerSourceRanges">adminui.service.loadBalancerSourceRanges</td>
			<td>array</td>
			<td>`nil`</td>
			<td>If specified and supported by the platform,   this will restrict traffic through the cloud-provider load-balancer will be restricted to the   specified client IPs. This field will be ignored if the cloud-provider does not support the   feature. More info: https://kubernetes.io/docs/tasks/access-application-cluster/configure-cloud-provider-firewall/</td>
      </tr>
      <tr>
			<td id="adminui--service--nodePort">adminui.service.nodePort</td>
			<td>integer</td>
			<td>`nil`</td>
			<td>The HTTPS port used to expose the Service on Kubernetes nodes</td>
      </tr>
      <tr>
			<td id="adminui--service--nodePortHTTP">adminui.service.nodePortHTTP</td>
			<td>integer</td>
			<td>`nil`</td>
			<td>The HTTP port used to expose the Service on Kubernetes nodes</td>
      </tr>
      <tr>
			<td id="adminui--service--type">adminui.service.type</td>
			<td>string</td>
			<td>`"ClusterIP"`</td>
			<td>The type used for the service of the UI: * Set to LoadBalancer to create a load balancer (if supported by the kubernetes cluster)   to allow connect from Internet to the UI. Note that enabling this feature will probably incurr in   some fee that depend on the host of the kubernetes cluster (for example this is true for EKS, GKE   and AKS). * Set to NodePort to expose admin UI from kubernetes nodes.</td>
      </tr>
      <tr>
			<td id="allowedNamespaces">allowedNamespaces</td>
			<td>list</td>
			<td>`[]`</td>
			<td>Section to configure Operator allowed namespaces that the operator is allowed to use. If empty all namespaces will be allowed (default). </td>
      </tr>
      <tr>
			<td id="authentication--createAdminSecret">authentication.createAdminSecret</td>
			<td>boolean</td>
			<td>`true`</td>
			<td>When `true` will create the secret used to store the `admin` user credentials to access the UI.</td>
      </tr>
      <tr>
			<td id="authentication--oidc">authentication.oidc</td>
			<td>string</td>
			<td>`nil`</td>
			<td></td>
      </tr>
      <tr>
			<td id="authentication--password">authentication.password</td>
			<td>string</td>
			<td>`nil`</td>
			<td>The admin password that will be required to access the UI</td>
      </tr>
      <tr>
			<td id="authentication--type">authentication.type</td>
			<td>string</td>
			<td>`"jwt"`</td>
			<td>Specify the authentication mechanism to use. By default is `jwt`, see https://stackgres.io/doc/latest/api/rbac#local-secret-mechanism.   If set to `oidc` then see https://stackgres.io/doc/latest/api/rbac/#openid-connect-provider-mechanism.</td>
      </tr>
      <tr>
			<td id="authentication--user">authentication.user</td>
			<td>string</td>
			<td>`"admin"`</td>
			<td>The admin username that will be required to access the UI</td>
      </tr>
      <tr>
			<td id="cert--autoapprove">cert.autoapprove</td>
			<td>bool</td>
			<td>`true`</td>
			<td>If set to `true` the CertificateSigningRequest used to generate the certificate used by   Webhooks will be approved by the Operator Installation Job.</td>
      </tr>
      <tr>
			<td id="cert--certDuration">cert.certDuration</td>
			<td>integer</td>
			<td>`730`</td>
			<td>The duration in days of the generated certificate for the Operator after which it will expire and be regenerated.   If not specified it will be set to 730 (2 years) by default.</td>
      </tr>
      <tr>
			<td id="cert--certManager--autoConfigure">cert.certManager.autoConfigure</td>
			<td>bool</td>
			<td>`false`</td>
			<td>When set to `true` then Issuer and Certificate for Operator and Web Console / REST API   Pods will be generated</td>
      </tr>
      <tr>
			<td id="cert--certManager--duration">cert.certManager.duration</td>
			<td>string</td>
			<td>`"2160h"`</td>
			<td>The requested duration (i.e. lifetime) of the Certificates. See https://cert-manager.io/docs/reference/api-docs/#cert-manager.io%2fv1</td>
      </tr>
      <tr>
			<td id="cert--certManager--encoding">cert.certManager.encoding</td>
			<td>string</td>
			<td>`"PKCS1"`</td>
			<td>The private key cryptography standards (PKCS) encoding for this certificate’s private key to be encoded in. See https://cert-manager.io/docs/reference/api-docs/#cert-manager.io/v1.CertificatePrivateKey</td>
      </tr>
      <tr>
			<td id="cert--certManager--renewBefore">cert.certManager.renewBefore</td>
			<td>string</td>
			<td>`"360h"`</td>
			<td>How long before the currently issued certificate’s expiry cert-manager should renew the certificate. See https://cert-manager.io/docs/reference/api-docs/#cert-manager.io%2fv1</td>
      </tr>
      <tr>
			<td id="cert--certManager--size">cert.certManager.size</td>
			<td>int</td>
			<td>`2048`</td>
			<td>Size is the key bit size of the corresponding private key for this certificate. See https://cert-manager.io/docs/reference/api-docs/#cert-manager.io/v1.CertificatePrivateKey</td>
      </tr>
      <tr>
			<td id="cert--createForCollector">cert.createForCollector</td>
			<td>bool</td>
			<td>`true`</td>
			<td>When set to `true` the OpenTelemetry Collector certificate will be created.</td>
      </tr>
      <tr>
			<td id="cert--createForOperator">cert.createForOperator</td>
			<td>bool</td>
			<td>`true`</td>
			<td>When set to `true` the Operator certificate will be created.</td>
      </tr>
      <tr>
			<td id="cert--createForWebApi">cert.createForWebApi</td>
			<td>bool</td>
			<td>`true`</td>
			<td>When set to `true` the Web Console / REST API certificate will be created.</td>
      </tr>
      <tr>
			<td id="cert--crt">cert.crt</td>
			<td>string</td>
			<td>`nil`</td>
			<td>The Operator Webhooks certificate issued by Kubernetes cluster CA.</td>
      </tr>
      <tr>
			<td id="cert--jwtRsaKey">cert.jwtRsaKey</td>
			<td>string</td>
			<td>`nil`</td>
			<td>The private RSA key used to generate JWTs used in REST API authentication.</td>
      </tr>
      <tr>
			<td id="cert--jwtRsaPub">cert.jwtRsaPub</td>
			<td>string</td>
			<td>`nil`</td>
			<td>The public RSA key used to verify JWTs used in REST API authentication.</td>
      </tr>
      <tr>
			<td id="cert--key">cert.key</td>
			<td>string</td>
			<td>`nil`</td>
			<td>The private RSA key used to create the Operator Webhooks certificate issued by the   Kubernetes cluster CA.</td>
      </tr>
      <tr>
			<td id="cert--regenerateCert">cert.regenerateCert</td>
			<td>bool</td>
			<td>`true`</td>
			<td>When set to `true` the Operator certificates will be regenerated if `createForOperator` is set to `true`, and the certificate is expired or invalid.</td>
      </tr>
      <tr>
			<td id="cert--regenerateWebCert">cert.regenerateWebCert</td>
			<td>bool</td>
			<td>`true`</td>
			<td>When set to `true` the Web Console / REST API certificates will be regenerated if `createForWebApi` is set to `true`, and the certificate is expired or invalid.</td>
      </tr>
      <tr>
			<td id="cert--regenerateWebRsa">cert.regenerateWebRsa</td>
			<td>bool</td>
			<td>`true`</td>
			<td>When set to `true` the Web Console / REST API RSA key pair will be regenerated if `createForWebApi` is set to `true`, and the certificate is expired or invalid.</td>
      </tr>
      <tr>
			<td id="cert--secretName">cert.secretName</td>
			<td>string</td>
			<td>`nil`</td>
			<td>The Secret name with the Operator Webhooks certificate issued by the Kubernetes cluster CA   of type kubernetes.io/tls. See https://kubernetes.io/docs/concepts/configuration/secret/#tls-secrets</td>
      </tr>
      <tr>
			<td id="cert--webCertDuration">cert.webCertDuration</td>
			<td>integer</td>
			<td>`nil`</td>
			<td>The duration in days of the generated certificate for the Web Console / REST API after which it will expire and be regenerated.   If not specified it will be set to 730 (2 years) by default.</td>
      </tr>
      <tr>
			<td id="cert--webCrt">cert.webCrt</td>
			<td>string</td>
			<td>`nil`</td>
			<td>The Web Console / REST API certificate</td>
      </tr>
      <tr>
			<td id="cert--webKey">cert.webKey</td>
			<td>string</td>
			<td>`nil`</td>
			<td>The private RSA key used to create the Web Console / REST API certificate</td>
      </tr>
      <tr>
			<td id="cert--webRsaDuration">cert.webRsaDuration</td>
			<td>integer</td>
			<td>`nil`</td>
			<td>The duration in days of the generated RSA key pair for the Web Console / REST API after which it will expire and be regenerated.   If not specified it will be set to 730 (2 years) by default.</td>
      </tr>
      <tr>
			<td id="cert--webSecretName">cert.webSecretName</td>
			<td>string</td>
			<td>`nil`</td>
			<td>The Secret name with the Web Console / REST API certificate   of type kubernetes.io/tls. See https://kubernetes.io/docs/concepts/configuration/secret/#tls-secrets</td>
      </tr>
      <tr>
			<td id="collector--affinity">collector.affinity</td>
			<td>object</td>
			<td>`{}`</td>
			<td>OpenTelemetry Collector Pod affinity</td>
      </tr>
      <tr>
			<td id="collector--annotations">collector.annotations</td>
			<td>object</td>
			<td>`{}`</td>
			<td>OpenTelemetry Collector Pod annotations. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#affinity-v1-core</td>
      </tr>
      <tr>
			<td id="collector--config--exporters--otlp--endpoint">collector.config.exporters.otlp.endpoint</td>
			<td>string</td>
			<td>`"stackgres-collector:4317"`</td>
			<td></td>
      </tr>
      <tr>
			<td id="collector--config--exporters--otlp--tls--ca_file">collector.config.exporters.otlp.tls.ca_file</td>
			<td>string</td>
			<td>`"/etc/operator/certs/tls.crt"`</td>
			<td></td>
      </tr>
      <tr>
			<td id="collector--config--exporters--prometheus--enable_open_metrics">collector.config.exporters.prometheus.enable_open_metrics</td>
			<td>bool</td>
			<td>`false`</td>
			<td></td>
      </tr>
      <tr>
			<td id="collector--config--exporters--prometheus--endpoint">collector.config.exporters.prometheus.endpoint</td>
			<td>string</td>
			<td>`"0.0.0.0:9464"`</td>
			<td></td>
      </tr>
      <tr>
			<td id="collector--config--exporters--prometheus--metric_expiration">collector.config.exporters.prometheus.metric_expiration</td>
			<td>string</td>
			<td>`"5m"`</td>
			<td></td>
      </tr>
      <tr>
			<td id="collector--config--exporters--prometheus--resource_to_telemetry_conversion--enabled">collector.config.exporters.prometheus.resource_to_telemetry_conversion.enabled</td>
			<td>bool</td>
			<td>`false`</td>
			<td></td>
      </tr>
      <tr>
			<td id="collector--config--exporters--prometheus--send_timestamps">collector.config.exporters.prometheus.send_timestamps</td>
			<td>bool</td>
			<td>`true`</td>
			<td></td>
      </tr>
      <tr>
			<td id="collector--config--exporters--prometheus--tls--ca_file">collector.config.exporters.prometheus.tls.ca_file</td>
			<td>string</td>
			<td>`"/etc/operator/certs/tls.crt"`</td>
			<td></td>
      </tr>
      <tr>
			<td id="collector--config--exporters--prometheus--tls--cert_file">collector.config.exporters.prometheus.tls.cert_file</td>
			<td>string</td>
			<td>`"/etc/operator/certs/tls.crt"`</td>
			<td></td>
      </tr>
      <tr>
			<td id="collector--config--exporters--prometheus--tls--key_file">collector.config.exporters.prometheus.tls.key_file</td>
			<td>string</td>
			<td>`"/etc/operator/certs/tls.key"`</td>
			<td></td>
      </tr>
      <tr>
			<td id="collector--config--exporters--prometheus--tls--reload_interval">collector.config.exporters.prometheus.tls.reload_interval</td>
			<td>string</td>
			<td>`"10m"`</td>
			<td></td>
      </tr>
      <tr>
			<td id="collector--config--processors--memory_limiter--check_interval">collector.config.processors.memory_limiter.check_interval</td>
			<td>string</td>
			<td>`"1s"`</td>
			<td></td>
      </tr>
      <tr>
			<td id="collector--config--processors--memory_limiter--limit_percentage">collector.config.processors.memory_limiter.limit_percentage</td>
			<td>int</td>
			<td>`80`</td>
			<td></td>
      </tr>
      <tr>
			<td id="collector--config--processors--memory_limiter--spike_limit_percentage">collector.config.processors.memory_limiter.spike_limit_percentage</td>
			<td>int</td>
			<td>`15`</td>
			<td></td>
      </tr>
      <tr>
			<td id="collector--config--receivers--otlp--protocols--grpc--endpoint">collector.config.receivers.otlp.protocols.grpc.endpoint</td>
			<td>string</td>
			<td>`"0.0.0.0:4317"`</td>
			<td></td>
      </tr>
      <tr>
			<td id="collector--config--receivers--otlp--protocols--grpc--tls--ca_file">collector.config.receivers.otlp.protocols.grpc.tls.ca_file</td>
			<td>string</td>
			<td>`"/etc/operator/certs/tls.crt"`</td>
			<td></td>
      </tr>
      <tr>
			<td id="collector--config--receivers--otlp--protocols--grpc--tls--cert_file">collector.config.receivers.otlp.protocols.grpc.tls.cert_file</td>
			<td>string</td>
			<td>`"/etc/operator/certs/tls.crt"`</td>
			<td></td>
      </tr>
      <tr>
			<td id="collector--config--receivers--otlp--protocols--grpc--tls--key_file">collector.config.receivers.otlp.protocols.grpc.tls.key_file</td>
			<td>string</td>
			<td>`"/etc/operator/certs/tls.key"`</td>
			<td></td>
      </tr>
      <tr>
			<td id="collector--config--service--extensions">collector.config.service.extensions</td>
			<td>list</td>
			<td>`[]`</td>
			<td></td>
      </tr>
      <tr>
			<td id="collector--config--service--pipelines--metrics--exporters[0]">collector.config.service.pipelines.metrics.exporters[0]</td>
			<td>string</td>
			<td>`"prometheus"`</td>
			<td></td>
      </tr>
      <tr>
			<td id="collector--config--service--pipelines--metrics--processors[0]">collector.config.service.pipelines.metrics.processors[0]</td>
			<td>string</td>
			<td>`"memory_limiter"`</td>
			<td></td>
      </tr>
      <tr>
			<td id="collector--config--service--pipelines--metrics--receivers[0]">collector.config.service.pipelines.metrics.receivers[0]</td>
			<td>string</td>
			<td>`"prometheus"`</td>
			<td></td>
      </tr>
      <tr>
			<td id="collector--name">collector.name</td>
			<td>string</td>
			<td>`"stackgres-collector"`</td>
			<td>OpenTelemetry Collector Deployment/DeamonSet base name</td>
      </tr>
      <tr>
			<td id="collector--nodeSelector">collector.nodeSelector</td>
			<td>object</td>
			<td>`{}`</td>
			<td>OpenTelemetry Collector Pod node slector</td>
      </tr>
      <tr>
			<td id="collector--ports[0]--containerPort">collector.ports[0].containerPort</td>
			<td>int</td>
			<td>`9464`</td>
			<td></td>
      </tr>
      <tr>
			<td id="collector--ports[0]--name">collector.ports[0].name</td>
			<td>string</td>
			<td>`"prom-http"`</td>
			<td></td>
      </tr>
      <tr>
			<td id="collector--ports[0]--protocol">collector.ports[0].protocol</td>
			<td>string</td>
			<td>`"TCP"`</td>
			<td></td>
      </tr>
      <tr>
			<td id="collector--prometheusOperator--allowDiscovery">collector.prometheusOperator.allowDiscovery</td>
			<td>bool</td>
			<td>`true`</td>
			<td>If set to false or monitors is set automatic bind to Prometheus   created using the [Prometheus Operator](https://github.com/prometheus-operator/prometheus-operator) will be disabled. If disabled the cluster will not be binded to Prometheus automatically and will require manual configuration. Will be ignored if monitors is set</td>
      </tr>
      <tr>
			<td id="collector--prometheusOperator--monitors">collector.prometheusOperator.monitors</td>
			<td>string</td>
			<td>`nil`</td>
			<td></td>
      </tr>
      <tr>
			<td id="collector--receivers--deployments">collector.receivers.deployments</td>
			<td>string</td>
			<td>`nil`</td>
			<td></td>
      </tr>
      <tr>
			<td id="collector--receivers--enabled">collector.receivers.enabled</td>
			<td>bool</td>
			<td>`false`</td>
			<td>When true the OpenTelemetry Collector receivers will be enabled</td>
      </tr>
      <tr>
			<td id="collector--receivers--exporters">collector.receivers.exporters</td>
			<td>int</td>
			<td>`1`</td>
			<td>Allow to increase the number of OpenTelemetry Collector exporters if receivers is enabled</td>
      </tr>
      <tr>
			<td id="collector--resources">collector.resources</td>
			<td>object</td>
			<td>`{"limits":{"cpu":"1","memory":"4Gi"},"requests":{"cpu":"250m","memory":"1Gi"}}`</td>
			<td>OpenTelemetry Collector Pod resources. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#resourcerequirements-v1-core</td>
      </tr>
      <tr>
			<td id="collector--service--annotations">collector.service.annotations</td>
			<td>object</td>
			<td>`{}`</td>
			<td>OpenTelemetry Collector Service annotations</td>
      </tr>
      <tr>
			<td id="collector--service--spec--ports[0]--name">collector.service.spec.ports[0].name</td>
			<td>string</td>
			<td>`"prom-http"`</td>
			<td></td>
      </tr>
      <tr>
			<td id="collector--service--spec--ports[0]--port">collector.service.spec.ports[0].port</td>
			<td>int</td>
			<td>`9464`</td>
			<td></td>
      </tr>
      <tr>
			<td id="collector--service--spec--ports[0]--protocol">collector.service.spec.ports[0].protocol</td>
			<td>string</td>
			<td>`"TCP"`</td>
			<td></td>
      </tr>
      <tr>
			<td id="collector--service--spec--ports[0]--targetPort">collector.service.spec.ports[0].targetPort</td>
			<td>string</td>
			<td>`"prom-http"`</td>
			<td></td>
      </tr>
      <tr>
			<td id="collector--service--spec--type">collector.service.spec.type</td>
			<td>string</td>
			<td>`"ClusterIP"`</td>
			<td></td>
      </tr>
      <tr>
			<td id="collector--serviceAccount--annotations">collector.serviceAccount.annotations</td>
			<td>object</td>
			<td>`{}`</td>
			<td>OpenTelemetry Collector ServiceAccount annotations</td>
      </tr>
      <tr>
			<td id="collector--tolerations">collector.tolerations</td>
			<td>list</td>
			<td>`[]`</td>
			<td>OpenTelemetry Collector Pod tolerations. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.27/#toleration-v1-core</td>
      </tr>
      <tr>
			<td id="collector--volumeMounts">collector.volumeMounts</td>
			<td>list</td>
			<td>`[]`</td>
			<td></td>
      </tr>
      <tr>
			<td id="collector--volumes">collector.volumes</td>
			<td>list</td>
			<td>`[]`</td>
			<td></td>
      </tr>
      <tr>
			<td id="containerRegistry">containerRegistry</td>
			<td>string</td>
			<td>`"quay.io"`</td>
			<td>The container registry host (and port) where the images will be pulled from.</td>
      </tr>
      <tr>
			<td id="deploy--collector">deploy.collector</td>
			<td>bool</td>
			<td>`true`</td>
			<td>When set to `true` the OpenTelemetry Collector will be deployed.</td>
      </tr>
      <tr>
			<td id="deploy--operator">deploy.operator</td>
			<td>bool</td>
			<td>`true`</td>
			<td>When set to `true` the Operator will be deployed.</td>
      </tr>
      <tr>
			<td id="deploy--restapi">deploy.restapi</td>
			<td>bool</td>
			<td>`true`</td>
			<td>When set to `true` the Web Console / REST API will be deployed.</td>
      </tr>
      <tr>
			<td id="developer--allowPullExtensionsFromImageRepository">developer.allowPullExtensionsFromImageRepository</td>
			<td>bool</td>
			<td>`false`</td>
			<td>If set to `true` and `extensions.cache.enabled` is also `true`   it will try to download extensions from images (experimental)</td>
      </tr>
      <tr>
			<td id="developer--disableArbitraryUser">developer.disableArbitraryUser</td>
			<td>bool</td>
			<td>`false`</td>
			<td>It set to `true` disable arbitrary user that is set for OpenShift clusters</td>
      </tr>
      <tr>
			<td id="developer--enableJvmDebug">developer.enableJvmDebug</td>
			<td>bool</td>
			<td>`false`</td>
			<td>Only work with JVM version and allow connect on port 8000 of operator Pod with jdb or similar</td>
      </tr>
      <tr>
			<td id="developer--enableJvmDebugSuspend">developer.enableJvmDebugSuspend</td>
			<td>bool</td>
			<td>`false`</td>
			<td>Only work with JVM version and if `enableJvmDebug` is `true`   suspend the JVM until a debugger session is started</td>
      </tr>
      <tr>
			<td id="developer--externalOperatorIp">developer.externalOperatorIp</td>
			<td>string</td>
			<td>`nil`</td>
			<td>Set the external Operator IP</td>
      </tr>
      <tr>
			<td id="developer--externalOperatorPort">developer.externalOperatorPort</td>
			<td>integer</td>
			<td>`nil`</td>
			<td>Set the external Operator port</td>
      </tr>
      <tr>
			<td id="developer--externalRestApiIp">developer.externalRestApiIp</td>
			<td>string</td>
			<td>`nil`</td>
			<td>Set the external REST API IP</td>
      </tr>
      <tr>
			<td id="developer--externalRestApiPort">developer.externalRestApiPort</td>
			<td>integer</td>
			<td>`nil`</td>
			<td>Set the external REST API port</td>
      </tr>
      <tr>
			<td id="developer--logLevel">developer.logLevel</td>
			<td>string</td>
			<td>`nil`</td>
			<td>Set `quarkus.log.level`. See https://quarkus.io/guides/logging#root-logger-configuration</td>
      </tr>
      <tr>
			<td id="developer--patches--adminui--volumeMounts">developer.patches.adminui.volumeMounts</td>
			<td>list</td>
			<td>`[]`</td>
			<td>Pod's container volume mounts. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.27/#volumemount-v1-core</td>
      </tr>
      <tr>
			<td id="developer--patches--adminui--volumes">developer.patches.adminui.volumes</td>
			<td>list</td>
			<td>`[]`</td>
			<td>Pod volumes. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.27/#volume-v1-core</td>
      </tr>
      <tr>
			<td id="developer--patches--clusterController--volumeMounts">developer.patches.clusterController.volumeMounts</td>
			<td>list</td>
			<td>`[]`</td>
			<td>Pod's container volume mounts. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.27/#volumemount-v1-core</td>
      </tr>
      <tr>
			<td id="developer--patches--clusterController--volumes">developer.patches.clusterController.volumes</td>
			<td>list</td>
			<td>`[]`</td>
			<td>Pod volumes. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.27/#volume-v1-core</td>
      </tr>
      <tr>
			<td id="developer--patches--jobs--volumeMounts">developer.patches.jobs.volumeMounts</td>
			<td>list</td>
			<td>`[]`</td>
			<td>Pod's container volume mounts. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.27/#volumemount-v1-core</td>
      </tr>
      <tr>
			<td id="developer--patches--jobs--volumes">developer.patches.jobs.volumes</td>
			<td>list</td>
			<td>`[]`</td>
			<td>Pod volumes. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.27/#volume-v1-core</td>
      </tr>
      <tr>
			<td id="developer--patches--operator--volumeMounts">developer.patches.operator.volumeMounts</td>
			<td>list</td>
			<td>`[]`</td>
			<td>Pod's container volume mounts. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.27/#volumemount-v1-core</td>
      </tr>
      <tr>
			<td id="developer--patches--operator--volumes">developer.patches.operator.volumes</td>
			<td>list</td>
			<td>`[]`</td>
			<td>Pod volumes. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.27/#volume-v1-core</td>
      </tr>
      <tr>
			<td id="developer--patches--restapi--volumeMounts">developer.patches.restapi.volumeMounts</td>
			<td>list</td>
			<td>`[]`</td>
			<td>Pod's container volume mounts. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.27/#volumemount-v1-core</td>
      </tr>
      <tr>
			<td id="developer--patches--restapi--volumes">developer.patches.restapi.volumes</td>
			<td>list</td>
			<td>`[]`</td>
			<td>Pod volumes. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.27/#volume-v1-core</td>
      </tr>
      <tr>
			<td id="developer--patches--stream--volumeMounts">developer.patches.stream.volumeMounts</td>
			<td>list</td>
			<td>`[]`</td>
			<td>Pod's container volume mounts. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.27/#volumemount-v1-core</td>
      </tr>
      <tr>
			<td id="developer--patches--stream--volumes">developer.patches.stream.volumes</td>
			<td>list</td>
			<td>`[]`</td>
			<td>Pod volumes. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.27/#volume-v1-core</td>
      </tr>
      <tr>
			<td id="developer--showDebug">developer.showDebug</td>
			<td>bool</td>
			<td>`false`</td>
			<td>If set to `true` add extra debug to any script controlled by the reconciliation cycle of the operator configuration</td>
      </tr>
      <tr>
			<td id="developer--showStackTraces">developer.showStackTraces</td>
			<td>bool</td>
			<td>`false`</td>
			<td>Set `quarkus.log.console.format` to `%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c{4.}] (%t) %s%e%n`. See https://quarkus.io/guides/logging#logging-format</td>
      </tr>
      <tr>
			<td id="developer--version">developer.version</td>
			<td>string</td>
			<td>`nil`</td>
			<td>Set the operator version (used for testing)</td>
      </tr>
      <tr>
			<td id="extensions--cache--enabled">extensions.cache.enabled</td>
			<td>bool</td>
			<td>`false`</td>
			<td>When set to `true` enable the extensions cache.  This feature is in beta and may cause failures, please use with caution and report any   error to https://gitlab.com/ongresinc/stackgres/-/issues/new</td>
      </tr>
      <tr>
			<td id="extensions--cache--hostPath">extensions.cache.hostPath</td>
			<td>string</td>
			<td>`nil`</td>
			<td>If set, will use a host path volume with the specified path for the extensions cache   instead of a PersistentVolume</td>
      </tr>
      <tr>
			<td id="extensions--cache--persistentVolume--size">extensions.cache.persistentVolume.size</td>
			<td>string</td>
			<td>`"1Gi"`</td>
			<td>The PersistentVolume size for the extensions cache  Only use whole numbers (e.g. not 1e6) and K/Ki/M/Mi/G/Gi as units</td>
      </tr>
      <tr>
			<td id="extensions--cache--persistentVolume--storageClass">extensions.cache.persistentVolume.storageClass</td>
			<td>string</td>
			<td>`nil`</td>
			<td>If defined set storage class If set to "-" (equivalent to storageClass: "" in a PV spec) disables   dynamic provisioning If undefined (the default) or set to null, no storageClass spec is   set, choosing the default provisioner.  (gp2 on AWS, standard on   GKE, AWS & OpenStack)</td>
      </tr>
      <tr>
			<td id="extensions--cache--preloadedExtensions">extensions.cache.preloadedExtensions</td>
			<td>list</td>
			<td>`["x86_64/linux/timescaledb-1\\.7\\.4-pg12"]`</td>
			<td>An array of extensions pattern used to pre-loaded estensions into the extensions cache</td>
      </tr>
      <tr>
			<td id="extensions--repositoryUrls">extensions.repositoryUrls</td>
			<td>list</td>
			<td>`["https://extensions.stackgres.io/postgres/repository"]`</td>
			<td>A list of extensions repository URLs used to retrieve extensions  To set a proxy for extensions repository add parameter proxyUrl to the URL:   `https://extensions.stackgres.io/postgres/repository?proxyUrl=<proxy scheme>%3A%2F%2F<proxy host>[%3A<proxy port>]` (URL encoded)  Other URL parameters are:  * `skipHostnameVerification`: set it to `true` in order to use a server or a proxy with a self signed certificate * `retry`: set it to `<max retriex>[:<sleep before next retry>]` in order to retry a request on failure * `setHttpScheme`: set it to `true` in order to force using HTTP scheme</td>
      </tr>
      <tr>
			<td id="grafana--autoEmbed">grafana.autoEmbed</td>
			<td>bool</td>
			<td>`false`</td>
			<td>When set to `true` embed automatically Grafana into the Web Console by creating the   StackGres dashboards and the read-only role used to read it from the Web Console </td>
      </tr>
      <tr>
			<td id="grafana--dashboardConfigMap">grafana.dashboardConfigMap</td>
			<td>string</td>
			<td>`nil`</td>
			<td>The ConfigMap name with the dashboard JSONs   that will be created in Grafana. If not set the default   StackGres dashboards will be created. (used to embed automatically Grafana)</td>
      </tr>
      <tr>
			<td id="grafana--datasourceName">grafana.datasourceName</td>
			<td>string</td>
			<td>`"Prometheus"`</td>
			<td>The datasource name used to create the StackGres Dashboards into Grafana</td>
      </tr>
      <tr>
			<td id="grafana--password">grafana.password</td>
			<td>string</td>
			<td>`"prom-operator"`</td>
			<td>The password to access Grafana. By default prom-operator (the default in for   kube-prometheus-stack helm chart). (used to embed automatically Grafana)</td>
      </tr>
      <tr>
			<td id="grafana--schema">grafana.schema</td>
			<td>string</td>
			<td>`"http"`</td>
			<td>The schema to access Grafana. By default http. (used to embed manually and   automatically grafana)</td>
      </tr>
      <tr>
			<td id="grafana--secretName">grafana.secretName</td>
			<td>string</td>
			<td>`nil`</td>
			<td>The name of secret with credentials to access Grafana. (used to embed   automatically Grafana, alternative to use `user` and `password`)</td>
      </tr>
      <tr>
			<td id="grafana--secretNamespace">grafana.secretNamespace</td>
			<td>string</td>
			<td>`nil`</td>
			<td>The namespace of secret with credentials to access Grafana. (used to   embed automatically Grafana, alternative to use `user` and `password`)</td>
      </tr>
      <tr>
			<td id="grafana--secretPasswordKey">grafana.secretPasswordKey</td>
			<td>string</td>
			<td>`nil`</td>
			<td>The key of secret with password used to access Grafana. (used to   embed automatically Grafana, alternative to use `user` and `password`)</td>
      </tr>
      <tr>
			<td id="grafana--secretUserKey">grafana.secretUserKey</td>
			<td>string</td>
			<td>`nil`</td>
			<td>The key of secret with username used to access Grafana. (used to embed   automatically Grafana, alternative to use `user` and `password`)</td>
      </tr>
      <tr>
			<td id="grafana--token">grafana.token</td>
			<td>string</td>
			<td>`nil`</td>
			<td>The Grafana API token to access the PostgreSQL dashboards created   in Grafana (used to embed manually Grafana)</td>
      </tr>
      <tr>
			<td id="grafana--urls">grafana.urls</td>
			<td>array</td>
			<td>`nil`</td>
			<td>The URLs of the PostgreSQL dashboards created in Grafana (used to embed manually   Grafana). It must contain an entry for each JSON file under `grafana-dashboards` folder: `archiving.json`,    `connection-pooling.json`, `current-activity.json`, `db-info.json`, `db-objects.json`, `db-os.json`, `queries.json`   and `replication.json`</td>
      </tr>
      <tr>
			<td id="grafana--user">grafana.user</td>
			<td>string</td>
			<td>`"admin"`</td>
			<td>The username to access Grafana. By default admin. (used to embed automatically   Grafana)</td>
      </tr>
      <tr>
			<td id="grafana--webHost">grafana.webHost</td>
			<td>string</td>
			<td>`nil`</td>
			<td>The service host name to access grafana (used to embed manually and   automatically Grafana).  The parameter value should point to the grafana service following the    [DNS reference](https://kubernetes.io/docs/concepts/services-networking/dns-pod-service/) `svc_name.namespace`</td>
      </tr>
      <tr>
			<td id="imagePullPolicy">imagePullPolicy</td>
			<td>string</td>
			<td>`"IfNotPresent"`</td>
			<td>Image pull policy used for images loaded by the Operator</td>
      </tr>
      <tr>
			<td id="jobs--affinity">jobs.affinity</td>
			<td>object</td>
			<td>`{}`</td>
			<td>Operator Installation Jobs affinity. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.27/#affinity-v1-core</td>
      </tr>
      <tr>
			<td id="jobs--annotations">jobs.annotations</td>
			<td>object</td>
			<td>`{}`</td>
			<td>Operator Installation Jobs annotations</td>
      </tr>
      <tr>
			<td id="jobs--image--name">jobs.image.name</td>
			<td>string</td>
			<td>`"stackgres/jobs"`</td>
			<td>Operator Installation Jobs image name</td>
      </tr>
      <tr>
			<td id="jobs--image--pullPolicy">jobs.image.pullPolicy</td>
			<td>string</td>
			<td>`"IfNotPresent"`</td>
			<td>Operator Installation Jobs image pull policy</td>
      </tr>
      <tr>
			<td id="jobs--image--tag">jobs.image.tag</td>
			<td>string</td>
			<td>`"1.16.3"`</td>
			<td>Operator Installation Jobs image tag</td>
      </tr>
      <tr>
			<td id="jobs--nodeSelector">jobs.nodeSelector</td>
			<td>object</td>
			<td>`{}`</td>
			<td>Operator Installation Jobs node selector</td>
      </tr>
      <tr>
			<td id="jobs--resources">jobs.resources</td>
			<td>object</td>
			<td>`{}`</td>
			<td>Operator Installation Jobs resources. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.27/#resourcerequirements-v1-core</td>
      </tr>
      <tr>
			<td id="jobs--tolerations">jobs.tolerations</td>
			<td>list</td>
			<td>`[]`</td>
			<td>Operator Installation Jobs tolerations. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.27/#toleration-v1-core</td>
      </tr>
      <tr>
			<td id="operator--affinity">operator.affinity</td>
			<td>object</td>
			<td>`{}`</td>
			<td>Operator Pod affinity. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.27/#affinity-v1-core</td>
      </tr>
      <tr>
			<td id="operator--annotations">operator.annotations</td>
			<td>object</td>
			<td>`{}`</td>
			<td>Operator Pod annotations</td>
      </tr>
      <tr>
			<td id="operator--image--name">operator.image.name</td>
			<td>string</td>
			<td>`"stackgres/operator"`</td>
			<td>Operator image name</td>
      </tr>
      <tr>
			<td id="operator--image--pullPolicy">operator.image.pullPolicy</td>
			<td>string</td>
			<td>`"IfNotPresent"`</td>
			<td>Operator image pull policy</td>
      </tr>
      <tr>
			<td id="operator--image--tag">operator.image.tag</td>
			<td>string</td>
			<td>`"1.16.3"`</td>
			<td>Operator image tag</td>
      </tr>
      <tr>
			<td id="operator--nodeSelector">operator.nodeSelector</td>
			<td>object</td>
			<td>`{}`</td>
			<td>Operator Pod node selector</td>
      </tr>
      <tr>
			<td id="operator--resources">operator.resources</td>
			<td>object</td>
			<td>`{}`</td>
			<td>Operator Pod resources. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.27/#resourcerequirements-v1-core</td>
      </tr>
      <tr>
			<td id="operator--service--annotations">operator.service.annotations</td>
			<td>object</td>
			<td>`{}`</td>
			<td>Section to configure Operator Service annotations</td>
      </tr>
      <tr>
			<td id="operator--serviceAccount--annotations">operator.serviceAccount.annotations</td>
			<td>object</td>
			<td>`{}`</td>
			<td>Section to configure Operator ServiceAccount annotations</td>
      </tr>
      <tr>
			<td id="operator--serviceAccount--repoCredentials">operator.serviceAccount.repoCredentials</td>
			<td>list</td>
			<td>`[]`</td>
			<td>Repositories credentials Secret names to attach to ServiceAccounts and Pods</td>
      </tr>
      <tr>
			<td id="operator--tolerations">operator.tolerations</td>
			<td>list</td>
			<td>`[]`</td>
			<td>Operator Pod tolerations. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.27/#toleration-v1-core</td>
      </tr>
      <tr>
			<td id="rbac--create">rbac.create</td>
			<td>bool</td>
			<td>`true`</td>
			<td>When set to `true` the admin user is assigned the `cluster-admin` ClusterRole by creating   ClusterRoleBinding.</td>
      </tr>
      <tr>
			<td id="restapi--affinity">restapi.affinity</td>
			<td>object</td>
			<td>`{}`</td>
			<td>REST API Pod affinity. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.27/#affinity-v1-core</td>
      </tr>
      <tr>
			<td id="restapi--annotations">restapi.annotations</td>
			<td>object</td>
			<td>`{}`</td>
			<td>REST API Pod annotations</td>
      </tr>
      <tr>
			<td id="restapi--image--name">restapi.image.name</td>
			<td>string</td>
			<td>`"stackgres/restapi"`</td>
			<td>REST API image name</td>
      </tr>
      <tr>
			<td id="restapi--image--pullPolicy">restapi.image.pullPolicy</td>
			<td>string</td>
			<td>`"IfNotPresent"`</td>
			<td>REST API image pull policy</td>
      </tr>
      <tr>
			<td id="restapi--image--tag">restapi.image.tag</td>
			<td>string</td>
			<td>`"1.16.3"`</td>
			<td>REST API image tag</td>
      </tr>
      <tr>
			<td id="restapi--name">restapi.name</td>
			<td>string</td>
			<td>`"stackgres-restapi"`</td>
			<td>REST API Deployment name</td>
      </tr>
      <tr>
			<td id="restapi--nodeSelector">restapi.nodeSelector</td>
			<td>object</td>
			<td>`{}`</td>
			<td>REST API Pod node selector</td>
      </tr>
      <tr>
			<td id="restapi--resources">restapi.resources</td>
			<td>object</td>
			<td>`{}`</td>
			<td>REST API Pod resources. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.27/#resourcerequirements-v1-core</td>
      </tr>
      <tr>
			<td id="restapi--service--annotations">restapi.service.annotations</td>
			<td>object</td>
			<td>`{}`</td>
			<td>REST API Service annotations</td>
      </tr>
      <tr>
			<td id="restapi--serviceAccount--annotations">restapi.serviceAccount.annotations</td>
			<td>object</td>
			<td>`{}`</td>
			<td>REST API ServiceAccount annotations</td>
      </tr>
      <tr>
			<td id="restapi--serviceAccount--repoCredentials">restapi.serviceAccount.repoCredentials</td>
			<td>list</td>
			<td>`[]`</td>
			<td>Repositories credentials Secret names to attach to ServiceAccounts and Pods</td>
      </tr>
      <tr>
			<td id="restapi--tolerations">restapi.tolerations</td>
			<td>list</td>
			<td>`[]`</td>
			<td>REST API Pod tolerations. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.27/#toleration-v1-core</td>
      </tr>
      <tr>
			<td id="serviceAccount--annotations">serviceAccount.annotations</td>
			<td>object</td>
			<td>`{}`</td>
			<td>Section to configure Operator ServiceAccount annotations</td>
      </tr>
      <tr>
			<td id="serviceAccount--create">serviceAccount.create</td>
			<td>bool</td>
			<td>`true`</td>
			<td>If `true` the Operator Installation ServiceAccount will be created</td>
      </tr>
      <tr>
			<td id="serviceAccount--repoCredentials">serviceAccount.repoCredentials</td>
			<td>list</td>
			<td>`[]`</td>
			<td>Repositories credentials Secret names to attach to ServiceAccounts and Pods</td>
      </tr>
      <tr>
			<td id="specFields">specFields</td>
			<td>list</td>
			<td>`["containerRegistry","imagePullPolicy","imagePullSecrets","allowedNamespaces","allowedNamespaceLabelSelector","disableClusterRole","allowImpersonationForRestApi","disableCrdsAndWebhooksUpdate","sgConfigNamespace","serviceAccount","operator","restapi","adminui","collector","jobs","deploy","cert","rbac","authentication","prometheus","grafana","extensions","shardingSphere","developer"]`</td>
			<td>The list of fields that are serialized into the spec of SGConfig</td>
      </tr>
	</tbody>
</table>
---
title: Operator Parameters
weight: 1
url: /install/helm/parameters
aliases: [ /install/operator/parameters ]
description: Details about cluster parameters that can be used with Helm to set up the operator.
showToc: true
---

Helm values will be mapped with the [`spec` section of SGConfig]({{% relref "06-crd-reference/12-sgconfig#sgconfigspec" %}}). 

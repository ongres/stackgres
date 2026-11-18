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
    image:
      pullPolicy: IfNotPresent
    service:
      exposeHTTP: false
      type: ClusterIP
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
>  The single SGConfig will be created automatically during installation.
>  When using Helm the values of the SGConfig may be overwritten by the values specified for the helm chart.
>  More SGConfig may exist only when allowedNamespaces or allowedNamespaceLabelSelector is used.


{{< crd-properties >}}
[
  {
    "description": "stackgres.io/v1",
    "immutable": true,
    "name": "apiVersion",
    "required": true,
    "type": "string"
  },
  {
    "description": "SGConfig",
    "immutable": true,
    "name": "kind",
    "required": true,
    "type": "string"
  },
  {
    "description": "Refer to the Kubernetes API documentation for the fields of the `metadata` field.",
    "link": "https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.27/#objectmeta-v1-meta",
    "name": "metadata",
    "required": true,
    "type": "object"
  },
  {
    "anchor": "sgconfigspec",
    "description": "Spec defines the desired state of SGConfig",
    "name": "spec",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgconfigstatus",
    "description": "Status defines the observed state of SGConfig",
    "name": "status",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

## SGConfig.spec
<sup><sup>[↩ Parent](#sgconfig)</sup></sup>

Spec defines the desired state of SGConfig

{{< crd-properties >}}
[
  {
    "anchor": "sgconfigspecadminui",
    "description": "Section to configure Web Console container",
    "name": "adminui",
    "required": false,
    "type": "object"
  },
  {
    "description": "When set to `true` the cluster role for impersonation will be created even if `disableClusterRole` is set to `true`.\n\nIt is `false` by default.\n\n\u003e This value can only be set in operator helm chart.",
    "name": "allowImpersonationForRestApi",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "Section to configure namespaces that the operator is allowed to use. If allowedNamespaces is defined it will be used instead. If empty all namespaces will be allowed (default).\n\nSee https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#labelselector-v1-meta\n\n\u003e This value can only be set in operator helm chart.\n\u003e It is set by OLM for scoped operator installation. See https://olm.operatorframework.io/docs/advanced-tasks/operator-scoping-with-operatorgroups/",
    "name": "allowedNamespaceLabelSelector",
    "required": false,
    "type": "map[string]string"
  },
  {
    "description": "Section to configure allowed namespaces that the operator is allowed to use. If empty all namespaces will be allowed (default).\n\n\u003e This value can only be set in operator helm chart or with the environment variable `ALLOWED_NAMESPACES`.\n\u003e It is set by OLM for scoped operator installation. See https://olm.operatorframework.io/docs/advanced-tasks/operator-scoping-with-operatorgroups/",
    "name": "allowedNamespaces",
    "required": false,
    "type": "[]string"
  },
  {
    "anchor": "sgconfigspecauthentication",
    "description": "Section to configure Web Console authentication",
    "name": "authentication",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgconfigspeccert",
    "description": "Section to configure the Operator, REST API and Web Console certificates and JWT RSA key-pair.",
    "name": "cert",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgconfigspeccollector",
    "default": {
      "config": {
        "exporters": {
          "otlp": {
            "endpoint": "stackgres-collector:4317",
            "tls": {
              "ca_file": "/etc/operator/certs/tls.crt"
            }
          },
          "prometheus": {
            "enable_open_metrics": false,
            "endpoint": "0.0.0.0:9464",
            "metric_expiration": "5m",
            "resource_to_telemetry_conversion": {
              "enabled": false
            },
            "send_timestamps": true,
            "tls": {
              "ca_file": "/etc/operator/certs/tls.crt",
              "cert_file": "/etc/operator/certs/tls.crt",
              "key_file": "/etc/operator/certs/tls.key",
              "reload_interval": "10m"
            }
          }
        },
        "processors": {
          "memory_limiter": {
            "check_interval": "1s",
            "limit_percentage": 80,
            "spike_limit_percentage": 15
          }
        },
        "receivers": {
          "otlp": {
            "protocols": {
              "grpc": {
                "endpoint": "0.0.0.0:4317",
                "tls": {
                  "ca_file": "/etc/operator/certs/tls.crt",
                  "cert_file": "/etc/operator/certs/tls.crt",
                  "key_file": "/etc/operator/certs/tls.key"
                }
              }
            }
          }
        },
        "service": {
          "pipelines": {
            "metrics": {
              "exporters": [
                "prometheus"
              ],
              "processors": [
                "memory_limiter"
              ],
              "receivers": [
                "prometheus"
              ]
            }
          }
        }
      },
      "ports": [
        {
          "containerPort": 9464,
          "name": "prom-http",
          "protocol": "TCP"
        }
      ],
      "prometheusOperator": {
        "allowDiscovery": true
      },
      "resources": {
        "limits": {
          "cpu": "1",
          "memory": "4Gi"
        },
        "requests": {
          "cpu": "250m",
          "memory": "1Gi"
        }
      },
      "service": {
        "spec": {
          "ports": [
            {
              "name": "prom-http",
              "port": 9464,
              "protocol": "TCP",
              "targetPort": "prom-http"
            }
          ],
          "type": "ClusterIP"
        }
      }
    },
    "description": "Section to configure OpenTelemetry Collector\n\nBy default a single instance of OpenTelemetry Collector will receive metrics\n from all monitored Pods and will then exports those metrics to\n a configured target (by default will expose a Prometheus exporter).\n\nSee receivers section to scale this architecture to a set of OpenTelemetry Collectors.",
    "name": "collector",
    "required": false,
    "type": "object"
  },
  {
    "default": "quay.io",
    "description": "The container registry host (and port) where the images will be pulled from.\n\n\u003e This value can only be set in operator helm chart or with the environment variable `SG_CONTAINER_REGISTRY`.\n\u003e See also https://github.com/operator-framework/operator-lifecycle-manager/blob/master/doc/design/subscription-config.md",
    "name": "containerRegistry",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgconfigspecdeploy",
    "description": "Section to configure deployment aspects.",
    "name": "deploy",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgconfigspecdeveloper",
    "description": "Section to configure developer options.\n\nFollowing options are for developers only, but can also be useful in some cases ;)",
    "name": "developer",
    "required": false,
    "type": "object"
  },
  {
    "description": "When set to `true` the creation of the operator ClusterRole and ClusterRoleBinding is disabled.\n Also, when `true`, some features that rely on unnamespaced resources permissions will be disabled:\n\n* Creation and upgrade of CustomResourceDefinitions\n* Set CA bundle for Webhooks\n* Check existence of CustomResourceDefinition when listing custom resources\n* Validation of StorageClass\n* REST API endpoint `can-i/{verb}/{resource}` and `can-i` will always return the full list of permissions for any resource and verb since they rely on creation of subjectaccessreviews unnamespaced resource that requires a cluster role.\n* Other REST API endpoints will not work since they rely on impersonation that requires a cluster role.\n  This point in particular breaks the Web Console completely. You may still enable this specific cluster role with `.allowImpersonationForRestApi`.\n  If you do not need the Web Console you may still disable it completely by setting `.deploy.restapi` to `false`.\n\nWhen set to `true` and `allowedNamespaces` is not set or is empty then `allowedNamespaces` will be considered set and containing only the namespace of the operator.\n\nIt is `false` by default.\n\n\u003e This value can only be set in operator helm chart.\n\u003e It is set by OLM for global operator installation. See https://olm.operatorframework.io/docs/advanced-tasks/operator-scoping-with-operatorgroups/",
    "name": "disableClusterRole",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "When set to `true` the cluster role to update or patch CRDs will be disabled.\n\nIt is `false` by default.\n\n\u003e This value can only be set in operator helm chart.\n\u003e For OLM instalations CRDs and Webhooks are created automatically by OLM itself.",
    "name": "disableCrdsAndWebhooksUpdate",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "When set to `true` the conversion Webhooks will be enabled.\n\nIt is `false` by default.\n\n\u003e This value can only be set in operator helm chart.\n\u003e For OLM instalations conversion Webhooks are disabled since they are not supported for scoped operator installation.",
    "name": "enableConversionWebhooks",
    "required": false,
    "type": "boolean"
  },
  {
    "anchor": "sgconfigspecextensions",
    "default": {},
    "description": "Section to configure extensions",
    "name": "extensions",
    "required": false,
    "type": "object"
  },
  {
    "description": "A list of StackGres feature gates to enable globally for all the SGClusters and SGShardedClusters managed by the operator.\n\nSome features require elevated privileges or a relaxed Pod security and, for this reason, can only be enabled by the\n operator administrator at the operator level instead of by the authors of each SGCluster or SGShardedCluster.\n\nAvailable feature gates are:\n* `io-limits`: Allow to set per-volume I/O limits under `.spec.pods.persistentVolume.ioLimits` of SGCluster and SGShardedCluster. Be aware that this requires the Pods to run a privileged `setup-io-limits` init container as root (with only the `CHOWN` capability) and to mount the host cgroup filesystem read-write.",
    "name": "featureGates",
    "required": false,
    "type": "[]string"
  },
  {
    "anchor": "sgconfigspecgrafana",
    "description": "Section to configure Grafana integration",
    "name": "grafana",
    "required": false,
    "type": "object"
  },
  {
    "default": "IfNotPresent",
    "description": "Image pull policy used for images loaded by the Operator\n\n\u003e This value can only be set in operator helm chart or with the environment variable `SG_IMAGE_PULL_POLICY`.\n\u003e See also https://github.com/operator-framework/operator-lifecycle-manager/blob/master/doc/design/subscription-config.md",
    "name": "imagePullPolicy",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgconfigspecimagepullsecretsindex",
    "description": "The list of references to secrets in the same namespace where a ServiceAccount is created by the operator to use for pulling any images in pods that reference such ServiceAccount. ImagePullSecrets are distinct from Secrets because Secrets can be mounted in the pod, but ImagePullSecrets are only accessed by the kubelet. More info: https://kubernetes.io/docs/concepts/containers/images/#specifying-imagepullsecrets-on-a-pod",
    "name": "imagePullSecrets",
    "required": false,
    "type": "[]object"
  },
  {
    "anchor": "sgconfigspecjobs",
    "description": "Section to configure Operator Installation Jobs",
    "name": "jobs",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgconfigspecoperator",
    "description": "Section to configure Operator Pod\n\n\u003e For OLM instalations this section is not used. See https://github.com/operator-framework/operator-lifecycle-manager/blob/master/doc/design/subscription-config.md",
    "name": "operator",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgconfigspecprometheus",
    "description": "**Deprecated** this section has been replaced by `.spec.collector.prometheusOperator`.\n\nSection to configure Prometheus integration.",
    "name": "prometheus",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgconfigspecrbac",
    "description": "Section to configure RBAC for Web Console admin user",
    "name": "rbac",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgconfigspecrestapi",
    "description": "Section to configure REST API Pod",
    "name": "restapi",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgconfigspecserviceaccount",
    "description": "Section to configure Operator Installation ServiceAccount\n\n\u003e For OLM instalations this section is not used.",
    "name": "serviceAccount",
    "required": false,
    "type": "object"
  },
  {
    "description": "When set will indicate the namespace where the SGConfig used by the operator will be created.\n\nBy default the SGConfig will be created in the same namespace as the operator.\n\n\u003e This value can only be set in operator helm chart.\n\u003e This is the namespace where the Subscription is created for OLM installations.",
    "name": "sgConfigNamespace",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgconfigspecshardingsphere",
    "description": "Section to configure integration with ShardingSphere operator",
    "name": "shardingSphere",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

### SGConfig.spec.adminui
<sup><sup>[↩ Parent](#sgconfigspec)</sup></sup>

Section to configure Web Console container

{{< crd-properties >}}
[
  {
    "anchor": "sgconfigspecadminuiimage",
    "description": "Section to configure Web Console image",
    "name": "image",
    "required": false,
    "type": "object"
  },
  {
    "description": "Web Console resources. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#resourcerequirements-v1-core",
    "name": "resources",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgconfigspecadminuiservice",
    "description": "Section to configure Web Console service.",
    "name": "service",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

#### SGConfig.spec.adminui.image
<sup><sup>[↩ Parent](#sgconfigspecadminui)</sup></sup>

Section to configure Web Console image

{{< crd-properties >}}
[
  {
    "default": "stackgres/admin-ui",
    "description": "Web Console image name",
    "name": "name",
    "required": false,
    "type": "string"
  },
  {
    "default": "IfNotPresent",
    "description": "Web Console image pull policy",
    "name": "pullPolicy",
    "required": false,
    "type": "string"
  },
  {
    "description": "Web Console image tag",
    "name": "tag",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

#### SGConfig.spec.adminui.service
<sup><sup>[↩ Parent](#sgconfigspecadminui)</sup></sup>

Section to configure Web Console service.

{{< crd-properties >}}
[
  {
    "default": false,
    "description": "When set to `true` the HTTP port will be exposed in the Web Console Service",
    "name": "exposeHTTP",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "LoadBalancer will get created with the IP specified in\nthis field. This feature depends on whether the underlying cloud-provider supports specifying\nthe loadBalancerIP when a load balancer is created. This field will be ignored if the\ncloud-provider does not support the feature.",
    "name": "loadBalancerIP",
    "required": false,
    "type": "string"
  },
  {
    "description": "If specified and supported by the platform,\nthis will restrict traffic through the cloud-provider load-balancer will be restricted to the\nspecified client IPs. This field will be ignored if the cloud-provider does not support the\nfeature.\nMore info: https://kubernetes.io/docs/tasks/access-application-cluster/configure-cloud-provider-firewall/",
    "name": "loadBalancerSourceRanges",
    "required": false,
    "type": "[]string"
  },
  {
    "description": "The HTTPS port used to expose the Service on Kubernetes nodes",
    "name": "nodePort",
    "required": false,
    "type": "integer"
  },
  {
    "description": "The HTTP port used to expose the Service on Kubernetes nodes",
    "name": "nodePortHTTP",
    "required": false,
    "type": "integer"
  },
  {
    "default": "ClusterIP",
    "description": "The type used for the service of the UI:\n* Set to LoadBalancer to create a load balancer (if supported by the kubernetes cluster)\n  to allow connect from Internet to the UI. Note that enabling this feature will probably incur\n  some fee that depend on the host of the kubernetes cluster (for example this is true for EKS, GKE\n  and AKS).\n* Set to NodePort to expose admin UI from kubernetes nodes.",
    "name": "type",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

### SGConfig.spec.authentication
<sup><sup>[↩ Parent](#sgconfigspec)</sup></sup>

Section to configure Web Console authentication

{{< crd-properties >}}
[
  {
    "default": true,
    "description": "When `true` will create the secret used to store the admin user credentials to access the UI.",
    "name": "createAdminSecret",
    "required": false,
    "type": "boolean"
  },
  {
    "anchor": "sgconfigspecauthenticationoidc",
    "description": "Section to configure Web Console OIDC authentication",
    "name": "oidc",
    "required": false,
    "type": "object"
  },
  {
    "description": "The admin password that will be created for the Web Console.\n\nIf not specified a random password will be generated.",
    "name": "password",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgconfigspecauthenticationsecretref",
    "description": "Allow to specify a reference to a Secret with the admin user credentials for the Web Console.\n\nIn order to assign properly permissions. Make sure the `user` field match the value of the `k8sUsername` key in the referenced Secret.",
    "name": "secretRef",
    "required": false,
    "type": "object"
  },
  {
    "default": "jwt",
    "description": "Specify the authentication mechanism to use. By default is `jwt`, see https://stackgres.io/doc/latest/api/rbac#local-secret-mechanism.\n If set to `oidc` then see https://stackgres.io/doc/latest/api/rbac/#openid-connect-provider-mechanism.",
    "name": "type",
    "required": false,
    "type": "string"
  },
  {
    "default": "admin",
    "description": "The admin username that will be created for the Web Console\n\nOperator bundle installation can not change the default value of this field.",
    "name": "user",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

#### SGConfig.spec.authentication.oidc
<sup><sup>[↩ Parent](#sgconfigspecauthentication)</sup></sup>

Section to configure Web Console OIDC authentication

{{< crd-properties >}}
[
  {
    "description": "",
    "name": "authServerUrl",
    "required": false,
    "type": "string"
  },
  {
    "description": "",
    "name": "clientId",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgconfigspecauthenticationoidcclientidsecretref",
    "description": "",
    "name": "clientIdSecretRef",
    "required": false,
    "type": "object"
  },
  {
    "description": "",
    "name": "credentialsSecret",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgconfigspecauthenticationoidccredentialssecretsecretref",
    "description": "",
    "name": "credentialsSecretSecretRef",
    "required": false,
    "type": "object"
  },
  {
    "description": "Can be one of `required`, `certificate-validation` or `none`",
    "name": "tlsVerification",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGConfig.spec.authentication.oidc.clientIdSecretRef
<sup><sup>[↩ Parent](#sgconfigspecauthenticationoidc)</sup></sup>



{{< crd-properties >}}
[
  {
    "description": "",
    "name": "key",
    "required": false,
    "type": "string"
  },
  {
    "description": "",
    "name": "name",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGConfig.spec.authentication.oidc.credentialsSecretSecretRef
<sup><sup>[↩ Parent](#sgconfigspecauthenticationoidc)</sup></sup>



{{< crd-properties >}}
[
  {
    "description": "",
    "name": "key",
    "required": false,
    "type": "string"
  },
  {
    "description": "",
    "name": "name",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

#### SGConfig.spec.authentication.secretRef
<sup><sup>[↩ Parent](#sgconfigspecauthentication)</sup></sup>

Allow to specify a reference to a Secret with the admin user credentials for the Web Console.

In order to assign properly permissions. Make sure the `user` field match the value of the `k8sUsername` key in the referenced Secret.


{{< crd-properties >}}
[
  {
    "description": "The name of the Secret.",
    "name": "name",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

### SGConfig.spec.cert
<sup><sup>[↩ Parent](#sgconfigspec)</sup></sup>

Section to configure the Operator, REST API and Web Console certificates and JWT RSA key-pair.


{{< crd-properties >}}
[
  {
    "default": true,
    "description": "If set to `true` the CertificateSigningRequest used to generate the certificate used by\n Webhooks will be approved by the Operator Installation Job.",
    "name": "autoapprove",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "The duration in days of the generated certificate for the Operator after which it will expire and be regenerated.\nIf not specified it will be set to 730 (2 years) by default.\n\n\u003e This value can only be set in operator helm chart.\n\u003e For OLM instalations the certificate for the operator is created by the OLM itself.",
    "name": "certDuration",
    "required": false,
    "type": "integer"
  },
  {
    "anchor": "sgconfigspeccertcertmanager",
    "description": "Section to configure cert-manager integration to generate Operator certificates\n\n\u003e This value can only be set in operator helm chart.\n\u003e For OLM instalations the certificate for the operator is created by the OLM itself.",
    "name": "certManager",
    "required": false,
    "type": "object"
  },
  {
    "description": "The duration in days of the generated certificate for the OpenTelemetry Collector after which it will expire and be regenerated.\nIf not specified it will be set to 730 (2 years) by default.",
    "name": "collectorCertDuration",
    "required": false,
    "type": "integer"
  },
  {
    "description": "The Secret name with the OpenTelemetry Collector certificate\n of type kubernetes.io/tls. See https://kubernetes.io/docs/concepts/configuration/secret/#tls-secrets",
    "name": "collectorSecretName",
    "required": false,
    "type": "string"
  },
  {
    "default": true,
    "description": "When set to `true` the OpenTelemetry Collector certificate will be created.",
    "name": "createForCollector",
    "required": false,
    "type": "boolean"
  },
  {
    "default": true,
    "description": "When set to `true` the Operator certificate will be created.\n\n\u003e This value can only be set in operator helm chart.\n\u003e For OLM instalations the certificate for the operator is created by the OLM itself.",
    "name": "createForOperator",
    "required": false,
    "type": "boolean"
  },
  {
    "default": true,
    "description": "When set to `true` the Web Console / REST API certificate will be created.",
    "name": "createForWebApi",
    "required": false,
    "type": "boolean"
  },
  {
    "default": true,
    "description": "When set to `true` the Operator certificates will be regenerated if `createForOperator` is set to `true`, and the certificate is expired or invalid.\n\n\u003e This value can only be set in operator helm chart.\n\u003e For OLM instalations the certificate for the operator is created by the OLM itself.",
    "name": "regenerateCert",
    "required": false,
    "type": "boolean"
  },
  {
    "default": true,
    "description": "When set to `true` the OpenTelemetry Collector certificates will be regenerated if `createForCollector` is set to `true`, and the certificate is expired or invalid.",
    "name": "regenerateCollectorCert",
    "required": false,
    "type": "boolean"
  },
  {
    "default": true,
    "description": "When set to `true` the Web Console / REST API certificates will be regenerated if `createForWebApi` is set to `true`, and the certificate is expired or invalid.",
    "name": "regenerateWebCert",
    "required": false,
    "type": "boolean"
  },
  {
    "default": true,
    "description": "When set to `true` the Web Console / REST API RSA key pair will be regenerated if `createForWebApi` is set to `true`, and the certificate is expired or invalid.",
    "name": "regenerateWebRsa",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "The Secret name with the Operator Webhooks certificate issued by the Kubernetes cluster CA\n of type kubernetes.io/tls. See https://kubernetes.io/docs/concepts/configuration/secret/#tls-secrets\n\n\u003e This value can only be set in operator helm chart.\n\u003e For OLM instalations the certificate for the operator is created by the OLM itself.",
    "name": "secretName",
    "required": false,
    "type": "string"
  },
  {
    "description": "The duration in days of the generated certificate for the Web Console / REST API after which it will expire and be regenerated.\nIf not specified it will be set to 730 (2 years) by default.",
    "name": "webCertDuration",
    "required": false,
    "type": "integer"
  },
  {
    "description": "The duration in days of the generated RSA key pair for the Web Console / REST API after which it will expire and be regenerated.\nIf not specified it will be set to 730 (2 years) by default.",
    "name": "webRsaDuration",
    "required": false,
    "type": "integer"
  },
  {
    "description": "The Secret name with the Web Console / REST API certificate\n of type kubernetes.io/tls. See https://kubernetes.io/docs/concepts/configuration/secret/#tls-secrets",
    "name": "webSecretName",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

#### SGConfig.spec.cert.certManager
<sup><sup>[↩ Parent](#sgconfigspeccert)</sup></sup>

Section to configure cert-manager integration to generate Operator certificates

> This value can only be set in operator helm chart.
> For OLM instalations the certificate for the operator is created by the OLM itself.


{{< crd-properties >}}
[
  {
    "default": false,
    "description": "When set to `true` then Issuer and Certificate for Operator, Web Console / REST API and OpenTelemetry Collector\n Pods will be generated",
    "name": "autoConfigure",
    "required": false,
    "type": "boolean"
  },
  {
    "default": "2160h",
    "description": "The requested duration (i.e. lifetime) of the Certificates. See https://cert-manager.io/docs/reference/api-docs/#cert-manager.io%2fv1",
    "name": "duration",
    "required": false,
    "type": "string"
  },
  {
    "default": "PKCS1",
    "description": "The private key cryptography standards (PKCS) encoding for this certificate’s private key to be encoded in. See https://cert-manager.io/docs/reference/api-docs/#cert-manager.io/v1.CertificatePrivateKey",
    "name": "encoding",
    "required": false,
    "type": "string"
  },
  {
    "default": "360h",
    "description": "How long before the currently issued certificate’s expiry cert-manager should renew the certificate. See https://cert-manager.io/docs/reference/api-docs/#cert-manager.io%2fv1",
    "name": "renewBefore",
    "required": false,
    "type": "string"
  },
  {
    "default": 2048,
    "description": "Size is the key bit size of the corresponding private key for this certificate. See https://cert-manager.io/docs/reference/api-docs/#cert-manager.io/v1.CertificatePrivateKey",
    "name": "size",
    "required": false,
    "type": "integer"
  }
]
{{< /crd-properties >}}

### SGConfig.spec.collector
<sup><sup>[↩ Parent](#sgconfigspec)</sup></sup>

Section to configure OpenTelemetry Collector

By default a single instance of OpenTelemetry Collector will receive metrics
 from all monitored Pods and will then exports those metrics to
 a configured target (by default will expose a Prometheus exporter).

See receivers section to scale this architecture to a set of OpenTelemetry Collectors.


{{< crd-properties >}}
[
  {
    "description": "OpenTelemetry Collector Pod affinity. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#affinity-v1-core",
    "name": "affinity",
    "required": false,
    "type": "object"
  },
  {
    "default": false,
    "description": "When `true` the collcetor will always be running even when no SGCluster is binded to it.",
    "name": "alwaysEnabled",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "OpenTelemetry Collector Pod annotations",
    "name": "annotations",
    "required": false,
    "type": "object"
  },
  {
    "description": "Section to configure OpenTelemetry Collector Configuration. See https://opentelemetry.io/docs/collector/configuration",
    "name": "config",
    "required": false,
    "type": "object"
  },
  {
    "default": "stackgres-collector",
    "description": "OpenTelemetry Collector Deployment/DaemonSet base name",
    "name": "name",
    "required": false,
    "type": "string"
  },
  {
    "description": "OpenTelemetry Collector Pod node selector",
    "name": "nodeSelector",
    "required": false,
    "type": "object"
  },
  {
    "description": "Section to configure OpenTelemetry Collector ports. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#containerport-v1-core",
    "name": "ports",
    "required": false,
    "type": "[]object"
  },
  {
    "description": "OpenTelemetry Collector Pod preemption policy. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#container-v1-core",
    "name": "preemptionPolicy",
    "required": false,
    "type": "string"
  },
  {
    "description": "OpenTelemetry Collector Pod priority class name. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#container-v1-core",
    "name": "priorityClassName",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgconfigspeccollectorprometheusoperator",
    "description": "Section to configure OpenTelemetry Collector integration with Prometheus Operator.",
    "name": "prometheusOperator",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgconfigspeccollectorreceivers",
    "description": "This section allow to configure a variable number of OpenTelemetry Collector\n receivers (by default equals to the number of Pod with metrics enabled)\n that will scrape the metrics separately and send them to a defined number\n of OpenTelemetry Collector exporters (by default 1) that exports those metrics\n to one or more configured targets (by default will expose a Prometheus exporter).",
    "name": "receivers",
    "required": false,
    "type": "object"
  },
  {
    "description": "OpenTelemetry Collector Pod resources. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#resourcerequirements-v1-core",
    "name": "resources",
    "required": false,
    "type": "object"
  },
  {
    "description": "OpenTelemetry Collector Pod runtime class name. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#container-v1-core",
    "name": "runtimeClassName",
    "required": false,
    "type": "string"
  },
  {
    "description": "OpenTelemetry Collector Pod scheduler name. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#container-v1-core",
    "name": "schedulerName",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgconfigspeccollectorservice",
    "description": "Section to configure OpenTelemetry Collector Service",
    "name": "service",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgconfigspeccollectorserviceaccount",
    "description": "Section to configure OpenTelemetry Collector ServiceAccount",
    "name": "serviceAccount",
    "required": false,
    "type": "object"
  },
  {
    "description": "OpenTelemetry Collector Pod tolerations. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#toleration-v1-core",
    "name": "tolerations",
    "required": false,
    "type": "[]object"
  },
  {
    "description": "Section to configure OpenTelemetry Collector Volume Mounts. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#volumemount-v1-core",
    "name": "volumeMounts",
    "required": false,
    "type": "[]object"
  },
  {
    "description": "Section to configure OpenTelemetry Collector Volumes. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#volume-v1-core",
    "name": "volumes",
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

#### SGConfig.spec.collector.prometheusOperator
<sup><sup>[↩ Parent](#sgconfigspeccollector)</sup></sup>

Section to configure OpenTelemetry Collector integration with Prometheus Operator.

{{< crd-properties >}}
[
  {
    "default": true,
    "description": "If set to false or monitors is set automatic bind to Prometheus\n created using the [Prometheus Operator](https://github.com/prometheus-operator/prometheus-operator) will be disabled.\n\nIf disabled the cluster will not be bound to Prometheus automatically and will require manual configuration.\n\nWill be ignored if monitors is set.",
    "name": "allowDiscovery",
    "required": false,
    "type": "boolean"
  },
  {
    "anchor": "sgconfigspeccollectorprometheusoperatormonitorsindex",
    "description": "Optional section to configure PodMonitors for specific Prometheus instances\n\n*WARNING*: resources created by this integration that does set\n the metadata namespace to the same as the operator will not\n be removed when removing the helm chart. Changing the namespace\n may require configure the Prometheus CR properly in order to\n discover PodMonitor in such namespace.",
    "name": "monitors",
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGConfig.spec.collector.prometheusOperator.monitors[index]
<sup><sup>[↩ Parent](#sgconfigspeccollectorprometheusoperator)</sup></sup>

Section to configure a PodMonitor for a specific Prometheus instance that will scrape from the collector Pod pointing by default to the prometheus exporter

{{< crd-properties >}}
[
  {
    "anchor": "sgconfigspeccollectorprometheusoperatormonitorsindexmetadata",
    "description": "Section to overwrite some PodMonitor metadata",
    "name": "metadata",
    "required": false,
    "type": "object"
  },
  {
    "description": "The name of the Prometheus resource that will scrape from the collector Pod pointing by default to the prometheus exporter",
    "name": "name",
    "required": false,
    "type": "string"
  },
  {
    "description": "The namespace of the Prometheus resource that will scrape from the collector Pod pointing by default to the prometheus exporter",
    "name": "namespace",
    "required": false,
    "type": "string"
  },
  {
    "description": "The PodMonitor spec that will be overwritten by the operator. See https://github.com/prometheus-operator/prometheus-operator/blob/main/Documentation/api.md#monitoring.coreos.com/v1.PodMonitorSpec",
    "name": "spec",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGConfig.spec.collector.prometheusOperator.monitors[index].metadata
<sup><sup>[↩ Parent](#sgconfigspeccollectorprometheusoperatormonitorsindex)</sup></sup>

Section to overwrite some PodMonitor metadata

{{< crd-properties >}}
[
  {
    "description": "The annotations to set for the PodMonitor",
    "name": "annotations",
    "required": false,
    "type": "object"
  },
  {
    "description": "The labels to set for the PodMonitor",
    "name": "labels",
    "required": false,
    "type": "object"
  },
  {
    "description": "The name of the PodMonitor",
    "name": "name",
    "required": false,
    "type": "string"
  },
  {
    "description": "The namespace of the PodMonitor. Changing the namespace may require configure the Prometheus CR properly in order to discover PodMonitor in such namespace.",
    "name": "namespace",
    "required": false,
    "type": "string"
  },
  {
    "description": "The ownerReferences to set for the PodMonitor in order to be garbage collected by the specified object.",
    "name": "ownerReferences",
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

#### SGConfig.spec.collector.receivers
<sup><sup>[↩ Parent](#sgconfigspeccollector)</sup></sup>

This section allow to configure a variable number of OpenTelemetry Collector
 receivers (by default equals to the number of Pod with metrics enabled)
 that will scrape the metrics separately and send them to a defined number
 of OpenTelemetry Collector exporters (by default 1) that exports those metrics
 to one or more configured targets (by default will expose a Prometheus exporter).


{{< crd-properties >}}
[
  {
    "anchor": "sgconfigspeccollectorreceiversdeploymentsindex",
    "description": "A set of separate Deployments of 1 instance each that allow to set the OpenTelemetry Collectors receivers to a specified number of instances.\n\nWhen not set the number of Deployment of OpenTelemetry Collectors receivers will match the number of instances of all the existing SGClusters\n that has the field `.spec.configurations.observability.enableMetrics` set to `true`. Also, when not set, each Deployment will include a pod\n affinity rule matching any of the SGClusters Pods set defined below. This will allow to create an OpenTelemetry Collector receiver instance\n dedicated to each SGCluster Pod running in the same Node.\n\nEach Deployment will use a configuration for the OpenTelemetry Collector that will scrape from a set of SGClusters Pods that has the field\n `.spec.configurations.observability.enableMetrics` set to `true`. The set of Pods of each of those OpenTelemetry Collector configuration\n will be a partition of the list of SGClusters Pods that has the field `.spec.configurations.observability.enableMetrics` set to `true`\n ordered by the field `Pod.metadata.creationTimestamp` (from the oldest to the newest) and ordered crescently alphabetically by the fields\n `Pod.metadata.namespace` and `Pod.metadata.name`.\n\nIf is possible to override (even partially) the list of SGCluster Pods using the `sgClusters` section.",
    "name": "deployments",
    "required": false,
    "type": "[]object"
  },
  {
    "default": false,
    "description": "When set to `true` it enables the creation of a set of OpenTelemetry Collectors receivers\n that will be scraping from the SGCluster Pods and allow to scale the observability\n architecture and a set of OpenTelemetry Collectors exporters that exports those metrics\n to one or more configured targets.",
    "name": "enabled",
    "required": false,
    "type": "boolean"
  },
  {
    "default": 1,
    "description": "When receivers are enabled indicates the number of OpenTelemetry Collectors exporters that\n exports metrics to one or more configured targets.",
    "name": "exporters",
    "required": false,
    "type": "integer"
  }
]
{{< /crd-properties >}}

##### SGConfig.spec.collector.receivers.deployments[index]
<sup><sup>[↩ Parent](#sgconfigspeccollectorreceivers)</sup></sup>



{{< crd-properties >}}
[
  {
    "description": "OpenTelemetry Collector Pod affinity. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#affinity-v1-core",
    "name": "affinity",
    "required": false,
    "type": "object"
  },
  {
    "description": "OpenTelemetry Collector Pod annotations",
    "name": "annotations",
    "required": false,
    "type": "object"
  },
  {
    "description": "OpenTelemetry Collector Pod node selector",
    "name": "nodeSelector",
    "required": false,
    "type": "object"
  },
  {
    "description": "OpenTelemetry Collector Pod preemption policy. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#container-v1-core",
    "name": "preemptionPolicy",
    "required": false,
    "type": "string"
  },
  {
    "description": "OpenTelemetry Collector Pod priority class name. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#container-v1-core",
    "name": "priorityClassName",
    "required": false,
    "type": "string"
  },
  {
    "description": "OpenTelemetry Collector Pod resources. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#resourcerequirements-v1-core",
    "name": "resources",
    "required": false,
    "type": "object"
  },
  {
    "description": "OpenTelemetry Collector Pod runtime class name. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#container-v1-core",
    "name": "runtimeClassName",
    "required": false,
    "type": "string"
  },
  {
    "description": "OpenTelemetry Collector Pod scheduler name. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#container-v1-core",
    "name": "schedulerName",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgconfigspeccollectorreceiversdeploymentsindexsgclustersindex",
    "description": "List of SGCluster Pods to scrape from this Deployment's Pod that will be included to the OpenTelemetry Collector\n configuration alongside the SGCluster Pods assigned as described in `SGConfig.spec.collector.receivers.deployments`.",
    "name": "sgClusters",
    "required": false,
    "type": "[]object"
  },
  {
    "description": "OpenTelemetry Collector Pod tolerations. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#toleration-v1-core",
    "name": "tolerations",
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGConfig.spec.collector.receivers.deployments[index].sgClusters[index]
<sup><sup>[↩ Parent](#sgconfigspeccollectorreceiversdeploymentsindex)</sup></sup>



{{< crd-properties >}}
[
  {
    "description": "The indexes of the SGCluster's Pods that will be included to the OpenTelemetry Collector configuration alongside\n the SGCluster Pods assigned as described in `SGConfig.spec.collector.receivers.deployments`.\n\nIf not specified all the SGCluster's Pods will be included.",
    "name": "indexes",
    "required": false,
    "type": "[]integer"
  },
  {
    "description": "The name of the SGCluster",
    "name": "name",
    "required": false,
    "type": "string"
  },
  {
    "description": "The namespace of the SGCluster",
    "name": "namespace",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

#### SGConfig.spec.collector.service
<sup><sup>[↩ Parent](#sgconfigspeccollector)</sup></sup>

Section to configure OpenTelemetry Collector Service

{{< crd-properties >}}
[
  {
    "description": "OpenTelemetry Collector Service annotations",
    "name": "annotations",
    "required": false,
    "type": "object"
  },
  {
    "description": "Section to configure OpenTelemetry Collector Service specs. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#servicespec-v1-core",
    "name": "spec",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

#### SGConfig.spec.collector.serviceAccount
<sup><sup>[↩ Parent](#sgconfigspeccollector)</sup></sup>

Section to configure OpenTelemetry Collector ServiceAccount

{{< crd-properties >}}
[
  {
    "description": "OpenTelemetry Collector ServiceAccount annotations",
    "name": "annotations",
    "required": false,
    "type": "object"
  },
  {
    "description": "Repositories credentials Secret names",
    "name": "repoCredentials",
    "required": false,
    "type": "[]string"
  }
]
{{< /crd-properties >}}

### SGConfig.spec.deploy
<sup><sup>[↩ Parent](#sgconfigspec)</sup></sup>

Section to configure deployment aspects.

{{< crd-properties >}}
[
  {
    "default": true,
    "description": "When set to `true` the OpenTelemetry Collector will be deployed.",
    "name": "collector",
    "required": false,
    "type": "boolean"
  },
  {
    "default": true,
    "description": "When set to `true` the Operator will be deployed.",
    "name": "operator",
    "required": false,
    "type": "boolean"
  },
  {
    "default": true,
    "description": "When set to `true` the Web Console / REST API will be deployed.",
    "name": "restapi",
    "required": false,
    "type": "boolean"
  }
]
{{< /crd-properties >}}

### SGConfig.spec.developer
<sup><sup>[↩ Parent](#sgconfigspec)</sup></sup>

Section to configure developer options.

Following options are for developers only, but can also be useful in some cases ;)


{{< crd-properties >}}
[
  {
    "default": false,
    "description": "If set to `true` and `extensions.cache.enabled` is also `true`\n it will try to download extensions from images (experimental)",
    "name": "allowPullExtensionsFromImageRepository",
    "required": false,
    "type": "boolean"
  },
  {
    "default": false,
    "description": "It set to `true` disable arbitrary user that is set for OpenShift clusters",
    "name": "disableArbitraryUser",
    "required": false,
    "type": "boolean"
  },
  {
    "default": false,
    "description": "Only work with JVM version and allow connect\n on port 8000 of operator Pod with jdb or similar",
    "name": "enableJvmDebug",
    "required": false,
    "type": "boolean"
  },
  {
    "default": false,
    "description": "Only work with JVM version and if `enableJvmDebug` is `true`\n suspend the JVM until a debugger session is started",
    "name": "enableJvmDebugSuspend",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "Set the external Operator IP",
    "name": "externalOperatorIp",
    "required": false,
    "type": "string"
  },
  {
    "description": "Set the external Operator port",
    "name": "externalOperatorPort",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Set the external REST API IP",
    "name": "externalRestApiIp",
    "required": false,
    "type": "string"
  },
  {
    "description": "Set the external REST API port",
    "name": "externalRestApiPort",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Set `quarkus.log.level`. See https://quarkus.io/guides/logging#root-logger-configuration",
    "name": "logLevel",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgconfigspecdeveloperpatches",
    "description": "Section to define patches for some StackGres Pods",
    "name": "patches",
    "required": false,
    "type": "object"
  },
  {
    "default": false,
    "description": "If set to `true` add extra debug to any script controlled by the reconciliation cycle of the operator configuration",
    "name": "showDebug",
    "required": false,
    "type": "boolean"
  },
  {
    "default": false,
    "description": "Set `quarkus.log.console.format` to `%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c{4.}] (%t) %s%e%n`. See https://quarkus.io/guides/logging#logging-format",
    "name": "showStackTraces",
    "required": false,
    "type": "boolean"
  },
  {
    "default": false,
    "description": "The operator will use JVM version of the images",
    "name": "useJvmImages",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "Set the operator version (used for testing)",
    "name": "version",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

#### SGConfig.spec.developer.patches
<sup><sup>[↩ Parent](#sgconfigspecdeveloper)</sup></sup>

Section to define patches for some StackGres Pods


{{< crd-properties >}}
[
  {
    "anchor": "sgconfigspecdeveloperpatchesadminui",
    "description": "Section to define volumes to be used by the adminui container",
    "name": "adminui",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgconfigspecdeveloperpatchesclustercontroller",
    "description": "Section to define volumes to be used by the cluster controller container",
    "name": "clusterController",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgconfigspecdeveloperpatchesoperator",
    "description": "Section to define volumes to be used by the operator container",
    "name": "operator",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgconfigspecdeveloperpatchesrestapi",
    "description": "Section to define volumes to be used by the restapi container",
    "name": "restapi",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgconfigspecdeveloperpatchesstream",
    "description": "Section to define volumes to be used by the stream container",
    "name": "stream",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGConfig.spec.developer.patches.adminui
<sup><sup>[↩ Parent](#sgconfigspecdeveloperpatches)</sup></sup>

Section to define volumes to be used by the adminui container


{{< crd-properties >}}
[
  {
    "description": "Pod's container volume mounts. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#volumemount-v1-core",
    "name": "volumeMounts",
    "required": false,
    "type": "[]object"
  },
  {
    "description": "Pod volumes. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#volume-v1-core",
    "name": "volumes",
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGConfig.spec.developer.patches.clusterController
<sup><sup>[↩ Parent](#sgconfigspecdeveloperpatches)</sup></sup>

Section to define volumes to be used by the cluster controller container


{{< crd-properties >}}
[
  {
    "description": "Pod's container volume mounts. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#volumemount-v1-core",
    "name": "volumeMounts",
    "required": false,
    "type": "[]object"
  },
  {
    "description": "Pod volumes. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#volume-v1-core",
    "name": "volumes",
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGConfig.spec.developer.patches.operator
<sup><sup>[↩ Parent](#sgconfigspecdeveloperpatches)</sup></sup>

Section to define volumes to be used by the operator container


{{< crd-properties >}}
[
  {
    "description": "Pod's container volume mounts. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#volumemount-v1-core",
    "name": "volumeMounts",
    "required": false,
    "type": "[]object"
  },
  {
    "description": "Pod volumes. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#volume-v1-core",
    "name": "volumes",
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGConfig.spec.developer.patches.restapi
<sup><sup>[↩ Parent](#sgconfigspecdeveloperpatches)</sup></sup>

Section to define volumes to be used by the restapi container


{{< crd-properties >}}
[
  {
    "description": "Pod's container volume mounts. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#volumemount-v1-core",
    "name": "volumeMounts",
    "required": false,
    "type": "[]object"
  },
  {
    "description": "Pod volumes. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#volume-v1-core",
    "name": "volumes",
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

##### SGConfig.spec.developer.patches.stream
<sup><sup>[↩ Parent](#sgconfigspecdeveloperpatches)</sup></sup>

Section to define volumes to be used by the stream container


{{< crd-properties >}}
[
  {
    "description": "Pod's container volume mounts. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#volumemount-v1-core",
    "name": "volumeMounts",
    "required": false,
    "type": "[]object"
  },
  {
    "description": "Pod volumes. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#volume-v1-core",
    "name": "volumes",
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

### SGConfig.spec.extensions
<sup><sup>[↩ Parent](#sgconfigspec)</sup></sup>

Section to configure extensions

{{< crd-properties >}}
[
  {
    "anchor": "sgconfigspecextensionscache",
    "description": "Section to configure extensions cache (experimental).\n\nThis feature is in beta and may cause failures, please use with caution and report any\n error to https://gitlab.com/ongresinc/stackgres/-/issues/new\n\nThis feature can only be set in the StackGres Helm chart",
    "name": "cache",
    "required": false,
    "type": "object"
  },
  {
    "default": true,
    "description": "When set to `false` the extensions metadata is retrieved only once and never\nrefreshed afterwards.",
    "name": "refreshEnabled",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "The interval, as an ISO-8601 duration (e.g. `P7D`, `PT1H`), between refreshes of\nthe cached extensions metadata. When not set the default of 1 week is used. Values\nlower than the minimum of 1 hour are clamped up to 1 hour.",
    "name": "refreshInterval",
    "required": false,
    "type": "string"
  },
  {
    "default": [
      "https://extensions.stackgres.io/postgres/repository"
    ],
    "description": "A list of extensions repository URLs used to retrieve extensions\n\n\u003e This value is overridden by the environment variable `EXTENSIONS_REPOSITORY_URLS`\n\u003e   when it is set (for example through the OLM bundle Subscription).\n\nTo set a proxy for extensions repository add parameter proxyUrl to the URL:\n    `https://extensions.stackgres.io/postgres/repository?proxyUrl=\u003cproxy scheme\u003e%3A%2F%2F\u003cproxy host\u003e[%3A\u003cproxy port\u003e]` (URL encoded)\n\nOther URL parameters are:\n\n* `skipHostnameVerification`: set it to `true` in order to use a server or a proxy with a self signed certificate\n* `retry`: set it to `\u003cmax retries\u003e[:\u003csleep before next retry\u003e]` in order to retry a request on failure\n* `setHttpScheme`: set it to `true` in order to force using HTTP scheme",
    "name": "repositoryUrls",
    "required": false,
    "type": "[]string"
  }
]
{{< /crd-properties >}}

#### SGConfig.spec.extensions.cache
<sup><sup>[↩ Parent](#sgconfigspecextensions)</sup></sup>

Section to configure extensions cache (experimental).

This feature is in beta and may cause failures, please use with caution and report any
 error to https://gitlab.com/ongresinc/stackgres/-/issues/new

This feature can only be set in the StackGres Helm chart


{{< crd-properties >}}
[
  {
    "default": false,
    "description": "When set to `true` enable the extensions cache.\n\nThis feature is in beta and may cause failures, please use with caution and report any\n error to https://gitlab.com/ongresinc/stackgres/-/issues/new",
    "name": "enabled",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "If set, will use a host path volume with the specified path for the extensions cache\n instead of a PersistentVolume",
    "name": "hostPath",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgconfigspecextensionscachepersistentvolume",
    "description": "Section to configure the extensions cache PersistentVolume",
    "name": "persistentVolume",
    "required": false,
    "type": "object"
  },
  {
    "default": [
      "x86_64/linux/timescaledb-1\\.7\\.4-pg12"
    ],
    "description": "An array of extensions pattern used to pre-loaded extensions into the extensions cache",
    "name": "preloadedExtensions",
    "required": false,
    "type": "[]string"
  },
  {
    "default": {
      "requests": {
        "cpu": "50m",
        "memory": "64Mi"
      }
    },
    "description": "The resources of the extensions cache containers. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#resourcerequirements-v1-core",
    "name": "resources",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGConfig.spec.extensions.cache.persistentVolume
<sup><sup>[↩ Parent](#sgconfigspecextensionscache)</sup></sup>

Section to configure the extensions cache PersistentVolume

{{< crd-properties >}}
[
  {
    "default": "1Gi",
    "description": "The PersistentVolume size for the extensions cache\n\nOnly use whole numbers (e.g. not 1e6) and K/Ki/M/Mi/G/Gi as units",
    "name": "size",
    "required": false,
    "type": "string"
  },
  {
    "description": "If defined set storage class\nIf set to \"-\" (equivalent to storageClass: \"\" in a PV spec) disables\n dynamic provisioning\nIf undefined (the default) or set to null, no storageClass spec is\n set, choosing the default provisioner.  (gp2 on AWS, standard on\n GKE, AWS \u0026 OpenStack)",
    "name": "storageClass",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

### SGConfig.spec.grafana
<sup><sup>[↩ Parent](#sgconfigspec)</sup></sup>

Section to configure Grafana integration

{{< crd-properties >}}
[
  {
    "default": false,
    "description": "When set to `true` embed automatically Grafana into the Web Console by creating the\n StackGres dashboard and the read-only role used to read it from the Web Console",
    "name": "autoEmbed",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "The ConfigMap name with the dashboard JSON in the key `grafana-dashboard.json`\n that will be created in Grafana. If not set the default",
    "name": "dashboardConfigMap",
    "required": false,
    "type": "string"
  },
  {
    "description": "The dashboard id that will be create in Grafana\n (see https://grafana.com/grafana/dashboards). By default 9628. (used to embed automatically\n Grafana)\n\nManual Steps:\n \nCreate grafana dashboard for postgres exporter and copy/paste share URL:\n- Grafana \u003e Create \u003e Import \u003e Grafana.com Dashboard 9628\nCopy/paste grafana dashboard URL for postgres exporter:\n- Grafana \u003e Dashboard \u003e Manage \u003e Select postgres exporter dashboard \u003e Copy URL",
    "name": "dashboardId",
    "required": false,
    "type": "string"
  },
  {
    "default": "Prometheus",
    "description": "The datasource name used to create the StackGres Dashboard into Grafana",
    "name": "datasourceName",
    "required": false,
    "type": "string"
  },
  {
    "default": "prom-operator",
    "description": "The password to access Grafana. By default prom-operator (the default in for\n kube-prometheus-stack helm chart). (used to embed automatically Grafana)",
    "name": "password",
    "required": false,
    "type": "string"
  },
  {
    "default": "http",
    "description": "The schema to access Grafana. By default http. (used to embed manually and\n automatically grafana)",
    "name": "schema",
    "required": false,
    "type": "string"
  },
  {
    "description": "The name of secret with credentials to access Grafana. (used to embed\n automatically Grafana, alternative to use `user` and `password`)",
    "name": "secretName",
    "required": false,
    "type": "string"
  },
  {
    "description": "The namespace of secret with credentials to access Grafana. (used to\n embed automatically Grafana, alternative to use `user` and `password`)",
    "name": "secretNamespace",
    "required": false,
    "type": "string"
  },
  {
    "description": "The key of secret with password used to access Grafana. (used to\n embed automatically Grafana, alternative to use `user` and `password`)",
    "name": "secretPasswordKey",
    "required": false,
    "type": "string"
  },
  {
    "description": "The key of secret with username used to access Grafana. (used to embed\n automatically Grafana, alternative to use `user` and `password`)",
    "name": "secretUserKey",
    "required": false,
    "type": "string"
  },
  {
    "description": "The Grafana API token to access the PostgreSQL dashboard created\n in Grafana (used to embed manually Grafana)\n\nManual Steps:\n \nCreate and copy/paste grafana API token:\n- Grafana \u003e Configuration \u003e API Keys \u003e Add API key (for viewer) \u003e Copy key value",
    "name": "token",
    "required": false,
    "type": "string"
  },
  {
    "description": "The URL of the PostgreSQL dashboard created in Grafana (used to embed manually\n Grafana)",
    "name": "url",
    "required": false,
    "type": "string"
  },
  {
    "default": "admin",
    "description": "The username to access Grafana. By default admin. (used to embed automatically\n Grafana)",
    "name": "user",
    "required": false,
    "type": "string"
  },
  {
    "description": "The service host name to access grafana (used to embed manually and\n automatically Grafana). \nThe parameter value should point to the grafana service following the \n [DNS reference](https://kubernetes.io/docs/concepts/services-networking/dns-pod-service/) `svc_name.namespace`",
    "name": "webHost",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

### SGConfig.spec.imagePullSecrets[index]
<sup><sup>[↩ Parent](#sgconfigspec)</sup></sup>

A reference to a secrets in the same namespace where a ServiceAccount is created by the operator to use for pulling any images in pods that reference such ServiceAccount.

> This value can only be set in operator helm chart.
> You can set it in OLM in the CatalogSource. See https://olm.operatorframework.io/docs/tasks/make-catalog-available-on-cluster/#using-registry-images-that-require-authentication-as-catalogbundleoperatoroperand-images


{{< crd-properties >}}
[
  {
    "description": "The name of the referenced Secret.",
    "name": "name",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

### SGConfig.spec.jobs
<sup><sup>[↩ Parent](#sgconfigspec)</sup></sup>

Section to configure Operator Installation Jobs

{{< crd-properties >}}
[
  {
    "description": "Operator Installation Jobs affinity. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#affinity-v1-core",
    "name": "affinity",
    "required": false,
    "type": "object"
  },
  {
    "description": "Operator Installation Jobs annotations",
    "name": "annotations",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgconfigspecjobsimage",
    "description": "Section to configure Operator Installation Jobs image",
    "name": "image",
    "required": false,
    "type": "object"
  },
  {
    "description": "Operator Installation Jobs node selector",
    "name": "nodeSelector",
    "required": false,
    "type": "object"
  },
  {
    "description": "Operator Installation Job's Pod preemption policy. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#container-v1-core",
    "name": "preemptionPolicy",
    "required": false,
    "type": "string"
  },
  {
    "description": "Operator Installation Job's Pod priority class name. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#container-v1-core",
    "name": "priorityClassName",
    "required": false,
    "type": "string"
  },
  {
    "description": "Operator Installation Jobs resources. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#resourcerequirements-v1-core",
    "name": "resources",
    "required": false,
    "type": "object"
  },
  {
    "description": "Operator Installation Job's Pod runtime class name. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#container-v1-core",
    "name": "runtimeClassName",
    "required": false,
    "type": "string"
  },
  {
    "description": "Operator Installation Job's Pod scheduler name. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#container-v1-core",
    "name": "schedulerName",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgconfigspecjobsserviceaccount",
    "description": "Section to configure Jobs ServiceAccount",
    "name": "serviceAccount",
    "required": false,
    "type": "object"
  },
  {
    "description": "Operator Installation Jobs tolerations. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#toleration-v1-core",
    "name": "tolerations",
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

#### SGConfig.spec.jobs.image
<sup><sup>[↩ Parent](#sgconfigspecjobs)</sup></sup>

Section to configure Operator Installation Jobs image

{{< crd-properties >}}
[
  {
    "default": "IfNotPresent",
    "description": "Operator Installation Jobs image pull policy",
    "name": "pullPolicy",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

#### SGConfig.spec.jobs.serviceAccount
<sup><sup>[↩ Parent](#sgconfigspecjobs)</sup></sup>

Section to configure Jobs ServiceAccount

{{< crd-properties >}}
[
  {
    "description": "Jobs ServiceAccount annotations",
    "name": "annotations",
    "required": false,
    "type": "object"
  },
  {
    "description": "Repositories credentials Secret names",
    "name": "repoCredentials",
    "required": false,
    "type": "[]string"
  }
]
{{< /crd-properties >}}

### SGConfig.spec.operator
<sup><sup>[↩ Parent](#sgconfigspec)</sup></sup>

Section to configure Operator Pod

> For OLM instalations this section is not used. See https://github.com/operator-framework/operator-lifecycle-manager/blob/master/doc/design/subscription-config.md


{{< crd-properties >}}
[
  {
    "description": "Operator Pod affinity. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#affinity-v1-core\n\n\u003e This value can only be set in operator helm chart.",
    "name": "affinity",
    "required": false,
    "type": "object"
  },
  {
    "description": "Operator Pod annotations",
    "name": "annotations",
    "required": false,
    "type": "object"
  },
  {
    "default": false,
    "description": "Host networking requested for this pod. Use the host's network namespace. If this option is set, the ports that will be used must be specified. Default to false.\n\n\u003e This value can only be set in operator helm chart.",
    "name": "hostNetwork",
    "required": false,
    "type": "boolean"
  },
  {
    "anchor": "sgconfigspecoperatorimage",
    "description": "Section to configure Operator image",
    "name": "image",
    "required": false,
    "type": "object"
  },
  {
    "default": 8080,
    "description": "The port that the operator will use to listen for HTTP\n\n\u003e This value can only be set in operator helm chart or with the environment variable `OPERATOR_HTTP_PORT`.",
    "name": "internalHttpPort",
    "required": false,
    "type": "integer"
  },
  {
    "default": 8443,
    "description": "The port that the operator will use to listen for HTTPS\n\n\u003e This value can only be set in operator helm chart or with the environment variable `OPERATOR_HTTPS_PORT`.",
    "name": "internalHttpsPort",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Operator Pod node selector\n\n\u003e This value can only be set in operator helm chart.",
    "name": "nodeSelector",
    "required": false,
    "type": "object"
  },
  {
    "default": 443,
    "description": "The port that will be exposed by the operator Service for HTTPS\n\n\u003e This value can only be set in operator helm chart.",
    "name": "port",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Operator Pod preemption policy. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#container-v1-core",
    "name": "preemptionPolicy",
    "required": false,
    "type": "string"
  },
  {
    "description": "Operator Pod priority class name. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#container-v1-core",
    "name": "priorityClassName",
    "required": false,
    "type": "string"
  },
  {
    "description": "Operator Pod resources. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#resourcerequirements-v1-core\n\n\u003e This value can only be set in operator helm chart.",
    "name": "resources",
    "required": false,
    "type": "object"
  },
  {
    "description": "Operator Pod runtime class name. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#container-v1-core",
    "name": "runtimeClassName",
    "required": false,
    "type": "string"
  },
  {
    "description": "Operator Pod scheduler name. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#container-v1-core",
    "name": "schedulerName",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgconfigspecoperatorservice",
    "description": "Section to configure Operator Service",
    "name": "service",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgconfigspecoperatorserviceaccount",
    "description": "Section to configure Operator ServiceAccount",
    "name": "serviceAccount",
    "required": false,
    "type": "object"
  },
  {
    "description": "Operator Pod tolerations. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#toleration-v1-core\n\n\u003e This value can only be set in operator helm chart.",
    "name": "tolerations",
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

#### SGConfig.spec.operator.image
<sup><sup>[↩ Parent](#sgconfigspecoperator)</sup></sup>

Section to configure Operator image

{{< crd-properties >}}
[
  {
    "default": "stackgres/operator",
    "description": "Operator image name\n\n\u003e This value can only be set in operator helm chart.",
    "name": "name",
    "required": false,
    "type": "string"
  },
  {
    "default": "IfNotPresent",
    "description": "Operator image pull policy\n\n\u003e This value can only be set in operator helm chart.",
    "name": "pullPolicy",
    "required": false,
    "type": "string"
  },
  {
    "description": "Operator image tag\n\n\u003e This value can only be set in operator helm chart.",
    "name": "tag",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

#### SGConfig.spec.operator.service
<sup><sup>[↩ Parent](#sgconfigspecoperator)</sup></sup>

Section to configure Operator Service

{{< crd-properties >}}
[
  {
    "description": "Section to configure Operator Service annotations\n\n\u003e This value can only be set in operator helm chart.",
    "name": "annotations",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

#### SGConfig.spec.operator.serviceAccount
<sup><sup>[↩ Parent](#sgconfigspecoperator)</sup></sup>

Section to configure Operator ServiceAccount

{{< crd-properties >}}
[
  {
    "description": "Section to configure Operator ServiceAccount annotations\n\n\u003e This value can only be set in operator helm chart.",
    "name": "annotations",
    "required": false,
    "type": "object"
  },
  {
    "description": "Repositories credentials Secret names\n\n\u003e This value can only be set in operator helm chart.",
    "name": "repoCredentials",
    "required": false,
    "type": "[]string"
  }
]
{{< /crd-properties >}}

### SGConfig.spec.prometheus
<sup><sup>[↩ Parent](#sgconfigspec)</sup></sup>

**Deprecated** this section has been replaced by `.spec.collector.prometheusOperator`.

Section to configure Prometheus integration.


{{< crd-properties >}}
[
  {
    "default": true,
    "description": "**Deprecated** this field has been replaced by `.spec.collector.prometheusOperator.allowDiscovery`.\n\nIf set to false disable automatic bind to Prometheus\n  created using the [Prometheus Operator](https://github.com/prometheus-operator/prometheus-operator).\nIf disabled the cluster will not be bound to Prometheus automatically and will require manual\n  intervention by the Kubernetes cluster administrator.",
    "name": "allowAutobind",
    "required": false,
    "type": "boolean"
  }
]
{{< /crd-properties >}}

### SGConfig.spec.rbac
<sup><sup>[↩ Parent](#sgconfigspec)</sup></sup>

Section to configure RBAC for Web Console admin user

{{< crd-properties >}}
[
  {
    "default": true,
    "description": "When set to `true` the admin user is assigned the `cluster-admin` ClusterRole by creating\n ClusterRoleBinding.\n\n\u003e This value can only be set in operator helm chart.\n\u003e For OLM instalations the admin user is always created.",
    "name": "create",
    "required": false,
    "type": "boolean"
  }
]
{{< /crd-properties >}}

### SGConfig.spec.restapi
<sup><sup>[↩ Parent](#sgconfigspec)</sup></sup>

Section to configure REST API Pod

{{< crd-properties >}}
[
  {
    "description": "REST API Pod affinity. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#affinity-v1-core",
    "name": "affinity",
    "required": false,
    "type": "object"
  },
  {
    "description": "REST API Pod annotations",
    "name": "annotations",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgconfigspecrestapiimage",
    "description": "Section to configure REST API image",
    "name": "image",
    "required": false,
    "type": "object"
  },
  {
    "default": "stackgres-restapi",
    "description": "REST API Deployment name",
    "name": "name",
    "required": false,
    "type": "string"
  },
  {
    "description": "REST API Pod node selector",
    "name": "nodeSelector",
    "required": false,
    "type": "object"
  },
  {
    "description": "REST API Pod preemption policy. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#container-v1-core",
    "name": "preemptionPolicy",
    "required": false,
    "type": "string"
  },
  {
    "description": "REST API Pod priority class name. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#container-v1-core",
    "name": "priorityClassName",
    "required": false,
    "type": "string"
  },
  {
    "description": "REST API Pod resources. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#resourcerequirements-v1-core",
    "name": "resources",
    "required": false,
    "type": "object"
  },
  {
    "description": "REST API Pod runtime class name. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#container-v1-core",
    "name": "runtimeClassName",
    "required": false,
    "type": "string"
  },
  {
    "description": "REST API Pod scheduler name. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#container-v1-core",
    "name": "schedulerName",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgconfigspecrestapiservice",
    "description": "Section to configure REST API Service",
    "name": "service",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgconfigspecrestapiserviceaccount",
    "description": "Section to configure REST API ServiceAccount",
    "name": "serviceAccount",
    "required": false,
    "type": "object"
  },
  {
    "description": "REST API Pod tolerations. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#toleration-v1-core",
    "name": "tolerations",
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

#### SGConfig.spec.restapi.image
<sup><sup>[↩ Parent](#sgconfigspecrestapi)</sup></sup>

Section to configure REST API image

{{< crd-properties >}}
[
  {
    "default": "stackgres/restapi",
    "description": "REST API image name",
    "name": "name",
    "required": false,
    "type": "string"
  },
  {
    "default": "IfNotPresent",
    "description": "REST API image pull policy",
    "name": "pullPolicy",
    "required": false,
    "type": "string"
  },
  {
    "description": "REST API image tag",
    "name": "tag",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

#### SGConfig.spec.restapi.service
<sup><sup>[↩ Parent](#sgconfigspecrestapi)</sup></sup>

Section to configure REST API Service

{{< crd-properties >}}
[
  {
    "description": "REST API Service annotations",
    "name": "annotations",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

#### SGConfig.spec.restapi.serviceAccount
<sup><sup>[↩ Parent](#sgconfigspecrestapi)</sup></sup>

Section to configure REST API ServiceAccount

{{< crd-properties >}}
[
  {
    "description": "REST API ServiceAccount annotations",
    "name": "annotations",
    "required": false,
    "type": "object"
  },
  {
    "description": "Repositories credentials Secret names",
    "name": "repoCredentials",
    "required": false,
    "type": "[]string"
  }
]
{{< /crd-properties >}}

### SGConfig.spec.serviceAccount
<sup><sup>[↩ Parent](#sgconfigspec)</sup></sup>

Section to configure Operator Installation ServiceAccount

> For OLM instalations this section is not used.


{{< crd-properties >}}
[
  {
    "description": "Section to configure Installation ServiceAccount annotations",
    "name": "annotations",
    "required": false,
    "type": "object"
  },
  {
    "default": true,
    "description": "If `true` the Operator Installation ServiceAccount will be created\n\n\u003e This value can only be set in operator helm chart.",
    "name": "create",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "Repositories credentials Secret names\n\n\u003e This value can only be set in operator helm chart.",
    "name": "repoCredentials",
    "required": false,
    "type": "[]string"
  }
]
{{< /crd-properties >}}

### SGConfig.spec.shardingSphere
<sup><sup>[↩ Parent](#sgconfigspec)</sup></sup>

Section to configure integration with ShardingSphere operator

{{< crd-properties >}}
[
  {
    "anchor": "sgconfigspecshardingsphereserviceaccount",
    "description": "Section to configure ServiceAccount used by ShardingSphere operator.\n\nYou may configure a specific value for a sharded cluster under section\n `SGShardedCluster.spec.coordinator.configurations.shardingSphere.serviceAccount`.",
    "name": "serviceAccount",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

#### SGConfig.spec.shardingSphere.serviceAccount
<sup><sup>[↩ Parent](#sgconfigspecshardingsphere)</sup></sup>

Section to configure ServiceAccount used by ShardingSphere operator.

You may configure a specific value for a sharded cluster under section
 `SGShardedCluster.spec.coordinator.configurations.shardingSphere.serviceAccount`.


{{< crd-properties >}}
[
  {
    "description": "The name of the ServiceAccount used by ShardingSphere operator",
    "name": "name",
    "required": true,
    "type": "string"
  },
  {
    "description": "The namespace of the ServiceAccount used by ShardingSphere operator",
    "name": "namespace",
    "required": true,
    "type": "string"
  }
]
{{< /crd-properties >}}

## SGConfig.status
<sup><sup>[↩ Parent](#sgconfig)</sup></sup>

Status defines the observed state of SGConfig

{{< crd-properties >}}
[
  {
    "anchor": "sgconfigstatusconditionsindex",
    "description": "",
    "name": "conditions",
    "required": false,
    "type": "[]object"
  },
  {
    "description": "Indicate the version to which existing CRs have been updated to",
    "name": "existingCrUpdatedToVersion",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgconfigstatusgrafana",
    "description": "",
    "name": "grafana",
    "required": false,
    "type": "object"
  },
  {
    "description": "A hash that identify the installation",
    "name": "installationId",
    "required": false,
    "type": "string"
  },
  {
    "description": "Latest version of the operator used to check for updates",
    "name": "version",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

### SGConfig.status.conditions[index]
<sup><sup>[↩ Parent](#sgconfigstatus)</sup></sup>



{{< crd-properties >}}
[
  {
    "description": "Last time the condition transitioned from one status to another.",
    "name": "lastTransitionTime",
    "required": false,
    "type": "string"
  },
  {
    "description": "A human readable message indicating details about the transition.",
    "name": "message",
    "required": false,
    "type": "string"
  },
  {
    "description": "The .metadata.generation the condition was computed against.",
    "format": "int64",
    "name": "observedGeneration",
    "required": false,
    "type": "integer"
  },
  {
    "description": "The reason for the condition's last transition.",
    "name": "reason",
    "required": false,
    "type": "string"
  },
  {
    "description": "Status of the condition, one of True, False, Unknown.",
    "name": "status",
    "required": false,
    "type": "string"
  },
  {
    "description": "Type of deployment condition.",
    "name": "type",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

### SGConfig.status.grafana
<sup><sup>[↩ Parent](#sgconfigstatus)</sup></sup>



{{< crd-properties >}}
[
  {
    "description": "Grafana configuration hash",
    "name": "configHash",
    "required": false,
    "type": "string"
  },
  {
    "description": "Grafana Token that allow to access dashboards",
    "name": "token",
    "required": false,
    "type": "string"
  },
  {
    "description": "Grafana URLs to StackGres dashboards",
    "name": "urls",
    "required": false,
    "type": "[]string"
  }
]
{{< /crd-properties >}}

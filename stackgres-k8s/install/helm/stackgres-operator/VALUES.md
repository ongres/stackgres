| <div style="width:12rem">Property</dev> | <div style="width:5rem">Type</dev> | <div style="width:5rem">Default</div> | Description |
|-----|------|---------|-------------|
| adminui.image.name | string | `"stackgres/admin-ui"` | Web Console image name |
| adminui.image.pullPolicy | string | `"IfNotPresent"` | Web Console image pull policy |
| adminui.image.tag | string | `"main-1.5"` | Web Console image tag |
| adminui.name | string | `"stackgres-adminui"` | Web Console container name |
| adminui.resources | object | `{}` | Web Console resources. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.27/#resourcerequirements-v1-core |
| adminui.service.exposeHTTP | bool | `false` | When set to `true` the HTTP port will be exposed in the Web Console Service |
| adminui.service.loadBalancerIP | string | `nil` | LoadBalancer will get created with the IP specified in   this field. This feature depends on whether the underlying cloud-provider supports specifying   the loadBalancerIP when a load balancer is created. This field will be ignored if the   cloud-provider does not support the feature. |
| adminui.service.loadBalancerSourceRanges | string | `nil` | If specified and supported by the platform,   this will restrict traffic through the cloud-provider load-balancer will be restricted to the   specified client IPs. This field will be ignored if the cloud-provider does not support the   feature. More info: https://kubernetes.io/docs/tasks/access-application-cluster/configure-cloud-provider-firewall/ |
| adminui.service.nodePort | string | `nil` | The HTTPS port used to expose the Service on Kubernetes nodes |
| adminui.service.nodePortHTTP | string | `nil` | The HTTP port used to expose the Service on Kubernetes nodes |
| adminui.service.type | string | `"ClusterIP"` | The type used for the service of the UI: * Set to LoadBalancer to create a load balancer (if supported by the kubernetes cluster)   to allow connect from Internet to the UI. Note that enabling this feature will probably incurr in   some fee that depend on the host of the kubernetes cluster (for example this is true for EKS, GKE   and AKS). * Set to NodePort to expose admin UI from kubernetes nodes. |
| authentication.oidc.authServerUrl | string | `nil` |  |
| authentication.oidc.clientId | string | `nil` |  |
| authentication.oidc.clientIdSecretRef.key | string | `nil` |  |
| authentication.oidc.clientIdSecretRef.name | string | `nil` |  |
| authentication.oidc.credentialsSecret | string | `nil` |  |
| authentication.oidc.credentialsSecretSecretRef.key | string | `nil` |  |
| authentication.oidc.credentialsSecretSecretRef.name | string | `nil` |  |
| authentication.oidc.tlsVerification | string | `nil` | Can be one of `required`, `certificate-validation` or `none` |
| authentication.password | string | `nil` | The admin password that will be required to access the UI |
| authentication.resetPassword | bool | `false` | When set to `true` the admin user password will be reset. |
| authentication.secretRef.name | string | `nil` | The admin user Secret name to be used. Allow to specify the secret name that will be used store the credentials to access the UI.   It simply prevent creating the secret automatically. |
| authentication.type | string | `"jwt"` | Specify the authentication mechanism to use. By default is `jwt`, see https://stackgres.io/doc/latest/api/rbac#local-secret-mechanism.   If set to `oidc` then see https://stackgres.io/doc/latest/api/rbac/#openid-connect-provider-mechanism. |
| authentication.user | string | `"admin"` | The admin username that will be required to access the UI |
| cert.autoapprove | bool | `true` | If set to `true` the CertificateSigningRequest used to generate the certificate used by   Webhooks will be approved by the Operator Installation Job. |
| cert.certManager.autoConfigure | bool | `false` | When set to `true` then Issuer and Certificate for Operator and Web Console / REST API   Pods will be generated |
| cert.certManager.duration | string | `"2160h"` | The requested duration (i.e. lifetime) of the Certificates. See https://cert-manager.io/docs/reference/api-docs/#cert-manager.io%2fv1 |
| cert.certManager.encoding | string | `"PKCS1"` | The private key cryptography standards (PKCS) encoding for this certificate’s private key to be encoded in. See https://cert-manager.io/docs/reference/api-docs/#cert-manager.io/v1.CertificatePrivateKey |
| cert.certManager.renewBefore | string | `"360h"` | How long before the currently issued certificate’s expiry cert-manager should renew the certificate. See https://cert-manager.io/docs/reference/api-docs/#cert-manager.io%2fv1 |
| cert.certManager.size | int | `2048` | Size is the key bit size of the corresponding private key for this certificate. See https://cert-manager.io/docs/reference/api-docs/#cert-manager.io/v1.CertificatePrivateKey |
| cert.createForOperator | bool | `true` | When set to `true` the Operator certificate will be created. |
| cert.createForWebApi | bool | `true` | When set to `true` the Web Console / REST API certificate will be created. |
| cert.crt | string | `nil` | The Operator Webhooks certificate issued by Kubernetes cluster CA. |
| cert.jwtRsaKey | string | `nil` | The private RSA key used to generate JWTs used in REST API authentication. |
| cert.jwtRsaPub | string | `nil` | The public RSA key used to verify JWTs used in REST API authentication. |
| cert.key | string | `nil` | The private RSA key used to create the Operator Webhooks certificate issued by the   Kubernetes cluster CA. |
| cert.resetCerts | bool | `false` | When set to `true` the Web Console / REST API certificates will be reset. |
| cert.secretName | string | `nil` | The Secret name with the Operator Webhooks certificate issued by the Kubernetes cluster CA   of type kubernetes.io/tls. See https://kubernetes.io/docs/concepts/configuration/secret/#tls-secrets |
| cert.webCrt | string | `nil` | The Web Console / REST API certificate |
| cert.webKey | string | `nil` | The private RSA key used to create the Web Console / REST API certificate |
| cert.webSecretName | string | `nil` | The Secret name with the Web Console / REST API certificate   of type kubernetes.io/tls. See https://kubernetes.io/docs/concepts/configuration/secret/#tls-secrets |
| containerRegistry | string | `"quay.io"` | The container registry host (and port) where the images will be pulled from. |
| deploy.operator | bool | `true` | When set to `true` the Operator will be deployed. |
| deploy.restapi | bool | `true` | When set to `true` the Web Console / REST API will be deployed. |
| developer.allowPullExtensionsFromImageRepository | bool | `false` | If set to `true` and `extensions.cache.enabled` is also `true`   it will try to download extensions from images (experimental) |
| developer.disableArbitraryUser | bool | `false` | It set to `true` disable arbitrary user that is set for OpenShift clusters |
| developer.enableJvmDebug | bool | `false` | Only work with JVM version and allow connect on port 8000 of operator Pod with jdb or similar |
| developer.enableJvmDebugSuspend | bool | `false` | Only work with JVM version and if `enableJvmDebug` is `true`   suspend the JVM until a debugger session is started |
| developer.externalOperatorIp | string | `nil` | Set the external Operator IP |
| developer.externalOperatorPort | integer | `nil` | Set the external Operator port |
| developer.externalRestApiIp | string | `nil` | Set the external REST API IP |
| developer.externalRestApiPort | integer | `nil` | Set the external REST API port |
| developer.logLevel | string | `nil` | Set `quarkus.log.level`. See https://quarkus.io/guides/logging#root-logger-configuration |
| developer.showStackTraces | bool | `false` | Set `quarkus.log.console.format` to `%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c{4.}] (%t) %s%e%n`. See https://quarkus.io/guides/logging#logging-format |
| extensions.cache.enabled | bool | `false` | When set to `true` enable the extensions cache.  This feature is in beta and may cause failures, please use with caution and report any   error to https://gitlab.com/ongresinc/stackgres/-/issues/new |
| extensions.cache.hostPath | string | `nil` | If set, will use a host path volume with the specified path for the extensions cache   instead of a PersistentVolume |
| extensions.cache.persistentVolume.size | string | `"1Gi"` | The PersistentVolume size for the extensions cache  Only use whole numbers (e.g. not 1e6) and K/Ki/M/Mi/G/Gi as units |
| extensions.cache.persistentVolume.storageClass | string | `nil` | If defined set storage class If set to "-" (equivalent to storageClass: "" in a PV spec) disables   dynamic provisioning If undefined (the default) or set to null, no storageClass spec is   set, choosing the default provisioner.  (gp2 on AWS, standard on   GKE, AWS & OpenStack) |
| extensions.cache.preloadedExtensions | list | `["x86_64/linux/timescaledb-1\\.7\\.4-pg12"]` | An array of extensions pattern used to pre-loaded estensions into the extensions cache |
| extensions.repositoryUrls | list | `["https://extensions.stackgres.io/postgres/repository"]` | A list of extensions repository URLs used to retrieve extensions  To set a proxy for extensions repository add parameter proxyUrl to the URL:   `https://extensions.stackgres.io/postgres/repository?proxyUrl=<proxy scheme>%3A%2F%2F<proxy host>[%3A<proxy port>]` (URL encoded) |
| grafana.autoEmbed | bool | `false` | When set to `true` embed automatically Grafana into the Web Console by creating the   StackGres dashboard and the read-only role used to read it from the Web Console  |
| grafana.dashboardConfigMap | string | `nil` | The ConfigMap name with the dashboard JSON in the key `grafana-dashboard.json`   that will be created in Grafana. If not set the default   StackGres dashboard will be created. (used to embed automatically Grafana) |
| grafana.dashboardId | string | `nil` | The dashboard id that will be create in Grafana   (see https://grafana.com/grafana/dashboards). By default 9628. (used to embed automatically   Grafana) |
| grafana.datasourceName | string | `"Prometheus"` | The datasource name used to create the StackGres Dashboard into Grafana |
| grafana.password | string | `"prom-operator"` | The password to access Grafana. By default prom-operator (the default in for   kube-prometheus-stack helm chart). (used to embed automatically Grafana) |
| grafana.schema | string | `"http"` | The schema to access Grafana. By default http. (used to embed manually and   automatically grafana) |
| grafana.secretName | string | `nil` | The name of secret with credentials to access Grafana. (used to embed   automatically Grafana, alternative to use `user` and `password`) |
| grafana.secretNamespace | string | `nil` | The namespace of secret with credentials to access Grafana. (used to   embed automatically Grafana, alternative to use `user` and `password`) |
| grafana.secretPasswordKey | string | `nil` | The key of secret with password used to access Grafana. (used to   embed automatically Grafana, alternative to use `user` and `password`) |
| grafana.secretUserKey | string | `nil` | The key of secret with username used to access Grafana. (used to embed   automatically Grafana, alternative to use `user` and `password`) |
| grafana.token | string | `nil` | The Grafana API token to access the PostgreSQL dashboard created   in Grafana (used to embed manually Grafana) |
| grafana.url | string | `nil` | The URL of the PostgreSQL dashboard created in Grafana (used to embed manually   Grafana) |
| grafana.user | string | `"admin"` | The username to access Grafana. By default admin. (used to embed automatically   Grafana) |
| grafana.webHost | string | `nil` | The service host name to access grafana (used to embed manually and   automatically Grafana).  The parameter value should point to the grafana service following the    [DNS reference](https://kubernetes.io/docs/concepts/services-networking/dns-pod-service/) `svc_name.namespace` |
| imagePullPolicy | string | `"IfNotPresent"` | Image pull policy used for images loaded by the Operator |
| initClusterRole | string | `"cluster-admin"` | The ClusterRole assigned to the Operation Installation Jobs. By default is `cluster-admin`. |
| jobs.affinity | object | `{}` | Operator Installation Jobs affinity. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.27/#affinity-v1-core |
| jobs.annotations | object | `{}` | Operator Installation Jobs annotations |
| jobs.image.name | string | `"stackgres/jobs"` | Operator Installation Jobs image name |
| jobs.image.pullPolicy | string | `"IfNotPresent"` | Operator Installation Jobs image pull policy |
| jobs.image.tag | string | `"main-1.5-jvm"` | Operator Installation Jobs image tag |
| jobs.nodeSelector | object | `{}` | Operator Installation Jobs node selector |
| jobs.resources | object | `{}` | Operator Installation Jobs resources. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.27/#resourcerequirements-v1-core |
| jobs.tolerations | list | `[]` | Operator Installation Jobs tolerations. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.27/#toleration-v1-core |
| operator.affinity | object | `{}` | Operator Pod affinity. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.27/#affinity-v1-core |
| operator.annotations | object | `{}` | Operator Pod annotations |
| operator.image.name | string | `"stackgres/operator"` | Operator image name |
| operator.image.pullPolicy | string | `"IfNotPresent"` | Operator image pull policy |
| operator.image.tag | string | `"main-1.5-jvm"` | Operator image tag |
| operator.nodeSelector | object | `{}` | Operator Pod node selector |
| operator.resources | object | `{}` | Operator Pod resources. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.27/#resourcerequirements-v1-core |
| operator.service.annotations | object | `{}` | Section to configure Operator Service annotations |
| operator.serviceAccount.annotations | object | `{}` | Section to configure Operator ServiceAccount annotations |
| operator.tolerations | list | `[]` | Operator Pod tolerations. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.27/#toleration-v1-core |
| prometheus.allowAutobind | bool | `true` | If set to false disable automatic bind to Prometheus   created using the [Prometheus Operator](https://github.com/prometheus-operator/prometheus-operator). If disabled the cluster will not be binded to Prometheus automatically and will require manual   intervention by the Kubernetes cluster administrator. |
| rbac.create | bool | `true` | When set to `true` the admin user is assigned the `cluster-admin` ClusterRole by creating   ClusterRoleBinding. |
| restapi.affinity | object | `{}` | REST API Pod affinity. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.27/#affinity-v1-core |
| restapi.annotations | object | `{}` | REST API Pod annotations |
| restapi.image.name | string | `"stackgres/restapi"` | REST API image name |
| restapi.image.pullPolicy | string | `"IfNotPresent"` | REST API image pull policy |
| restapi.image.tag | string | `"main-1.5-jvm"` | REST API image tag |
| restapi.name | string | `"stackgres-restapi"` | REST API container name |
| restapi.nodeSelector | object | `{}` | REST API Pod node selector |
| restapi.resources | object | `{}` | REST API Pod resources. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.27/#resourcerequirements-v1-core |
| restapi.service.annotations | object | `{}` | REST API Service annotations |
| restapi.serviceAccount.annotations | object | `{}` | REST API ServiceAccount annotations |
| restapi.tolerations | list | `[]` | REST API Pod tolerations. See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.27/#toleration-v1-core |
| serviceAccount.create | bool | `true` | If `true` the Operator Installation ServiceAccount will be created |
| serviceAccount.repoCredentials | list | `[]` | Repositories credentials Secret names to attach to ServiceAccounts and Pods |
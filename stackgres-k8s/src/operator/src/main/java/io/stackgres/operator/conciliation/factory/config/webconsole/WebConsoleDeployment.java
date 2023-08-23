/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.config.webconsole;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.ObjectFieldSelectorBuilder;
import io.fabric8.kubernetes.api.model.ProbeBuilder;
import io.fabric8.kubernetes.api.model.Toleration;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.stackgres.common.StackGresProperty;
import io.stackgres.common.crd.sgconfig.StackGresAuthenticationType;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgconfig.StackGresConfigAdminui;
import io.stackgres.common.crd.sgconfig.StackGresConfigAuthentication;
import io.stackgres.common.crd.sgconfig.StackGresConfigAuthenticationOidc;
import io.stackgres.common.crd.sgconfig.StackGresConfigDeploy;
import io.stackgres.common.crd.sgconfig.StackGresConfigDeveloper;
import io.stackgres.common.crd.sgconfig.StackGresConfigDeveloperContainerPatches;
import io.stackgres.common.crd.sgconfig.StackGresConfigDeveloperPatches;
import io.stackgres.common.crd.sgconfig.StackGresConfigExtensions;
import io.stackgres.common.crd.sgconfig.StackGresConfigGrafana;
import io.stackgres.common.crd.sgconfig.StackGresConfigImage;
import io.stackgres.common.crd.sgconfig.StackGresConfigRestapi;
import io.stackgres.common.crd.sgconfig.StackGresConfigServiceAccount;
import io.stackgres.common.crd.sgconfig.StackGresConfigSpec;
import io.stackgres.common.crd.sgconfig.StackGresConfigStatus;
import io.stackgres.common.crd.sgconfig.StackGresConfigStatusGrafana;
import io.stackgres.common.labels.LabelFactoryForConfig;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.config.StackGresConfigContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.Seq;

@Singleton
@OperatorVersionBinder
public class WebConsoleDeployment
    implements ResourceGenerator<StackGresConfigContext> {

  private static final Pattern IMAGE_NAME_WITH_REGISTRY_PATTERN =
      Pattern.compile("^[^/]+\\.[^/]+/.*$");
  private static final Pattern URL_PATTERN = Pattern.compile("^https?://[^/]+(/.*)$");
  private static final String WEBCONSOLE_CERTS = "web-certs";
  private static final String ADMINUI_NGINX = "adminui-nginx";
  private static final String ADMINUI_NGINX_ETC = "adminui-nginx-etc";

  private final LabelFactoryForConfig labelFactory;

  private final WebConsolePodSecurityFactory webConsolePodSecurityContext;

  public static String name(StackGresConfig config) {
    return ResourceUtil.resourceName(
        Optional.of(config.getSpec())
        .map(StackGresConfigSpec::getRestapi)
        .map(StackGresConfigRestapi::getName)
        .orElse("stackgres-restapi"));
  }

  @Inject
  public WebConsoleDeployment(
      LabelFactoryForConfig labelFactory,
      WebConsolePodSecurityFactory webConsolePodSecurityContext) {
    super();
    this.labelFactory = labelFactory;
    this.webConsolePodSecurityContext = webConsolePodSecurityContext;
  }

  /**
   * Create the Secret for Web Console.
   */
  @Override
  public @NotNull Stream<HasMetadata> generateResource(StackGresConfigContext context) {
    if (!Optional.ofNullable(context.getSource().getSpec())
        .map(StackGresConfigSpec::getDeploy)
        .map(StackGresConfigDeploy::getRestapi)
        .orElse(true)) {
      return Stream.of();
    }

    final StackGresConfig config = context.getSource();
    final String namespace = config.getMetadata().getNamespace();
    final Map<String, String> labels = labelFactory.genericLabels(config);
    final Optional<StackGresConfigRestapi> restapi =
        Optional.ofNullable(context.getSource().getSpec())
        .map(StackGresConfigSpec::getRestapi);
    final Optional<StackGresConfigAdminui> adminui =
        Optional.ofNullable(context.getSource().getSpec())
        .map(StackGresConfigSpec::getAdminui);

    return Stream.of(new DeploymentBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(name(config))
        .withLabels(labels)
        .endMetadata()
        .withNewSpec()
        .withReplicas(1)
        .withNewSelector()
        .withMatchLabels(labelFactory.restapiLabels(config))
        .endSelector()
        .withNewTemplate()
        .withNewMetadata()
        .withAnnotations(restapi
            .map(StackGresConfigRestapi::getAnnotations)
            .orElse(null))
        .withLabels(labelFactory.restapiLabels(config))
        .endMetadata()
        .withNewSpec()
        .withAffinity(restapi
            .map(StackGresConfigRestapi::getAffinity)
            .orElse(null))
        .withTolerations(restapi
            .map(StackGresConfigRestapi::getTolerations)
            .stream()
            .flatMap(List::stream)
            .map(Toleration.class::cast)
            .toList())
        .withNodeSelector(restapi
            .map(StackGresConfigRestapi::getNodeSelector)
            .orElse(null))
        .withServiceAccount(name(config))
        .withImagePullSecrets(Optional.of(config.getSpec())
            .map(StackGresConfigSpec::getServiceAccount)
            .map(StackGresConfigServiceAccount::getRepoCredentials)
            .stream()
            .flatMap(List::stream)
            .map(LocalObjectReference::new)
            .toList())
        .withSecurityContext(webConsolePodSecurityContext.createRestApiPodSecurityContext(context))
        .withContainers(
            new ContainerBuilder()
            .withName("stackgres-restapi")
            .withImage(getImageNameWithTag(
                context,
                restapi.map(StackGresConfigRestapi::getImage),
                "stackgres/restapi"))
            .withImagePullPolicy(
                restapi
                .map(StackGresConfigRestapi::getImage)
                .map(StackGresConfigImage::getPullPolicy)
                .orElse("IfNotPresent"))
            .withSecurityContext(webConsolePodSecurityContext.createRestapiSecurityContext(context))
            .withEnv(new EnvVarBuilder()
                .withName("RESTAPI_NAMESPACE")
                .withValueFrom(new EnvVarSourceBuilder()
                    .withFieldRef(new ObjectFieldSelectorBuilder()
                        .withApiVersion("v1")
                        .withFieldPath("metadata.namespace")
                        .build())
                    .build())
                .build(),
                new EnvVarBuilder()
                .withName("RESTAPI_IP")
                .withValueFrom(new EnvVarSourceBuilder()
                    .withFieldRef(new ObjectFieldSelectorBuilder()
                        .withApiVersion("v1")
                        .withFieldPath("status.podIP")
                        .build())
                    .build())
                .build(),
                new EnvVarBuilder()
                .withName("RESTAPI_LOG_LEVEL")
                .withValue(
                    Optional.of(context.getSource().getSpec())
                    .map(StackGresConfigSpec::getDeveloper)
                    .map(StackGresConfigDeveloper::getLogLevel)
                    .orElse(""))
                .build(),
                new EnvVarBuilder()
                .withName("RESTAPI_SHOW_STACK_TRACES")
                .withValue(
                    Optional.of(context.getSource().getSpec())
                    .map(StackGresConfigSpec::getDeveloper)
                    .map(StackGresConfigDeveloper::getShowStackTraces)
                    .map(Object::toString)
                    .orElse(""))
                .build(),
                new EnvVarBuilder()
                .withName("APP_OPTS")
                .withValue(Optional.ofNullable(System.getenv("APP_OPTS")).orElse(null))
                .build(),
                new EnvVarBuilder()
                .withName("JAVA_OPTS")
                .withValue(Optional.ofNullable(System.getenv("JAVA_OPTS")).orElse(null))
                .build(),
                new EnvVarBuilder()
                .withName("DEBUG_RESTAPI")
                .withValue(
                    Optional.of(context.getSource().getSpec())
                    .map(StackGresConfigSpec::getDeveloper)
                    .map(StackGresConfigDeveloper::getEnableJvmDebug)
                    .map(Object::toString)
                    .orElse(""))
                .build(),
                new EnvVarBuilder()
                .withName("DEBUG_RESTAPI_SUSPEND")
                .withValue(
                    Optional.of(context.getSource().getSpec())
                    .map(StackGresConfigSpec::getDeveloper)
                    .map(StackGresConfigDeveloper::getEnableJvmDebugSuspend)
                    .map(Object::toString)
                    .orElse(""))
                .build(),
                new EnvVarBuilder()
                .withName("GRAFANA_EMBEDDED")
                .withValue(Optional.of(context.isGrafanaIntegrated())
                    .filter(isGrafanaIntegrated -> isGrafanaIntegrated)
                    .map(Object::toString)
                    .orElse(""))
                .build(),
                new EnvVarBuilder()
                .withName("USE_ARBITRARY_USER")
                .withValue(Optional.ofNullable(System.getenv("USE_ARBITRARY_USER")).orElse(null))
                .build(),
                new EnvVarBuilder()
                .withName("EXTENSIONS_REPOSITORY_URLS")
                .withValue(
                    Optional.of(context.getSource().getSpec())
                    .map(StackGresConfigSpec::getExtensions)
                    .map(StackGresConfigExtensions::getRepositoryUrls)
                    .stream()
                    .flatMap(List::stream)
                    .collect(Collectors.joining(",")))
                .build(),
                new EnvVarBuilder()
                .withName("AUTHENTICATION_TYPE")
                .withValue(
                    Optional.of(context.getSource().getSpec())
                    .map(StackGresConfigSpec::getAuthentication)
                    .map(StackGresConfigAuthentication::getType)
                    .orElse(""))
                .build(),
                new EnvVarBuilder()
                .withName("OPERATOR_VERSION")
                .withValue(Optional.ofNullable(System.getenv(
                    StackGresProperty.OPERATOR_VERSION.getEnvironmentVariableName())).orElse(null))
                .build())
            .addAllToEnv(
                Optional.of(context.getSource().getSpec())
                .map(StackGresConfigSpec::getAuthentication)
                .map(StackGresConfigAuthentication::getType)
                .filter(StackGresAuthenticationType.OIDC.toString()::equals)
                .map(ignore ->  List.of(
                    new EnvVarBuilder()
                    .withName("QUARKUS_OIDC_APPLICATION_TYPE")
                    .withValue(
                        Optional.of(context.getSource().getSpec())
                        .map(StackGresConfigSpec::getAuthentication)
                        .map(StackGresConfigAuthentication::getType)
                        .map(StackGresAuthenticationType.OIDC.toString()::equals)
                        .map(Object::toString)
                        .orElse(""))
                    .build(),
                    new EnvVarBuilder()
                    .withName("QUARKUS_OIDC_PUBLIC_KEY")
                    .withValue(
                        Optional.of(context.getSource().getSpec())
                        .map(StackGresConfigSpec::getAuthentication)
                        .map(StackGresConfigAuthentication::getType)
                        .filter(StackGresAuthenticationType.OIDC.toString()::equals)
                        .map(type -> "")
                        .orElse(""))
                    .build(),
                    new EnvVarBuilder()
                    .withName("QUARKUS_OIDC_AUTH_SERVER_URL")
                    .withValue(
                        Optional.of(context.getSource().getSpec())
                        .map(StackGresConfigSpec::getAuthentication)
                        .map(StackGresConfigAuthentication::getType)
                        .filter(StackGresAuthenticationType.OIDC.toString()::equals)
                        .flatMap(type -> Optional.of(context.getSource().getSpec())
                            .map(StackGresConfigSpec::getAuthentication)
                            .map(StackGresConfigAuthentication::getOidc)
                            .map(StackGresConfigAuthenticationOidc::getAuthServerUrl))
                        .orElse(""))
                    .build(),
                    new EnvVarBuilder()
                    .withName("QUARKUS_OIDC_TLS_VERIFICATION")
                    .withValue(
                        Optional.of(context.getSource().getSpec())
                        .map(StackGresConfigSpec::getAuthentication)
                        .map(StackGresConfigAuthentication::getType)
                        .filter(StackGresAuthenticationType.OIDC.toString()::equals)
                        .flatMap(type -> Optional.of(context.getSource().getSpec())
                            .map(StackGresConfigSpec::getAuthentication)
                            .map(StackGresConfigAuthentication::getOidc)
                            .map(StackGresConfigAuthenticationOidc::getTlsVerification))
                        .orElse(""))
                    .build(),
                    Optional.of(context.getSource().getSpec())
                    .map(StackGresConfigSpec::getAuthentication)
                    .map(StackGresConfigAuthentication::getType)
                    .filter(StackGresAuthenticationType.OIDC.toString()::equals)
                    .flatMap(type -> Optional.of(context.getSource().getSpec())
                        .map(StackGresConfigSpec::getAuthentication)
                        .map(StackGresConfigAuthentication::getOidc)
                        .map(StackGresConfigAuthenticationOidc::getClientIdSecretRef))
                    .map(clientIdSecretRef -> new EnvVarBuilder()
                        .withName("QUARKUS_OIDC_CLIENT_ID")
                        .withValueFrom(new EnvVarSourceBuilder()
                            .withNewSecretKeyRef()
                            .withName(clientIdSecretRef.getName())
                            .withKey(clientIdSecretRef.getKey())
                            .endSecretKeyRef()
                            .build())
                        .build())
                    .orElseGet(() -> new EnvVarBuilder()
                        .withName("QUARKUS_OIDC_CLIENT_ID")
                        .withValue(
                            Optional.of(context.getSource().getSpec())
                            .map(StackGresConfigSpec::getAuthentication)
                            .map(StackGresConfigAuthentication::getType)
                            .filter(StackGresAuthenticationType.OIDC.toString()::equals)
                            .flatMap(type -> Optional.of(context.getSource().getSpec())
                                .map(StackGresConfigSpec::getAuthentication)
                                .map(StackGresConfigAuthentication::getOidc)
                                .map(StackGresConfigAuthenticationOidc::getClientId))
                            .orElse(""))
                        .build()),
                    Optional.of(context.getSource().getSpec())
                    .map(StackGresConfigSpec::getAuthentication)
                    .map(StackGresConfigAuthentication::getType)
                    .filter(StackGresAuthenticationType.OIDC.toString()::equals)
                    .flatMap(type -> Optional.of(context.getSource().getSpec())
                        .map(StackGresConfigSpec::getAuthentication)
                        .map(StackGresConfigAuthentication::getOidc)
                        .map(StackGresConfigAuthenticationOidc::getCredentialsSecretSecretRef))
                    .map(credentialsSecretSecretRef -> new EnvVarBuilder()
                        .withName("QUARKUS_OIDC_CREDENTIALS_SECRET")
                        .withValueFrom(new EnvVarSourceBuilder()
                            .withNewSecretKeyRef()
                            .withName(credentialsSecretSecretRef.getName())
                            .withKey(credentialsSecretSecretRef.getKey())
                            .endSecretKeyRef()
                            .build())
                        .build())
                    .orElseGet(() -> new EnvVarBuilder()
                        .withName("QUARKUS_OIDC_CREDENTIALS_SECRET")
                        .withValue(
                            Optional.of(context.getSource().getSpec())
                            .map(StackGresConfigSpec::getAuthentication)
                            .map(StackGresConfigAuthentication::getType)
                            .filter(StackGresAuthenticationType.OIDC.toString()::equals)
                            .flatMap(type -> Optional.of(context.getSource().getSpec())
                                .map(StackGresConfigSpec::getAuthentication)
                                .map(StackGresConfigAuthentication::getOidc)
                                .map(StackGresConfigAuthenticationOidc::getCredentialsSecret))
                            .orElse(""))
                        .build())))
                .orElse(List.of()))
            .withPorts(new ContainerPortBuilder()
                .withName("resthttp")
                .withContainerPort(8080)
                .withProtocol("TCP")
                .build(),
                new ContainerPortBuilder()
                .withName("resthttps")
                .withContainerPort(8443)
                .withProtocol("TCP")
                .build())
            .withLivenessProbe(new ProbeBuilder()
                .withNewHttpGet()
                .withPath("/q/health/live")
                .withPort(new IntOrString(8080))
                .withScheme("HTTP")
                .endHttpGet()
                .withInitialDelaySeconds(5)
                .withPeriodSeconds(30)
                .withTimeoutSeconds(10)
                .build())
            .withReadinessProbe(new ProbeBuilder()
                .withNewHttpGet()
                .withPath("/q/health/ready")
                .withPort(new IntOrString(8080))
                .withScheme("HTTP")
                .endHttpGet()
                .withInitialDelaySeconds(0)
                .withPeriodSeconds(2)
                .withTimeoutSeconds(1)
                .build())
            .withResources(restapi
                .map(StackGresConfigRestapi::getResources)
                .orElse(null))
            .withVolumeMounts(
                new VolumeMountBuilder()
                .withName(WEBCONSOLE_CERTS)
                .withMountPath("/etc/operator/certs")
                .withReadOnly(true)
                .build())
            .addAllToVolumeMounts(Optional.of(context.getSource().getSpec())
                .map(StackGresConfigSpec::getDeveloper)
                .map(StackGresConfigDeveloper::getPatches)
                .map(StackGresConfigDeveloperPatches::getRestapi)
                .map(StackGresConfigDeveloperContainerPatches::getVolumeMounts)
                .stream()
                .flatMap(List::stream)
                .map(VolumeMount.class::cast)
                .toList())
            .build(),
            new ContainerBuilder()
            .withName("stackgres-adminui")
            .withImage(getImageNameWithTag(
                context,
                adminui.map(StackGresConfigAdminui::getImage),
                "stackgres/admin-ui"))
            .withImagePullPolicy(
                adminui
                .map(StackGresConfigAdminui::getImage)
                .map(StackGresConfigImage::getPullPolicy)
                .orElse("IfNotPresent"))
            .withCommand("/bin/sh", "-ex",
                "/usr/local/bin/start-nginx.sh")
            .withSecurityContext(webConsolePodSecurityContext.createAdminuiSecurityContext(context))
            .withEnv(
                new EnvVarBuilder()
                .withName("SHOW_DEBUG")
                .withValue(
                    Optional.of(context.getSource().getSpec())
                    .map(StackGresConfigSpec::getDeveloper)
                    .map(StackGresConfigDeveloper::getShowDebug)
                    .map(Object::toString)
                    .orElse(""))
                .build(),
                new EnvVarBuilder()
                .withName("GRAFANA_EMBEDDED")
                .withValue(Optional.of(context.isGrafanaIntegrated())
                    .filter(isGrafanaIntegrated -> isGrafanaIntegrated)
                    .map(Object::toString)
                    .orElse(""))
                .build(),
                new EnvVarBuilder()
                .withName("GRAFANA_URL_PATH")
                .withValue(
                    Optional.ofNullable(context.getSource().getStatus())
                    .map(StackGresConfigStatus::getGrafana)
                    .map(StackGresConfigStatusGrafana::getUrl)
                    .map(url -> Optional.of(url)
                      .map(URL_PATTERN::matcher)
                      .filter(Matcher::find)
                      .map(matcher -> matcher.group(1))
                      .orElse(url))
                    .orElse("/"))
                .build(),
                new EnvVarBuilder()
                .withName("GRAFANA_SCHEMA")
                .withValue(
                    Optional.of(context.getSource().getSpec())
                    .map(StackGresConfigSpec::getGrafana)
                    .map(StackGresConfigGrafana::getSchema)
                    .orElse("http"))
                .build(),
                new EnvVarBuilder()
                .withName("GRAFANA_WEB_HOST")
                .withValue(
                    Optional.ofNullable(context.getSource().getSpec())
                    .map(StackGresConfigSpec::getGrafana)
                    .map(StackGresConfigGrafana::getWebHost)
                    .orElse("localhost:8080"))
                .build(),
                new EnvVarBuilder()
                .withName("GRAFANA_TOKEN")
                .withValue(
                    Optional.ofNullable(context.getSource().getStatus())
                    .map(StackGresConfigStatus::getGrafana)
                    .map(StackGresConfigStatusGrafana::getToken)
                    .orElse("unknown"))
                .build())
            .withPorts(new ContainerPortBuilder()
                .withName("http")
                .withContainerPort(9080)
                .withProtocol("TCP")
                .build(),
                new ContainerPortBuilder()
                .withName("https")
                .withContainerPort(9443)
                .withProtocol("TCP")
                .build())
            .withResources(adminui
                .map(StackGresConfigAdminui::getResources)
                .orElse(null))
            .withVolumeMounts(
                new VolumeMountBuilder()
                .withName(WEBCONSOLE_CERTS)
                .withMountPath("/etc/operator/certs")
                .withReadOnly(true)
                .build(),
                new VolumeMountBuilder()
                .withName(ADMINUI_NGINX)
                .withMountPath("/usr/local/bin/start-nginx.sh")
                .withSubPath("start-nginx.sh")
                .withReadOnly(true)
                .build(),
                new VolumeMountBuilder()
                .withName(ADMINUI_NGINX)
                .withMountPath("/etc/nginx/nginx.conf")
                .withSubPath("nginx.conf")
                .withReadOnly(true)
                .build(),
                new VolumeMountBuilder()
                .withName(ADMINUI_NGINX)
                .withMountPath("/etc/nginx/template.d")
                .withReadOnly(false)
                .build(),
                new VolumeMountBuilder()
                .withName(ADMINUI_NGINX_ETC)
                .withMountPath("/etc/nginx/conf.d")
                .withSubPath("etc/nginx/conf.d")
                .withReadOnly(false)
                .build(),
                new VolumeMountBuilder()
                .withName(ADMINUI_NGINX_ETC)
                .withMountPath("/var/cache/nginx")
                .withSubPath("var/cache/nginx")
                .withReadOnly(false)
                .build(),
                new VolumeMountBuilder()
                .withName(ADMINUI_NGINX_ETC)
                .withMountPath("/var/run")
                .withSubPath("var/run")
                .withReadOnly(false)
                .build(),
                new VolumeMountBuilder()
                .withName(ADMINUI_NGINX_ETC)
                .withMountPath("/var/log/nginx")
                .withSubPath("var/log/nginx")
                .withReadOnly(false)
                .build())
            .addAllToVolumeMounts(Optional.of(context.getSource().getSpec())
                .map(StackGresConfigSpec::getDeveloper)
                .map(StackGresConfigDeveloper::getPatches)
                .map(StackGresConfigDeveloperPatches::getAdminui)
                .map(StackGresConfigDeveloperContainerPatches::getVolumeMounts)
                .stream()
                .flatMap(List::stream)
                .map(VolumeMount.class::cast)
                .toList())
            .build())
        .withVolumes(
            new VolumeBuilder()
            .withName(WEBCONSOLE_CERTS)
            .withNewSecret()
            .withSecretName(WebConsoleSecret.name(context.getSource()))
            .withOptional(false)
            .endSecret()
            .build(),
            new VolumeBuilder()
            .withName(ADMINUI_NGINX)
            .withNewConfigMap()
            .withName(AdminuiNginxConfigMap.name(context.getSource()))
            .withOptional(false)
            .endConfigMap()
            .build(),
            new VolumeBuilder()
            .withName(ADMINUI_NGINX_ETC)
            .withNewEmptyDir()
            .endEmptyDir()
            .build())
        .addAllToVolumes(Seq.seq(
            Optional.of(context.getSource().getSpec())
            .map(StackGresConfigSpec::getDeveloper)
            .map(StackGresConfigDeveloper::getPatches)
            .map(StackGresConfigDeveloperPatches::getRestapi)
            .map(StackGresConfigDeveloperContainerPatches::getVolumes)
            .stream()
            .flatMap(List::stream)
            .map(Volume.class::cast))
            .append(Optional.of(context.getSource().getSpec())
                .map(StackGresConfigSpec::getDeveloper)
                .map(StackGresConfigDeveloper::getPatches)
                .map(StackGresConfigDeveloperPatches::getAdminui)
                .map(StackGresConfigDeveloperContainerPatches::getVolumes)
                .stream()
                .flatMap(List::stream)
                .map(Volume.class::cast))
            .grouped(volume -> volume.getName())
            .flatMap(t -> t.v2.limit(1))
            .toList())
        .endSpec()
        .endTemplate()
        .endSpec()
        .build());
  }

  public String getImageNameWithTag(
      StackGresConfigContext context,
      Optional<StackGresConfigImage> image,
      String defaultImageName) {
    final String containerRegistry = context.getSource().getSpec().getContainerRegistry();
    String imageName = image
        .map(StackGresConfigImage::getName)
        .orElse(defaultImageName);
    String imageTag = image
        .map(StackGresConfigImage::getTag)
        .or(() -> StackGresProperty.OPERATOR_IMAGE_VERSION.get())
        .orElseThrow(() -> new IllegalArgumentException(
            "Can not determine the image tag."
                + " Missing OPERATOR_IMAGE_VERSION environment variable"));
    return IMAGE_NAME_WITH_REGISTRY_PATTERN.matcher(imageName).matches()
        ? imageName + ":" + imageTag
            : containerRegistry + "/" + imageName + ":" + imageTag;
  }

}

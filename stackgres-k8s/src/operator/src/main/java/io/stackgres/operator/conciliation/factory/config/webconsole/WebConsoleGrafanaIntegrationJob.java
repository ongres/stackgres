/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.config.webconsole;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.ServiceAccountBuilder;
import io.fabric8.kubernetes.api.model.Toleration;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.api.model.rbac.PolicyRuleBuilder;
import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.api.model.rbac.RoleBindingBuilder;
import io.fabric8.kubernetes.api.model.rbac.RoleBuilder;
import io.fabric8.kubernetes.api.model.rbac.SubjectBuilder;
import io.stackgres.common.KubectlUtil;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgconfig.StackGresConfigDeploy;
import io.stackgres.common.crd.sgconfig.StackGresConfigGrafana;
import io.stackgres.common.crd.sgconfig.StackGresConfigJobs;
import io.stackgres.common.crd.sgconfig.StackGresConfigServiceAccount;
import io.stackgres.common.crd.sgconfig.StackGresConfigSpec;
import io.stackgres.common.labels.LabelFactoryForConfig;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.config.StackGresConfigContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

@Singleton
@OperatorVersionBinder
public class WebConsoleGrafanaIntegrationJob
    implements ResourceGenerator<StackGresConfigContext> {

  private final LabelFactoryForConfig labelFactory;
  private final KubectlUtil kubectl;
  private final WebConsolePodSecurityFactory webConsolePodSecurityContext;

  public static String name(StackGresConfig config) {
    return ResourceUtil.resourceName(config.getMetadata().getName() + "-integrate-grafana");
  }

  @Inject
  public WebConsoleGrafanaIntegrationJob(
      LabelFactoryForConfig labelFactory,
      KubectlUtil kubectl,
      WebConsolePodSecurityFactory webConsolePodSecurityContext) {
    this.labelFactory = labelFactory;
    this.kubectl = kubectl;
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
        .orElse(true)
        || !Optional.ofNullable(context.getSource().getSpec())
        .map(StackGresConfigSpec::getGrafana)
        .map(StackGresConfigGrafana::getAutoEmbed)
        .orElse(false)
        || context.isGrafanaIntegrated()
        || context.isGrafanaIntegrationJobFailed()) {
      return Stream.of();
    }

    final StackGresConfig config = context.getSource();
    final String namespace = config.getMetadata().getNamespace();
    final Map<String, String> labels = labelFactory.genericLabels(config);
    final Optional<StackGresConfigJobs> jobs =
        Optional.ofNullable(context.getSource().getSpec())
        .map(StackGresConfigSpec::getJobs);

    return Seq.<HasMetadata>of(
        new ServiceAccountBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(name(config))
        .withLabels(labels)
        .endMetadata()
        .build(),
        new RoleBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(name(config))
        .withLabels(labels)
        .endMetadata()
        .withRules(
            new PolicyRuleBuilder()
            .withApiGroups(HasMetadata.getGroup(StackGresConfig.class))
            .withResources(HasMetadata.getPlural(StackGresConfig.class))
            .withVerbs("get")
            .build(),
            new PolicyRuleBuilder()
            .withApiGroups(HasMetadata.getGroup(StackGresConfig.class))
            .withResources(HasMetadata.getPlural(StackGresConfig.class) + "/status")
            .withVerbs("update")
            .build())
        .build(),
        new RoleBindingBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(name(config))
        .withLabels(labels)
        .endMetadata()
        .withSubjects(new SubjectBuilder()
            .withKind(HasMetadata.getKind(ServiceAccount.class))
            .withName(name(config))
            .withNamespace(namespace)
            .build())
        .withNewRoleRef()
        .withApiGroup(HasMetadata.getGroup(Role.class))
        .withKind(HasMetadata.getKind(Role.class))
        .withName(name(config))
        .endRoleRef()
        .build(),
        new SecretBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(name(config))
        .endMetadata()
        .withData(ResourceUtil.encodeSecret(Seq.<Tuple2<String, String>>of()
            .append(context.getGrafanaUser()
                .map(grafanaUser -> Tuple.tuple("GRAFANA_USER", grafanaUser)))
            .append(context.getGrafanaPassword()
                .map(grafanaPassword -> Tuple.tuple("GRAFANA_PASSWORD", grafanaPassword)))
            .collect(Collectors.toMap(Tuple2::v1, Tuple2::v2))))
        .build(),
        new JobBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(name(config))
        .withLabels(labels)
        .endMetadata()
        .withNewSpec()
        .withBackoffLimit(3)
        .withNewTemplate()
        .withNewMetadata()
        .withAnnotations(jobs
            .map(StackGresConfigJobs::getAnnotations)
            .orElse(null))
        .withLabels(labelFactory.grafanaIntegrationLabels(config))
        .endMetadata()
        .withNewSpec()
        .withRestartPolicy("OnFailure")
        .withTerminationGracePeriodSeconds(0L)
        .withAffinity(jobs
            .map(StackGresConfigJobs::getAffinity)
            .orElse(null))
        .withTolerations(jobs
            .map(StackGresConfigJobs::getTolerations)
            .stream()
            .flatMap(List::stream)
            .map(Toleration.class::cast)
            .toList())
        .withNodeSelector(jobs
            .map(StackGresConfigJobs::getNodeSelector)
            .orElse(null))
        .withServiceAccount(name(config))
        .withImagePullSecrets(Optional.of(config.getSpec())
            .map(StackGresConfigSpec::getServiceAccount)
            .map(StackGresConfigServiceAccount::getRepoCredentials)
            .stream()
            .flatMap(List::stream)
            .map(LocalObjectReference::new)
            .toList())
        .withSecurityContext(webConsolePodSecurityContext
            .createGrafanaIntegrationPodSecurityContext(context))
        .withContainers(
            new ContainerBuilder()
            .withName("integrate-grafana")
            .withImage(kubectl.getImageName(StackGresVersion.LATEST))
            .withCommand("/bin/sh", "-x", "/usr/local/bin/integrate-grafana.sh")
            .withImagePullPolicy("IfNotPresent")
            .withEnv(
                new EnvVarBuilder()
                .withName("SGCONFIG_NAMESPACE")
                .withValue(Optional.ofNullable(System.getenv(
                    OperatorProperty.SGCONFIG_NAMESPACE.getEnvironmentVariableName()))
                    .orElseGet(OperatorProperty.OPERATOR_NAMESPACE::getEnvironmentVariableName))
                .build(),
                new EnvVarBuilder()
                .withName("OPERATOR_NAME")
                .withValue(Optional.ofNullable(System.getenv(
                    OperatorProperty.OPERATOR_NAME.getEnvironmentVariableName())).orElse(null))
                .build(),
                new EnvVarBuilder()
                .withName("GRAFANA_DASHBOARD_LIST")
                .withValue(WebConsoleGrafanaIntegartionConfigMap.getDashboards()
                    .stream()
                    .collect(Collectors.joining("\n")))
                .build(),
                new EnvVarBuilder()
                .withName("GRAFANA_USER")
                .withValueFrom(new EnvVarSourceBuilder()
                    .withNewSecretKeyRef()
                    .withName(name(config))
                    .withKey("GRAFANA_USER")
                    .withOptional(false)
                    .endSecretKeyRef()
                    .build())
                .build(),
                new EnvVarBuilder()
                .withName("GRAFANA_PASSWORD")
                .withValueFrom(new EnvVarSourceBuilder()
                    .withNewSecretKeyRef()
                    .withName(name(config))
                    .withKey("GRAFANA_PASSWORD")
                    .withOptional(false)
                    .endSecretKeyRef()
                    .build())
                .build(),
                new EnvVarBuilder()
                .withName("GRAFANA_WEB_HOST")
                .withValue(
                    Optional.of(context.getSource().getSpec())
                    .map(StackGresConfigSpec::getGrafana)
                    .map(StackGresConfigGrafana::getWebHost)
                    .orElse(""))
                .build(),
                new EnvVarBuilder()
                .withName("GRAFANA_SCHEMA")
                .withValue(
                    Optional.of(context.getSource().getSpec())
                    .map(StackGresConfigSpec::getGrafana)
                    .map(StackGresConfigGrafana::getSchema)
                    .orElse(""))
                .build(),
                new EnvVarBuilder()
                .withName("GRAFANA_DASHBOARD_ID")
                .withValue(
                    Optional.of(context.getSource().getSpec())
                    .map(StackGresConfigSpec::getGrafana)
                    .map(StackGresConfigGrafana::getDashboardId)
                    .orElse(""))
                .build(),
                new EnvVarBuilder()
                .withName("GRAFANA_DATASOURCE_NAME")
                .withValue(
                    Optional.of(context.getSource().getSpec())
                    .map(StackGresConfigSpec::getGrafana)
                    .map(StackGresConfigGrafana::getDatasourceName)
                    .orElse(""))
                .build(),
                new EnvVarBuilder()
                .withName("GRAFANA_CONFIG_HASH")
                .withValue(
                    Optional.of(context.getSource().getSpec())
                    .map(StackGresConfigSpec::getGrafana)
                    .map(Object::hashCode)
                    .map(String::valueOf)
                    .orElse(""))
                .build())
            .withResources(jobs
                .map(StackGresConfigJobs::getResources)
                .orElse(null))
            .withVolumeMounts(
                new VolumeMountBuilder()
                .withName("grafana-integration")
                .withMountPath("/usr/local/bin/integrate-grafana.sh")
                .withReadOnly(true)
                .withSubPath("integrate-grafana.sh")
                .build(),
                new VolumeMountBuilder()
                .withName("grafana-integration")
                .withMountPath("/etc/grafana")
                .withReadOnly(true)
                .build())
            .build())
        .withVolumes(
            new VolumeBuilder()
            .withName("grafana-integration")
            .withNewConfigMap()
            .withName(
                Optional.of(context.getSource().getSpec())
                .map(StackGresConfigSpec::getGrafana)
                .map(StackGresConfigGrafana::getDashboardConfigMap)
                .orElse(WebConsoleGrafanaIntegartionConfigMap.name(context.getSource())))
            .withOptional(false)
            .endConfigMap()
            .build())
        .endSpec()
        .endTemplate()
        .endSpec()
        .build());
  }

}

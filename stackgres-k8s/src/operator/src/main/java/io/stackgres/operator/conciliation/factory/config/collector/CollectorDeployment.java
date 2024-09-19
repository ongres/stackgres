/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.config.collector;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.ProbeBuilder;
import io.fabric8.kubernetes.api.model.Toleration;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.stackgres.common.ConfigPath;
import io.stackgres.common.KubectlUtil;
import io.stackgres.common.StackGresProperty;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgconfig.StackGresConfigCollector;
import io.stackgres.common.crd.sgconfig.StackGresConfigDeploy;
import io.stackgres.common.crd.sgconfig.StackGresConfigImage;
import io.stackgres.common.crd.sgconfig.StackGresConfigRestapi;
import io.stackgres.common.crd.sgconfig.StackGresConfigSpec;
import io.stackgres.common.labels.LabelFactoryForConfig;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.config.StackGresConfigContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@OperatorVersionBinder
public class CollectorDeployment
    implements ResourceGenerator<StackGresConfigContext> {

  private static final Logger LOGGER = LoggerFactory.getLogger("io.stackgres.collector");

  private static final String COLLECTOR_CERTS = "collector-certs";
  private static final String COLLECTOR_CONFIG = "collector-config";
  private static final String COLLECTOR_SCRIPTS = "collector-scripts";

  private final LabelFactoryForConfig labelFactory;

  private final CollectorPodSecurityFactory collectorPodSecurityContext;

  private final KubectlUtil kubectlUtil;

  public static String name(StackGresConfig config) {
    return ResourceUtil.resourceName(
        Optional.of(config.getSpec())
        .map(StackGresConfigSpec::getCollector)
        .map(StackGresConfigCollector::getName)
        .orElse("stackgres-collector"));
  }

  public static String namespacedClusterRoleBindingName(StackGresConfig config) {
    return ResourceUtil.resourceName(
        Optional.of(config.getSpec())
        .map(StackGresConfigSpec::getRestapi)
        .map(StackGresConfigRestapi::getName)
        .orElse("stackgres-collector")
        + "-" + config.getMetadata().getNamespace());
  }

  @Inject
  public CollectorDeployment(
      LabelFactoryForConfig labelFactory,
      CollectorPodSecurityFactory webConsolePodSecurityContext,
      KubectlUtil kubectlUtil) {
    super();
    this.labelFactory = labelFactory;
    this.collectorPodSecurityContext = webConsolePodSecurityContext;
    this.kubectlUtil = kubectlUtil;
  }

  /**
   * Create the Secret for Web Console.
   */
  @Override
  public @NotNull Stream<HasMetadata> generateResource(StackGresConfigContext context) {
    if (!Optional.ofNullable(context.getSource().getSpec())
        .map(StackGresConfigSpec::getDeploy)
        .map(StackGresConfigDeploy::getCollector)
        .orElse(true)
        || context.getObservedClusters().isEmpty()) {
      return Stream.of();
    }

    final StackGresConfig config = context.getSource();
    final String namespace = config.getMetadata().getNamespace();
    final Map<String, String> labels = labelFactory.genericLabels(config);
    final Optional<StackGresConfigCollector> collector =
        Optional.ofNullable(context.getSource().getSpec())
        .map(StackGresConfigSpec::getCollector);

    return Stream.of(new DeploymentBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(name(config))
        .withLabels(labels)
        .endMetadata()
        .withNewSpec()
        .withReplicas(1)
        .withNewSelector()
        .withMatchLabels(labelFactory.collectorLabels(config))
        .endSelector()
        .withNewTemplate()
        .withNewMetadata()
        .withAnnotations(collector
            .map(StackGresConfigCollector::getAnnotations)
            .orElse(null))
        .withLabels(labelFactory.collectorLabels(config))
        .endMetadata()
        .withNewSpec()
        .withAffinity(collector
            .map(StackGresConfigCollector::getAffinity)
            .orElse(null))
        .withTolerations(collector
            .map(StackGresConfigCollector::getTolerations)
            .stream()
            .flatMap(List::stream)
            .map(Toleration.class::cast)
            .toList())
        .withNodeSelector(collector
            .map(StackGresConfigCollector::getNodeSelector)
            .orElse(null))
        .withServiceAccount(name(config))
        .withSecurityContext(collectorPodSecurityContext.createCollectorPodSecurityContext(context))
        .withShareProcessNamespace()
        .withContainers(
            new ContainerBuilder()
            .withName("stackgres-collector")
            .withImage(StackGresUtil.getCollectorImageNameWithTag(context))
            .withImagePullPolicy(
                collector
                .map(StackGresConfigCollector::getImage)
                .map(StackGresConfigImage::getPullPolicy)
                .orElse("IfNotPresent"))
            .withArgs(
                "--config", ConfigPath.COLLECTOR_CONFIG_PATH.path(),
                "--feature-gates", "-component.UseLocalHostAsDefaultHost")
            .withSecurityContext(collectorPodSecurityContext.createCollectorSecurityContext(context))
            .withEnv(
                new EnvVarBuilder()
                .withName(StackGresProperty.OPERATOR_VERSION.getEnvironmentVariableName())
                .withValue(Optional.ofNullable(System.getenv(
                    StackGresProperty.OPERATOR_VERSION.getEnvironmentVariableName())).orElse(null))
                .build())
            .withPorts(collector
                .map(StackGresConfigCollector::getPorts)
                .orElse(null))
            .withLivenessProbe(new ProbeBuilder()
                .withNewHttpGet()
                .withPath(CollectorConfigMap.OTEL_HEALTH_CHECK_PATH)
                .withPort(new IntOrString(CollectorConfigMap.OTEL_HEALTH_CHECK_PORT))
                .withScheme("HTTP")
                .endHttpGet()
                .withInitialDelaySeconds(5)
                .withPeriodSeconds(30)
                .withTimeoutSeconds(10)
                .build())
            .withReadinessProbe(new ProbeBuilder()
                .withNewHttpGet()
                .withPath(CollectorConfigMap.OTEL_HEALTH_CHECK_PATH)
                .withPort(new IntOrString(CollectorConfigMap.OTEL_HEALTH_CHECK_PORT))
                .withScheme("HTTP")
                .endHttpGet()
                .withInitialDelaySeconds(0)
                .withPeriodSeconds(2)
                .withTimeoutSeconds(1)
                .build())
            .withResources(collector
                .map(StackGresConfigCollector::getResources)
                .orElse(null))
            .withVolumeMounts(
                new VolumeMountBuilder()
                .withName(COLLECTOR_CERTS)
                .withMountPath("/etc/collector/certs")
                .withReadOnly(true)
                .build(),
                new VolumeMountBuilder()
                .withName(COLLECTOR_CONFIG)
                .withMountPath(ConfigPath.ETC_COLLECTOR_PATH.path())
                .withReadOnly(true)
                .build())
            .addAllToVolumeMounts(collector
                .map(StackGresConfigCollector::getVolumeMounts)
                .orElse(List.of()))
            .build(),
            new ContainerBuilder()
            .withName("stackgres-collector-controller")
            .withImage(kubectlUtil.getImageName(StackGresVersion.LATEST))
            .withImagePullPolicy(
                collector
                .map(StackGresConfigCollector::getImage)
                .map(StackGresConfigImage::getPullPolicy)
                .orElse("IfNotPresent"))
            .withCommand(
                "/bin/bash",
                "-e" + (LOGGER.isTraceEnabled() ? "x" : ""),
                ConfigPath.LOCAL_BIN_START_OTEL_COLLECTOR_SH_PATH.path())
            .withSecurityContext(collectorPodSecurityContext.createCollectorControllerSecurityContext(context))
            .withEnv(
                new EnvVarBuilder()
                .withName("HOME")
                .withValue("/tmp")
                .build(),
                new EnvVarBuilder()
                .withName(StackGresProperty.OPERATOR_VERSION.getEnvironmentVariableName())
                .withValue(Optional.ofNullable(System.getenv(
                    StackGresProperty.OPERATOR_VERSION.getEnvironmentVariableName())).orElse(null))
                .build(),
                new EnvVarBuilder()
                .withName(ConfigPath.COLLECTOR_CONFIG_PATH.name())
                .withValue(ConfigPath.COLLECTOR_CONFIG_PATH.path())
                .build())
            .withVolumeMounts(
                new VolumeMountBuilder()
                .withName(COLLECTOR_CONFIG)
                .withMountPath(ConfigPath.ETC_COLLECTOR_PATH.path())
                .withReadOnly(true)
                .build(),
                new VolumeMountBuilder()
                .withName(COLLECTOR_SCRIPTS)
                .withMountPath(ConfigPath.LOCAL_BIN_PATH.path())
                .withReadOnly(true)
                .build())
            .build())
        .withVolumes(
            new VolumeBuilder()
            .withName(COLLECTOR_CERTS)
            .withNewSecret()
            .withSecretName(CollectorSecret.name(context.getSource()))
            .withOptional(false)
            .endSecret()
            .build(),
            new VolumeBuilder()
            .withName(COLLECTOR_CONFIG)
            .withNewConfigMap()
            .withName(CollectorConfigMap.name(context.getSource()))
            .withOptional(false)
            .addNewItem()
            .withKey(ConfigPath.COLLECTOR_CONFIG_PATH.filename())
            .withPath(ConfigPath.COLLECTOR_CONFIG_PATH.filename())
            .endItem()
            .endConfigMap()
            .build(),
            new VolumeBuilder()
            .withName(COLLECTOR_SCRIPTS)
            .withNewConfigMap()
            .withName(CollectorConfigMap.name(context.getSource()))
            .withOptional(false)
            .addNewItem()
            .withKey(ConfigPath.LOCAL_BIN_START_OTEL_COLLECTOR_SH_PATH.filename())
            .withPath(ConfigPath.LOCAL_BIN_START_OTEL_COLLECTOR_SH_PATH.filename())
            .endItem()
            .endConfigMap()
            .build())
        .addAllToVolumes(collector
            .map(StackGresConfigCollector::getVolumes)
            .orElse(List.of()))
        .endSpec()
        .endTemplate()
        .endSpec()
        .build());
  }

}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.config.collector;

import static io.stackgres.common.StackGresUtil.getDefaultPullPolicy;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.ProbeBuilder;
import io.fabric8.kubernetes.api.model.Toleration;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.stackgres.common.ConfigPath;
import io.stackgres.common.StackGresProperty;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgconfig.StackGresConfigCollector;
import io.stackgres.common.crd.sgconfig.StackGresConfigCollectorReceiver;
import io.stackgres.common.crd.sgconfig.StackGresConfigCollectorReceiverDeployment;
import io.stackgres.common.crd.sgconfig.StackGresConfigCollectorReceiverDeploymentBuilder;
import io.stackgres.common.crd.sgconfig.StackGresConfigDeploy;
import io.stackgres.common.crd.sgconfig.StackGresConfigRestapi;
import io.stackgres.common.crd.sgconfig.StackGresConfigSpec;
import io.stackgres.common.labels.LabelFactoryForConfig;
import io.stackgres.operator.common.ObservedClusterContext;
import io.stackgres.operator.common.ObservedClusterContext.CollectorPodContext;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.config.StackGresConfigContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@OperatorVersionBinder
public class CollectorDeployments
    implements ResourceGenerator<StackGresConfigContext> {

  private static final Logger LOGGER = LoggerFactory.getLogger("io.stackgres.collector");

  private static final String COLLECTOR_CERTS = "collector-certs";
  private static final String COLLECTOR_CONFIG = "collector-config";
  private static final String COLLECTOR_SCRIPTS = "collector-scripts";

  private final LabelFactoryForConfig labelFactory;

  private final CollectorPodSecurityFactory collectorPodSecurityContext;

  public static String name(StackGresConfig config) {
    return ResourceUtil.resourceName(
        Optional.of(config.getSpec())
        .map(StackGresConfigSpec::getCollector)
        .map(StackGresConfigCollector::getName)
        .orElse("stackgres-collector"));
  }

  public static String receiversName(StackGresConfig config, int index) {
    return ResourceUtil.resourceName(
        Optional.of(config.getSpec())
        .map(StackGresConfigSpec::getCollector)
        .map(StackGresConfigCollector::getName)
        .map(name -> name + "-" + index)
        .orElse("stackgres-collector-" + index));
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
  public CollectorDeployments(
      LabelFactoryForConfig labelFactory,
      CollectorPodSecurityFactory webConsolePodSecurityContext) {
    super();
    this.labelFactory = labelFactory;
    this.collectorPodSecurityContext = webConsolePodSecurityContext;
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

    Deployment controllerDeployment = getCollectorDeployment(context);

    return Seq.<HasMetadata>of(controllerDeployment)
        .append(Seq.seq(CollectorConfigMaps.getObserverdClusterPodsPartitions(context))
            .zipWithIndex()
            .map(observedClusterPodsPartition -> getCollectorReceiversDeployment(
                context, observedClusterPodsPartition.v2.intValue(), observedClusterPodsPartition.v1)));
  }

  public Deployment getCollectorDeployment(StackGresConfigContext context) {
    final Deployment collectorDeployment = getBasicCollectorDeployment(context);

    return new DeploymentBuilder(collectorDeployment)
        .editMetadata()
        .withName(name(context.getSource()))
        .endMetadata()
        .editSpec()
        .withReplicas(Optional.ofNullable(context.getSource().getSpec())
            .map(StackGresConfigSpec::getCollector)
            .map(StackGresConfigCollector::getReceivers)
            .filter(receivers -> Optional.ofNullable(receivers.getEnabled())
                .orElse(false))
            .map(StackGresConfigCollectorReceiver::getExporters)
            .orElse(1))
        .editTemplate()
        .editSpec()
        .editContainer(0)
        .addNewPort()
        .withName("oltp-port")
        .withProtocol("TCP")
        .withContainerPort(4317)
        .endPort()
        .endContainer()
        .addToVolumes(
            new VolumeBuilder()
            .withName(COLLECTOR_CONFIG)
            .withNewConfigMap()
            .withName(CollectorConfigMaps.name(context.getSource()))
            .withOptional(false)
            .addNewItem()
            .withKey(ConfigPath.COLLECTOR_CONFIG_PATH.filename())
            .withPath(ConfigPath.COLLECTOR_CONFIG_PATH.filename())
            .endItem()
            .endConfigMap()
            .build())
        .endSpec()
        .endTemplate()
        .endSpec()
        .build();
  }

  public Deployment getCollectorReceiversDeployment(
      StackGresConfigContext context,
      int index,
      List<Tuple2<ObservedClusterContext, CollectorPodContext>> observerClusterPodsPartition) {
    final Deployment collectorDeployment = getBasicCollectorDeployment(context);
    final CollectorPodContext firstObserverClusterPod =
        observerClusterPodsPartition.getFirst().v1.getPods().getFirst();
    final Optional<StackGresConfigCollectorReceiverDeployment> collectorReceiversDeployment =
        Optional.ofNullable(context.getSource().getSpec())
        .map(StackGresConfigSpec::getCollector)
        .map(StackGresConfigCollector::getReceivers)
        .map(StackGresConfigCollectorReceiver::getDeployments)
        .filter(Predicate.not(List::isEmpty))
        .map(deployments -> deployments.get(index))
        .or(() -> Optional.of(
            new StackGresConfigCollectorReceiverDeploymentBuilder()
            .withNewAffinity()
            .withNewPodAffinity()
            .addNewPreferredDuringSchedulingIgnoredDuringExecution()
            .withNewPodAffinityTerm()
            .withNamespaces(firstObserverClusterPod.getNamespace())
            .withNewLabelSelector()
            .withMatchLabels(Map.of("statefulset.kubernetes.io/pod-name", firstObserverClusterPod.getName()))
            .endLabelSelector()
            .withTopologyKey("topology.kubernetes.io/zone")
            .endPodAffinityTerm()
            .withWeight(1)
            .endPreferredDuringSchedulingIgnoredDuringExecution()
            .endPodAffinity()
            .endAffinity()
            .build()));

    final Map<String, String> collectorReceiversAnnotations = collectorReceiversDeployment
        .map(StackGresConfigCollectorReceiverDeployment::getAnnotations)
        .orElse(Map.of());
    return new DeploymentBuilder(collectorDeployment)
        .editMetadata()
        .withName(receiversName(context.getSource(), index))
        .withAnnotations(
            Optional.ofNullable(collectorDeployment.getMetadata().getAnnotations())
            .map(Seq::seq)
            .map(annotations -> annotations
                .filter(annotation -> !collectorReceiversAnnotations.containsKey(annotation.v1))
                .append(Seq.seq(collectorReceiversAnnotations))
                .toMap(Tuple2::v1, Tuple2::v2))
            .or(() -> collectorReceiversDeployment
                .map(StackGresConfigCollectorReceiverDeployment::getAnnotations))
            .orElse(null))
        .endMetadata()
        .editSpec()
        .editTemplate()
        .editMetadata()
        .withAnnotations(
            Optional.ofNullable(
                collectorDeployment.getSpec().getTemplate().getMetadata().getAnnotations())
            .map(Seq::seq)
            .map(annotations -> annotations
                .filter(annotation -> !collectorReceiversAnnotations.containsKey(annotation.v1))
                .append(Seq.seq(collectorReceiversAnnotations))
                .toMap(Tuple2::v1, Tuple2::v2))
            .or(() -> collectorReceiversDeployment
                .map(StackGresConfigCollectorReceiverDeployment::getAnnotations))
            .orElse(null))
        .endMetadata()
        .editSpec()
        .withAffinity(collectorReceiversDeployment
            .map(StackGresConfigCollectorReceiverDeployment::getAffinity)
            .orElse(null))
        .withTolerations(collectorReceiversDeployment
            .map(StackGresConfigCollectorReceiverDeployment::getTolerations)
            .stream()
            .flatMap(List::stream)
            .map(Toleration.class::cast)
            .toList())
        .withNodeSelector(collectorReceiversDeployment
            .map(StackGresConfigCollectorReceiverDeployment::getNodeSelector)
            .orElse(null))
        .addToVolumes(
            new VolumeBuilder()
            .withName(COLLECTOR_CONFIG)
            .withNewConfigMap()
            .withName(CollectorConfigMaps.receiversName(context.getSource(), index))
            .withOptional(false)
            .addNewItem()
            .withKey(ConfigPath.COLLECTOR_CONFIG_PATH.filename())
            .withPath(ConfigPath.COLLECTOR_CONFIG_PATH.filename())
            .endItem()
            .endConfigMap()
            .build())
        .endSpec()
        .endTemplate()
        .endSpec()
        .build();
  }

  public Deployment getBasicCollectorDeployment(StackGresConfigContext context) {
    final StackGresConfig config = context.getSource();
    final String namespace = config.getMetadata().getNamespace();
    final Map<String, String> labels = labelFactory.genericLabels(config);
    final Optional<StackGresConfigCollector> collector =
        Optional.ofNullable(context.getSource().getSpec())
        .map(StackGresConfigSpec::getCollector);

    Deployment collectorDeployment = new DeploymentBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withLabels(labels)
        .withAnnotations(collector
            .map(StackGresConfigCollector::getAnnotations)
            .orElse(null))
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
        .withServiceAccount(name(config))
        .withSecurityContext(collectorPodSecurityContext.createCollectorPodSecurityContext(context))
        .withShareProcessNamespace()
        .withContainers(
            new ContainerBuilder()
            .withName("stackgres-collector")
            .withImage(StackGresUtil.getCollectorImageNameWithTag(context))
            .withImagePullPolicy(getDefaultPullPolicy())
            .withCommand(
                "/bin/bash",
                "-e" + (LOGGER.isTraceEnabled() ? "x" : ""),
                ConfigPath.LOCAL_BIN_START_OTEL_COLLECTOR_SH_PATH.path())
            .withSecurityContext(collectorPodSecurityContext.createCollectorSecurityContext(context))
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
            .withPorts(collector
                .map(StackGresConfigCollector::getPorts)
                .orElse(null))
            .withLivenessProbe(new ProbeBuilder()
                .withNewHttpGet()
                .withPath(CollectorConfigMaps.OTEL_HEALTH_CHECK_PATH)
                .withPort(new IntOrString(CollectorConfigMaps.OTEL_HEALTH_CHECK_PORT))
                .withScheme("HTTP")
                .endHttpGet()
                .withInitialDelaySeconds(5)
                .withPeriodSeconds(30)
                .withTimeoutSeconds(10)
                .build())
            .withReadinessProbe(new ProbeBuilder()
                .withNewHttpGet()
                .withPath(CollectorConfigMaps.OTEL_HEALTH_CHECK_PATH)
                .withPort(new IntOrString(CollectorConfigMaps.OTEL_HEALTH_CHECK_PORT))
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
                .withMountPath(ConfigPath.ETC_CERTIFICATES_PATH.path())
                .withReadOnly(true)
                .build(),
                new VolumeMountBuilder()
                .withName(COLLECTOR_CONFIG)
                .withMountPath(ConfigPath.ETC_COLLECTOR_PATH.path())
                .withReadOnly(true)
                .build(),
                new VolumeMountBuilder()
                .withName(COLLECTOR_SCRIPTS)
                .withMountPath(ConfigPath.LOCAL_BIN_START_OTEL_COLLECTOR_SH_PATH.path())
                .withSubPath(ConfigPath.LOCAL_BIN_START_OTEL_COLLECTOR_SH_PATH.filename())
                .withReadOnly(true)
                .build())
            .addAllToVolumeMounts(collector
                .map(StackGresConfigCollector::getVolumeMounts)
                .orElse(List.of()))
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
            .withName(COLLECTOR_SCRIPTS)
            .withNewConfigMap()
            .withName(CollectorConfigMaps.name(context.getSource()))
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
        .build();
    return collectorDeployment;
  }

}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.controller;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.KubernetesClientFactory;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.ObjectMapperProvider;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDistributedLogs;
import io.stackgres.common.crd.sgcluster.StackGresClusterInitData;
import io.stackgres.common.crd.sgcluster.StackGresClusterNonProduction;
import io.stackgres.common.crd.sgcluster.StackGresClusterPod;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodScheduling;
import io.stackgres.common.crd.sgcluster.StackGresClusterScriptEntry;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecAnnotations;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecMetadata;
import io.stackgres.common.crd.sgcluster.StackGresPodPersistentVolume;
import io.stackgres.common.crd.sgdistributedlogs.DistributedLogsEventReason;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsNonProduction;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsSpec;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsSpecMetadata;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigSpec;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.operator.cluster.factory.ClusterStatefulSet;
import io.stackgres.operator.common.ImmutableStackGresDistributedLogsContext;
import io.stackgres.operator.common.ImmutableStackGresDistributedLogsGeneratorContext;
import io.stackgres.operator.common.SidecarEntry;
import io.stackgres.operator.common.StackGresComponents;
import io.stackgres.operator.common.StackGresDistributedLogsContext;
import io.stackgres.operator.common.StackGresDistributedLogsGeneratorContext;
import io.stackgres.operator.configuration.OperatorPropertyContext;
import io.stackgres.operator.distributedlogs.controller.DistributedLogsController;
import io.stackgres.operator.distributedlogs.factory.DistributedLogs;
import io.stackgres.operator.distributedlogs.fluentd.Fluentd;
import io.stackgres.operator.resource.DistributedLogsResourceHandlerSelector;
import io.stackgres.operatorframework.resource.ResourceGenerator;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.lambda.Unchecked;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.helpers.MessageFormatter;

@ApplicationScoped
public class DistributedLogsReconciliationCycle
    extends StackGresReconciliationCycle<StackGresDistributedLogsContext,
      StackGresDistributedLogs, DistributedLogsResourceHandlerSelector> {

  private final DistributedLogs distributeLogs;
  private final DistributedLogsController distributedLogsController;
  private final Fluentd fluentd;
  private final EventController eventController;
  private final OperatorPropertyContext operatorContext;
  private final LabelFactory<StackGresDistributedLogs> labelFactory;
  private final CustomResourceScanner<StackGresCluster> clusterScanner;

  @Dependent
  public static class Parameters {
    @Inject KubernetesClientFactory clientFactory;
    @Inject DistributedLogsReconciliator reconciliator;
    @Inject DistributedLogs distributeLogs;
    @Inject DistributedLogsController distributedLogsController;
    @Inject Fluentd fluentd;
    @Inject DistributedLogsResourceHandlerSelector handlerSelector;
    @Inject EventController eventController;
    @Inject ObjectMapperProvider objectMapperProvider;
    @Inject OperatorPropertyContext operatorContext;
    @Inject LabelFactory<StackGresDistributedLogs> labelFactory;
    @Inject CustomResourceScanner<StackGresDistributedLogs> distributedLogsScanner;
    @Inject CustomResourceScanner<StackGresCluster> clusterScanner;
  }

  @Inject
  public DistributedLogsReconciliationCycle(Parameters parameters) {
    super("DistributeLogs", parameters.clientFactory::create,
        parameters.reconciliator,
        StackGresDistributedLogsContext::getDistributedLogs,
        parameters.handlerSelector, parameters.distributedLogsScanner);
    this.distributeLogs = parameters.distributeLogs;
    this.distributedLogsController = parameters.distributedLogsController;
    this.fluentd = parameters.fluentd;
    this.eventController = parameters.eventController;
    this.operatorContext = parameters.operatorContext;
    this.labelFactory = parameters.labelFactory;
    this.clusterScanner = parameters.clusterScanner;
  }

  public DistributedLogsReconciliationCycle() {
    super(null, null, null, c -> null, null, null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
    this.distributeLogs = null;
    this.distributedLogsController = null;
    this.fluentd = null;
    this.eventController = null;
    this.operatorContext = null;
    this.labelFactory = null;
    this.clusterScanner = null;
  }

  public static DistributedLogsReconciliationCycle create(Consumer<Parameters> consumer) {
    Stream<Parameters> parameters = Optional.of(new Parameters()).stream().peek(consumer);
    return new DistributedLogsReconciliationCycle(parameters.findAny().get());
  }

  void onStart(@Observes StartupEvent ev) {
    start();
  }

  void onStop(@Observes ShutdownEvent ev) {
    stop();
  }

  @Override
  protected void onError(Exception ex) {
    String message = MessageFormatter.arrayFormat(
        "StackGres DistributeLogs reconciliation cycle failed",
        new String[] {
        }).getMessage();
    logger.error(message, ex);
    try (KubernetesClient client = clientSupplier.get()) {
      eventController.sendEvent(DistributedLogsEventReason.DISTRIBUTED_LOGS_CONFIG_ERROR,
          message + ": " + ex.getMessage(), client);
    }
  }

  @Override
  protected void onConfigError(StackGresDistributedLogsContext context,
      HasMetadata configResource, Exception ex) {
    String message = MessageFormatter.arrayFormat(
        "StackGres DistributeLogs {}.{} reconciliation failed",
        new String[] {
            configResource.getMetadata().getNamespace(),
            configResource.getMetadata().getName(),
        }).getMessage();
    logger.error(message, ex);
    try (KubernetesClient client = clientSupplier.get()) {
      eventController.sendEvent(DistributedLogsEventReason.DISTRIBUTED_LOGS_CONFIG_ERROR,
          message + ": " + ex.getMessage(), configResource, client);
    }
  }

  @Override
  protected ImmutableList<HasMetadata> getRequiredResources(
      StackGresDistributedLogsContext context) {
    return ResourceGenerator.<StackGresDistributedLogsGeneratorContext>with(
        ImmutableStackGresDistributedLogsGeneratorContext.builder()
            .distributedLogsContext(context)
            .build())
        .of(HasMetadata.class)
        .append(distributeLogs)
        .stream()
        .collect(ImmutableList.toImmutableList());
  }

  @Override
  protected StackGresDistributedLogsContext getContextWithExistingResourcesOnly(
      StackGresDistributedLogsContext context,
      ImmutableList<Tuple2<HasMetadata, Optional<HasMetadata>>> existingResourcesOnly) {
    return ImmutableStackGresDistributedLogsContext.copyOf(context)
        .withExistingResources(existingResourcesOnly);
  }

  @Override
  protected StackGresDistributedLogsContext getContextWithExistingAndRequiredResources(
      StackGresDistributedLogsContext context,
      ImmutableList<Tuple2<HasMetadata, Optional<HasMetadata>>> requiredResources,
      ImmutableList<Tuple2<HasMetadata, Optional<HasMetadata>>> existingResources) {
    return ImmutableStackGresDistributedLogsContext.copyOf(context)
        .withRequiredResources(requiredResources)
        .withExistingResources(existingResources);
  }

  @Override
  protected StackGresDistributedLogsContext mapResourceToContext(
      StackGresDistributedLogs resource) {
    return this.getDistributedLogsContext(resource);
  }

  private StackGresDistributedLogsContext getDistributedLogsContext(
      StackGresDistributedLogs distributedLogs) {
    final StackGresCluster cluster = getStackGresCLusterForDistributedLogs(distributedLogs);
    final StackGresPostgresConfig postgresConfig = getStackGresPostgresConfigForDistributedLogs(
        distributedLogs);
    return ImmutableStackGresDistributedLogsContext.builder()
        .operatorContext(operatorContext)
        .distributedLogs(distributedLogs)
        .connectedClusters(getConnectedClusters(distributedLogs))
        .cluster(cluster)
        .backupContext(Optional.empty())
        .restoreContext(Optional.empty())
        .postgresConfig(Optional.of(postgresConfig))
        .profile(Optional.empty())
        .prometheus(Optional.empty())
        .labels(labelFactory.clusterLabels(cluster))
        .clusterNamespace(labelFactory.clusterNamespace(cluster))
        .clusterName(labelFactory.clusterName(cluster))
        .clusterKey(labelFactory.getLabelMapper().clusterKey())
        .scheduledBackupKey(labelFactory.getLabelMapper().scheduledBackupKey())
        .backupKey(labelFactory.getLabelMapper().backupKey())
        .dbOpsKey(labelFactory.getLabelMapper().dbOpsKey())
        .ownerReferences(ImmutableList.of(ResourceUtil.getOwnerReference(distributedLogs)))
        .addBackups()
        .addSidecars(
            new SidecarEntry<>(
                fluentd.toStackGresClusterSidecarResourceFactory(),
                Optional.empty()),
            new SidecarEntry<>(
                distributedLogsController.toStackGresClusterSidecarResourceFactory(),
                Optional.empty()))
        .build();
  }

  private StackGresPostgresConfig getStackGresPostgresConfigForDistributedLogs(
      StackGresDistributedLogs distributedLogs) {
    StackGresPostgresConfig config = new StackGresPostgresConfig();
    config.getMetadata().setNamespace(
        distributedLogs.getMetadata().getNamespace());
    config.getMetadata().setName(
        distributedLogs.getMetadata().getName());
    config.getMetadata().setUid(
        distributedLogs.getMetadata().getUid());
    StackGresPostgresConfigSpec spec = new StackGresPostgresConfigSpec();
    spec.setPostgresVersion(StackGresComponents.getPostgresMajorVersion(
        StackGresComponents.calculatePostgresVersion(StackGresComponents.LATEST)));
    spec.setPostgresqlConf(new HashMap<>());
    spec.getPostgresqlConf().put("shared_preload_libraries",
        "pg_stat_statements, auto_explain, timescaledb");
    spec.getPostgresqlConf().put("timescaledb.telemetry_level", "off");
    config.setSpec(spec);
    return config;
  }

  private StackGresCluster getStackGresCLusterForDistributedLogs(
      StackGresDistributedLogs distributedLogs) {
    final StackGresCluster distributedLogsCluster = new StackGresCluster();
    distributedLogsCluster.getMetadata().setNamespace(
        distributedLogs.getMetadata().getNamespace());
    distributedLogsCluster.getMetadata().setName(
        distributedLogs.getMetadata().getName());
    distributedLogsCluster.getMetadata().setUid(
        distributedLogs.getMetadata().getUid());
    final StackGresClusterSpec spec = new StackGresClusterSpec();
    spec.setPostgresVersion(StackGresComponents.calculatePostgresVersion(
        StackGresComponents.LATEST));
    spec.setInstances(1);
    final StackGresClusterPod pod = new StackGresClusterPod();
    final StackGresPodPersistentVolume persistentVolume = new StackGresPodPersistentVolume();
    persistentVolume.setSize(
        distributedLogs.getSpec().getPersistentVolume().getSize());
    persistentVolume.setStorageClass(
        distributedLogs.getSpec().getPersistentVolume().getStorageClass());
    pod.setPersistentVolume(persistentVolume);
    StackGresClusterPodScheduling scheduling = new StackGresClusterPodScheduling();
    Optional.of(distributedLogs)
        .map(StackGresDistributedLogs::getSpec)
        .map(StackGresDistributedLogsSpec::getScheduling)
        .ifPresent(distributedLogsScheduling -> {
          scheduling.setNodeSelector(distributedLogsScheduling.getNodeSelector());
          scheduling.setTolerations(distributedLogsScheduling.getTolerations());
        });
    pod.setScheduling(scheduling);
    spec.setPod(pod);
    final StackGresClusterInitData initData = new StackGresClusterInitData();
    final StackGresClusterScriptEntry script = new StackGresClusterScriptEntry();
    script.setName("distributed-logs-template");
    script.setDatabase("template1");
    script.setScript(Unchecked.supplier(() -> Resources
          .asCharSource(ClusterStatefulSet.class.getResource("/distributed-logs-template.sql"),
              StandardCharsets.UTF_8)
          .read()).get());
    initData.setScripts(ImmutableList.of(script));
    spec.setInitData(initData);
    final StackGresClusterNonProduction nonProduction = new StackGresClusterNonProduction();
    nonProduction.setDisableClusterPodAntiAffinity(
        Optional.ofNullable(distributedLogs.getSpec().getNonProduction())
        .map(StackGresDistributedLogsNonProduction::getDisableClusterPodAntiAffinity)
        .orElse(false));
    spec.setNonProduction(nonProduction);
    final StackGresClusterSpecMetadata metadata = new StackGresClusterSpecMetadata();
    final StackGresClusterSpecAnnotations annotations = new StackGresClusterSpecAnnotations();
    Optional.of(distributedLogs)
        .map(StackGresDistributedLogs::getSpec)
        .map(StackGresDistributedLogsSpec::getMetadata)
        .map(StackGresDistributedLogsSpecMetadata::getAnnotations)
        .ifPresent(distributedLogsAnnotations -> {
          annotations.setAllResources(distributedLogsAnnotations.getAllResources());
          annotations.setPods(distributedLogsAnnotations.getPods());
          annotations.setServices(distributedLogsAnnotations.getServices());
        });
    metadata.setAnnotations(annotations);
    spec.setMetadata(metadata);
    distributedLogsCluster.setSpec(spec);
    return distributedLogsCluster;
  }

  private ImmutableList<StackGresCluster> getConnectedClusters(
      StackGresDistributedLogs distributedLogs) {
    final String namespace = distributedLogs.getMetadata().getNamespace();
    final String name = distributedLogs.getMetadata().getName();
    return clusterScanner.getResources()
        .stream()
        .filter(cluster -> Optional.ofNullable(cluster.getSpec().getDistributedLogs())
            .map(StackGresClusterDistributedLogs::getDistributedLogs)
            .map(distributedLogsRelativeId -> StackGresUtil.getNamespaceFromRelativeId(
                distributedLogsRelativeId,
                cluster.getMetadata().getNamespace()).equals(namespace)
                && StackGresUtil.getNameFromRelativeId(
                    distributedLogsRelativeId).equals(name))
            .orElse(false))
        .collect(ImmutableList.toImmutableList());
  }

}

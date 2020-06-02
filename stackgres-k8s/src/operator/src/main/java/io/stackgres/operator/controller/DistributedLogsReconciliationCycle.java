/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.controller;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.ArcUtil;
import io.stackgres.common.KubernetesClientFactory;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.NonProduction;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDefinition;
import io.stackgres.common.crd.sgcluster.StackGresClusterDistributedLogs;
import io.stackgres.common.crd.sgcluster.StackGresClusterDoneable;
import io.stackgres.common.crd.sgcluster.StackGresClusterInitData;
import io.stackgres.common.crd.sgcluster.StackGresClusterList;
import io.stackgres.common.crd.sgcluster.StackGresClusterPod;
import io.stackgres.common.crd.sgcluster.StackGresClusterScript;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresPodPersistentVolume;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsDefinition;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsDoneable;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsList;
import io.stackgres.operator.app.ObjectMapperProvider;
import io.stackgres.operator.cluster.factory.ClusterStatefulSet;
import io.stackgres.operator.common.ImmutableStackGresDistributedLogsContext;
import io.stackgres.operator.common.ImmutableStackGresDistributedLogsGeneratorContext;
import io.stackgres.operator.common.SidecarEntry;
import io.stackgres.operator.common.StackGresComponents;
import io.stackgres.operator.common.StackGresDistributedLogsContext;
import io.stackgres.operator.common.StackGresDistributedLogsGeneratorContext;
import io.stackgres.operator.configuration.OperatorContext;
import io.stackgres.operator.distributedlogs.factory.DistributeLogs;
import io.stackgres.operator.distributedlogs.fluentd.Fluentd;
import io.stackgres.operator.resource.DistributedLogsResourceHandlerSelector;
import io.stackgres.operatorframework.reconciliation.AbstractReconciliationCycle;
import io.stackgres.operatorframework.reconciliation.AbstractReconciliator;
import io.stackgres.operatorframework.resource.ResourceGenerator;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.lambda.Unchecked;
import org.jooq.lambda.tuple.Tuple2;

@ApplicationScoped
public class DistributedLogsReconciliationCycle
    extends AbstractReconciliationCycle<StackGresDistributedLogsContext,
      StackGresDistributedLogs, DistributedLogsResourceHandlerSelector> {

  private final DistributeLogs distributeLogs;
  private final Fluentd fluentd;
  private final DistributedLogsStatusManager statusManager;
  private final EventController eventController;
  private final OperatorContext operatorContext;
  private final LabelFactory<StackGresDistributedLogs> labelFactory;

  /**
   * Create a {@code DistributeLogsReconciliationCycle} instance.
   */
  @Inject
  public DistributedLogsReconciliationCycle(
      KubernetesClientFactory clientFactory,
      DistributeLogs distributeLogs, Fluentd fluentd,
      DistributedLogsResourceHandlerSelector handlerSelector,
      DistributedLogsStatusManager statusManager, EventController eventController,
      ObjectMapperProvider objectMapperProvider,
      OperatorContext operatorContext,
      LabelFactory<StackGresDistributedLogs> labelFactory) {
    super("DistributeLogs", clientFactory::create,
        StackGresDistributedLogsContext::getDistributedLogs,
        handlerSelector, objectMapperProvider.objectMapper());
    this.distributeLogs = distributeLogs;
    this.fluentd = fluentd;
    this.statusManager = statusManager;
    this.eventController = eventController;
    this.operatorContext = operatorContext;
    this.labelFactory = labelFactory;
  }

  public DistributedLogsReconciliationCycle() {
    super(null, null, c -> null, null, null);
    ArcUtil.checkPublicNoArgsConstructorIsCalledFromArc();
    this.distributeLogs = null;
    this.fluentd = null;
    this.statusManager = null;
    this.eventController = null;
    this.operatorContext = null;
    this.labelFactory = null;
  }

  @Override
  protected void onError(Exception ex) {
    eventController.sendEvent(EventReason.DISTRIBUTED_LOGS_CONFIG_ERROR,
        "StackGres Cluster reconciliation cycle failed: "
            + ex.getMessage());
  }

  @Override
  protected void onConfigError(StackGresDistributedLogsContext context,
      HasMetadata configResource, Exception ex) {
    eventController.sendEvent(EventReason.DISTRIBUTED_LOGS_CONFIG_ERROR,
        "StackGres DistributeLogs " + configResource.getMetadata().getNamespace() + "."
            + configResource.getMetadata().getName() + " reconciliation failed: "
            + ex.getMessage(), configResource);
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
  protected AbstractReconciliator<StackGresDistributedLogsContext, StackGresDistributedLogs,
      DistributedLogsResourceHandlerSelector> createReconciliator(
          KubernetesClient client, StackGresDistributedLogsContext context) {
    return DistributedLogsReconciliator.builder()
        .withEventController(eventController)
        .withHandlerSelector(handlerSelector)
        .withStatusManager(statusManager)
        .withClient(client)
        .withObjectMapper(objectMapper)
        .withDistributedLogsContext(context)
        .build();
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
  protected ImmutableList<StackGresDistributedLogsContext> getExistingConfigs(
      KubernetesClient client) {
    return ResourceUtil.getCustomResource(client, StackGresDistributedLogsDefinition.NAME)
        .map(crd -> client
            .customResources(crd,
                StackGresDistributedLogs.class,
                StackGresDistributedLogsList.class,
                StackGresDistributedLogsDoneable.class)
            .inAnyNamespace()
            .list()
            .getItems()
            .stream()
            .map(distributedLogs -> getDistributedLogsContext(distributedLogs, client))
            .collect(ImmutableList.toImmutableList()))
        .orElseThrow(() -> new IllegalStateException("StackGres is not correctly installed:"
            + " CRD " + StackGresDistributedLogsDefinition.NAME + " not found."));
  }

  private StackGresDistributedLogsContext getDistributedLogsContext(
      StackGresDistributedLogs distributedLogs, KubernetesClient client) {
    final StackGresCluster cluster = getStackGresCLusterForDistributedLogs(distributedLogs);
    return ImmutableStackGresDistributedLogsContext.builder()
        .operatorContext(operatorContext)
        .distributedLogs(distributedLogs)
        .connectedClusters(getConnectedClusters(distributedLogs, client))
        .cluster(cluster)
        .backupContext(Optional.empty())
        .restoreContext(Optional.empty())
        .postgresConfig(Optional.empty())
        .profile(Optional.empty())
        .prometheus(Optional.empty())
        .labels(labelFactory.clusterLabels(cluster))
        .clusterNamespace(labelFactory.clusterNamespace(cluster))
        .clusterName(labelFactory.clusterName(cluster))
        .clusterKey(labelFactory.getLabelMapper().clusterKey())
        .backupKey(labelFactory.getLabelMapper().backupKey())
        .ownerReferences(ImmutableList.of(ResourceUtil.getOwnerReference(distributedLogs)))
        .addBackups()
        .addSidecars(new SidecarEntry<>(
            fluentd.toStackGresClusterSidecarResourceFactory(),
            Optional.empty()))
        .build();
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
    spec.setPostgresVersion(StackGresComponents.LATEST);
    spec.setInstances(1);
    final StackGresClusterPod pod = new StackGresClusterPod();
    final StackGresPodPersistentVolume persistentVolume = new StackGresPodPersistentVolume();
    persistentVolume.setVolumeSize(
        distributedLogs.getSpec().getPersistentVolume().getVolumeSize());
    persistentVolume.setStorageClass(
        distributedLogs.getSpec().getPersistentVolume().getStorageClass());
    pod.setPersistentVolume(persistentVolume);
    spec.setPod(pod);
    final StackGresClusterInitData initData = new StackGresClusterInitData();
    final StackGresClusterScript script = new StackGresClusterScript();
    script.setName("distributed-logs-template");
    script.setDatabase("template1");
    script.setValue(Unchecked.supplier(() -> Resources
          .asCharSource(ClusterStatefulSet.class.getResource("/distributed-logs-template.sql"),
              StandardCharsets.UTF_8)
          .read()).get());
    initData.setScripts(ImmutableList.of(script));
    spec.setInitData(initData);
    final NonProduction nonProduction = new NonProduction();
    nonProduction.setDisableClusterPodAntiAffinity(
        distributedLogs.getSpec().getNonProduction().getDisableClusterPodAntiAffinity());
    spec.setNonProduction(nonProduction);
    distributedLogsCluster.setSpec(spec);
    return distributedLogsCluster;
  }

  private ImmutableList<StackGresCluster> getConnectedClusters(
      StackGresDistributedLogs distributedLogs, KubernetesClient client) {
    final String namespace = distributedLogs.getMetadata().getNamespace();
    final String name = distributedLogs.getMetadata().getName();
    return ResourceUtil.getCustomResource(client, StackGresClusterDefinition.NAME)
        .map(crd -> client
            .customResources(crd,
                StackGresCluster.class,
                StackGresClusterList.class,
                StackGresClusterDoneable.class)
            .inAnyNamespace()
            .list()
            .getItems()
            .stream()
            .filter(cluster -> Optional.ofNullable(cluster.getSpec().getDistributedLogs())
                .map(StackGresClusterDistributedLogs::getDistributedLogs)
                .map(distributedLogsRelativeId -> StackGresUtil.getNamespaceFromRelativeId(
                    distributedLogsRelativeId,
                    cluster.getMetadata().getNamespace()).equals(namespace)
                    && StackGresUtil.getNameFromRelativeId(
                        distributedLogsRelativeId).equals(name))
                .orElse(false))
            .collect(ImmutableList.toImmutableList()))
        .orElse(ImmutableList.of());
  }
}

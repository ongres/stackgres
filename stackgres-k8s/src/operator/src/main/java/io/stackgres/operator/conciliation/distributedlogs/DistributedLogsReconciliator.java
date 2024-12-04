/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.distributedlogs;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.EndpointSubsetBuilder;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.Condition;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgdistributedlogs.DistributedLogsEventReason;
import io.stackgres.common.crd.sgdistributedlogs.DistributedLogsStatusCondition;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsStatus;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsStatusCluster;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigSpec;
import io.stackgres.common.event.EventEmitter;
import io.stackgres.common.labels.LabelFactoryForDistributedLogs;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceWriter;
import io.stackgres.operator.app.OperatorLockHolder;
import io.stackgres.operator.conciliation.AbstractConciliator;
import io.stackgres.operator.conciliation.AbstractReconciliator;
import io.stackgres.operator.conciliation.DeployedResourcesCache;
import io.stackgres.operator.conciliation.HandlerDelegator;
import io.stackgres.operator.conciliation.ReconciliationResult;
import io.stackgres.operator.conciliation.ReconciliatorWorkerThreadPool;
import io.stackgres.operator.conciliation.StatusManager;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.helpers.MessageFormatter;

@ApplicationScoped
public class DistributedLogsReconciliator extends AbstractReconciliator<StackGresDistributedLogs> {

  @Dependent
  static class Parameters {
    @Inject CustomResourceScanner<StackGresDistributedLogs> scanner;
    @Inject CustomResourceFinder<StackGresDistributedLogs> finder;
    @Inject AbstractConciliator<StackGresDistributedLogs> conciliator;
    @Inject DeployedResourcesCache deployedResourcesCache;
    @Inject HandlerDelegator<StackGresDistributedLogs> handlerDelegator;
    @Inject KubernetesClient client;
    @Inject ConnectedClustersScanner connectedClustersScanner;
    @Inject CustomResourceScheduler<StackGresDistributedLogs> distributedLogsScheduler;
    @Inject StatusManager<StackGresDistributedLogs, Condition> statusManager;
    @Inject EventEmitter<StackGresDistributedLogs> eventController;
    @Inject OperatorLockHolder operatorLockReconciliator;
    @Inject ReconciliatorWorkerThreadPool reconciliatorWorkerThreadPool;
    @Inject LabelFactoryForDistributedLogs labelFactory;
    @Inject CustomResourceScanner<StackGresCluster> clusterScanner;
    @Inject CustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder;
    @Inject ResourceFinder<ConfigMap> configMapFinder;
    @Inject ResourceWriter<ConfigMap> configMapWriter;
    @Inject ResourceFinder<Pod> podFinder;
    @Inject ResourceFinder<Endpoints> endpointsFinder;
    @Inject ResourceWriter<Endpoints> endpointsWriter;
  }

  private final ConnectedClustersScanner connectedClustersScanner;
  private final CustomResourceScheduler<StackGresDistributedLogs> distributedLogsScheduler;
  private final StatusManager<StackGresDistributedLogs, Condition> statusManager;
  private final EventEmitter<StackGresDistributedLogs> eventController;
  private final LabelFactoryForDistributedLogs labelFactory;
  private final CustomResourceScanner<StackGresCluster> clusterScanner;
  private final CustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder;
  private final ResourceFinder<ConfigMap> configMapFinder;
  private final ResourceWriter<ConfigMap> configMapWriter;
  private final ResourceFinder<Pod> podFinder;
  private final ResourceFinder<Endpoints> endpointsFinder;
  private final ResourceWriter<Endpoints> endpointsWriter;

  @Inject
  public DistributedLogsReconciliator(Parameters parameters) {
    super(parameters.scanner, parameters.finder,
        parameters.conciliator, parameters.deployedResourcesCache,
        parameters.handlerDelegator, parameters.client,
        parameters.operatorLockReconciliator,
        parameters.reconciliatorWorkerThreadPool,
        StackGresDistributedLogs.KIND);
    this.connectedClustersScanner = parameters.connectedClustersScanner;
    this.distributedLogsScheduler = parameters.distributedLogsScheduler;
    this.statusManager = parameters.statusManager;
    this.eventController = parameters.eventController;
    this.labelFactory = parameters.labelFactory;
    this.clusterScanner = parameters.clusterScanner;
    this.postgresConfigFinder = parameters.postgresConfigFinder;
    this.configMapFinder = parameters.configMapFinder;
    this.configMapWriter = parameters.configMapWriter;
    this.podFinder = parameters.podFinder;
    this.endpointsFinder = parameters.endpointsFinder;
    this.endpointsWriter = parameters.endpointsWriter;
  }

  void onStart(@Observes StartupEvent ev) {
    start();
  }

  void onStop(@Observes ShutdownEvent ev) {
    stop();
  }

  @Override
  protected void reconciliationCycle(StackGresDistributedLogs configKey, int retry, boolean load) {
    super.reconciliationCycle(configKey, retry, load);
  }

  @Override
  protected void onPreReconciliation(StackGresDistributedLogs config) {
  }

  @Override
  protected void onPostReconciliation(StackGresDistributedLogs config) {
    Optional<StackGresCluster> foundCluster = clusterScanner.getResourcesWithLabels(
        config.getMetadata().getNamespace(),
        labelFactory.genericLabels(config))
        .stream()
        .filter(cluster -> Objects.equals(
            cluster.getMetadata().getName(),
            config.getMetadata().getName()))
        .findFirst();
    Optional<StackGresPostgresConfig> foundPostgresConfig = postgresConfigFinder.findByNameAndNamespace(
        config.getSpec().getConfigurations().getSgPostgresConfig(),
        config.getMetadata().getNamespace());
    foundCluster.ifPresent(cluster -> distributedLogsScheduler
        .update(config, foundConfig -> {
          setVersionFromCluster(cluster, foundConfig);
          setClusterConfigurationIfMajorVersionMismatch(foundPostgresConfig, cluster, foundConfig);
        }));

    if (StackGresVersion.getStackGresVersionAsNumber(config) <= StackGresVersion.V_1_14.getVersionAsNumber()) {
      if (!Optional.of(config)
          .map(StackGresDistributedLogs::getStatus)
          .map(StackGresDistributedLogsStatus::getOldConfigMapRemoved)
          .orElse(false)) {
        configMapFinder.findByNameAndNamespace(
            config.getMetadata().getName(),
            config.getMetadata().getNamespace())
            .ifPresent(resource -> {
              if (resource.getMetadata().getOwnerReferences().stream()
                  .anyMatch(ResourceUtil.getControllerOwnerReference(config)::equals)) {
                configMapWriter.delete(resource);
              }
              if (config.getStatus() == null) {
                config.setStatus(new StackGresDistributedLogsStatus());
              }
              config.getStatus().setOldConfigMapRemoved(true);
            });
      }
      var foundEndpoints = endpointsFinder.findByNameAndNamespace(
          PatroniUtil.readWriteNameForDistributedLogs(config.getMetadata().getName()),
          config.getMetadata().getNamespace());
      foundEndpoints.ifPresent(endpoints -> {
        var foundPod = podFinder.findByNameAndNamespace(
            config.getMetadata().getName() + "-0",
            config.getMetadata().getNamespace());
        foundPod.ifPresent(pod -> {
          try {
            endpointsWriter.update(endpoints, currentEndpoints -> {
              currentEndpoints.setSubsets(List.of(
                  new EndpointSubsetBuilder()
                  .addNewAddress()
                  .withHostname(pod.getMetadata().getName())
                  .withIp(pod.getStatus().getPodIP())
                  .withNodeName(pod.getSpec().getNodeName())
                  .withNewTargetRef()
                  .withKind(pod.getKind())
                  .withName(pod.getMetadata().getName())
                  .withNamespace(pod.getMetadata().getNamespace())
                  .withUid(pod.getMetadata().getUid())
                  .endTargetRef()
                  .endAddress()
                  .addNewPort()
                  .withProtocol("TCP")
                  .withName(EnvoyUtil.POSTGRES_PORT_NAME)
                  .withPort(EnvoyUtil.PG_PORT)
                  .endPort()
                  .addNewPort()
                  .withProtocol("TCP")
                  .withName(EnvoyUtil.POSTGRES_REPLICATION_PORT_NAME)
                  .withPort(EnvoyUtil.PG_REPL_ENTRY_PORT)
                  .endPort()
                  .build()));
            });
          } catch (Exception ex) {
            LOGGER.error("Error while updating SGDistributedLogs endpoints", ex);
          }
        });
      });
    }

    refreshConnectedClusters(config);

    statusManager.refreshCondition(config);
    distributedLogsScheduler.update(config,
        (currentDistributedLogs) -> currentDistributedLogs.setStatus(config.getStatus()));
  }

  private void setVersionFromCluster(StackGresCluster cluster, StackGresDistributedLogs foundConfig) {
    foundConfig.getMetadata().setAnnotations(
        Seq.of(
            Optional.ofNullable(cluster.getMetadata().getAnnotations())
            .orElse(Map.of()))
        .flatMap(annotations -> Seq.seq(annotations)
            .filter(annotation -> annotation.v1.equals(StackGresContext.VERSION_KEY))
            .append(Stream.of(
                Optional.ofNullable(foundConfig.getMetadata().getAnnotations())
                .orElse(Map.of()))
                .flatMap(existingAnnotations -> Seq.seq(existingAnnotations)
                    .filter(annotation -> !annotations.containsKey(StackGresContext.VERSION_KEY)
                        || !annotation.v1.equals(StackGresContext.VERSION_KEY)))))
        .toMap(Tuple2::v1, Tuple2::v2));
  }

  private void setClusterConfigurationIfMajorVersionMismatch(Optional<StackGresPostgresConfig> foundPostgresConfig,
      StackGresCluster cluster, StackGresDistributedLogs foundConfig) {
    if (foundPostgresConfig
        .map(StackGresPostgresConfig::getSpec)
        .map(StackGresPostgresConfigSpec::getPostgresVersion)
        .flatMap(postgresMajorVersion -> Optional.of(cluster)
            .map(StackGresCluster::getSpec)
            .map(StackGresClusterSpec::getPostgres)
            .map(StackGresClusterPostgres::getVersion)
            .filter(postgresVersion -> postgresVersion.startsWith(postgresMajorVersion + ".")))
        .isEmpty()
        && Optional.of(cluster)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getConfigurations)
        .map(StackGresClusterConfigurations::getSgPostgresConfig)
        .isPresent()) {
      foundConfig.getSpec().getConfigurations().setSgPostgresConfig(
          cluster.getSpec().getConfigurations().getSgPostgresConfig());
    }
  }

  private void refreshConnectedClusters(StackGresDistributedLogs config) {
    var clusters = connectedClustersScanner.getConnectedClusters(config);

    config.setStatus(
        Optional.ofNullable(config.getStatus())
            .orElseGet(StackGresDistributedLogsStatus::new));
    config.getStatus()
        .setConnectedClusters(clusters.stream()
            .map(cluster -> {
              StackGresDistributedLogsStatusCluster connectedCluster =
                  new StackGresDistributedLogsStatusCluster();
              connectedCluster.setNamespace(cluster.getMetadata().getNamespace());
              connectedCluster.setName(cluster.getMetadata().getName());
              connectedCluster.setConfig(cluster.getSpec().getDistributedLogs());
              return connectedCluster;
            })
            .collect(Collectors.toList()));
  }

  @Override
  protected void onConfigCreated(StackGresDistributedLogs distributedLogs,
                              ReconciliationResult result) {
    final ObjectMeta metadata = distributedLogs.getMetadata();
    eventController.sendEvent(DistributedLogsEventReason.DISTRIBUTED_LOGS_CREATED,
        "SGDistributeLogs " + metadata.getNamespace() + "."
            + metadata.getName() + " created",
        distributedLogs);

    statusManager.updateCondition(
        DistributedLogsStatusCondition.FALSE_FAILED.getCondition(), distributedLogs);
  }

  @Override
  protected void onConfigUpdated(StackGresDistributedLogs distributedLogs,
                              ReconciliationResult result) {
    final ObjectMeta metadata = distributedLogs.getMetadata();
    eventController.sendEvent(DistributedLogsEventReason.DISTRIBUTED_LOGS_UPDATED,
        "SGDistributeLogs " + metadata.getNamespace() + "."
            + metadata.getName() + " updated",
        distributedLogs);
    statusManager.updateCondition(
        DistributedLogsStatusCondition.FALSE_FAILED.getCondition(), distributedLogs);
  }

  @Override
  protected void onError(Exception ex, StackGresDistributedLogs context) {
    String message = MessageFormatter.arrayFormat(
        "SGDistributeLogs reconciliation cycle failed",
        new String[]{
        }).getMessage();
    eventController.sendEvent(DistributedLogsEventReason.DISTRIBUTED_LOGS_CONFIG_ERROR,
        message + ": " + ex.getMessage(), context);
  }

}

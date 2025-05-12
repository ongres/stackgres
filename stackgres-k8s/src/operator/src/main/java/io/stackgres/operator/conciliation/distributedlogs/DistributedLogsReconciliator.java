/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.distributedlogs;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.Condition;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgdistributedlogs.DistributedLogsEventReason;
import io.stackgres.common.crd.sgdistributedlogs.DistributedLogsStatusCondition;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigSpec;
import io.stackgres.common.event.EventEmitter;
import io.stackgres.common.labels.LabelFactoryForDistributedLogs;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.app.OperatorLockHolder;
import io.stackgres.operator.common.Metrics;
import io.stackgres.operator.conciliation.AbstractConciliator;
import io.stackgres.operator.conciliation.AbstractReconciliator;
import io.stackgres.operator.conciliation.DeployedResourcesCache;
import io.stackgres.operator.conciliation.HandlerDelegator;
import io.stackgres.operator.conciliation.ReconciliationResult;
import io.stackgres.operator.conciliation.ReconciliatorWorkerThreadPool;
import io.stackgres.operator.conciliation.StatusManager;
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
    @Inject CustomResourceScheduler<StackGresDistributedLogs> distributedLogsScheduler;
    @Inject StatusManager<StackGresDistributedLogs, Condition> statusManager;
    @Inject EventEmitter<StackGresDistributedLogs> eventController;
    @Inject OperatorLockHolder operatorLockReconciliator;
    @Inject ReconciliatorWorkerThreadPool reconciliatorWorkerThreadPool;
    @Inject LabelFactoryForDistributedLogs labelFactory;
    @Inject CustomResourceScanner<StackGresCluster> clusterScanner;
    @Inject CustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder;
    @Inject Metrics metrics;
  }

  private final CustomResourceScheduler<StackGresDistributedLogs> distributedLogsScheduler;
  private final StatusManager<StackGresDistributedLogs, Condition> statusManager;
  private final EventEmitter<StackGresDistributedLogs> eventController;
  private final LabelFactoryForDistributedLogs labelFactory;
  private final CustomResourceScanner<StackGresCluster> clusterScanner;
  private final CustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder;

  @Inject
  public DistributedLogsReconciliator(Parameters parameters) {
    super(parameters.scanner, parameters.finder,
        parameters.conciliator, parameters.deployedResourcesCache,
        parameters.handlerDelegator, parameters.client,
        parameters.operatorLockReconciliator,
        parameters.reconciliatorWorkerThreadPool,
        parameters.metrics,
        StackGresDistributedLogs.KIND);
    this.distributedLogsScheduler = parameters.distributedLogsScheduler;
    this.statusManager = parameters.statusManager;
    this.eventController = parameters.eventController;
    this.labelFactory = parameters.labelFactory;
    this.clusterScanner = parameters.clusterScanner;
    this.postgresConfigFinder = parameters.postgresConfigFinder;
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
    LOGGER.debug("{} {} was {}",
        StackGresCluster.KIND,
        config.getMetadata().getName(),
        foundCluster.isPresent() ? "found" : "not found");
    Optional<StackGresPostgresConfig> foundPostgresConfig = postgresConfigFinder.findByNameAndNamespace(
        config.getSpec().getConfigurations().getSgPostgresConfig(),
        config.getMetadata().getNamespace());
    LOGGER.debug("{} {} was {}",
        StackGresPostgresConfig.KIND,
        config.getSpec().getConfigurations().getSgPostgresConfig(),
        foundPostgresConfig.isPresent() ? "found" : "not found");
    foundCluster.ifPresent(cluster -> distributedLogsScheduler
        .update(config, foundConfig -> {
          setVersionFromCluster(cluster, foundConfig);
          setClusterConfigurationIfMajorVersionMismatch(foundPostgresConfig, cluster, foundConfig);
        }));

    statusManager.refreshCondition(config);
    distributedLogsScheduler.update(config,
        (currentDistributedLogs) -> currentDistributedLogs.setStatus(config.getStatus()));
  }

  private void setVersionFromCluster(StackGresCluster cluster, StackGresDistributedLogs config) {
    String currentVersion = Optional.ofNullable(config.getMetadata().getAnnotations())
        .stream()
        .map(Map::entrySet)
        .flatMap(Set::stream)
        .filter(entry -> entry.getKey().equals(StackGresContext.VERSION_KEY))
        .map(Map.Entry::getValue)
        .findFirst()
        .orElse(null);
    config.getMetadata().setAnnotations(
        Seq.of(
            Optional.ofNullable(cluster.getMetadata().getAnnotations())
            .orElse(Map.of()))
        .flatMap(annotations -> Seq.seq(annotations)
            .filter(annotation -> annotation.v1.equals(StackGresContext.VERSION_KEY))
            .append(Stream.of(
                Optional.ofNullable(config.getMetadata().getAnnotations())
                .orElse(Map.of()))
                .flatMap(existingAnnotations -> Seq.seq(existingAnnotations)
                    .filter(annotation -> !annotations.containsKey(StackGresContext.VERSION_KEY)
                        || !annotation.v1.equals(StackGresContext.VERSION_KEY)))))
        .toMap(Tuple2::v1, Tuple2::v2));
    String updatedVersion = Optional.ofNullable(config.getMetadata().getAnnotations())
        .stream()
        .map(Map::entrySet)
        .flatMap(Set::stream)
        .filter(entry -> entry.getKey().equals(StackGresContext.VERSION_KEY))
        .map(Map.Entry::getValue)
        .findFirst()
        .orElse(null);
    if (!Objects.equals(currentVersion, updatedVersion)) {
      LOGGER.debug("{} {} {} annotation was updated from {} to {}",
          StackGresDistributedLogs.KIND,
          config.getMetadata().getName(),
          StackGresContext.VERSION_KEY,
          currentVersion,
          updatedVersion);
    }
  }

  private void setClusterConfigurationIfMajorVersionMismatch(
      Optional<StackGresPostgresConfig> foundPostgresConfig,
      StackGresCluster cluster,
      StackGresDistributedLogs config) {
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
      LOGGER.debug("Postgres configuration {} (version {}) was updated to {}"
          + " (cluster postgres version {})",
          config.getSpec().getConfigurations().getSgPostgresConfig(),
          foundPostgresConfig
          .map(StackGresPostgresConfig::getSpec)
          .map(StackGresPostgresConfigSpec::getPostgresVersion)
          .orElse(null),
          cluster.getSpec().getConfigurations().getSgPostgresConfig(),
          cluster.getSpec().getPostgres().getVersion());
      config.getSpec().getConfigurations().setSgPostgresConfig(
          cluster.getSpec().getConfigurations().getSgPostgresConfig());
    }
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

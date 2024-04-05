/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.stackgres.cluster.common.ClusterControllerEventReason;
import io.stackgres.cluster.configuration.ClusterControllerPropertyContext;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.ClusterControllerProperty;
import io.stackgres.common.ClusterPath;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StackGresVolume;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceWriter;
import io.stackgres.operatorframework.reconciliation.ReconciliationResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class PatroniBackupFailoverRestartReconciliator {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(PatroniBackupFailoverRestartReconciliator.class);

  private final EventController eventController;
  private final CustomResourceScheduler<StackGresCluster> clusterScheduler;
  private final ResourceFinder<ConfigMap> configMapFinder;
  private final ResourceWriter<PersistentVolumeClaim> pvcWriter;
  private final ResourceWriter<Pod> podWriter;
  private final String podName;
  private final AtomicReference<String> replicationInitializationFailed = new AtomicReference<>();

  @Dependent
  public static class Parameters {
    @Inject EventController eventController;
    @Inject CustomResourceScheduler<StackGresCluster> clusterScheduler;
    @Inject ResourceFinder<ConfigMap> configMapFinder;
    @Inject ResourceWriter<PersistentVolumeClaim> pvcWriter;
    @Inject ResourceWriter<Pod> podWriter;
    @Inject ClusterControllerPropertyContext propertyContext;
  }

  @Inject
  public PatroniBackupFailoverRestartReconciliator(Parameters parameters) {
    this.eventController = parameters.eventController;
    this.clusterScheduler = parameters.clusterScheduler;
    this.configMapFinder = parameters.configMapFinder;
    this.pvcWriter = parameters.pvcWriter;
    this.podWriter = parameters.podWriter;
    this.podName = parameters.propertyContext
        .getString(ClusterControllerProperty.CLUSTER_CONTROLLER_POD_NAME);
  }

  public ReconciliationResult<Void> reconcile(KubernetesClient client, ClusterContext context)
      throws Exception {
    try {
      reconcilePatroniForBackupFailover(context);
    } catch (Exception ex) {
      LOGGER.error("An error occurred while reconciling patroni for backup failover", ex);
      try {
        eventController.sendEvent(ClusterControllerEventReason.CLUSTER_CONTROLLER_ERROR,
            "An error occurred while reconciling patroni for backup failover: "
                + ex.getMessage(),
            client);
      } catch (Exception eventEx) {
        LOGGER.error("An error occurred while sending an event", eventEx);
      }
      return new ReconciliationResult<>(ex);
    }
    return new ReconciliationResult<>();
  }

  private void reconcilePatroniForBackupFailover(ClusterContext context) throws Exception {
    if (replicationInitializationFailed.get() != null
        || Files.exists(Paths.get(ClusterPath.PG_REPLICATION_INITIALIZATION_FAILED_BACKUP_PATH.path()))) {
      final String replicaInitializationBackupName;
      if (replicationInitializationFailed.get() != null) {
        replicaInitializationBackupName = replicationInitializationFailed.get();
      } else {
        replicaInitializationBackupName =
            Files.readString(Paths.get(ClusterPath.PG_REPLICATION_INITIALIZATION_FAILED_BACKUP_PATH.path()));
        replicationInitializationFailed.set(replicaInitializationBackupName);
      }
      LOGGER.info("Replica initialization backup failover detected while trying to restore using SGBackup {}"
          + ", proceed to restart the Pod", replicaInitializationBackupName);
      clusterScheduler.update(context.getCluster(), currentCluster -> {
        if (currentCluster.getStatus() == null) {
          currentCluster.setStatus(new StackGresClusterStatus());
        }
        currentCluster.getStatus().setReplicationInitializationFailedSgBackup(replicaInitializationBackupName);
      });
      LOGGER.info("Waiting for the operator to change the reconciliation initialization backup name");
      var changeForReconciliationInitializationBackupName =
          waitChangeForReconciliationInitializationBackupName(
              context, replicaInitializationBackupName);
      if (!changeForReconciliationInitializationBackupName.isChanged) {
        if (Files.exists(Paths.get(ClusterPath.PG_REPLICATION_INITIALIZATION_FAILED_BACKUP_PATH.path()))) {
          Files.delete(Paths.get(ClusterPath.PG_REPLICATION_INITIALIZATION_FAILED_BACKUP_PATH.path()));
        }
        if (!changeForReconciliationInitializationBackupName.changedBackupName.isPresent()) {
          LOGGER.info("Deleting PVC in order to allow restore from volume snapshot");
          try {
            pvcWriter.delete(new PersistentVolumeClaimBuilder()
                .withNewMetadata()
                .withNamespace(context.getCluster().getMetadata().getNamespace())
                .withName(StackGresUtil.statefulSetDataPersistentVolumeName(context.getCluster()) + "-" + podName)
                .endMetadata()
                .build());
          } catch (KubernetesClientException ex) {
            if (ex.getCode() == 404) {
              LOGGER.warn("PVC already deleted");
            } else {
              throw ex;
            }
          }
        }
        podWriter.delete(new PodBuilder()
            .withNewMetadata()
            .withNamespace(context.getCluster().getMetadata().getNamespace())
            .withName(podName)
            .endMetadata()
            .build());
        replicationInitializationFailed.set(null);
      } else {
        LOGGER.warn("Replica initialization backup failover aborted due to timeout waiting for the operator"
            + " to change the replicaiton initialization backup from {}. Will retry...",
            replicaInitializationBackupName);
      }
    }
  }

  record ChangeForReconciliationInitializationBackupName(
      boolean isChanged,
      Optional<String> changedBackupName) {};

  private ChangeForReconciliationInitializationBackupName waitChangeForReconciliationInitializationBackupName(
      ClusterContext context,
      String replicaInitializationBackupName) {
    Instant start = Instant.now();
    Instant later = start.plus(Duration.ofSeconds(30));
    while (later.isAfter(Instant.now())) {
      var configMap = configMapFinder.findByNameAndNamespace(
          StackGresVolume.REPLICATION_INITIALIZATION_ENV.getResourceName(
              context.getCluster().getMetadata().getName()),
          context.getCluster().getMetadata().getNamespace());
      Optional<String> configMapReconciliationInitializationBackup = configMap
          .map(ConfigMap::getData)
          .map(Map::entrySet)
          .stream()
          .flatMap(Set::stream)
          .filter(entry -> Objects.equals(
              entry.getKey(),
              PatroniUtil.REPLICATION_INITIALIZATION_BACKUP))
          .map(Map.Entry::getValue)
          .findAny();
      if (configMapReconciliationInitializationBackup.isPresent()
          && configMapReconciliationInitializationBackup
          .filter(replicaInitializationBackupName::equals)
          .isEmpty()) {
        return new ChangeForReconciliationInitializationBackupName(
            true, configMapReconciliationInitializationBackup);
      }
    }
    return new ChangeForReconciliationInitializationBackupName(
        false, Optional.empty());
  }

}

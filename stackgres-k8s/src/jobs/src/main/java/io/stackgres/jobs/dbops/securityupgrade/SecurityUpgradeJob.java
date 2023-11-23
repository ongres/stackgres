/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.securityupgrade;

import java.time.Duration;
import java.util.Map;

import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.smallrye.mutiny.Uni;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresProperty;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSecurityUpgradeStatus;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsStatus;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceWriter;
import io.stackgres.jobs.dbops.ClusterRestartStateHandler;
import io.stackgres.jobs.dbops.DatabaseOperation;
import io.stackgres.jobs.dbops.DatabaseOperationJob;
import io.stackgres.jobs.dbops.DbOpsExecutorService;
import io.stackgres.jobs.dbops.MutinyUtil;
import io.stackgres.jobs.dbops.StateHandler;
import io.stackgres.jobs.dbops.clusterrestart.ClusterRestartState;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@DatabaseOperation("securityUpgrade")
public class SecurityUpgradeJob implements DatabaseOperationJob {

  private static final Logger LOGGER = LoggerFactory.getLogger(SecurityUpgradeJob.class);

  @Inject
  CustomResourceFinder<StackGresCluster> clusterFinder;

  @Inject
  CustomResourceScheduler<StackGresCluster> clusterScheduler;

  @Inject
  ResourceFinder<StatefulSet> statefulSetFinder;

  @Inject
  ResourceWriter<StatefulSet> statefulSetWriter;

  @Inject
  @StateHandler("securityUpgrade")
  ClusterRestartStateHandler restartStateHandler;

  @Inject
  CustomResourceScheduler<StackGresDbOps> dbOpsScheduler;

  @Inject
  CustomResourceFinder<StackGresDbOps> dbOpsFinder;

  @Inject
  DbOpsExecutorService executorService;

  @Override
  public Uni<ClusterRestartState> runJob(StackGresDbOps dbOps, StackGresCluster cluster) {
    LOGGER.info("Starting security upgrade for SGDbOps {}", dbOps.getMetadata().getName());

    return pauseReconciliationAndUpgradeCluster(cluster)
        .chain(this::upgradeSts)
        .chain(this::resumeReconciliation)
        .chain(() -> restartStateHandler.restartCluster(dbOps))
        .onItemOrFailure()
        .transformToUni((item, ex) -> {
          if (ex != null) {
            return executorService.invokeAsync(() -> reportFailure(dbOps, ex))
                .onItem()
                .failWith(() -> ex)
                .map(ignored -> item);
          }
          return Uni.createFrom().item(item);
        });
  }

  private Uni<StackGresCluster> pauseReconciliationAndUpgradeCluster(
      StackGresCluster targetCluster) {
    return getCluster(targetCluster)
        .map(cluster -> {
          markClusterAsIgnored(cluster);
          upgradeOperatorVersion(cluster);
          return clusterScheduler.update(cluster);
        })
        .onFailure()
        .transform(MutinyUtil.logOnFailureToRetry("updating version of SGCluster"))
        .onFailure()
        .retry()
        .withBackOff(Duration.ofMillis(5), Duration.ofSeconds(5))
        .indefinitely();
  }

  private Uni<StackGresCluster> upgradeSts(StackGresCluster cluster) {
    return getSts(cluster)
        .chain(this::deleteSts)
        .map(ignored -> cluster)
        .onFailure()
        .transform(MutinyUtil.logOnFailureToRetry("updating the StatefulSet"))
        .onFailure()
        .retry()
        .withBackOff(Duration.ofMillis(5), Duration.ofSeconds(5))
        .atMost(10);
  }

  private Uni<StackGresCluster> resumeReconciliation(StackGresCluster targetCluster) {
    return getCluster(targetCluster)
        .map(cluster -> {
          removeIgnoredMark(cluster);
          return clusterScheduler.update(cluster);
        })
        .onFailure()
        .transform(MutinyUtil.logOnFailureToRetry("updating SGCluster"))
        .onFailure()
        .retry()
        .withBackOff(Duration.ofMillis(5), Duration.ofSeconds(5))
        .indefinitely();
  }

  private Uni<StackGresCluster> getCluster(StackGresCluster targetCluster) {
    return executorService.itemAsync(() -> {
      String name = targetCluster.getMetadata().getName();
      String namespace = targetCluster.getMetadata().getNamespace();
      return clusterFinder.findByNameAndNamespace(name, namespace)
          .orElseThrow(() -> new IllegalStateException("Could not find SGCluster " + name));
    });
  }

  private Uni<StatefulSet> getSts(StackGresCluster targetCluster) {
    return executorService.itemAsync(() -> {
      String name = targetCluster.getMetadata().getName();
      String namespace = targetCluster.getMetadata().getNamespace();
      return statefulSetFinder.findByNameAndNamespace(name, namespace)
          .orElseThrow(() -> new IllegalStateException(
              "Could not find StatefulSet of SGCluster " + name));
    });
  }

  private Uni<Void> deleteSts(StatefulSet sts) {
    return executorService.invokeAsync(() -> statefulSetWriter.deleteWithoutCascading(sts));
  }

  private void reportFailure(StackGresDbOps dbOps, Throwable ex) {
    String message = ex.getMessage();
    String dbOpsName = dbOps.getMetadata().getName();
    String namespace = dbOps.getMetadata().getNamespace();

    dbOpsFinder.findByNameAndNamespace(dbOpsName, namespace)
        .ifPresent(savedDbOps -> {
          if (savedDbOps.getStatus() == null) {
            savedDbOps.setStatus(new StackGresDbOpsStatus());
          }

          if (savedDbOps.getStatus().getSecurityUpgrade() == null) {
            savedDbOps.getStatus().setSecurityUpgrade(new StackGresDbOpsSecurityUpgradeStatus());
          }

          savedDbOps.getStatus().getSecurityUpgrade().setFailure(message);

          dbOpsScheduler.update(savedDbOps);
        });
  }

  private StackGresCluster upgradeOperatorVersion(StackGresCluster targetCluster) {
    final Map<String, String> clusterAnnotations = targetCluster.getMetadata().getAnnotations();
    clusterAnnotations
        .put(StackGresContext.VERSION_KEY, StackGresProperty.OPERATOR_VERSION.getString());
    return targetCluster;
  }

  private void markClusterAsIgnored(StackGresCluster cluster) {
    final Map<String, String> clusterAnnotations = cluster.getMetadata().getAnnotations();
    clusterAnnotations.put(StackGresContext.RECONCILIATION_PAUSE_KEY, StackGresContext.RIGHT_VALUE);
  }

  private void removeIgnoredMark(StackGresCluster cluster) {
    final Map<String, String> clusterAnnotations = cluster.getMetadata().getAnnotations();
    clusterAnnotations.remove(StackGresContext.RECONCILIATION_PAUSE_KEY);
  }
}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.securityupgrade;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

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
import io.stackgres.jobs.dbops.StateHandler;
import io.stackgres.jobs.dbops.clusterrestart.ClusterRestartState;
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

  @Override
  public Uni<ClusterRestartState> runJob(StackGresDbOps dbOps, StackGresCluster cluster) {
    LOGGER.info("Starting security upgrade for SGDbOps {}", dbOps.getMetadata().getName());

    return upgradeClusterAndPauseReconciliation(cluster)
        .chain(this::upgradeSts)
        .chain(this::resumeReconciliation)
        .chain(() -> restartStateHandler.restartCluster(dbOps))
        .onFailure().invoke(ex -> reportFailure(dbOps, ex));
  }

  private Uni<StackGresCluster> upgradeClusterAndPauseReconciliation(
      StackGresCluster targetCluster) {
    return getCluster(targetCluster)
        .map(cluster -> {
          upgradeOperatorVersion(cluster);
          markClusterAsIgnored(cluster);
          return clusterScheduler.update(cluster);
        })
        .onFailure()
        .retry()
        .withBackOff(Duration.ofMillis(5), Duration.ofSeconds(5))
        .indefinitely();
  }

  private Uni<StackGresCluster> upgradeSts(StackGresCluster cluster) {
    return Uni.createFrom().<StackGresCluster>emitter(em -> {
      getSts(cluster)
          .chain(this::deleteSts)
          .subscribe().with((v) -> em.complete(cluster), em::fail);
    })
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
        .retry()
        .withBackOff(Duration.ofMillis(5), Duration.ofSeconds(5))
        .indefinitely();
  }

  private Uni<StackGresCluster> getCluster(StackGresCluster targetCluster) {
    return Uni.createFrom().<StackGresCluster>emitter(em -> {
      String name = targetCluster.getMetadata().getName();
      String namespace = targetCluster.getMetadata().getNamespace();
      Optional<StackGresCluster> cluster = clusterFinder.findByNameAndNamespace(name, namespace);
      if (cluster.isPresent()) {
        em.complete(cluster.get());
      } else {
        em.fail(new IllegalStateException("Could not find SGCluster " + name));
      }
    });
  }

  private Uni<StatefulSet> getSts(StackGresCluster targetCluster) {
    return Uni.createFrom().<StatefulSet>emitter(em -> {
      String name = targetCluster.getMetadata().getName();
      String namespace = targetCluster.getMetadata().getNamespace();
      Optional<StatefulSet> sts = statefulSetFinder.findByNameAndNamespace(name, namespace);
      if (sts.isPresent()) {
        em.complete(sts.get());
      } else {
        em.fail(new IllegalStateException("Could not find StatefulSet of SGCluster " + name));
      }
    });
  }

  private Uni<Void> deleteSts(StatefulSet sts) {
    return Uni.createFrom().emitter(em -> {
      statefulSetWriter.delete(sts);
      em.complete(null);
    });
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

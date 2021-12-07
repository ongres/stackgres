/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.minorversionupgrade;

import java.time.Duration;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.smallrye.mutiny.Uni;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsMinorVersionUpgradeStatus;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsStatus;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.jobs.dbops.ClusterRestartStateHandler;
import io.stackgres.jobs.dbops.DatabaseOperation;
import io.stackgres.jobs.dbops.DatabaseOperationJob;
import io.stackgres.jobs.dbops.StateHandler;
import io.stackgres.jobs.dbops.clusterrestart.ClusterRestartState;
import io.stackgres.jobs.dbops.securityupgrade.SecurityUpgradeJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@DatabaseOperation("minorVersionUpgrade")
public class MinorVersionUpgradeJob implements DatabaseOperationJob {

  private static final Logger LOGGER = LoggerFactory.getLogger(SecurityUpgradeJob.class);

  @Inject
  CustomResourceFinder<StackGresCluster> clusterFinder;

  @Inject
  CustomResourceScheduler<StackGresCluster> clusterScheduler;

  @Inject
  @StateHandler("minorVersionUpgrade")
  ClusterRestartStateHandler restartStateHandler;

  @Inject
  CustomResourceScheduler<StackGresDbOps> dbOpsScheduler;

  @Inject
  CustomResourceFinder<StackGresDbOps> dbOpsFinder;

  @Override
  public Uni<ClusterRestartState> runJob(StackGresDbOps dbOps, StackGresCluster cluster) {
    LOGGER.info("Starting minor version upgrade for SGDbOps {}", dbOps.getMetadata().getName());

    return setClusterTargetMinorVersion(dbOps, cluster)
        .chain(() -> restartStateHandler.restartCluster(dbOps))
        .onFailure().invoke(ex -> reportFailure(dbOps, ex));
  }

  private Uni<StackGresCluster> setClusterTargetMinorVersion(
      StackGresDbOps dbOps, StackGresCluster targetCluster) {
    return getCluster(targetCluster)
        .map(cluster -> {
          setTargetMinorVersion(dbOps, cluster);
          return clusterScheduler.update(cluster);
        })
        .onFailure()
        .retry()
        .withBackOff(Duration.ofMillis(5), Duration.ofSeconds(5))
        .atMost(10);
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

  private StackGresCluster setTargetMinorVersion(StackGresDbOps dbOps,
      StackGresCluster targetCluster) {
    targetCluster.getSpec().getPostgres().setVersion(
        dbOps.getSpec().getMinorVersionUpgrade().getPostgresVersion());
    return targetCluster;
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

          if (savedDbOps.getStatus().getMinorVersionUpgrade() == null) {
            savedDbOps.getStatus().setMinorVersionUpgrade(
                new StackGresDbOpsMinorVersionUpgradeStatus());
          }

          savedDbOps.getStatus().getMinorVersionUpgrade().setFailure(message);

          dbOpsScheduler.update(savedDbOps);
        });
  }

}

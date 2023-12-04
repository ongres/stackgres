/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.securityupgrade;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.ObjectMeta;
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
import io.stackgres.jobs.dbops.ClusterRestartStateHandler;
import io.stackgres.jobs.dbops.DatabaseOperation;
import io.stackgres.jobs.dbops.DatabaseOperationJob;
import io.stackgres.jobs.dbops.MutinyUtil;
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
  @StateHandler("securityUpgrade")
  ClusterRestartStateHandler restartStateHandler;

  @Inject
  CustomResourceScheduler<StackGresDbOps> dbOpsScheduler;

  @Inject
  CustomResourceFinder<StackGresDbOps> dbOpsFinder;

  @Override
  public Uni<ClusterRestartState> runJob(StackGresDbOps dbOps, StackGresCluster cluster) {
    LOGGER.info("Starting security upgrade for SGDbOps {}", dbOps.getMetadata().getName());

    return upgradeCluster(cluster)
        .call(() -> waitStatefulSetUpgrade(cluster))
        .chain(() -> restartStateHandler.restartCluster(dbOps))
        .onFailure().invoke(ex -> reportFailure(dbOps, ex));
  }

  private Uni<StackGresCluster> upgradeCluster(
      StackGresCluster targetCluster) {
    return getCluster(targetCluster)
        .map(cluster -> {
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

  private Uni<Void> waitStatefulSetUpgrade(
      StackGresCluster targetCluster) {
    return isClusterStatefulSetUpgraded(targetCluster)
        .onFailure()
        .transform(MutinyUtil.logOnFailureToRetry("waiting updated version of StatefulSet"))
        .onFailure()
        .retry()
        .withBackOff(Duration.ofMillis(5), Duration.ofSeconds(5))
        .indefinitely()
        .replaceWithVoid();
  }

  private Uni<StatefulSet> isClusterStatefulSetUpgraded(StackGresCluster targetCluster) {
    return Uni.createFrom().<StatefulSet>emitter(em -> {
      String name = targetCluster.getMetadata().getName();
      String namespace = targetCluster.getMetadata().getNamespace();
      Optional<StatefulSet> statefulSet = statefulSetFinder.findByNameAndNamespace(name, namespace);
      String version = statefulSet
          .map(StatefulSet::getMetadata)
          .map(ObjectMeta::getAnnotations)
          .map(annotations -> annotations.get(StackGresContext.VERSION_KEY))
          .orElse(null);
      if (statefulSet.isPresent()) {
        if (Objects.equals(version, StackGresProperty.OPERATOR_VERSION.getString())) {
          em.complete(statefulSet.get());
        } else {
          em.fail(new IllegalStateException(
              "StatefulSet " + name + " still at version " + version));
        }
      } else {
        em.fail(new IllegalStateException("StatefulSet " + name + " not found"));
      }
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

}

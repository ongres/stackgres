/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.securityupgrade;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.smallrye.mutiny.Uni;
import io.stackgres.common.crd.sgcluster.ClusterDbOpsRestartStatus;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsSecurityUpgradeStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.crd.sgdbops.DbOpsMethodType;
import io.stackgres.common.crd.sgdbops.DbOpsRestartStatus;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSecurityUpgrade;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSecurityUpgradeStatus;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSpec;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsStatus;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.jobs.dbops.AbstractRestartStateHandler;
import io.stackgres.jobs.dbops.DbOpsExecutorService;
import io.stackgres.jobs.dbops.StateHandler;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@StateHandler("securityUpgrade")
public class SecurityUpgradeStateHandler extends AbstractRestartStateHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRestartStateHandler.class);

  @Inject
  LabelFactoryForCluster labelFactory;

  @Inject
  ResourceScanner<Pod> podScanner;

  @Inject
  DbOpsExecutorService executorService;

  @Override
  protected DbOpsRestartStatus getDbOpRestartStatus(StackGresDbOps dbOps) {
    return Optional.ofNullable(dbOps.getStatus())
        .map(StackGresDbOpsStatus::getSecurityUpgrade)
        .orElseGet(() -> {
          if (dbOps.getStatus() == null) {
            dbOps.setStatus(new StackGresDbOpsStatus());
          }
          dbOps.getStatus().setSecurityUpgrade(new StackGresDbOpsSecurityUpgradeStatus());

          return dbOps.getStatus().getSecurityUpgrade();
        });
  }

  @Override
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void setDbOpRestartStatus(StackGresDbOps dbOps, DbOpsRestartStatus dbOpsStatus) {
    dbOps.getStatus().setSecurityUpgrade((StackGresDbOpsSecurityUpgradeStatus) dbOpsStatus);
  }

  @Override
  protected ClusterDbOpsRestartStatus getClusterRestartStatus(StackGresCluster cluster) {
    return Optional.ofNullable(cluster.getStatus())
        .map(StackGresClusterStatus::getDbOps)
        .map(StackGresClusterDbOpsStatus::getSecurityUpgrade)
        .orElseGet(() -> {
          if (cluster.getStatus() == null) {
            cluster.setStatus(new StackGresClusterStatus());
          }
          if (cluster.getStatus().getDbOps() == null) {
            cluster.getStatus().setDbOps(new StackGresClusterDbOpsStatus());
          }
          if (cluster.getStatus().getDbOps().getSecurityUpgrade() == null) {
            cluster.getStatus().getDbOps()
                .setSecurityUpgrade(new StackGresClusterDbOpsSecurityUpgradeStatus());
          }
          return cluster.getStatus().getDbOps().getSecurityUpgrade();
        });
  }

  @Override
  protected void cleanClusterStatus(StackGresCluster cluster) {
    cluster.getStatus().setDbOps(null);
  }

  @Override
  protected boolean isSgClusterDbOpsStatusInitialized(StackGresCluster cluster) {
    return Optional.ofNullable(cluster.getStatus())
        .map(StackGresClusterStatus::getDbOps)
        .map(StackGresClusterDbOpsStatus::getSecurityUpgrade)
        .filter(status -> status.getInitialInstances() != null
            && !status.getInitialInstances().isEmpty())
        .isPresent();
  }

  @Override
  protected boolean isDbOpsStatusInitialized(StackGresDbOps cluster) {
    return Optional.ofNullable(cluster.getStatus())
        .map(StackGresDbOpsStatus::getSecurityUpgrade)
        .filter(status -> status.getInitialInstances() != null
            && !status.getInitialInstances().isEmpty()
            && status.getPrimaryInstance() != null)
        .isPresent();
  }

  @Override
  protected Optional<DbOpsMethodType> getRestartMethod(StackGresDbOps op) {
    return Optional.ofNullable(op.getSpec())
        .map(StackGresDbOpsSpec::getSecurityUpgrade)
        .map(StackGresDbOpsSecurityUpgrade::getMethod)
        .map(DbOpsMethodType::fromString);
  }

  @Override
  protected Uni<List<Pod>> scanClusterPods(StackGresCluster cluster) {
    return executorService.itemAsync(() -> {
      String namespace = cluster.getMetadata().getNamespace();
      List<Pod> clusterPods = podScanner.getResourcesInNamespace(namespace)
          .stream()
          .filter(pod -> ResourceUtil.getNameWithIndexPattern(cluster.getMetadata().getName())
              .matcher(pod.getMetadata().getName())
              .find())
          .toList();
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Retrieved cluster pods with name following pattern {}: {}",
            ResourceUtil.getNameWithIndexPatternString(cluster.getMetadata().getName()),
            clusterPods.stream()
            .map(HasMetadata::getMetadata)
            .map(ObjectMeta::getName)
            .collect(Collectors.joining(" ")));
        List<Pod> allPods = podScanner.getResourcesInNamespace(namespace);
        LOGGER.trace("Found pods with labels: {}",
            allPods.stream()
            .map(HasMetadata::getMetadata)
            .map(metadata -> metadata.getName() + ":"
                + Optional.ofNullable(metadata.getLabels())
                .map(Map::entrySet)
                .stream()
                .flatMap(Set::stream)
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(",")))
            .collect(Collectors.joining(" ")));
      }
      return clusterPods;
    });
  }

}

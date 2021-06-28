/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.minorversionupgrade;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Pod;
import io.smallrye.mutiny.Uni;
import io.stackgres.common.crd.sgcluster.ClusterDbOpsRestartStatus;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsMinorVersionUpgradeStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.crd.sgdbops.DbOpsRestartStatus;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsMinorVersionUpgrade;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsMinorVersionUpgradeStatus;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSpec;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsStatus;
import io.stackgres.jobs.dbops.AbstractRestartStateHandler;
import io.stackgres.jobs.dbops.StateHandler;
import io.stackgres.jobs.dbops.clusterrestart.PatroniApiHandler;
import io.stackgres.jobs.dbops.clusterrestart.PatroniInformation;

@ApplicationScoped
@StateHandler("minorVersionUpgrade")
public class MinorVersionUpgradeRestartStateHandlerImpl extends AbstractRestartStateHandler {

  @Inject
  PatroniApiHandler patroniApi;

  private static String convertToPostgresVersion(Integer serverVersion) {
    int majorVersion = serverVersion / 10000;
    int minorVersion = serverVersion % 10000;

    return String.format("%d.%d", majorVersion, minorVersion);
  }

  private Uni<String> getTargetPostgresVersion(StackGresCluster cluster) {
    return Uni.createFrom().item(cluster.getSpec().getPostgresVersion());
  }

  private Uni<String> getSourcePostgresVersion(StackGresCluster cluster) {
    String clusterName = cluster.getMetadata().getName();
    String namespace = cluster.getMetadata().getNamespace();
    return patroniApi.getMembersPatroniInformation(clusterName, namespace)
        .onItem().transform(patronis ->
            patronis.stream()
                .map(PatroniInformation::getServerVersion)
                .min(Integer::compareTo)
                .map(MinorVersionUpgradeRestartStateHandlerImpl::convertToPostgresVersion)
                .orElseThrow());
  }

  @Override
  protected Uni<StackGresCluster> initRestartStatusValues(StackGresCluster cluster,
                                                          List<Pod> pods) {

    return super.initRestartStatusValues(cluster, pods)
        .chain(ignore -> Uni.combine().all()
            .unis(
                getSourcePostgresVersion(cluster),
                getTargetPostgresVersion(cluster)
            ).asTuple()
        )
        .onItem().transform(versionTuple -> {
          StackGresClusterDbOpsMinorVersionUpgradeStatus restartStatus =
              cluster.getStatus().getDbOps().getMinorVersionUpgrade();
          restartStatus.setSourcePostgresVersion(versionTuple.getItem1());
          restartStatus.setTargetPostgresVersion(versionTuple.getItem2());
          return cluster;
        });
  }

  @Override
  protected void cleanClusterStatus(StackGresCluster cluster) {
    cluster.getStatus().setDbOps(null);
  }

  @Override
  protected DbOpsRestartStatus getDbOpRestartStatus(StackGresDbOps dbOps) {
    return Optional.ofNullable(dbOps.getStatus())
        .map(StackGresDbOpsStatus::getMinorVersionUpgrade)
        .orElseGet(() -> {
          if (dbOps.getStatus() == null) {
            dbOps.setStatus(new StackGresDbOpsStatus());
          }
          dbOps.getStatus().setMinorVersionUpgrade(new StackGresDbOpsMinorVersionUpgradeStatus());

          return dbOps.getStatus().getMinorVersionUpgrade();
        });
  }

  @Override
  protected ClusterDbOpsRestartStatus getClusterRestartStatus(StackGresCluster dbOps) {
    return Optional.ofNullable(dbOps.getStatus())
        .map(StackGresClusterStatus::getDbOps)
        .map(StackGresClusterDbOpsStatus::getMinorVersionUpgrade)
        .orElseGet(() -> {
          if (dbOps.getStatus() == null) {
            dbOps.setStatus(new StackGresClusterStatus());
          }
          if (dbOps.getStatus().getDbOps() == null) {
            dbOps.getStatus().setDbOps(new StackGresClusterDbOpsStatus());
          }
          if (dbOps.getStatus().getDbOps().getMinorVersionUpgrade() == null) {
            dbOps.getStatus().getDbOps()
                .setMinorVersionUpgrade(new StackGresClusterDbOpsMinorVersionUpgradeStatus());
          }
          return dbOps.getStatus().getDbOps().getMinorVersionUpgrade();
        });
  }

  @Override
  protected boolean isSgClusterDbOpsStatusInitialized(StackGresCluster cluster) {
    return Optional.ofNullable(cluster.getStatus())
        .map(StackGresClusterStatus::getDbOps)
        .map(StackGresClusterDbOpsStatus::getMinorVersionUpgrade)
        .filter(status -> status.getPrimaryInstance() != null
            && status.getSourcePostgresVersion() != null
            && status.getTargetPostgresVersion() != null)
        .isPresent();
  }

  @Override
  protected boolean isDbOpsStatusInitialized(StackGresDbOps cluster) {
    return Optional.ofNullable(cluster.getStatus())
        .map(StackGresDbOpsStatus::getMinorVersionUpgrade)
        .filter(status -> status.getPrimaryInstance() != null
            && status.getInitialInstances() != null
            && !status.getInitialInstances().isEmpty())
        .isPresent();
  }

  @Override
  protected Optional<String> getRestartMethod(StackGresDbOps op) {
    return Optional.ofNullable(op.getSpec())
        .map(StackGresDbOpsSpec::getMinorVersionUpgrade)
        .map(StackGresDbOpsMinorVersionUpgrade::getMethod);
  }

}

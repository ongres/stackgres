/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.minorversionupgrade;

import java.util.Optional;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.smallrye.mutiny.Uni;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsMinorVersionUpgradeStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.crd.sgdbops.DbOpsMethodType;
import io.stackgres.common.crd.sgdbops.DbOpsRestartStatus;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsMinorVersionUpgrade;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsMinorVersionUpgradeStatus;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSpec;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsStatus;
import io.stackgres.jobs.dbops.AbstractRestartStateHandler;
import io.stackgres.jobs.dbops.DbOpsExecutorService;
import io.stackgres.jobs.dbops.StateHandler;
import io.stackgres.jobs.dbops.clusterrestart.ClusterRestartState;
import io.stackgres.jobs.dbops.clusterrestart.PatroniApiHandler;
import io.stackgres.jobs.dbops.clusterrestart.PatroniInformation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
@StateHandler("minorVersionUpgrade")
public class MinorVersionUpgradeRestartStateHandler extends AbstractRestartStateHandler {

  @Inject
  PatroniApiHandler patroniApi;

  @Inject
  DbOpsExecutorService executorService;

  private static String convertToPostgresVersion(Integer serverVersion) {
    int majorVersion = serverVersion / 10000;
    int minorVersion = serverVersion % 10000;

    return String.format("%d.%d", majorVersion, minorVersion);
  }

  private Uni<String> getTargetPostgresVersion(StackGresDbOps dbOps) {
    return executorService.itemAsync(
        () -> dbOps.getSpec().getMinorVersionUpgrade().getPostgresVersion());
  }

  private Uni<String> getSourcePostgresVersion(StackGresCluster cluster) {
    String clusterName = cluster.getMetadata().getName();
    String namespace = cluster.getMetadata().getNamespace();
    return Uni.createFrom()
        .item(Optional.ofNullable(cluster.getStatus())
            .map(StackGresClusterStatus::getDbOps)
            .map(StackGresClusterDbOpsStatus::getMinorVersionUpgrade)
            .map(StackGresClusterDbOpsMinorVersionUpgradeStatus::getSourcePostgresVersion))
        .chain(sourcePostgresVersion -> {
          if (sourcePostgresVersion.isPresent()) {
            return Uni.createFrom().item(sourcePostgresVersion.get());
          } else {
            return patroniApi.getClusterMembersPatroniInformation(clusterName, namespace)
                .onItem().transform(patronis -> patronis.stream()
                    .map(PatroniInformation::getServerVersion)
                    .flatMap(Optional::stream)
                    .min(Integer::compareTo)
                    .map(MinorVersionUpgradeRestartStateHandler::convertToPostgresVersion)
                    .orElseThrow());
          }
        });
  }

  @Override
  protected Uni<Void> initClusterDbOpsStatusValues(ClusterRestartState clusterRestartState,
      StackGresDbOps dbOps, StackGresCluster cluster) {
    return super.initClusterDbOpsStatusValues(clusterRestartState, dbOps, cluster)
        .chain(ignore -> Uni.combine().all()
                .unis(
                    getSourcePostgresVersion(cluster),
                    getTargetPostgresVersion(dbOps)
                    ).asTuple()
        )
        .chain(versionTuple -> {
          StackGresClusterDbOpsMinorVersionUpgradeStatus minorVersionUpgradeStatus =
              cluster.getStatus().getDbOps().getMinorVersionUpgrade();
          minorVersionUpgradeStatus.setSourcePostgresVersion(versionTuple.getItem1());
          minorVersionUpgradeStatus.setTargetPostgresVersion(versionTuple.getItem2());
          return Uni.createFrom().voidItem();
        });
  }

  @Override
  protected Uni<Void> initDbOpsRestartStatusValues(ClusterRestartState clusterRestartState,
      StackGresDbOps dbOps, StackGresCluster cluster) {
    return super.initDbOpsRestartStatusValues(clusterRestartState, dbOps, cluster)
        .chain(ignore -> Uni.combine().all()
            .unis(
                getSourcePostgresVersion(cluster),
                getTargetPostgresVersion(dbOps)
                ).asTuple()
            )
        .chain(versionTuple -> {
          StackGresDbOpsMinorVersionUpgradeStatus minorVersionUpgradeStatus =
              dbOps.getStatus().getMinorVersionUpgrade();
          minorVersionUpgradeStatus.setSourcePostgresVersion(versionTuple.getItem1());
          minorVersionUpgradeStatus.setTargetPostgresVersion(versionTuple.getItem2());
          return Uni.createFrom().voidItem();
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
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void setDbOpRestartStatus(StackGresDbOps dbOps, DbOpsRestartStatus dbOpsStatus) {
    dbOps.getStatus().setMinorVersionUpgrade((StackGresDbOpsMinorVersionUpgradeStatus) dbOpsStatus);
  }

  @Override
  protected StackGresClusterDbOpsMinorVersionUpgradeStatus getClusterRestartStatus(
      StackGresCluster cluster) {
    return Optional.ofNullable(cluster.getStatus())
        .map(StackGresClusterStatus::getDbOps)
        .map(StackGresClusterDbOpsStatus::getMinorVersionUpgrade)
        .orElseGet(() -> {
          if (cluster.getStatus() == null) {
            cluster.setStatus(new StackGresClusterStatus());
          }
          if (cluster.getStatus().getDbOps() == null) {
            cluster.getStatus().setDbOps(new StackGresClusterDbOpsStatus());
          }
          if (cluster.getStatus().getDbOps().getMinorVersionUpgrade() == null) {
            cluster.getStatus().getDbOps()
                .setMinorVersionUpgrade(new StackGresClusterDbOpsMinorVersionUpgradeStatus());
          }
          return cluster.getStatus().getDbOps().getMinorVersionUpgrade();
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
            && !status.getInitialInstances().isEmpty()
            && status.getSourcePostgresVersion() != null
            && status.getTargetPostgresVersion() != null)
        .isPresent();
  }

  @Override
  protected Optional<DbOpsMethodType> getRestartMethod(StackGresDbOps op) {
    return Optional.ofNullable(op.getSpec())
        .map(StackGresDbOpsSpec::getMinorVersionUpgrade)
        .map(StackGresDbOpsMinorVersionUpgrade::getMethod)
        .map(DbOpsMethodType::fromString);
  }

}

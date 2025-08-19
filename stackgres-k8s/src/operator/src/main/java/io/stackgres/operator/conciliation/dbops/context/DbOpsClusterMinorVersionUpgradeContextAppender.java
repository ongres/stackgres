/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.dbops.context;

import java.util.Optional;

import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsMinorVersionUpgradeStatus;
import io.stackgres.operator.conciliation.dbops.StackGresDbOpsContext.Builder;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DbOpsClusterMinorVersionUpgradeContextAppender {

  public void appendContext(StackGresDbOps dbOps, StackGresCluster cluster, Builder contextBuilder) {
    if (dbOps.getStatus().getMinorVersionUpgrade() == null) {
      dbOps.getStatus().setMinorVersionUpgrade(new StackGresDbOpsMinorVersionUpgradeStatus());
    }
    if (dbOps.getStatus().getMinorVersionUpgrade().getSourcePostgresVersion() == null) {
      final String postgresVersion;
      if (StackGresVersion.getStackGresVersionAsNumber(cluster) <= StackGresVersion.V_1_18.getVersionAsNumber()) {
        postgresVersion = Optional.ofNullable(cluster.getStatus().getPostgresVersion())
            .orElse(cluster.getSpec().getPostgres().getVersion());
      } else {
        postgresVersion = cluster.getStatus().getPostgresVersion();
      }
      dbOps.getStatus().getMinorVersionUpgrade().setSourcePostgresVersion(postgresVersion);
    }
  }

}

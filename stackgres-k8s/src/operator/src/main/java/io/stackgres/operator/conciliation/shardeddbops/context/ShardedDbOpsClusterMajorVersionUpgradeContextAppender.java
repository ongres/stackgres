/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardeddbops.context;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.Optional;

import io.stackgres.common.component.StackGresContext;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterDbOpsMajorVersionUpgradeStatus;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterDbOpsStatus;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterStatus;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.StackGresVersionUtil;
import io.stackgres.operator.conciliation.shardeddbops.StackGresShardedDbOpsContext.Builder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;

@ApplicationScoped
public class ShardedDbOpsClusterMajorVersionUpgradeContextAppender {

  private final StackGresContext context;
  private final CustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder;

  @Inject
  public ShardedDbOpsClusterMajorVersionUpgradeContextAppender(
      StackGresContext context,
      CustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder) {
    this.context = context;
    this.postgresConfigFinder = postgresConfigFinder;
  }

  public void appendContext(StackGresShardedDbOps dbOps, StackGresShardedCluster cluster, Builder contextBuilder) {
    final String givenPgVersion = dbOps.getSpec().getMajorVersionUpgrade().getPostgresVersion();
    if (givenPgVersion != null
        && !isPostgresVersionSupported(cluster, givenPgVersion)) {
      final String message = "Unsupported postgres version " + givenPgVersion
          + ".  Supported postgres versions are: "
          + Seq.seq(StackGresVersionUtil.getSupportedPostgresVersions(context, cluster)).toString(", ");
      throw new IllegalArgumentException(message);
    }

    String givenMajorVersion = getPostgresFlavorComponent(cluster)
        .get(cluster).getMajorVersion(context, givenPgVersion);
    long givenMajorVersionIndex = getPostgresFlavorComponent(cluster)
        .get(cluster)
        .streamOrderedMajorVersions(context)
        .zipWithIndex()
        .filter(t -> t.v1.equals(givenMajorVersion))
        .map(Tuple2::v2)
        .findAny()
        .orElseThrow();
    String oldPgVersion = Optional.ofNullable(cluster.getStatus())
        .map(StackGresShardedClusterStatus::getDbOps)
        .map(StackGresShardedClusterDbOpsStatus::getMajorVersionUpgrade)
        .map(StackGresShardedClusterDbOpsMajorVersionUpgradeStatus::getSourcePostgresVersion)
        .orElse(cluster.getSpec().getPostgres().getVersion());
    String oldMajorVersion = getPostgresFlavorComponent(cluster)
        .get(cluster)
        .getMajorVersion(context, oldPgVersion);
    long oldMajorVersionIndex = getPostgresFlavorComponent(cluster)
        .get(cluster)
        .streamOrderedMajorVersions(context)
        .zipWithIndex()
        .filter(t -> t.v1.equals(oldMajorVersion))
        .map(Tuple2::v2)
        .findAny()
        .orElseThrow();

    if (givenMajorVersionIndex >= oldMajorVersionIndex) {
      throw new IllegalArgumentException(
          "postgres version must be a newer major version than the current one ("
              + givenMajorVersion + " <= " + oldMajorVersion + ")");
    }

    Optional<StackGresPostgresConfig> postgresConfig = postgresConfigFinder
        .findByNameAndNamespace(
            dbOps.getSpec().getMajorVersionUpgrade().getSgPostgresConfig(),
            dbOps.getMetadata().getNamespace());
    if (postgresConfig.isPresent()) {
      if (!postgresConfig.get().getSpec().getPostgresVersion().equals(givenMajorVersion)) {
        throw new IllegalArgumentException(
            StackGresPostgresConfig.KIND + " must be for postgres version "
                + givenMajorVersion + " but was for version "
                + postgresConfig.get().getSpec().getPostgresVersion());
      }
    } else {
      throw new IllegalArgumentException(
          StackGresPostgresConfig.KIND + " "
              + dbOps.getSpec().getMajorVersionUpgrade().getSgPostgresConfig() + " not found");
    }
  }

  private boolean isPostgresVersionSupported(StackGresShardedCluster cluster, String version) {
    return StackGresVersionUtil.getSupportedPostgresVersions(context, cluster)
        .contains(version);
  }

}

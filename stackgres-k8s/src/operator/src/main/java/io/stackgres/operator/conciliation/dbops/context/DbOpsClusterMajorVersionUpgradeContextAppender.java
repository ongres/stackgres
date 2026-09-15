/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.dbops.context;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.component.StackGresContext;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsMajorVersionUpgradeStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.StackGresVersionUtil;
import io.stackgres.operator.conciliation.dbops.StackGresDbOpsContext.Builder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;

@ApplicationScoped
public class DbOpsClusterMajorVersionUpgradeContextAppender {

  private final StackGresContext context;
  private final CustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder;
  private final DbOpsMajorVersionUpgradeExtensionsContextAppender
      dbOpsMajorVersionUpgradeExtensionsContextAppender;

  @Inject
  public DbOpsClusterMajorVersionUpgradeContextAppender(
      StackGresContext context,
      CustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder,
      DbOpsMajorVersionUpgradeExtensionsContextAppender
          dbOpsMajorVersionUpgradeExtensionsContextAppender) {
    this.context = context;
    this.postgresConfigFinder = postgresConfigFinder;
    this.dbOpsMajorVersionUpgradeExtensionsContextAppender =
        dbOpsMajorVersionUpgradeExtensionsContextAppender;
  }

  public void appendContext(StackGresDbOps dbOps, StackGresCluster cluster, Builder contextBuilder) {
    var foundOwnerReference = Optional.of(cluster)
        .map(StackGresCluster::getMetadata)
        .map(ObjectMeta::getOwnerReferences)
        .stream()
        .flatMap(List::stream)
        .filter(ownerReference -> !Objects.equals(
            ownerReference.getKind(),
            HasMetadata.getKind(StackGresDistributedLogs.class)))
        .filter(ownerReference -> ownerReference.getController() != null
            && ownerReference.getController())
        .findFirst();
    boolean ownedByShardedDbOps = Optional.of(dbOps)
        .map(StackGresDbOps::getMetadata)
        .map(ObjectMeta::getOwnerReferences)
        .stream()
        .flatMap(List::stream)
        .anyMatch(ownerReference -> Objects.equals(
            ownerReference.getKind(),
            HasMetadata.getKind(StackGresShardedDbOps.class)));
    boolean targetPostgresVersionAlreadySetOnCluster = Objects.equals(
        dbOps.getSpec().getMajorVersionUpgrade().getPostgresVersion(),
        cluster.getSpec().getPostgres().getVersion());
    if (foundOwnerReference.isPresent()
        && !ownedByShardedDbOps
        && !targetPostgresVersionAlreadySetOnCluster) {
      OwnerReference ownerReference = foundOwnerReference.get();
      throw new IllegalArgumentException(
          "Can not perform major version upgrade on " + StackGresCluster.KIND + " managed by "
              + ownerReference.getKind() + " " + ownerReference.getName());
    }

    final String givenPgVersion = dbOps.getSpec().getMajorVersionUpgrade().getPostgresVersion();
    if (givenPgVersion != null
        && !isPostgresVersionSupported(cluster, givenPgVersion)) {
      final String message = "Unsupported postgres version " + givenPgVersion
          + ".  Supported postgres versions are: "
          + Seq.seq(StackGresVersionUtil.getSupportedPostgresVersions(context, cluster)).toString(", ");
      throw new IllegalArgumentException(message);
    }

    final String postgresVersion;
    if (StackGresVersion.getStackGresVersionAsNumber(cluster) <= StackGresVersion.V_1_18.getVersionAsNumber()) {
      postgresVersion = Optional.ofNullable(cluster.getStatus().getPostgresVersion())
          .orElse(cluster.getSpec().getPostgres().getVersion());
    } else {
      postgresVersion = cluster.getStatus().getPostgresVersion();
    }
    if (postgresVersion == null) {
      throw new IllegalArgumentException(StackGresCluster.KIND
          + " " + cluster.getMetadata().getName() + " has no postgres version defined yet");
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
        .map(StackGresClusterStatus::getDbOps)
        .map(StackGresClusterDbOpsStatus::getMajorVersionUpgrade)
        .map(StackGresClusterDbOpsMajorVersionUpgradeStatus::getSourcePostgresVersion)
        .orElse(postgresVersion);
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

    dbOpsMajorVersionUpgradeExtensionsContextAppender.appendContext(dbOps, cluster, contextBuilder);
  }

  private boolean isPostgresVersionSupported(StackGresCluster cluster, String version) {
    return StackGresVersionUtil.getSupportedPostgresVersions(context, cluster)
        .contains(version);
  }

}

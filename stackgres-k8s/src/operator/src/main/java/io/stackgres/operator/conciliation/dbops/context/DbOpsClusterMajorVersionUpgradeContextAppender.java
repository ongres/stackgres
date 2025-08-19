/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.dbops.context;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsMajorVersionUpgradeStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.dbops.StackGresDbOpsContext.Builder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;

@ApplicationScoped
public class DbOpsClusterMajorVersionUpgradeContextAppender {

  private final CustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder;
  private final Map<StackGresComponent, Map<StackGresVersion, List<String>>> supportedPostgresVersions;

  @Inject
  public DbOpsClusterMajorVersionUpgradeContextAppender(
      CustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder) {
    this(
        postgresConfigFinder,
        Stream.of(StackGresComponent.POSTGRESQL, StackGresComponent.BABELFISH)
        .collect(ImmutableMap.toImmutableMap(Function.identity(),
            component -> component.getComponentVersions()
                .entrySet()
                .stream()
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey,
                    entry -> entry.getValue().streamOrderedVersions().toList())))));
  }

  public DbOpsClusterMajorVersionUpgradeContextAppender(
      CustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder,
      Map<StackGresComponent, Map<StackGresVersion, List<String>>> supportedPostgresVersions) {
    this.postgresConfigFinder = postgresConfigFinder;
    this.supportedPostgresVersions = supportedPostgresVersions;
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
    if (foundOwnerReference.isPresent()) {
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
          + Seq.seq(supportedPostgresVersions.get(getPostgresFlavorComponent(cluster))
              .get(StackGresVersion.getStackGresVersion(cluster))).toString(", ");
      throw new IllegalArgumentException(message);
    }

    if (cluster.getStatus() == null
        || cluster.getStatus().getPostgresVersion() == null) {
      throw new IllegalArgumentException(StackGresCluster.KIND
          + " " + cluster.getMetadata().getName() + " has no postgres version defined yet");
    }

    String givenMajorVersion = getPostgresFlavorComponent(cluster)
        .get(cluster).getMajorVersion(givenPgVersion);
    long givenMajorVersionIndex = getPostgresFlavorComponent(cluster)
        .get(cluster)
        .streamOrderedMajorVersions()
        .zipWithIndex()
        .filter(t -> t.v1.equals(givenMajorVersion))
        .map(Tuple2::v2)
        .findAny()
        .orElseThrow();
    String oldPgVersion = Optional.ofNullable(cluster.getStatus())
        .map(StackGresClusterStatus::getDbOps)
        .map(StackGresClusterDbOpsStatus::getMajorVersionUpgrade)
        .map(StackGresClusterDbOpsMajorVersionUpgradeStatus::getSourcePostgresVersion)
        .orElse(cluster.getStatus().getPostgresVersion());
    String oldMajorVersion = getPostgresFlavorComponent(cluster)
        .get(cluster)
        .getMajorVersion(oldPgVersion);
    long oldMajorVersionIndex = getPostgresFlavorComponent(cluster)
        .get(cluster)
        .streamOrderedMajorVersions()
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

  private boolean isPostgresVersionSupported(StackGresCluster cluster, String version) {
    return supportedPostgresVersions.get(getPostgresFlavorComponent(cluster))
        .get(StackGresVersion.getStackGresVersion(cluster))
        .contains(version);
  }

}

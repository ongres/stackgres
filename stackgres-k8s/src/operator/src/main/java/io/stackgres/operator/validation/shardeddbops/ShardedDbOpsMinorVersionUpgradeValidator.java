/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardeddbops;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.stackgres.common.ErrorType;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.ShardedDbOpsReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operator.validation.ValidationUtil;
import io.stackgres.operator.validation.cluster.PostgresConfigValidator;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;

@Singleton
@ValidationType(ErrorType.FORBIDDEN_CR_UPDATE)
public class ShardedDbOpsMinorVersionUpgradeValidator implements ShardedDbOpsValidator {

  private final CustomResourceFinder<StackGresShardedCluster> clusterFinder;
  private final Map<StackGresComponent, Map<StackGresVersion, List<String>>>
      supportedPostgresVersions;
  private final String errorPostgresMismatchUri;
  private final String errorForbiddenUpdateUri;

  @Inject
  public ShardedDbOpsMinorVersionUpgradeValidator(
      CustomResourceFinder<StackGresShardedCluster> clusterFinder) {
    this(clusterFinder, ValidationUtil.SUPPORTED_SHARDED_CLUSTER_POSTGRES_VERSIONS);
  }

  public ShardedDbOpsMinorVersionUpgradeValidator(
      CustomResourceFinder<StackGresShardedCluster> clusterFinder,
      Map<StackGresComponent, Map<StackGresVersion, List<String>>>
          orderedSupportedPostgresVersions) {
    this.clusterFinder = clusterFinder;
    this.supportedPostgresVersions = orderedSupportedPostgresVersions;
    this.errorPostgresMismatchUri = ErrorType.getErrorTypeUri(ErrorType.PG_VERSION_MISMATCH);
    this.errorForbiddenUpdateUri = ErrorType.getErrorTypeUri(ErrorType.FORBIDDEN_CR_UPDATE);
  }

  @Override
  @SuppressFBWarnings(value = "SF_SWITCH_NO_DEFAULT",
      justification = "False positive")
  public void validate(ShardedDbOpsReview review) throws ValidationFailed {
    switch (review.getRequest().getOperation()) {
      case CREATE:
        StackGresShardedDbOps dbOps = review.getRequest().getObject();
        if (dbOps.getSpec().isOpMinorVersionUpgrade()) {
          Optional<StackGresShardedCluster> foundCluster =
              clusterFinder.findByNameAndNamespace(
                  dbOps.getSpec().getSgShardedCluster(),
                  dbOps.getMetadata().getNamespace());
          if (foundCluster.isPresent()) {
            StackGresShardedCluster cluster = foundCluster.get();

            var foundOwnerReference = Optional.of(cluster.getMetadata())
                .map(ObjectMeta::getOwnerReferences)
                .stream()
                .flatMap(List::stream)
                .filter(ownerReference -> ownerReference.getController() != null
                    && ownerReference.getController())
                .findFirst();
            if (foundOwnerReference.isPresent()) {
              OwnerReference ownerReference = foundOwnerReference.get();
              fail("Can not perform minor version upgrade on SGShardedCluster managed by "
                  + ownerReference.getKind() + " " + ownerReference.getName());
            }

            String givenPgVersion = dbOps.getSpec().getMinorVersionUpgrade().getPostgresVersion();

            if (givenPgVersion != null
                && !isPostgresVersionSupported(cluster, givenPgVersion)) {
              final String message = "Unsupported postgres version " + givenPgVersion
                  + ".  Supported postgres versions are: "
                  + Seq.seq(supportedPostgresVersions.get(getPostgresFlavorComponent(cluster))
                      .get(StackGresVersion.getStackGresVersion(cluster))).toString(", ");
              fail(errorPostgresMismatchUri, message);
            }

            if (PostgresConfigValidator.BUGGY_PG_VERSIONS.keySet().contains(givenPgVersion)) {
              fail(errorForbiddenUpdateUri, "Do not use PostgreSQL " + givenPgVersion + ". "
                  + PostgresConfigValidator.BUGGY_PG_VERSIONS.get(givenPgVersion));
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
            String oldPgVersion = cluster.getSpec().getPostgres().getVersion();
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

            if (givenMajorVersionIndex != oldMajorVersionIndex) {
              fail(errorForbiddenUpdateUri,
                  "postgres version must have the same major version as the current one");
            }
          }
        }
        break;
      default:
    }

  }

  private boolean isPostgresVersionSupported(StackGresShardedCluster cluster, String version) {
    return supportedPostgresVersions.get(getPostgresFlavorComponent(cluster))
        .get(StackGresVersion.getStackGresVersion(cluster))
        .contains(version);
  }

}

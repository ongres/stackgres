/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.dbops;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.collect.ImmutableMap;
import io.stackgres.common.ErrorType;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.StackGresDbOpsReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;

@Singleton
@ValidationType(ErrorType.FORBIDDEN_CR_UPDATE)
public class DbOpsMinorVersionUpgradeValidator implements DbOpsValidator {

  private final CustomResourceFinder<StackGresCluster> clusterFinder;
  private final Map<StackGresComponent, List<String>> supportedPostgresVersions;
  private final String errorPostgresMismatchUri;
  private final String errorForbiddenUpdateUri;

  @Inject
  public DbOpsMinorVersionUpgradeValidator(
      CustomResourceFinder<StackGresCluster> clusterFinder) {
    this(clusterFinder, ImmutableMap.of(
        StackGresComponent.POSTGRESQL, StackGresComponent.POSTGRESQL.getOrderedVersions().toList(),
        StackGresComponent.BABELFISH, StackGresComponent.BABELFISH.getOrderedVersions().toList()));
  }

  public DbOpsMinorVersionUpgradeValidator(
      CustomResourceFinder<StackGresCluster> clusterFinder,
      Map<StackGresComponent, List<String>> orderedSupportedPostgresVersions) {
    this.clusterFinder = clusterFinder;
    this.supportedPostgresVersions = Seq.seq(orderedSupportedPostgresVersions)
        .map(t -> t.map2(ArrayList::new))
        .collect(ImmutableMap.toImmutableMap(Tuple2::v1, Tuple2::v2));
    this.errorPostgresMismatchUri = ErrorType.getErrorTypeUri(ErrorType.PG_VERSION_MISMATCH);
    this.errorForbiddenUpdateUri = ErrorType.getErrorTypeUri(ErrorType.FORBIDDEN_CR_UPDATE);
  }

  @Override
  public void validate(StackGresDbOpsReview review) throws ValidationFailed {
    switch (review.getRequest().getOperation()) {
      case CREATE:
        StackGresDbOps dbOps = review.getRequest().getObject();
        if (dbOps.getSpec().isOpMinorVersionUpgrade()) {
          Optional<StackGresCluster> foundCluster = clusterFinder.findByNameAndNamespace(
              dbOps.getSpec().getSgCluster(), dbOps.getMetadata().getNamespace());
          if (foundCluster.isPresent()) {
            StackGresCluster cluster = foundCluster.get();
            String givenPgVersion = dbOps.getSpec().getMinorVersionUpgrade().getPostgresVersion();

            if (givenPgVersion != null
                && !isPostgresVersionSupported(cluster, givenPgVersion)) {
              final String message = "Unsupported postgres version " + givenPgVersion
                  + ".  Supported postgres versions are: "
                  + Seq.seq(supportedPostgresVersions.get(
                      getPostgresFlavorComponent(cluster))).toString(", ");
              fail(errorPostgresMismatchUri, message);
            }

            String givenMajorVersion = getPostgresFlavorComponent(cluster).findMajorVersion(
                givenPgVersion);
            long givenMajorVersionIndex = getPostgresFlavorComponent(cluster)
                .getOrderedMajorVersions()
                .zipWithIndex()
                .filter(t -> t.v1.equals(givenMajorVersion))
                .map(Tuple2::v2)
                .findAny()
                .get();
            String oldPgVersion = cluster.getSpec().getPostgres().getVersion();
            String oldMajorVersion = getPostgresFlavorComponent(cluster)
                .findMajorVersion(oldPgVersion);
            long oldMajorVersionIndex = getPostgresFlavorComponent(cluster)
                .getOrderedMajorVersions()
                .zipWithIndex()
                .filter(t -> t.v1.equals(oldMajorVersion))
                .map(Tuple2::v2)
                .findAny()
                .get();

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

  private boolean isPostgresVersionSupported(StackGresCluster cluster, String version) {
    return supportedPostgresVersions.get(getPostgresFlavorComponent(cluster)).contains(version);
  }

}

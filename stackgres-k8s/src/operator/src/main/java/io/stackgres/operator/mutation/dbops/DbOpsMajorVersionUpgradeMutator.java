/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.dbops;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonpatch.JsonPatchOperation;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import io.stackgres.common.BackupStorageUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsMajorVersionUpgrade;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSpec;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.DbOpsReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;

@ApplicationScoped
public class DbOpsMajorVersionUpgradeMutator implements DbOpsMutator {

  CustomResourceFinder<StackGresCluster> clusterFinder;

  private JsonPointer backupPathPointer;

  @PostConstruct
  public void init() throws NoSuchFieldException {
    String majorVersionUpgradeJson = getJsonMappingField("majorVersionUpgrade",
        StackGresDbOpsSpec.class);
    String backupPathJson = getJsonMappingField("backupPath",
        StackGresDbOpsMajorVersionUpgrade.class);

    backupPathPointer = SPEC_POINTER
        .append(majorVersionUpgradeJson)
        .append(backupPathJson);
  }

  @Override
  public List<JsonPatchOperation> mutate(DbOpsReview review) {
    if (review.getRequest().getOperation() == Operation.CREATE
        || review.getRequest().getOperation() == Operation.UPDATE) {
      final StackGresDbOps dbOps = review.getRequest().getObject();

      ImmutableList.Builder<JsonPatchOperation> operations = ImmutableList.builder();
      if (dbOps.getSpec() != null
          && dbOps.getSpec().getSgCluster() != null
          && dbOps.getSpec().getMajorVersionUpgrade() != null
          && dbOps.getSpec().getMajorVersionUpgrade().getPostgresVersion() != null
          && dbOps.getSpec().getMajorVersionUpgrade().getBackupPath() == null) {
        Optional<StackGresCluster> cluster = clusterFinder.findByNameAndNamespace(
            dbOps.getSpec().getSgCluster(),
            dbOps.getMetadata().getNamespace());
        if (cluster.filter(this::clusterHasBackups).isPresent()) {
          final String backupPath = getBackupPath(dbOps, cluster.get());
          operations.add(applyAddValue(backupPathPointer, FACTORY.textNode(backupPath)));
        }
      }

      return operations.build();
    }

    return ImmutableList.of();
  }

  private boolean clusterHasBackups(StackGresCluster cluster) {
    var configurations = Optional.of(cluster)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getConfiguration);
    return configurations.map(StackGresClusterConfiguration::getBackupConfig).isPresent()
        || configurations.map(StackGresClusterConfiguration::getBackups)
        .filter(Predicates.not(List::isEmpty))
        .isPresent();
  }

  private String getBackupPath(StackGresDbOps dbOps, StackGresCluster cluster) {
    final String postgresVersion = dbOps.getSpec()
        .getMajorVersionUpgrade().getPostgresVersion();
    final String postgresFlavor = cluster.getSpec()
        .getPostgres().getFlavor();
    final String postgresMajorVersion = getPostgresFlavorComponent(postgresFlavor)
        .get(cluster)
        .findMajorVersion(postgresVersion);
    return BackupStorageUtil.getPath(
        cluster.getMetadata().getNamespace(),
        cluster.getMetadata().getName(),
        postgresMajorVersion);
  }

  @Inject
  public void setBackupConfigFinder(
      CustomResourceFinder<StackGresCluster> clusterFinder) {
    this.clusterFinder = clusterFinder;
  }

}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonpatch.JsonPatchOperation;
import com.google.common.collect.ImmutableList;
import io.stackgres.common.BackupStorageUtil;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigSpec;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterBackupConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class DefaultBackupPathMutator implements ClusterMutator {

  private static final long VERSION_1_1 = StackGresVersion.V_1_1.getVersionAsNumber();

  CustomResourceFinder<StackGresBackupConfig> backupConfigFinder;

  private JsonPointer backupPathPointer;
  private JsonPointer backupsPointer;
  private JsonPointer backupsPathPointer;

  @PostConstruct
  public void init() throws NoSuchFieldException {
    String configurationPathJson = getJsonMappingField("configuration",
        StackGresClusterSpec.class);
    String backupPathJson = getJsonMappingField("backupPath",
        StackGresClusterConfiguration.class);
    String backupsJson = getJsonMappingField("backups",
        StackGresClusterConfiguration.class);
    String backupsPathJson = getJsonMappingField("path",
        StackGresClusterBackupConfiguration.class);

    backupPathPointer = SPEC_POINTER
        .append(configurationPathJson)
        .append(backupPathJson);

    backupsPointer = SPEC_POINTER
        .append(configurationPathJson)
        .append(backupsJson);

    backupsPathPointer = JsonPointer.of(backupsPathJson);
  }

  @Override
  public List<JsonPatchOperation> mutate(StackGresClusterReview review) {
    if (review.getRequest().getOperation() == Operation.CREATE
        || review.getRequest().getOperation() == Operation.UPDATE) {
      final StackGresCluster cluster = review.getRequest().getObject();
      final StackGresClusterConfiguration configuration =
          Optional.ofNullable(cluster.getSpec().getConfiguration())
          .orElseGet(() -> new StackGresClusterConfiguration());

      ImmutableList.Builder<JsonPatchOperation> operations = ImmutableList.builder();
      if (configuration.getBackupConfig() != null && configuration.getBackupPath() == null) {
        final long version = StackGresVersion.getStackGresVersionAsNumber(cluster);
        final String backupPath;
        if (version <= VERSION_1_1) {
          backupPath = getBackupPathPre_1_2(configuration, cluster);
        } else {
          backupPath = getBackupPath(cluster);
        }
        operations.add(applyAddValue(backupPathPointer, FACTORY.textNode(backupPath)));
      }
      Optional.ofNullable(configuration.getBackups())
          .map(Seq::seq)
          .orElse(Seq.of())
          .zipWithIndex()
          .forEach(t -> {
            if (t.v1.getPath() == null) {
              final String backupPath = getBackupPath(cluster);
              operations.add(applyAddValue(
                  backupsPointer.append(t.v2.intValue()).append(backupsPathPointer),
                  FACTORY.textNode(backupPath)));
            }
          });

      return operations.build();
    }

    return ImmutableList.of();
  }

  private String getBackupPathPre_1_2(final StackGresClusterConfiguration configuration,
      final StackGresCluster cluster) {
    var storage = backupConfigFinder.findByNameAndNamespace(
        configuration.getBackupConfig(),
        cluster.getMetadata().getNamespace())
        .map(StackGresBackupConfig::getSpec)
        .map(StackGresBackupConfigSpec::getStorage);
    if (storage.isPresent()) {
      return BackupStorageUtil.getPathPre_1_2(
          cluster.getMetadata().getNamespace(),
          cluster.getMetadata().getName(),
          storage.orElseThrow());
    }
    return getBackupPath(cluster);
  }

  private String getBackupPath(final StackGresCluster cluster) {
    final String postgresVersion = cluster.getSpec()
        .getPostgres().getVersion();
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
      CustomResourceFinder<StackGresBackupConfig> backupConfigFinder) {
    this.backupConfigFinder = backupConfigFinder;
  }

}

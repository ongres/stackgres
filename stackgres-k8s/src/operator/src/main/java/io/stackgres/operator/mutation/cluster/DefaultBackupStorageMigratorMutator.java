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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonpatch.JsonPatchOperation;
import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.stackgres.common.BackupStorageUtil;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigSpec;
import io.stackgres.common.crd.sgbackupconfig.StackGresBaseBackupConfig;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterBackupConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.mutating.MutatorWeight;

@ApplicationScoped
@MutatorWeight(10)
public class DefaultBackupStorageMigratorMutator implements ClusterMutator {

  private static final long VERSION_1_1 = StackGresVersion.V_1_1.getVersionAsNumber();

  private final ObjectMapper mapper;
  private final CustomResourceFinder<StackGresBackupConfig> backupConfigFinder;
  private final CustomResourceFinder<StackGresObjectStorage> objectStorageFinder;
  private final CustomResourceScheduler<StackGresObjectStorage> objectStorageScheduler;

  @Inject
  public DefaultBackupStorageMigratorMutator(ObjectMapper mapper,
      CustomResourceFinder<StackGresBackupConfig> backupConfigFinder,
      CustomResourceFinder<StackGresObjectStorage> objectStorageFinder,
      CustomResourceScheduler<StackGresObjectStorage> objectStorageScheduler) {
    this.mapper = mapper;
    this.backupConfigFinder = backupConfigFinder;
    this.objectStorageFinder = objectStorageFinder;
    this.objectStorageScheduler = objectStorageScheduler;
  }

  private JsonPointer backupsPointer;
  private JsonPointer backupConfigPointer;
  private JsonPointer backupPathPointer;

  @PostConstruct
  public void init() throws NoSuchFieldException {
    String configurationPathJson = getJsonMappingField("configuration",
        StackGresClusterSpec.class);
    String backupsJson = getJsonMappingField("backups",
        StackGresClusterConfiguration.class);
    String backupConfigJson = getJsonMappingField("backupConfig",
        StackGresClusterConfiguration.class);
    String backupPathJson = getJsonMappingField("backupPath",
        StackGresClusterConfiguration.class);

    backupsPointer = SPEC_POINTER.append(configurationPathJson).append(backupsJson);
    backupConfigPointer = SPEC_POINTER.append(configurationPathJson).append(backupConfigJson);
    backupPathPointer = SPEC_POINTER.append(configurationPathJson).append(backupPathJson);
  }

  @Override
  public List<JsonPatchOperation> mutate(StackGresClusterReview review) {
    Operation operation = review.getRequest().getOperation();
    if (operation == Operation.CREATE || operation == Operation.UPDATE) {
      final StackGresCluster cluster = review.getRequest().getObject();
      final StackGresClusterConfiguration configuration =
          Optional.ofNullable(cluster.getSpec().getConfiguration())
              .orElseGet(StackGresClusterConfiguration::new);

      ImmutableList.Builder<JsonPatchOperation> operations = ImmutableList.builder();

      if (configuration.getBackupConfig() != null) {
        operations.add(applyRemoveValue(backupConfigPointer));
      }
      if (configuration.getBackupPath() != null) {
        operations.add(applyRemoveValue(backupPathPointer));
      }

      String objStorage = createObjectStorage(cluster);
      if (objStorage != null) {
        StackGresBaseBackupConfig copyBaseBackup = copyBaseBackup(cluster);
        StackGresClusterBackupConfiguration cbc = new StackGresClusterBackupConfiguration();
        cbc.setObjectStorage(objStorage);
        cbc.setCompression(copyBaseBackup.getCompression());
        cbc.setCronSchedule(copyBaseBackup.getCronSchedule());
        cbc.setPerformance(copyBaseBackup.getPerformance());
        cbc.setRetention(copyBaseBackup.getRetention());
        if (configuration.getBackupPath() != null) {
          cbc.setPath(configuration.getBackupPath());
        } else {
          final long version = StackGresVersion.getStackGresVersionAsNumber(cluster);
          final String backupPath = version <= VERSION_1_1
              ? getBackupPathPre_1_2(cluster)
              : getBackupPath(cluster);
          cbc.setPath(backupPath);
        }
        configuration.setBackups(List.of(cbc));

        JsonNode backups = mapper.valueToTree(configuration.getBackups());
        operations.add(applyAddValue(backupsPointer, backups));
      }
      return operations.build();
    }
    return List.of();
  }

  private StackGresBaseBackupConfig copyBaseBackup(final StackGresCluster cluster) {
    String backupConfig = cluster.getSpec().getConfiguration().getBackupConfig();
    String namespace = cluster.getMetadata().getNamespace();
    return backupConfigFinder.findByNameAndNamespace(backupConfig, namespace)
        .map(StackGresBackupConfig::getSpec)
        .map(StackGresBackupConfigSpec::getBaseBackups)
        .orElseGet(StackGresBaseBackupConfig::new);
  }

  private String createObjectStorage(final StackGresCluster cluster) {
    String backupConfig = cluster.getSpec().getConfiguration().getBackupConfig();
    String namespace = cluster.getMetadata().getNamespace();
    if (backupConfig != null && objectStorageFinder.findByNameAndNamespace(backupConfig, namespace)
        .isEmpty()) {
      backupConfigFinder.findByNameAndNamespace(backupConfig, namespace)
          .map(StackGresBackupConfig::getSpec)
          .map(StackGresBackupConfigSpec::getStorage)
          .ifPresent(storage -> {
            final StackGresObjectStorage objStorage = new StackGresObjectStorage();
            objStorage.setMetadata(new ObjectMetaBuilder()
                .withName(backupConfig)
                .withNamespace(namespace)
                .build());
            objStorage.setSpec(storage);
            objectStorageScheduler.create(objStorage);
          });
    }
    return backupConfig;
  }

  private String getBackupPathPre_1_2(final StackGresCluster cluster) {
    return backupConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(),
        cluster.getMetadata().getNamespace())
        .map(StackGresBackupConfig::getSpec)
        .map(StackGresBackupConfigSpec::getStorage)
        .map(storage -> BackupStorageUtil.getPathPre_1_2(
            cluster.getMetadata().getNamespace(),
            cluster.getMetadata().getName(),
            storage))
        .orElseGet(() -> getBackupPath(cluster));
  }

  private String getBackupPath(final StackGresCluster cluster) {
    final String postgresVersion = cluster.getSpec().getPostgres().getVersion();
    final String postgresFlavor = cluster.getSpec().getPostgres().getFlavor();
    final String postgresMajorVersion = getPostgresFlavorComponent(postgresFlavor)
        .get(cluster)
        .findMajorVersion(postgresVersion);
    return BackupStorageUtil.getPath(
        cluster.getMetadata().getNamespace(),
        cluster.getMetadata().getName(),
        postgresMajorVersion);
  }

}

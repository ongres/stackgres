/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.backup;

import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonpatch.JsonPatchOperation;
import com.google.common.collect.ImmutableList;
import io.stackgres.common.crd.sgbackup.StackGresBackupStatus;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigSpec;
import io.stackgres.common.crd.sgbackupconfig.StackGresBaseBackupConfig;
import io.stackgres.common.crd.sgbackupconfig.StackGresBaseBackupPerformance;
import io.stackgres.operator.common.BackupReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;

@ApplicationScoped
public class BackupPerformanceMutator implements BackupMutator {

  protected static final JsonMapper JSON_MAPPER = new JsonMapper();

  private JsonPointer maxDiskBandwidthPointer;
  private JsonPointer maxDiskBandwitdhPointer;
  private JsonPointer maxNetworkBandwidthPointer;
  private JsonPointer maxNetworkBandwitdhPointer;

  @PostConstruct
  public void init() throws NoSuchFieldException {
    String backupConfigJson = getJsonMappingField("backupConfig",
        StackGresBackupStatus.class);
    String baseBackupsJson = getJsonMappingField("baseBackups",
        StackGresBackupConfigSpec.class);
    String performanceJson = getJsonMappingField("performance",
        StackGresBaseBackupConfig.class);
    String maxDiskBandwidthJson = getJsonMappingField("maxDiskBandwidth",
        StackGresBaseBackupPerformance.class);
    String maxDiskBandwitdhJson = getJsonMappingField("maxDiskBandwitdh",
        StackGresBaseBackupPerformance.class);
    String maxNetworkBandwidthJson = getJsonMappingField("maxNetworkBandwidth",
        StackGresBaseBackupPerformance.class);
    String maxNetworkBandwitdhJson = getJsonMappingField("maxNetworkBandwitdh",
        StackGresBaseBackupPerformance.class);

    maxDiskBandwidthPointer = STATUS_POINTER.append(backupConfigJson).append(baseBackupsJson)
        .append(performanceJson).append(maxDiskBandwidthJson);
    maxDiskBandwitdhPointer = STATUS_POINTER.append(backupConfigJson).append(baseBackupsJson)
        .append(performanceJson).append(maxDiskBandwitdhJson);
    maxNetworkBandwidthPointer = STATUS_POINTER.append(backupConfigJson).append(baseBackupsJson)
        .append(performanceJson).append(maxNetworkBandwidthJson);
    maxNetworkBandwitdhPointer = STATUS_POINTER.append(backupConfigJson).append(baseBackupsJson)
        .append(performanceJson).append(maxNetworkBandwitdhJson);
  }

  @Override
  public List<JsonPatchOperation> mutate(BackupReview review) {
    if (review.getRequest().getOperation() == Operation.CREATE
        || review.getRequest().getOperation() == Operation.UPDATE) {
      final StackGresBaseBackupPerformance performance =
          Optional.ofNullable(review.getRequest().getObject().getStatus())
          .map(StackGresBackupStatus::getBackupConfig)
          .map(StackGresBackupConfigSpec::getBaseBackups)
          .map(StackGresBaseBackupConfig::getPerformance)
          .orElseGet(StackGresBaseBackupPerformance::new);

      ImmutableList.Builder<JsonPatchOperation> operations = ImmutableList.builder();

      if (performance.getMaxDiskBandwitdh() != null) {
        operations.add(applyRemoveValue(
            maxDiskBandwitdhPointer));
        if (performance.getMaxDiskBandwidth() == null) {
          operations.add(applyAddValue(
              maxDiskBandwidthPointer,
              FACTORY.numberNode(performance.getMaxDiskBandwitdh())));
        }
      }
      if (performance.getMaxNetworkBandwitdh() != null) {
        operations.add(applyRemoveValue(
            maxNetworkBandwitdhPointer));
        if (performance.getMaxNetworkBandwidth() == null) {
          operations.add(applyAddValue(
              maxNetworkBandwidthPointer,
              FACTORY.numberNode(performance.getMaxNetworkBandwitdh())));
        }
      }

      return operations.build();
    }

    return ImmutableList.of();
  }

}

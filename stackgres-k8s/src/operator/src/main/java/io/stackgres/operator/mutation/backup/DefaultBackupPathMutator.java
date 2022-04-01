/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.backup;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonpatch.JsonPatchOperation;
import com.google.common.collect.ImmutableList;
import io.stackgres.common.BackupStorageUtil;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupStatus;
import io.stackgres.operator.common.BackupReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;

@ApplicationScoped
public class DefaultBackupPathMutator implements BackupMutator {

  private static final long VERSION_1_1 = StackGresVersion.V_1_1.getVersionAsNumber();

  private JsonPointer backupPathPointer;

  @PostConstruct
  public void init() throws NoSuchFieldException {
    String backupPathJson = getJsonMappingField("backupPath",
        StackGresBackupStatus.class);

    backupPathPointer = STATUS_POINTER
        .append(backupPathJson);
  }

  @Override
  public List<JsonPatchOperation> mutate(BackupReview review) {
    if (review.getRequest().getOperation() == Operation.CREATE
        || review.getRequest().getOperation() == Operation.UPDATE) {
      final StackGresBackup backup = review.getRequest().getObject();
      final StackGresBackupStatus status = backup.getStatus();

      ImmutableList.Builder<JsonPatchOperation> operations = ImmutableList.builder();
      final long version = StackGresVersion.getStackGresVersionAsNumber(backup);
      if (backup.getStatus() != null
          && backup.getStatus().getBackupConfig() != null
          && backup.getStatus().getBackupConfig().getStorage() != null
          && status.getBackupPath() == null
          && version <= VERSION_1_1) {
        final String backupPath = getBackupPathPre_1_2(backup);
        operations.add(applyAddValue(backupPathPointer, FACTORY.textNode(backupPath)));
      }

      return operations.build();
    }

    return ImmutableList.of();
  }

  private String getBackupPathPre_1_2(final StackGresBackup backup) {
    return BackupStorageUtil.getPathPre_1_2(
        backup.getMetadata().getNamespace(),
        backup.getMetadata().getName(),
        backup.getStatus().getBackupConfig().getStorage());
  }

}

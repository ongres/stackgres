/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.backup;

import java.util.List;
import javax.enterprise.context.ApplicationScoped;

import com.github.fge.jsonpatch.JsonPatchOperation;
import io.stackgres.operator.customresource.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.operator.mutation.DefaultValuesMutator;
import io.stackgres.operator.validation.BackupConfigReview;

@ApplicationScoped
public class BackupConfigDefaultValuesMutator
    extends DefaultValuesMutator<StackGresBackupConfig, BackupConfigReview>
    implements BackupMutator {

  @Override
  public List<JsonPatchOperation> mutate(BackupConfigReview review) {

    return mutate(SG_BACKUP_CONFIG_POINTER, review.getRequest().getObject());
  }

}

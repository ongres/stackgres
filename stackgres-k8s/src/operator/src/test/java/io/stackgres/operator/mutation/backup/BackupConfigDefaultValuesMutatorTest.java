/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.backup;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonpatch.JsonPatchException;

import io.stackgres.operator.common.BackupConfigReview;
import io.stackgres.operator.customresource.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.operator.customresource.sgbackupconfig.StackGresBackupConfigSpec;
import io.stackgres.operator.mutation.DefaultValuesMutator;
import io.stackgres.operator.mutation.DefaultValuesMutatorTest;
import io.stackgres.operator.utils.JsonUtil;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BackupConfigDefaultValuesMutatorTest extends DefaultValuesMutatorTest<StackGresBackupConfig, BackupConfigReview> {


  @Override
  protected DefaultValuesMutator<StackGresBackupConfig, BackupConfigReview> getMutatorInstance() {
    return new BackupConfigDefaultValuesMutator();
  }

  @Override
  protected BackupConfigReview getEmptyReview() {
    BackupConfigReview backupConfigReview = JsonUtil
        .readFromJson("backupconfig_allow_request/create.json", BackupConfigReview.class);
    backupConfigReview.getRequest().getObject().setSpec(new StackGresBackupConfigSpec());
    return backupConfigReview;
  }

  @Override
  protected BackupConfigReview getDefaultReview() {
    return JsonUtil
        .readFromJson("backupconfig_allow_request/create.json", BackupConfigReview.class);
  }

  @Override
  protected StackGresBackupConfig getDefaultResource() {
    return JsonUtil.readFromJson("backup_config/default.json", StackGresBackupConfig.class);
  }

  @Override
  protected JsonNode getConfJson(JsonNode crJson) {
    return crJson.get("spec");
  }

}
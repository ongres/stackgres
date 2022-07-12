/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.backupconfig;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonpatch.JsonPatchOperation;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigSpec;
import io.stackgres.common.crd.storages.AwsS3Storage;
import io.stackgres.operator.common.BackupConfigReview;
import io.stackgres.operator.mutation.DefaultValuesMutator;
import io.stackgres.operator.mutation.DefaultValuesMutatorTest;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BackupConfigDefaultValuesMutatorTest
    extends DefaultValuesMutatorTest<StackGresBackupConfig, BackupConfigReview> {

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

  @Test
  public void givenConfWithAllDefaultsValuesSettledButNotDefaultStorage_shouldNotReturnAnyPatch() {

    BackupConfigReview review = getDefaultReview();
    review.getRequest().getObject().getSpec().getStorage().setType("s3");
    AwsS3Storage s3 = new AwsS3Storage();
    s3.setBucket(
        review.getRequest().getObject().getSpec().getStorage().getS3Compatible().getBucket());
    s3.setPath(review.getRequest().getObject().getSpec().getStorage().getS3Compatible().getPath());
    s3.setRegion(
        review.getRequest().getObject().getSpec().getStorage().getS3Compatible().getRegion());
    s3.setStorageClass(
        review.getRequest().getObject().getSpec().getStorage().getS3Compatible().getStorageClass());
    s3.setAwsCredentials(review.getRequest().getObject().getSpec().getStorage().getS3Compatible()
        .getAwsCredentials());
    review.getRequest().getObject().getSpec().getStorage().setS3(s3);
    review.getRequest().getObject().getSpec().getStorage().setS3Compatible(null);

    List<JsonPatchOperation> operators = mutator.mutate(review);

    assertEquals(0, operators.size());

  }

}

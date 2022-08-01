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
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.common.BackupConfigReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.mutation.DefaultValuesMutator;
import io.stackgres.operator.mutation.DefaultValuesMutatorTest;
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
    BackupConfigReview backupConfigReview = AdmissionReviewFixtures.backupConfig()
        .loadCreate().get();
    backupConfigReview.getRequest().getObject().setSpec(new StackGresBackupConfigSpec());
    return backupConfigReview;
  }

  @Override
  protected BackupConfigReview getDefaultReview() {
    return AdmissionReviewFixtures.backupConfig().loadCreate().get();
  }

  @Override
  protected StackGresBackupConfig getDefaultResource() {
    return Fixtures.backupConfig().loadDefault().get();
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

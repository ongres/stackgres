/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.backupconfig;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.json.JsonMapper;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigSpec;
import io.stackgres.common.crd.storages.AwsS3Storage;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.common.BackupConfigReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operator.mutation.AbstractValuesMutator;
import io.stackgres.operator.mutation.DefaultValuesMutatorTest;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BackupConfigDefaultValuesMutatorTest
    extends DefaultValuesMutatorTest<StackGresBackupConfig, BackupConfigReview> {

  @Override
  protected AbstractValuesMutator<StackGresBackupConfig, BackupConfigReview> getMutatorInstance(
      DefaultCustomResourceFactory<StackGresBackupConfig> factory, JsonMapper jsonMapper) {
    return new BackupConfigDefaultValuesMutator(factory, jsonMapper);
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

    var result = mutator.mutate(review, JsonUtil.copy(review.getRequest().getObject()));

    assertEquals(review.getRequest().getObject(), result);
  }

}

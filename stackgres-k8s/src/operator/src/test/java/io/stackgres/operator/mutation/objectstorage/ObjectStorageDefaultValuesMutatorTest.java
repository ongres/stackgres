/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.objectstorage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.json.JsonMapper;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.crd.storages.AwsS3Storage;
import io.stackgres.common.crd.storages.BackupStorage;
import io.stackgres.operator.common.StackGresObjectStorageReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operator.initialization.DefaultObjectStorageFactory;
import io.stackgres.operator.mutation.DefaultValuesMutatorTest;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ObjectStorageDefaultValuesMutatorTest
    extends DefaultValuesMutatorTest<StackGresObjectStorage, StackGresObjectStorageReview, HasMetadata> {

  @Override
  protected ObjectStorageDefaultValuesMutator getMutatorInstance(
      DefaultCustomResourceFactory<StackGresObjectStorage, HasMetadata> factory, JsonMapper jsonMapper) {
    return new ObjectStorageDefaultValuesMutator(factory, jsonMapper);
  }

  @Override
  protected DefaultCustomResourceFactory<StackGresObjectStorage, HasMetadata> createFactory() {
    return new DefaultObjectStorageFactory();
  }

  @Override
  protected StackGresObjectStorageReview getEmptyReview() {
    final StackGresObjectStorageReview review = AdmissionReviewFixtures.objectStorage().loadCreate().get();
    review.getRequest().getObject().setSpec(new BackupStorage());
    return review;
  }

  @Override
  protected StackGresObjectStorageReview getDefaultReview() {
    return AdmissionReviewFixtures.objectStorage().loadCreate().get();
  }

  @Override
  protected StackGresObjectStorage getDefaultResource() {
    return factory.buildResource(
        new ConfigMapBuilder()
        .withNewMetadata()
        .withName("default")
        .endMetadata()
        .build());
  }

  @Test
  public void givenConfWithAllDefaultsValuesSettledButNotDefaultStorage_shouldNotReturnAnyPatch() {
    StackGresObjectStorageReview review = getDefaultReview();
    review.getRequest().getObject().getSpec().setType("s3");
    AwsS3Storage s3 = new AwsS3Storage();
    s3.setBucket(
        review.getRequest().getObject().getSpec().getS3Compatible().getBucket());
    s3.setRegion(
        review.getRequest().getObject().getSpec().getS3Compatible().getRegion());
    s3.setStorageClass(
        review.getRequest().getObject().getSpec().getS3Compatible().getStorageClass());
    s3.setAwsCredentials(review.getRequest().getObject().getSpec().getS3Compatible()
        .getAwsCredentials());
    review.getRequest().getObject().getSpec().setS3(s3);
    review.getRequest().getObject().getSpec().setS3Compatible(null);

    StackGresObjectStorage result =
        mutator.mutate(review, JsonUtil.copy(review.getRequest().getObject()));

    assertEquals(review.getRequest().getObject().getSpec(), result.getSpec());
  }

}

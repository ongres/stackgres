/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.objectstorage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonpatch.JsonPatchOperation;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.crd.storages.AwsS3Storage;
import io.stackgres.common.crd.storages.BackupStorage;
import io.stackgres.operator.common.ObjectStorageReview;
import io.stackgres.operator.mutation.DefaultValuesMutator;
import io.stackgres.operator.mutation.DefaultValuesMutatorTest;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@SuppressWarnings("removal")
@ExtendWith(MockitoExtension.class)
class ObjectStorageDefaultValuesMutatorTest
    extends DefaultValuesMutatorTest<StackGresObjectStorage, ObjectStorageReview> {

  @Override
  protected DefaultValuesMutator<StackGresObjectStorage, ObjectStorageReview> getMutatorInstance() {
    return new ObjectStorageDefaultValuesMutator();
  }

  @Override
  protected ObjectStorageReview getEmptyReview() {
    final ObjectStorageReview review = JsonUtil
        .readFromJson("objectstorage_allow_request/create.json", ObjectStorageReview.class);
    review.getRequest().getObject().setSpec(new BackupStorage());
    return review;
  }

  @Override
  protected ObjectStorageReview getDefaultReview() {
    return JsonUtil.readFromJson("objectstorage_allow_request/create.json",
        ObjectStorageReview.class);
  }

  @Override
  protected StackGresObjectStorage getDefaultResource() {
    return JsonUtil.readFromJson("objectstorage/default.json", StackGresObjectStorage.class);
  }

  @Override
  protected JsonNode getConfJson(JsonNode crJson) {
    return crJson.get("spec");
  }

  @Test
  public void givenConfWithAllDefaultsValuesSettledButNotDefaultStorage_shouldNotReturnAnyPatch() {

    ObjectStorageReview review = getDefaultReview();
    review.getRequest().getObject().getSpec().setType("s3");
    AwsS3Storage s3 = new AwsS3Storage();
    s3.setBucket(
        review.getRequest().getObject().getSpec().getS3Compatible().getBucket());
    s3.setPath(review.getRequest().getObject().getSpec().getS3Compatible().getPath());
    s3.setRegion(
        review.getRequest().getObject().getSpec().getS3Compatible().getRegion());
    s3.setStorageClass(
        review.getRequest().getObject().getSpec().getS3Compatible().getStorageClass());
    s3.setAwsCredentials(review.getRequest().getObject().getSpec().getS3Compatible()
        .getAwsCredentials());
    review.getRequest().getObject().getSpec().setS3(s3);
    review.getRequest().getObject().getSpec().setS3Compatible(null);

    List<JsonPatchOperation> operators = mutator.mutate(review);

    assertEquals(0, operators.size());

  }
}

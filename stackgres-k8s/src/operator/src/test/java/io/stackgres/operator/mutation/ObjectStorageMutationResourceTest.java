/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation;

import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.operator.common.StackGresObjectStorageReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operatorframework.admissionwebhook.mutating.AbstractMutationResource;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ObjectStorageMutationResourceTest
    extends MutationResourceTest<StackGresObjectStorage, StackGresObjectStorageReview> {

  @Override
  protected AbstractMutationResource<StackGresObjectStorage, StackGresObjectStorageReview> getResource() {
    return new ObjectStorageMutationResource(JsonUtil.jsonMapper(), pipeline);
  }

  @Override
  protected StackGresObjectStorageReview getReview() {
    return AdmissionReviewFixtures.objectStorage().loadCreate().get();
  }

}

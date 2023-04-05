/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation;

import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.operator.common.ObjectStorageReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operatorframework.admissionwebhook.mutating.MutationResource;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ObjectStorageMutationResourceTest
    extends MutationResourceTest<StackGresObjectStorage, ObjectStorageReview> {

  @Override
  protected MutationResource<StackGresObjectStorage, ObjectStorageReview> getResource() {
    return new ObjectStorageMutationResource(JsonUtil.jsonMapper(), pipeline);
  }

  @Override
  protected ObjectStorageReview getReview() {
    return AdmissionReviewFixtures.objectStorage().loadCreate().get();
  }

}

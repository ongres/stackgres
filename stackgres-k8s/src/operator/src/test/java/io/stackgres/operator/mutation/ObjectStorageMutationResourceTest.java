/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation;

import io.stackgres.operator.common.ObjectStorageReview;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ObjectStorageMutationResourceTest extends MutationResourceTest<ObjectStorageReview> {

  @BeforeEach
  void setUp() {
    final ObjectStorageMutationResource resource = new ObjectStorageMutationResource();
    resource.setPipeline(pipeline);
    this.resource = resource;

    review = JsonUtil
        .readFromJson("objectstorage_allow_request/create.json", ObjectStorageReview.class);
  }

}

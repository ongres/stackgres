/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.objectstorage;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.common.ObjectStorageReview;
import io.stackgres.operator.validation.DependenciesValidator;
import io.stackgres.operator.validation.DependenciesValidatorTest;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ObjectStorageDependenciesValidatorTest
    extends DependenciesValidatorTest<ObjectStorageReview, ObjectStorageDependenciesValidator> {

  @Override
  protected DependenciesValidator<ObjectStorageReview> setUpValidation() {
    return new ObjectStorageDependenciesValidator();
  }

  @Override
  protected ObjectStorageReview getReview_givenAReviewCreation_itShouldDoNothing() {
    return JsonUtil.readFromJson("objectstorage_allow_request/create.json",
        ObjectStorageReview.class);
  }

  @Override
  protected ObjectStorageReview getReview_givenAReviewUpdate_itShouldDoNothing() {
    return JsonUtil.readFromJson("objectstorage_allow_request/update.json",
        ObjectStorageReview.class);
  }

  @Override
  protected ObjectStorageReview getReview_givenAReviewDelete_itShouldFailIfAClusterDependsOnIt() {
    return JsonUtil.readFromJson("objectstorage_allow_request/delete.json",
        ObjectStorageReview.class);
  }

  @Override
  protected ObjectStorageReview
      getReview_givenAReviewDelete_itShouldNotFailIfNoClusterDependsOnIt() {
    return JsonUtil.readFromJson("objectstorage_allow_request/delete.json",
        ObjectStorageReview.class);
  }

  @Override
  protected ObjectStorageReview getReview_givenAReviewDelete_itShouldNotFailIfNoClusterExists() {
    return JsonUtil.readFromJson("objectstorage_allow_request/delete.json",
        ObjectStorageReview.class);
  }

  @Override
  protected void makeClusterNotDependant(StackGresCluster cluster) {
    cluster.getSpec().getConfiguration().setBackups(null);
  }
}

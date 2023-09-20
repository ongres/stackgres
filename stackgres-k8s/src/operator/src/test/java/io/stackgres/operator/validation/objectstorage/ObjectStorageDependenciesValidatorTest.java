/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.objectstorage;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.common.ObjectStorageReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.validation.DependenciesValidatorTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ObjectStorageDependenciesValidatorTest
    extends DependenciesValidatorTest<ObjectStorageReview, ObjectStorageDependenciesValidator> {

  @Override
  protected ObjectStorageDependenciesValidator setUpValidation() {
    return new ObjectStorageDependenciesValidator();
  }

  @Override
  protected ObjectStorageReview getReview_givenAReviewCreation_itShouldDoNothing() {
    return AdmissionReviewFixtures.objectStorage().loadCreate().get();
  }

  @Override
  protected ObjectStorageReview getReview_givenAReviewUpdate_itShouldDoNothing() {
    return AdmissionReviewFixtures.objectStorage().loadUpdate().get();
  }

  @Override
  protected ObjectStorageReview getReview_givenAReviewDelete_itShouldFailIfAClusterDependsOnIt() {
    return AdmissionReviewFixtures.objectStorage().loadDelete().get();
  }

  @Override
  protected ObjectStorageReview
      getReview_givenAReviewDelete_itShouldNotFailIfNoClusterDependsOnIt() {
    return AdmissionReviewFixtures.objectStorage().loadDelete().get();
  }

  @Override
  protected ObjectStorageReview getReview_givenAReviewDelete_itShouldNotFailIfNoClusterExists() {
    return AdmissionReviewFixtures.objectStorage().loadDelete().get();
  }

  @Override
  protected void makeClusterNotDependant(StackGresCluster cluster) {
    cluster.getSpec().getConfigurations().setBackups(null);
  }
}

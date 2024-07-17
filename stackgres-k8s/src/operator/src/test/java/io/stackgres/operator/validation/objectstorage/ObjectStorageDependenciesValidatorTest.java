/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.objectstorage;

import java.util.List;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterBackupConfigurationBuilder;
import io.stackgres.operator.common.StackGresObjectStorageReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.validation.DependenciesValidatorTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ObjectStorageDependenciesValidatorTest
    extends DependenciesValidatorTest<StackGresObjectStorageReview, ObjectStorageDependenciesValidator> {

  @Override
  protected ObjectStorageDependenciesValidator setUpValidation() {
    return new ObjectStorageDependenciesValidator();
  }

  @Override
  protected StackGresObjectStorageReview getReview_givenAReviewCreation_itShouldDoNothing() {
    return AdmissionReviewFixtures.objectStorage().loadCreate().get();
  }

  @Override
  protected StackGresObjectStorageReview getReview_givenAReviewUpdate_itShouldDoNothing() {
    return AdmissionReviewFixtures.objectStorage().loadUpdate().get();
  }

  @Override
  protected StackGresObjectStorageReview getReview_givenAReviewDelete_itShouldFailIfAClusterDependsOnIt() {
    return AdmissionReviewFixtures.objectStorage().loadDelete().get();
  }

  @Override
  protected StackGresObjectStorageReview
      getReview_givenAReviewDelete_itShouldNotFailIfNoClusterDependsOnIt() {
    return AdmissionReviewFixtures.objectStorage().loadDelete().get();
  }

  @Override
  protected StackGresObjectStorageReview getReview_givenAReviewDelete_itShouldNotFailIfNoClusterExists() {
    return AdmissionReviewFixtures.objectStorage().loadDelete().get();
  }

  @Override
  protected void makeClusterDependant(StackGresCluster cluster, StackGresObjectStorageReview review) {
    cluster.getSpec().getConfigurations().setBackups(List.of(
        new StackGresClusterBackupConfigurationBuilder()
        .withSgObjectStorage(review.getRequest().getName())
        .build()));
  }

  @Override
  protected void makeClusterNotDependant(StackGresCluster cluster) {
    cluster.getSpec().getConfigurations().setBackups(null);
  }
}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.objectstorage;

import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.operator.common.StackGresObjectStorageReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.validation.AbstractDefaultConfigKeeper;
import io.stackgres.operator.validation.DefaultKeeperTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultObjectStorageConfigKeeperTest
    extends DefaultKeeperTest<StackGresObjectStorage, StackGresObjectStorageReview> {

  @Override
  protected AbstractDefaultConfigKeeper<
      StackGresObjectStorage, StackGresObjectStorageReview> getValidatorInstance() {
    return new DefaultObjectStorageConfigKeeper();
  }

  @Override
  protected StackGresObjectStorageReview getCreationSample() {
    return AdmissionReviewFixtures.objectStorage().loadCreate().get();
  }

  @Override
  protected StackGresObjectStorageReview getDeleteSample() {
    return AdmissionReviewFixtures.objectStorage().loadDelete().get();
  }

  @Override
  protected StackGresObjectStorageReview getUpdateSample() {
    return AdmissionReviewFixtures.objectStorage().loadUpdate().get();
  }

}

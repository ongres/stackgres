/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.storage.StorageClass;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StorageClassValidatorTest {

  private static final StorageClass DEFAULT_STORAGE_CLASS = Fixtures.storageClass()
      .loadDefault().get();

  private StorageClassValidator validator;

  @Mock
  private ResourceFinder<StorageClass> storageClassFinder;

  @BeforeEach
  void setUp() {
    validator = new StorageClassValidator(storageClassFinder);
  }

  @Test
  void givenValidStorageClassOnCreation_shouldNotFail() throws ValidationFailed {

    final StackGresClusterReview review = AdmissionReviewFixtures.cluster().loadCreate().get();

    String storageClass =
        review.getRequest().getObject().getSpec().getPods().getPersistentVolume().getStorageClass();
    when(storageClassFinder.findByName(storageClass))
        .thenReturn(Optional.of(DEFAULT_STORAGE_CLASS));

    validator.validate(review);

    verify(storageClassFinder).findByName(eq(storageClass));

  }

  @Test
  void giveInvalidStorageClassOnCreation_shouldFail() {

    final StackGresClusterReview review = AdmissionReviewFixtures.cluster().loadCreate().get();

    String storageClass =
        review.getRequest().getObject().getSpec().getPods().getPersistentVolume().getStorageClass();

    when(storageClassFinder.findByName(storageClass))
        .thenReturn(Optional.empty());

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = ex.getMessage();

    assertEquals("StorageClass " + storageClass + " not found", resultMessage);

  }

  @Test
  void giveAnAttemptToUpdateToAUnknownStorageClass_shouldFail() {

    final StackGresClusterReview review = AdmissionReviewFixtures.cluster()
        .loadStorageClassConfigUpdate().get();

    String storageClass =
        review.getRequest().getObject().getSpec().getPods().getPersistentVolume().getStorageClass();

    when(storageClassFinder.findByName(storageClass))
        .thenReturn(Optional.empty());

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = ex.getMessage();

    assertEquals("Cannot update to StorageClass " + storageClass
        + " because it doesn't exists", resultMessage);

    verify(storageClassFinder).findByName(eq(storageClass));

  }

  @Test
  void giveAnAttemptToUpdateToAKnownStorageClass_shouldNotFail() throws ValidationFailed {

    final StackGresClusterReview review = AdmissionReviewFixtures.cluster()
        .loadStorageClassConfigUpdate().get();

    String storageClass =
        review.getRequest().getObject().getSpec().getPods().getPersistentVolume().getStorageClass();

    when(storageClassFinder.findByName(storageClass))
        .thenReturn(Optional.of(DEFAULT_STORAGE_CLASS));

    validator.validate(review);

    verify(storageClassFinder).findByName(eq(storageClass));

  }

  @Test
  void giveAnAttemptToDelete_shouldNotFail() throws ValidationFailed {

    final StackGresClusterReview review = AdmissionReviewFixtures.cluster()
        .loadStorageClassConfigUpdate().get();
    review.getRequest().setOperation(Operation.DELETE);

    validator.validate(review);

    verify(storageClassFinder, never()).findByName(anyString());

  }
}

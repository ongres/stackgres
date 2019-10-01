/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.storage.StorageClass;
import io.stackgres.operator.services.KubernetesResourceFinder;
import io.stackgres.operator.utils.JsonUtil;
import io.stackgres.operator.validation.AdmissionReview;
import io.stackgres.operator.validation.Operation;
import io.stackgres.operator.validation.ValidationFailed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
class StorageClassValidatorTest {

  private StorageClassValidator validator;

  @Mock
  private KubernetesResourceFinder<StorageClass> storageClassFinder;

  private final static StorageClass DEFAULT_STORAGE_CLASS = JsonUtil.readFromJson("storage_class/standard.json", StorageClass.class);

  @BeforeEach
  void setUp() {
    validator = new StorageClassValidator(storageClassFinder);

  }

  @Test
  void givenValidStorageClassOnCreation_shouldNotFail() throws ValidationFailed {

    final AdmissionReview review = JsonUtil
        .readFromJson("allowed_requests/valid_creation.json", AdmissionReview.class);

    String storageClass = review.getRequest().getObject().getSpec().getStorageClass();
    when(storageClassFinder.findByName(storageClass))
        .thenReturn(Optional.of(DEFAULT_STORAGE_CLASS));

    validator.validate(review);

    verify(storageClassFinder).findByName(eq(storageClass));

  }

  @Test
  void giveInvalidStorageClassOnCreation_shouldFail() {

    final AdmissionReview review = JsonUtil
        .readFromJson("allowed_requests/valid_creation.json", AdmissionReview.class);

    String storageClass = review.getRequest().getObject().getSpec().getStorageClass();

    when(storageClassFinder.findByName(storageClass))
        .thenReturn(Optional.empty());

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = ex.getMessage();

    assertEquals("Storage class " + storageClass + " not found", resultMessage);

  }

  @Test
  void giveAnAttemptToUpdateToAUnknownStorageClass_shouldFail() {

    final AdmissionReview review = JsonUtil
        .readFromJson("allowed_requests/storage_class_config_update.json", AdmissionReview.class);

    String storageClass = review.getRequest().getObject().getSpec().getStorageClass();

    when(storageClassFinder.findByName(storageClass))
        .thenReturn(Optional.empty());

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = ex.getMessage();

    assertEquals("Cannot update to storage class " + storageClass
        + " because it doesn't exists", resultMessage);

    verify(storageClassFinder).findByName(eq(storageClass));

  }

  @Test
  void giveAnAttemptToUpdateToAKnownStorageClass_shouldNotFail() throws ValidationFailed {

    final AdmissionReview review = JsonUtil
        .readFromJson("allowed_requests/storage_class_config_update.json",
            AdmissionReview.class);

    String storageClass = review.getRequest().getObject().getSpec().getStorageClass();

    when(storageClassFinder.findByName(storageClass))
        .thenReturn(Optional.of(DEFAULT_STORAGE_CLASS));

    validator.validate(review);

    verify(storageClassFinder).findByName(eq(storageClass));

  }

  @Test
  void giveAnAttemptToDelete_shouldNotFail() throws ValidationFailed {

    final AdmissionReview review = JsonUtil
        .readFromJson("allowed_requests/storage_class_config_update.json",
            AdmissionReview.class);
    review.getRequest().setOperation(Operation.DELETE);

    String storageClass = review.getRequest().getObject().getSpec().getStorageClass();

    validator.validate(review);

    verify(storageClassFinder, never()).findByName(anyString());

  }
}

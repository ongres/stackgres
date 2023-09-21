/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardedcluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.storage.StorageClass;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShardBuilder;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
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
    final StackGresShardedClusterReview review = AdmissionReviewFixtures.shardedCluster()
        .loadCreate().get();

    review.getRequest().getObject().getSpec().getCoordinator()
        .getPods().getPersistentVolume().setStorageClass("coordinator");
    review.getRequest().getObject().getSpec().getShards()
        .getPods().getPersistentVolume().setStorageClass("shards");
    String coordinatorStorageClass =
        review.getRequest().getObject().getSpec().getCoordinator()
        .getPods().getPersistentVolume().getStorageClass();
    String shardsStorageClass =
        review.getRequest().getObject().getSpec().getShards()
        .getPods().getPersistentVolume().getStorageClass();
    when(storageClassFinder.findByName(coordinatorStorageClass))
        .thenReturn(Optional.of(DEFAULT_STORAGE_CLASS));
    when(storageClassFinder.findByName(shardsStorageClass))
        .thenReturn(Optional.of(DEFAULT_STORAGE_CLASS));

    validator.validate(review);

    verify(storageClassFinder).findByName(eq(coordinatorStorageClass));
    verify(storageClassFinder).findByName(eq(shardsStorageClass));
  }

  @Test
  void givenValidStorageClassOnCreationWithOverrides_shouldNotFail() throws ValidationFailed {
    final StackGresShardedClusterReview review = AdmissionReviewFixtures.shardedCluster()
        .loadCreate().get();

    review.getRequest().getObject().getSpec().getCoordinator()
        .getPods().getPersistentVolume().setStorageClass("coordinator");
    review.getRequest().getObject().getSpec().getShards()
        .getPods().getPersistentVolume().setStorageClass("shards");
    review.getRequest().getObject().getSpec().getShards().setOverrides(List.of(
        new StackGresShardedClusterShardBuilder()
        .withIndex(0)
        .withNewPodsForShards()
        .withNewPersistentVolume()
        .withStorageClass("overrideShards")
        .endPersistentVolume()
        .endPodsForShards()
        .build()));
    String coordinatorStorageClass =
        review.getRequest().getObject().getSpec().getCoordinator()
        .getPods().getPersistentVolume().getStorageClass();
    String shardsStorageClass =
        review.getRequest().getObject().getSpec().getShards()
        .getPods().getPersistentVolume().getStorageClass();
    String overrideShardsStorageClass =
        review.getRequest().getObject().getSpec().getShards().getOverrides().get(0)
        .getPodsForShards().getPersistentVolume().getStorageClass();
    when(storageClassFinder.findByName(coordinatorStorageClass))
        .thenReturn(Optional.of(DEFAULT_STORAGE_CLASS));
    when(storageClassFinder.findByName(shardsStorageClass))
        .thenReturn(Optional.of(DEFAULT_STORAGE_CLASS));
    when(storageClassFinder.findByName(overrideShardsStorageClass))
        .thenReturn(Optional.of(DEFAULT_STORAGE_CLASS));

    validator.validate(review);

    verify(storageClassFinder).findByName(eq(coordinatorStorageClass));
    verify(storageClassFinder).findByName(eq(shardsStorageClass));
    verify(storageClassFinder).findByName(eq(overrideShardsStorageClass));
  }

  @Test
  void giveInvalidCoordinatorStorageClassOnCreation_shouldFail() {
    final StackGresShardedClusterReview review = AdmissionReviewFixtures.shardedCluster()
        .loadCreate().get();

    String storageClass =
        review.getRequest().getObject().getSpec().getCoordinator()
        .getPods().getPersistentVolume().getStorageClass();

    when(storageClassFinder.findByName(storageClass))
        .thenReturn(Optional.empty());

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = ex.getMessage();

    assertEquals("Storage class " + storageClass + " not found for coordinator", resultMessage);
  }

  @Test
  void giveInvalidShardsStorageClassOnCreation_shouldFail() {
    final StackGresShardedClusterReview review = AdmissionReviewFixtures.shardedCluster()
        .loadCreate().get();

    review.getRequest().getObject().getSpec().getShards()
        .getPods().getPersistentVolume().setStorageClass("test");
    String storageClass =
        review.getRequest().getObject().getSpec().getShards()
        .getPods().getPersistentVolume().getStorageClass();

    when(storageClassFinder.findByName(review.getRequest().getObject().getSpec().getCoordinator()
        .getPods().getPersistentVolume().getStorageClass()))
        .thenReturn(Optional.of(DEFAULT_STORAGE_CLASS));
    when(storageClassFinder.findByName(storageClass))
        .thenReturn(Optional.empty());

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = ex.getMessage();

    assertEquals("Storage class " + storageClass + " not found for shards", resultMessage);
  }

  @Test
  void giveInvalidOverrideShardsStorageClassOnCreation_shouldFail() {
    final StackGresShardedClusterReview review = AdmissionReviewFixtures.shardedCluster()
        .loadCreate().get();

    review.getRequest().getObject().getSpec().getShards()
        .getPods().getPersistentVolume().setStorageClass("test");
    review.getRequest().getObject().getSpec().getShards().setOverrides(List.of(
        new StackGresShardedClusterShardBuilder()
        .withIndex(0)
        .withNewPodsForShards()
        .withNewPersistentVolume()
        .withStorageClass("overrideTest")
        .endPersistentVolume()
        .endPodsForShards()
        .build()));
    String storageClass =
        review.getRequest().getObject().getSpec().getShards()
        .getPods().getPersistentVolume().getStorageClass();
    String overrideStorageClass =
        review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0)
        .getPodsForShards().getPersistentVolume().getStorageClass();

    when(storageClassFinder.findByName(review.getRequest().getObject().getSpec().getCoordinator()
        .getPods().getPersistentVolume().getStorageClass()))
        .thenReturn(Optional.of(DEFAULT_STORAGE_CLASS));
    when(storageClassFinder.findByName(storageClass))
        .thenReturn(Optional.of(DEFAULT_STORAGE_CLASS));
    when(storageClassFinder.findByName(overrideStorageClass))
        .thenReturn(Optional.empty());

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = ex.getMessage();

    assertEquals("Storage class " + overrideStorageClass + " not found for shard 0", resultMessage);
  }

  @Test
  void giveAnAttemptToUpdateToAUnknownCoordinatorStorageClass_shouldFail() {
    final StackGresShardedClusterReview review = AdmissionReviewFixtures.shardedCluster()
        .loadStorageClassConfigUpdate().get();

    String storageClass =
        review.getRequest().getObject().getSpec().getCoordinator()
        .getPods().getPersistentVolume().getStorageClass();

    when(storageClassFinder.findByName(storageClass))
        .thenReturn(Optional.empty());

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = ex.getMessage();

    assertEquals("Cannot update coordinator to storage class " + storageClass
        + " because it doesn't exists", resultMessage);

    verify(storageClassFinder).findByName(eq(storageClass));
  }

  @Test
  void giveAnAttemptToUpdateToAUnknownShardsStorageClass_shouldFail() {
    final StackGresShardedClusterReview review = AdmissionReviewFixtures.shardedCluster()
        .loadUpdate().get();

    review.getRequest().getObject().getSpec().getShards()
        .getPods().getPersistentVolume().setStorageClass("test");
    String storageClass =
        review.getRequest().getObject().getSpec().getShards()
        .getPods().getPersistentVolume().getStorageClass();

    when(storageClassFinder.findByName(review.getRequest().getObject().getSpec().getCoordinator()
        .getPods().getPersistentVolume().getStorageClass()))
        .thenReturn(Optional.of(DEFAULT_STORAGE_CLASS));
    when(storageClassFinder.findByName(storageClass))
        .thenReturn(Optional.empty());

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = ex.getMessage();

    assertEquals("Cannot update shards to storage class " + storageClass
        + " because it doesn't exists", resultMessage);

    verify(storageClassFinder).findByName(eq(storageClass));
  }

  @Test
  void giveAnAttemptToUpdateToAUnknownOvverideShardsStorageClass_shouldFail() {
    final StackGresShardedClusterReview review = AdmissionReviewFixtures.shardedCluster()
        .loadUpdate().get();

    review.getRequest().getObject().getSpec().getShards()
        .getPods().getPersistentVolume().setStorageClass("test");
    review.getRequest().getObject().getSpec().getShards().setOverrides(List.of(
        new StackGresShardedClusterShardBuilder()
        .withIndex(0)
        .withNewPodsForShards()
        .withNewPersistentVolume()
        .withStorageClass("overrideTest")
        .endPersistentVolume()
        .endPodsForShards()
        .build()));
    String storageClass =
        review.getRequest().getObject().getSpec().getShards()
        .getPods().getPersistentVolume().getStorageClass();
    String overrideStorageClass =
        review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0)
        .getPodsForShards().getPersistentVolume().getStorageClass();

    when(storageClassFinder.findByName(review.getRequest().getObject().getSpec().getCoordinator()
        .getPods().getPersistentVolume().getStorageClass()))
        .thenReturn(Optional.of(DEFAULT_STORAGE_CLASS));
    when(storageClassFinder.findByName(storageClass))
        .thenReturn(Optional.of(DEFAULT_STORAGE_CLASS));
    when(storageClassFinder.findByName(overrideStorageClass))
        .thenReturn(Optional.empty());

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = ex.getMessage();

    assertEquals("Cannot update shard 0 to storage class " + overrideStorageClass
        + " because it doesn't exists", resultMessage);

    verify(storageClassFinder).findByName(eq(storageClass));
  }

  @Test
  void giveAnAttemptToUpdateToAKnownCoordinatorStorageClass_shouldNotFail()
      throws ValidationFailed {
    final StackGresShardedClusterReview review = AdmissionReviewFixtures.shardedCluster()
        .loadStorageClassConfigUpdate().get();

    review.getRequest().getObject().getSpec().getCoordinator()
        .getPods().getPersistentVolume().setStorageClass("test");
    String coordinatorStorageClass =
        review.getRequest().getObject().getSpec().getCoordinator()
        .getPods().getPersistentVolume().getStorageClass();
    String shardsStorageClass =
        review.getRequest().getObject().getSpec().getShards()
        .getPods().getPersistentVolume().getStorageClass();

    when(storageClassFinder.findByName(coordinatorStorageClass))
        .thenReturn(Optional.of(DEFAULT_STORAGE_CLASS));
    when(storageClassFinder.findByName(shardsStorageClass))
        .thenReturn(Optional.of(DEFAULT_STORAGE_CLASS));

    validator.validate(review);

    verify(storageClassFinder).findByName(eq(coordinatorStorageClass));
    verify(storageClassFinder).findByName(eq(shardsStorageClass));
  }

  @Test
  void giveAnAttemptToUpdateToAKnownShardsStorageClass_shouldNotFail()
      throws ValidationFailed {
    final StackGresShardedClusterReview review = AdmissionReviewFixtures.shardedCluster()
        .loadStorageClassConfigUpdate().get();

    String coordinatorStorageClass =
        review.getRequest().getObject().getSpec().getShards()
        .getPods().getPersistentVolume().getStorageClass();
    review.getRequest().getObject().getSpec().getShards()
        .getPods().getPersistentVolume().setStorageClass("test");
    String shardsStorageClass =
        review.getRequest().getObject().getSpec().getShards()
        .getPods().getPersistentVolume().getStorageClass();

    when(storageClassFinder.findByName(coordinatorStorageClass))
        .thenReturn(Optional.of(DEFAULT_STORAGE_CLASS));
    when(storageClassFinder.findByName(shardsStorageClass))
        .thenReturn(Optional.of(DEFAULT_STORAGE_CLASS));

    validator.validate(review);

    verify(storageClassFinder).findByName(eq(coordinatorStorageClass));
    verify(storageClassFinder).findByName(eq(shardsStorageClass));
  }

  @Test
  void giveAnAttemptToUpdateToAKnownOverrideShardsStorageClass_shouldNotFail()
      throws ValidationFailed {
    final StackGresShardedClusterReview review = AdmissionReviewFixtures.shardedCluster()
        .loadStorageClassConfigUpdate().get();

    String coordinatorStorageClass =
        review.getRequest().getObject().getSpec().getShards()
        .getPods().getPersistentVolume().getStorageClass();
    review.getRequest().getObject().getSpec().getShards()
        .getPods().getPersistentVolume().setStorageClass("test");
    review.getRequest().getObject().getSpec().getShards().setOverrides(List.of(
        new StackGresShardedClusterShardBuilder()
        .withIndex(0)
        .withNewPodsForShards()
        .withNewPersistentVolume()
        .withStorageClass("overrideTest")
        .endPersistentVolume()
        .endPodsForShards()
        .build()));
    String shardsStorageClass =
        review.getRequest().getObject().getSpec().getShards()
        .getPods().getPersistentVolume().getStorageClass();
    String overrideShardsStorageClass =
        review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0)
        .getPodsForShards().getPersistentVolume().getStorageClass();

    when(storageClassFinder.findByName(coordinatorStorageClass))
        .thenReturn(Optional.of(DEFAULT_STORAGE_CLASS));
    when(storageClassFinder.findByName(shardsStorageClass))
        .thenReturn(Optional.of(DEFAULT_STORAGE_CLASS));
    when(storageClassFinder.findByName(overrideShardsStorageClass))
        .thenReturn(Optional.of(DEFAULT_STORAGE_CLASS));

    validator.validate(review);

    verify(storageClassFinder).findByName(eq(coordinatorStorageClass));
    verify(storageClassFinder).findByName(eq(shardsStorageClass));
    verify(storageClassFinder).findByName(eq(overrideShardsStorageClass));
  }

  @Test
  void giveAnAttemptToDelete_shouldNotFail() throws ValidationFailed {
    final StackGresShardedClusterReview review = AdmissionReviewFixtures.shardedCluster()
        .loadStorageClassConfigUpdate().get();
    review.getRequest().setOperation(Operation.DELETE);

    validator.validate(review);

    verify(storageClassFinder, never()).findByName(anyString());
  }

}

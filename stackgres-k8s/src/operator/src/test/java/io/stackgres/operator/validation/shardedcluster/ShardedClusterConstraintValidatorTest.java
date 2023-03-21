/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardedcluster;

import javax.validation.constraints.Pattern;

import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresPodPersistentVolume;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.validation.ConstraintValidationTest;
import io.stackgres.operator.validation.ConstraintValidator;
import org.junit.jupiter.api.Test;

class ShardedClusterConstraintValidatorTest
    extends ConstraintValidationTest<StackGresShardedClusterReview> {

  @Override
  protected ConstraintValidator<StackGresShardedClusterReview> buildValidator() {
    return new ShardedClusterConstraintValidator();
  }

  @Override
  protected StackGresShardedClusterReview getValidReview() {
    return AdmissionReviewFixtures.shardedCluster().loadCreate().get();
  }

  @Override
  protected StackGresShardedClusterReview getInvalidReview() {
    final StackGresShardedClusterReview review =
        AdmissionReviewFixtures.shardedCluster().loadCreate().get();

    review.getRequest().getObject().setSpec(null);
    return review;
  }

  @Test
  void nullSpec_shouldFail() {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().setSpec(null);

    checkNotNullErrorCause(StackGresShardedCluster.class, "spec", review);
  }

  @Test
  void nullCoordinatorResourceProfile_shouldFail() {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getCoordinator().setResourceProfile(null);

    checkNotNullErrorCause(StackGresClusterSpec.class,
        "spec.coordinator.sgInstanceProfile", review);
  }

  @Test
  void nullShardsResourceProfile_shouldFail() {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getShards().setResourceProfile(null);

    checkNotNullErrorCause(StackGresClusterSpec.class,
        "spec.shards.sgInstanceProfile", review);
  }

  @Test
  void nullCoordinatorVolumeSize_shouldFail() {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getCoordinator()
        .getPod().getPersistentVolume().setSize(null);

    checkNotNullErrorCause(StackGresPodPersistentVolume.class,
        "spec.coordinator.pods.persistentVolume.size",
        review);
  }

  @Test
  void nullShardsVolumeSize_shouldFail() {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getShards()
        .getPod().getPersistentVolume().setSize(null);

    checkNotNullErrorCause(StackGresPodPersistentVolume.class,
        "spec.shards.pods.persistentVolume.size",
        review);
  }

  @Test
  void invalidCoordinatorVolumeSize_shouldFail() {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getCoordinator()
        .getPod().getPersistentVolume().setSize("512");

    checkErrorCause(StackGresPodPersistentVolume.class,
        "spec.coordinator.pods.persistentVolume.size",
        review, Pattern.class);
  }

  @Test
  void invalidShardsVolumeSize_shouldFail() {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getShards()
        .getPod().getPersistentVolume().setSize("512");

    checkErrorCause(StackGresPodPersistentVolume.class,
        "spec.shards.pods.persistentVolume.size",
        review, Pattern.class);
  }

}

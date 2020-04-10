/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import javax.validation.constraints.Pattern;

import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresPodPersistenceVolume;
import io.stackgres.operator.utils.JsonUtil;
import io.stackgres.operator.validation.ConstraintValidationTest;
import io.stackgres.operator.validation.ConstraintValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ClusterConstraintValidatorTest extends ConstraintValidationTest<StackGresClusterReview> {

  @Override
  protected ConstraintValidator<StackGresClusterReview> buildValidator() {
    return new ClusterConstraintValidator();
  }

  @Override
  protected StackGresClusterReview getValidReview() {
    return JsonUtil.readFromJson("cluster_allow_requests/valid_creation.json",
        StackGresClusterReview.class);
  }

  @Override
  protected StackGresClusterReview getInvalidReview() {
    final StackGresClusterReview review = JsonUtil
        .readFromJson("cluster_allow_requests/valid_creation.json",
            StackGresClusterReview.class);

    review.getRequest().getObject().setSpec(null);
    return review;
  }

  @Test
  void nullSpec_shouldFail() {
    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().setSpec(null);

    checkNotNullErrorCause(StackGresCluster.class, "spec", review);
  }

  @Test
  void nullResourceProfile_shouldFail() {
    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().setResourceProfile(null);

    checkNotNullErrorCause(StackGresClusterSpec.class, "spec.resourceProfile", review);
  }

  @Test
  void nullVolumeSize_shouldFail() {

    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getPod().getPersistentVolume().setVolumeSize(null);

    checkNotNullErrorCause(StackGresPodPersistenceVolume.class, "spec.pod.persistentVolume.volumeSize", review);
  }

  @Test
  void invalidVolumeSize_shouldFail() {

    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getPod().getPersistentVolume().setVolumeSize("512");

    checkErrorCause(StackGresPodPersistenceVolume.class, "spec.pod.persistentVolume.volumeSize",
        review, Pattern.class);

  }
}
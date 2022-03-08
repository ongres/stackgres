/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.validation.PersistentVolumeSizeExpansionValidator;
import io.stackgres.operator.validation.PersistentVolumeSizeExpansionValidatorTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClusterPersistentVolumeSizeExpansionValidatorTest extends
    PersistentVolumeSizeExpansionValidatorTest<StackGresClusterReview, StackGresCluster> {

  @Override
  protected StackGresClusterReview getAdmissionReview() {
    return AdmissionReviewFixtures.cluster().loadUpdate().get();
  }

  @Override
  protected void setVolumeSize(StackGresCluster cluster, String size) {
    cluster.getSpec().getPod().getPersistentVolume().setSize(size);
  }

  @Override
  protected void setStorageClassName(StackGresCluster cluster, String storageClassName) {
    cluster.getSpec().getPod().getPersistentVolume().setStorageClass(storageClassName);
  }

  @Override
  protected String getStorageClassName(StackGresCluster cluster) {
    return cluster.getSpec().getPod().getPersistentVolume().getStorageClass();
  }

  @Override
  protected PersistentVolumeSizeExpansionValidator<StackGresClusterReview,
      StackGresCluster> getValidator() {
    return new ClusterPersistentVolumeSizeExpansionValidator(finder, pvcScanner, labelFactory);
  }

}

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
    PersistentVolumeSizeExpansionValidatorTest<StackGresClusterReview,
        StackGresCluster, StackGresCluster> {

  @Override
  protected StackGresClusterReview getAdmissionReview() {
    return AdmissionReviewFixtures.cluster().loadUpdate().get();
  }

  @Override
  protected PersistentVolumeSizeExpansionValidator<StackGresClusterReview,
      StackGresCluster, StackGresCluster> getValidator() {
    return new ClusterPersistentVolumeSizeExpansionValidator(finder, pvcScanner, labelFactory);
  }

  @Override
  protected StackGresCluster getCluster(StackGresCluster resource) {
    return resource;
  }

  @Override
  protected void setVolumeSize(StackGresCluster cluster, String size) {
    cluster.getSpec().getPods().getPersistentVolume().setSize(size);
  }

  @Override
  protected void setStorageClassName(StackGresCluster cluster, String storageClassName) {
    cluster.getSpec().getPods().getPersistentVolume().setStorageClass(storageClassName);
  }

  @Override
  protected String getStorageClassName(StackGresCluster cluster) {
    return cluster.getSpec().getPods().getPersistentVolume().getStorageClass();
  }

}

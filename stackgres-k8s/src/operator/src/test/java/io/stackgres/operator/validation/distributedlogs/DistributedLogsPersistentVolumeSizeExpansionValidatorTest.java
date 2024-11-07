/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.distributedlogs;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.labels.LabelFactoryForDistributedLogs;
import io.stackgres.operator.common.StackGresDistributedLogsReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.conciliation.factory.distributedlogs.DistributedLogsCluster;
import io.stackgres.operator.validation.PersistentVolumeSizeExpansionValidator;
import io.stackgres.operator.validation.PersistentVolumeSizeExpansionValidatorTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DistributedLogsPersistentVolumeSizeExpansionValidatorTest extends
    PersistentVolumeSizeExpansionValidatorTest<StackGresDistributedLogsReview,
        StackGresDistributedLogs, StackGresDistributedLogs> {

  @Mock
  LabelFactoryForDistributedLogs labelFactoryForDistributedLogs;

  @Override
  protected StackGresDistributedLogsReview getAdmissionReview() {
    return AdmissionReviewFixtures.distributedLogs().loadUpdate().get();
  }

  @Override
  protected PersistentVolumeSizeExpansionValidator<StackGresDistributedLogsReview,
      StackGresDistributedLogs> getValidator() {
    return new DistributedLogsPersistentVolumeSizeExpansionValidator(
        finder,
        pvcScanner,
        labelFactoryForDistributedLogs,
        labelFactory
    );
  }

  @Override
  protected StackGresCluster getCluster(StackGresDistributedLogs resource) {
    return DistributedLogsCluster.getCluster(labelFactoryForDistributedLogs, resource);
  }

  @Override
  protected void setVolumeSize(StackGresDistributedLogs cluster, String size) {
    cluster.getSpec().getPersistentVolume().setSize(size);
  }

  @Override
  protected void setStorageClassName(StackGresDistributedLogs cluster, String storageClassName) {
    cluster.getSpec().getPersistentVolume().setStorageClass(storageClassName);
  }

  @Override
  protected String getStorageClassName(StackGresDistributedLogs cluster) {
    return cluster.getSpec().getPersistentVolume().getStorageClass();
  }
}

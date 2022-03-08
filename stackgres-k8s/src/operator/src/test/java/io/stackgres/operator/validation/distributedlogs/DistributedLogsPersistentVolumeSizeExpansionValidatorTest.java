/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.distributedlogs;

import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.operator.common.StackGresDistributedLogsReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.validation.PersistentVolumeSizeExpansionValidator;
import io.stackgres.operator.validation.PersistentVolumeSizeExpansionValidatorTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DistributedLogsPersistentVolumeSizeExpansionValidatorTest extends
    PersistentVolumeSizeExpansionValidatorTest<StackGresDistributedLogsReview,
        StackGresDistributedLogs> {

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
        labelFactory
    );
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

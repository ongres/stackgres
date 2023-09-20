/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import javax.inject.Singleton;

import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

@Singleton
@ValidationType(ErrorType.INVALID_CR_REFERENCE)
public class BackupConfigValidator implements ClusterValidator {

  @Override
  public void validate(StackGresClusterReview review) throws ValidationFailed {
    Operation operation = review.getRequest().getOperation();
    if (operation == Operation.CREATE || operation == Operation.UPDATE) {
      StackGresCluster cluster = review.getRequest().getObject();
      StackGresClusterConfigurations configuration = cluster.getSpec().getConfigurations();
      if (configuration.getSgBackupConfig() != null || configuration.getBackupPath() != null) {
        fail("spec.configuration.sgBackupConfig and spec.configuration.backupPath are deprecated,"
            + " please use the new spec.configuration.backups section and define a "
            + StackGresObjectStorage.KIND);
      }
    }
  }

}

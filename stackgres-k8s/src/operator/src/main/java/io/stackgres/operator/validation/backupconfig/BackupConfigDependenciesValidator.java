/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.backupconfig;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.inject.Inject;

import io.stackgres.operator.common.ArcUtil;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.resource.KubernetesCustomResourceScanner;
import io.stackgres.operator.validation.BackupConfigReview;
import io.stackgres.operator.validation.DependenciesValidator;
import io.stackgres.operatorframework.ValidationFailed;

@ApplicationScoped
public class BackupConfigDependenciesValidator extends DependenciesValidator<BackupConfigReview>
    implements BackupConfigValidator {

  @Inject
  public BackupConfigDependenciesValidator(
      @Any KubernetesCustomResourceScanner<StackGresCluster> clusterScanner) {
    super(clusterScanner);
  }

  public BackupConfigDependenciesValidator() {
    super(null);
    ArcUtil.checkPublicNoArgsConstructorIsCalledFromArc();
  }

  @Override
  public void validate(BackupConfigReview review, StackGresCluster i) throws ValidationFailed {
    if (review.getRequest().getName().equals(i.getSpec().getBackupConfig())) {
      fail(review, i);
    }
  }

}

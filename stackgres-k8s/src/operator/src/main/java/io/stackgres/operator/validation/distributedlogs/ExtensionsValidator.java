/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.distributedlogs;

import java.util.List;
import java.util.Optional;

import javax.inject.Singleton;

import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsSpec;
import io.stackgres.operator.common.StackGresDistributedLogsReview;
import io.stackgres.operator.validation.AbstractExtensionsValidator;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

@Singleton
public class ExtensionsValidator extends AbstractExtensionsValidator<StackGresDistributedLogsReview>
    implements DistributedLogsValidator {

  @Override
  protected Optional<List<StackGresClusterExtension>> getPostgresExtensions(
      StackGresDistributedLogsReview review) {
    return Optional.empty();
  }

  @Override
  protected Optional<List<StackGresClusterInstalledExtension>> getToInstallExtensions(
      StackGresDistributedLogsReview review) {
    return Optional.ofNullable(review.getRequest().getObject().getSpec())
        .map(StackGresDistributedLogsSpec::getToInstallPostgresExtensions);
  }

  @Override
  protected void failValidation(String reason, String message) throws ValidationFailed {
    fail(reason, message);
  }

}

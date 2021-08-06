/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import java.util.List;
import java.util.Optional;

import javax.inject.Singleton;

import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.validation.AbstractExtensionsValidator;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

@Singleton
public class ExtensionsValidator extends AbstractExtensionsValidator<StackGresClusterReview>
    implements ClusterValidator {

  @Override
  protected Optional<List<StackGresClusterExtension>> getPostgresExtensions(
      StackGresClusterReview review) {
    return Optional.ofNullable(review.getRequest().getObject().getSpec())
        .map(StackGresClusterSpec::getPostgresExtensions);
  }

  @Override
  protected Optional<List<StackGresClusterInstalledExtension>> getToInstallExtensions(
      StackGresClusterReview review) {
    return Optional.ofNullable(review.getRequest().getObject().getSpec())
        .map(StackGresClusterSpec::getToInstallPostgresExtensions);
  }

  @Override
  protected void failValidation(String reason, String message) throws ValidationFailed {
    fail(reason, message);
  }

}

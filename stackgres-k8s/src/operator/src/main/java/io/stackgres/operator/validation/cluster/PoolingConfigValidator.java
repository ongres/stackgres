/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.validation.AbstractReferenceValidator;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

@Singleton
@ValidationType(ErrorType.INVALID_CR_REFERENCE)
public class PoolingConfigValidator
    extends AbstractReferenceValidator<
      StackGresCluster, StackGresClusterReview, StackGresPoolingConfig>
    implements ClusterValidator {

  @Inject
  public PoolingConfigValidator(
      CustomResourceFinder<StackGresPoolingConfig> configFinder) {
    super(configFinder);
  }

  @Override
  protected Class<StackGresPoolingConfig> getReferenceClass() {
    return StackGresPoolingConfig.class;
  }

  @Override
  protected String getReference(StackGresCluster resource) {
    return resource.getSpec().getConfigurations().getSgPoolingConfig();
  }

  @Override
  protected boolean checkReferenceFilter(StackGresClusterReview review) {
    return !Optional.ofNullable(review.getRequest().getDryRun()).orElse(false)
        && Boolean.FALSE.equals(
        review.getRequest().getObject().getSpec().getPods().getDisableConnectionPooling());
  }

  @Override
  protected void onNotFoundReference(String message) throws ValidationFailed {
    fail(message);
  }

}

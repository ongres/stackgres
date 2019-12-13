/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pgbouncer;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.operator.common.PgBouncerReview;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.customresource.sgcluster.StackGresClusterList;
import io.stackgres.operator.resource.KubernetesResourceScanner;
import io.stackgres.operator.validation.DependenciesValidator;
import io.stackgres.operatorframework.ValidationFailed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class PgBouncerDependenciesValidator extends DependenciesValidator<PgBouncerReview>
    implements PgBouncerValidator {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(PgBouncerDependenciesValidator.class);

  @Inject
  public PgBouncerDependenciesValidator(
      KubernetesResourceScanner<StackGresClusterList> clusterScanner) {
    super(clusterScanner);
  }

  @Override
  public void validate(PgBouncerReview review, StackGresCluster i) throws ValidationFailed {
    LOGGER.info("validating deletion of " + review.getRequest().getName());
    if (review.getRequest().getName().equals(i.getSpec().getConnectionPoolingConfig())) {
      fail(review, i);
    }
  }

}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pgconfig;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.inject.Inject;

import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.customresource.sgcluster.StackGresClusterList;
import io.stackgres.operator.resource.KubernetesResourceScanner;
import io.stackgres.operator.validation.DependenciesValidator;
import io.stackgres.operator.validation.PgConfigReview;
import io.stackgres.operatorframework.ValidationFailed;

@ApplicationScoped
public class PgConfigDependenciesValidator extends DependenciesValidator<PgConfigReview>
    implements PgConfigValidator {

  @Inject
  public PgConfigDependenciesValidator(
      @Any KubernetesResourceScanner<StackGresClusterList> clusterScanner) {
    super(clusterScanner);
  }

  @Override
  public void validate(PgConfigReview review, StackGresCluster i) throws ValidationFailed {
    if (review.getRequest().getName().equals(i.getSpec().getPostgresConfig())) {
      fail(review, i);
    }
  }

}

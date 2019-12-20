/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pgconfig;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.operator.common.ArcUtil;
import io.stackgres.operator.common.PgConfigReview;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.resource.KubernetesCustomResourceScanner;
import io.stackgres.operator.validation.DependenciesValidator;
import io.stackgres.operatorframework.ValidationFailed;

@ApplicationScoped
public class PgConfigDependenciesValidator extends DependenciesValidator<PgConfigReview>
    implements PgConfigValidator {

  @Inject
  public PgConfigDependenciesValidator(
      KubernetesCustomResourceScanner<StackGresCluster> clusterScanner) {
    super(clusterScanner);
  }

  public PgConfigDependenciesValidator() {
    super(null);
    ArcUtil.checkPublicNoArgsConstructorIsCalledFromArc();
  }

  @Override
  public void validate(PgConfigReview review, StackGresCluster i) throws ValidationFailed {
    if (review.getRequest().getName().equals(i.getSpec().getPostgresConfig())) {
      fail(review, i);
    }
  }

}

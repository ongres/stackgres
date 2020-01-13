/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.profile;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.operator.common.ArcUtil;
import io.stackgres.operator.common.SgProfileReview;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.resource.KubernetesCustomResourceScanner;
import io.stackgres.operator.validation.DependenciesValidator;
import io.stackgres.operatorframework.ValidationFailed;

@ApplicationScoped
public class SgProfileDependenciesValidator extends DependenciesValidator<SgProfileReview>
    implements SgProfileValidator {

  @Inject
  public SgProfileDependenciesValidator(
      KubernetesCustomResourceScanner<StackGresCluster> clusterScanner) {
    super(clusterScanner);
  }

  public SgProfileDependenciesValidator() {
    super(null);
    ArcUtil.checkPublicNoArgsConstructorIsCalledFromArc();
  }

  @Override
  protected void validate(SgProfileReview review, StackGresCluster i) throws ValidationFailed {
    if (i.getSpec().getResourceProfile().equals(review.getRequest().getName())) {
      fail(review, i);
    }
  }
}

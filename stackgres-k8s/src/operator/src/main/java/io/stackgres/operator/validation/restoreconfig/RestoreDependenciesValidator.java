/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.restoreconfig;

import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.operator.common.ArcUtil;
import io.stackgres.operator.common.RestoreConfigReview;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.resource.KubernetesCustomResourceScanner;
import io.stackgres.operator.validation.DependenciesValidator;
import io.stackgres.operatorframework.ValidationFailed;

@ApplicationScoped
public class RestoreDependenciesValidator extends DependenciesValidator<RestoreConfigReview>
    implements RestoreConfigValidator {

  @Inject
  public RestoreDependenciesValidator(
      KubernetesCustomResourceScanner<StackGresCluster> clusterScanner) {
    super(clusterScanner);
  }

  public RestoreDependenciesValidator() {
    super(null);
    ArcUtil.checkPublicNoArgsConstructorIsCalledFromArc();
  }

  @Override
  protected void validate(RestoreConfigReview review, StackGresCluster cluster)
      throws ValidationFailed {

    Optional<String> restoreConfig = Optional.ofNullable(cluster.getSpec().getRestoreConfig());

    if (restoreConfig.isPresent() && restoreConfig.get().equals(review.getRequest().getName())) {
      fail(review, cluster);
    }

  }
}

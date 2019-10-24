/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pgconfig;

import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.operator.common.KubernetesScanner;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.customresource.sgcluster.StackGresClusterList;
import io.stackgres.operator.validation.Operation;
import io.stackgres.operator.validation.PgConfigReview;
import io.stackgres.operator.validation.ValidationFailed;

@ApplicationScoped
public class DependenciesValidator implements PgConfigValidator {

  private KubernetesScanner<StackGresClusterList> clusterScanner;

  @Inject
  public DependenciesValidator(KubernetesScanner<StackGresClusterList> clusterScanner) {
    this.clusterScanner = clusterScanner;
  }

  @Override
  public void validate(PgConfigReview review) throws ValidationFailed {

    if (review.getRequest().getOperation() == Operation.DELETE) {
      Optional<StackGresClusterList> clusters = clusterScanner
          .findResources(review.getRequest().getNamespace());

      if (clusters.isPresent()) {
        for (StackGresCluster i : clusters.get().getItems()) {
          if (review.getRequest().getName().equals(i.getSpec().getPostgresConfig())) {
            throw new ValidationFailed("Can't delete sppgconfig "
                + review.getRequest().getName() + " because the spcluster "
                + i.getMetadata().getName() + " dependes on it");
          }
        }
      }
    }
  }

}

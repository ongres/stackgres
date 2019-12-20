/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

import java.util.List;
import java.util.Optional;

import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.customresource.sgcluster.StackGresClusterDefinition;
import io.stackgres.operator.resource.KubernetesCustomResourceScanner;
import io.stackgres.operatorframework.AdmissionReview;
import io.stackgres.operatorframework.Operation;
import io.stackgres.operatorframework.ValidationFailed;
import io.stackgres.operatorframework.Validator;

public abstract class DependenciesValidator<T extends AdmissionReview<?>> implements Validator<T> {

  private final KubernetesCustomResourceScanner<StackGresCluster> clusterScanner;

  public DependenciesValidator(KubernetesCustomResourceScanner<StackGresCluster> clusterScanner) {
    this.clusterScanner = clusterScanner;
  }

  @Override
  public void validate(T review) throws ValidationFailed {
    if (review.getRequest().getOperation() == Operation.DELETE) {
      Optional<List<StackGresCluster>> clusters = clusterScanner
          .findResources(review.getRequest().getNamespace());

      if (clusters.isPresent()) {
        for (StackGresCluster i : clusters.get()) {
          validate(review, i);
        }
      }
    }
  }

  protected abstract void validate(T review, StackGresCluster i) throws ValidationFailed;

  protected void fail(T review, StackGresCluster i) throws ValidationFailed {
    throw new ValidationFailed("Can't delete "
        + review.getRequest().getResource().getResource()
        + "." + review.getRequest().getKind().getGroup()
        + " " + review.getRequest().getName() + " because the "
        + StackGresClusterDefinition.NAME + " " + i.getMetadata().getName() + " depends on it");
  }
}

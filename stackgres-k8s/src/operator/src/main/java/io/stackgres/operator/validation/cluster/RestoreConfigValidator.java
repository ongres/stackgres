/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.operator.common.StackgresClusterReview;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.customresource.sgrestoreconfig.StackgresRestoreConfig;
import io.stackgres.operator.resource.KubernetesCustomResourceFinder;
import io.stackgres.operatorframework.ValidationFailed;

@ApplicationScoped
public class RestoreConfigValidator implements ClusterValidator {

  private KubernetesCustomResourceFinder<StackgresRestoreConfig> restoreConfigFinder;

  @Inject
  public RestoreConfigValidator(
      KubernetesCustomResourceFinder<StackgresRestoreConfig> restoreConfigFinder) {
    this.restoreConfigFinder = restoreConfigFinder;
  }

  @Override
  public void validate(StackgresClusterReview review) throws ValidationFailed {

    StackGresCluster cluster = review.getRequest().getObject();
    String restoreConfig = cluster.getSpec().getRestoreConfig();

    switch (review.getRequest().getOperation()) {
      case CREATE:

        if (restoreConfig != null) {

          String namespace = cluster.getMetadata().getNamespace();
          Optional<StackgresRestoreConfig> config = restoreConfigFinder
              .findByNameAndNamespace(restoreConfig, namespace);

          if (!config.isPresent()) {
            throw new ValidationFailed("Restore config " + restoreConfig + " not found");
          }

        }
        break;
      case UPDATE:
        String oldRestoreConfig = review.getRequest().getOldObject().getSpec().getRestoreConfig();
        if (restoreConfig == null && oldRestoreConfig != null
            || restoreConfig != null && oldRestoreConfig == null) {
          throw new ValidationFailed("Cannot update cluster's restore config");
        }
        if (restoreConfig != null && !restoreConfig.equals(oldRestoreConfig)) {
          throw new ValidationFailed("Cannot update cluster's restore config");
        }
        break;
      default:
    }

  }
}

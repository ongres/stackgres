/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.common.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.services.PgBouncerConfigFinder;
import io.stackgres.operator.validation.AdmissionReview;
import io.stackgres.operator.validation.ValidationFailed;
import io.stackgres.sidecars.pgbouncer.customresources.StackGresPgbouncerConfig;

@ApplicationScoped
public class PgBouncerValidator implements ClusterValidator {

  private PgBouncerConfigFinder configFinder;

  @Inject
  public PgBouncerValidator(PgBouncerConfigFinder configFinder) {
    this.configFinder = configFinder;
  }

  @Override
  public void validate(AdmissionReview review) throws ValidationFailed {

    StackGresCluster cluster = review.getRequest().getObject();
    String poolingConfig = cluster.getSpec().getConnectionPoolingConfig();

    switch (review.getRequest().getOperation()) {
      case CREATE:
        checkIfPoolingConfigExists(review, "Pooling config " + poolingConfig
            + " not found");
        break;
      case UPDATE:
        checkIfPoolingConfigExists(review, "Cannot update to pooling config "
            + poolingConfig + " because it doesn't exists");
        break;
      default:
    }

  }

  private void checkIfPoolingConfigExists(AdmissionReview review,
                                          String onError) throws ValidationFailed {

    StackGresCluster cluster = review.getRequest().getObject();
    String poolingConfig = cluster.getSpec().getConnectionPoolingConfig();

    Optional<StackGresPgbouncerConfig> poolingConfigOpt = configFinder
        .findPgBouncerConfig(poolingConfig);

    if (!poolingConfigOpt.isPresent()) {
      throw new ValidationFailed(onError);
    }
  }

}

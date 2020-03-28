/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.stackgres.operator.common.ConfigContext;
import io.stackgres.operator.common.ErrorType;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.resource.CustomResourceFinder;
import io.stackgres.operator.sidecars.pgbouncer.customresources.StackGresPgbouncerConfig;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

@Singleton
@ValidationType(ErrorType.INVALID_CR_REFERENCE)
public class PoolingConfigValidator implements ClusterValidator {

  private CustomResourceFinder<StackGresPgbouncerConfig> configFinder;

  private ConfigContext context;

  @Inject
  public PoolingConfigValidator(
      CustomResourceFinder<StackGresPgbouncerConfig> configFinder, ConfigContext context) {
    this.configFinder = configFinder;
    this.context = context;
  }

  @Override
  public void validate(StackGresClusterReview review) throws ValidationFailed {

    StackGresCluster cluster = review.getRequest().getObject();
    String poolingConfig = cluster.getSpec().getConfiguration().getConnectionPoolingConfig();

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

  private void checkIfPoolingConfigExists(StackGresClusterReview review,
                                          String onError) throws ValidationFailed {

    StackGresCluster cluster = review.getRequest().getObject();
    String poolingConfig = cluster.getSpec().getConfiguration().getConnectionPoolingConfig();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    if (cluster.getSpec().getPod().getDisableConnectionPooling() == Boolean.FALSE) {
      checkIfProvided(poolingConfig, "sgPoolingConfig");
      Optional<StackGresPgbouncerConfig> poolingConfigOpt = configFinder
          .findByNameAndNamespace(poolingConfig, namespace);

      if (!poolingConfigOpt.isPresent()) {
        fail(context, onError);
      }
    }
  }

}

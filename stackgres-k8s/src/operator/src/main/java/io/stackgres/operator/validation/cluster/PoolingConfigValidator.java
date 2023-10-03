/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

@Singleton
@ValidationType(ErrorType.INVALID_CR_REFERENCE)
public class PoolingConfigValidator implements ClusterValidator {

  private final CustomResourceFinder<StackGresPoolingConfig> configFinder;

  @Inject
  public PoolingConfigValidator(
      CustomResourceFinder<StackGresPoolingConfig> configFinder) {
    this.configFinder = configFinder;
  }

  @Override
  @SuppressFBWarnings(value = "SF_SWITCH_NO_DEFAULT",
      justification = "False positive")
  public void validate(StackGresClusterReview review) throws ValidationFailed {
    switch (review.getRequest().getOperation()) {
      case CREATE: {
        StackGresCluster cluster = review.getRequest().getObject();
        String poolingConfig = cluster.getSpec().getConfigurations().getSgPoolingConfig();
        checkIfPoolingConfigExists(review, "Pooling config " + poolingConfig
            + " not found");
        break;
      }
      case UPDATE: {
        StackGresCluster cluster = review.getRequest().getObject();
        String poolingConfig = cluster.getSpec().getConfigurations().getSgPoolingConfig();
        checkIfPoolingConfigExists(review, "Cannot update to pooling config "
            + poolingConfig + " because it doesn't exists");
        break;
      }
      default:
    }

  }

  private void checkIfPoolingConfigExists(StackGresClusterReview review,
                                          String onError) throws ValidationFailed {

    StackGresCluster cluster = review.getRequest().getObject();
    String poolingConfig = cluster.getSpec().getConfigurations().getSgPoolingConfig();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    if (Boolean.FALSE.equals(cluster.getSpec().getPods().getDisableConnectionPooling())) {
      checkIfProvided(poolingConfig, "sgPoolingConfig");
      Optional<StackGresPoolingConfig> poolingConfigOpt = configFinder
          .findByNameAndNamespace(poolingConfig, namespace);

      if (poolingConfigOpt.isEmpty()) {
        fail(onError);
      }
    }
  }

}

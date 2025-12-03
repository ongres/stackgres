/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.inject.Singleton;

@Singleton
@ValidationType(ErrorType.FORBIDDEN_CLUSTER_UPDATE)
public class StatusUpdateValidator implements ClusterValidator {

  @Override
  public void validate(StackGresClusterReview review) throws ValidationFailed {
    switch (review.getRequest().getOperation()) {
      case UPDATE: {
        StackGresCluster cluster = review.getRequest().getObject();
        StackGresCluster oldCluster = review.getRequest().getOldObject();
        if ((cluster.getStatus().getPostgresVersion() != null
            && oldCluster.getStatus().getPostgresVersion() == null)
            || (cluster.getStatus().getBuildVersion() != null
            && oldCluster.getStatus().getBuildVersion() == null)) {
          fail("Setting postgresVersion or buildVersion to null is forbidden.");
        }
        break;
      }
      default:
    }

  }

}

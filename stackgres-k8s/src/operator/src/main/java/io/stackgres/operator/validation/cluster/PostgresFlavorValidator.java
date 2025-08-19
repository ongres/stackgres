/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.Objects;

import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.inject.Singleton;

@Singleton
@ValidationType(ErrorType.FORBIDDEN_CR_UPDATE)
public class PostgresFlavorValidator
    implements ClusterValidator {

  @Override
  public void validate(StackGresClusterReview review) throws ValidationFailed {
    StackGresCluster cluster = review.getRequest().getObject();

    switch (review.getRequest().getOperation()) {
      case UPDATE:
        StackGresCluster oldCluster = review.getRequest().getOldObject();
        if (!Objects.equals(
            getPostgresFlavorComponent(cluster),
            getPostgresFlavorComponent(oldCluster))) {
          fail("postgres flavor can not be changed");
        }
        break;
      default:
    }
  }

}

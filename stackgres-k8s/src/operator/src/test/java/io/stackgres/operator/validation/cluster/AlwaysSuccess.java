/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import io.stackgres.operator.common.StackGresClusterReview;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AlwaysSuccess implements ClusterValidator {

  @Override
  public void validate(StackGresClusterReview review) {
  }

}

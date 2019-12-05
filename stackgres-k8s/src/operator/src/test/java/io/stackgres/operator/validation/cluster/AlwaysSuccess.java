/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.operator.common.StackgresClusterReview;

@ApplicationScoped
public class AlwaysSuccess implements ClusterValidator {

  @Override
  public void validate(StackgresClusterReview review) {
  }

}

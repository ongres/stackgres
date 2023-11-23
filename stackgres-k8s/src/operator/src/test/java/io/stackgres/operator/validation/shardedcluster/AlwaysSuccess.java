/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardedcluster;

import io.stackgres.operator.common.StackGresShardedClusterReview;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AlwaysSuccess implements ShardedClusterValidator {

  @Override
  public void validate(StackGresShardedClusterReview review) {
  }

}

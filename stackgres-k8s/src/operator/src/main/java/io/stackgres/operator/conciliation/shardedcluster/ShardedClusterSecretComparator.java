/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster;

import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.operator.conciliation.ReconciliationScope;
import io.stackgres.operator.conciliation.comparator.SecretComparator;
import jakarta.enterprise.context.ApplicationScoped;

@ReconciliationScope(value = StackGresShardedCluster.class, kind = "Secret")
@ApplicationScoped
public class ShardedClusterSecretComparator extends SecretComparator {

}

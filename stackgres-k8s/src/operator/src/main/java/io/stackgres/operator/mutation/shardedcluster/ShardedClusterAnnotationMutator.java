/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.shardedcluster;

import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.mutation.AbstractAnnotationMutator;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ShardedClusterAnnotationMutator
    extends AbstractAnnotationMutator<StackGresShardedCluster, StackGresShardedClusterReview>
    implements ShardedClusterMutator {
}

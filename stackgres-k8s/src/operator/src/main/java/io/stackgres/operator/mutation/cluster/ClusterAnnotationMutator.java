/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.operator.common.StackgresClusterReview;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.mutation.AbstractAnnotationMutator;

@ApplicationScoped
public class ClusterAnnotationMutator
    extends AbstractAnnotationMutator<StackGresCluster, StackgresClusterReview>
    implements ClusterMutator {
}

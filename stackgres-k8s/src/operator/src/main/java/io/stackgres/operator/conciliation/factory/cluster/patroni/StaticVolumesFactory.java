/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.AbstractStaticVolumesFactory;
import jakarta.inject.Singleton;

@Singleton
@OperatorVersionBinder
public class StaticVolumesFactory
    extends AbstractStaticVolumesFactory<StackGresCluster, StackGresClusterContext> {
}

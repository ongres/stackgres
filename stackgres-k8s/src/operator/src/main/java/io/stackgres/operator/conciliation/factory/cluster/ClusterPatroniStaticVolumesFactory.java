/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import javax.inject.Singleton;

import io.stackgres.common.StackGresVersion;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.PatroniStaticVolumesFactory;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V10A3, stopAt = StackGresVersion.V11)
public class ClusterPatroniStaticVolumesFactory
    extends PatroniStaticVolumesFactory<StackGresClusterContext> {
}

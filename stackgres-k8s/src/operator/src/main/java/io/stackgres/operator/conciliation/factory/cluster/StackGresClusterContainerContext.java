/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ContainerContext;
import org.immutables.value.Value;

@Value.Immutable
public interface StackGresClusterContainerContext extends ContainerContext {

  StackGresClusterContext getClusterContext();
}

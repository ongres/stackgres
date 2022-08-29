/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import java.util.List;
import java.util.Optional;

import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ContainerContext;
import org.immutables.value.Value;

@Value.Immutable
public interface ClusterContainerContext extends ContainerContext {

  StackGresClusterContext getClusterContext();

  Optional<String> getOldPostgresVersion();

  List<StackGresClusterInstalledExtension> getInstalledExtensions();

}

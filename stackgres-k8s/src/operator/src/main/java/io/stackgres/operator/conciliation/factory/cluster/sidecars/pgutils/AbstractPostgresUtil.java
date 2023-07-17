/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.pgutils;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.Map;

import io.stackgres.common.StackGresContext;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.PostgresSocketMount;
import io.stackgres.operator.conciliation.factory.cluster.ClusterContainerContext;
import jakarta.inject.Inject;

public abstract class AbstractPostgresUtil
    implements ContainerFactory<ClusterContainerContext> {

  protected PostgresSocketMount postgresSocket;

  @Override
  public Map<String, String> getComponentVersions(ClusterContainerContext context) {
    return Map.of(
        StackGresContext.POSTGRES_VERSION_KEY,
        getPostgresFlavorComponent(context.getClusterContext().getCluster())
        .get(context.getClusterContext().getCluster())
        .getVersion(
            context.getClusterContext().getCluster().getSpec().getPostgres().getVersion()));
  }

  @Inject
  public void setPostgresSocket(
      PostgresSocketMount postgresSocket) {
    this.postgresSocket = postgresSocket;
  }
}

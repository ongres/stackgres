/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.pgutils;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;
import static io.stackgres.operator.conciliation.VolumeMountProviderName.POSTGRES_SOCKET;

import java.util.Map;

import javax.inject.Inject;

import com.google.common.collect.ImmutableMap;
import io.stackgres.common.StackGresContext;
import io.stackgres.operator.conciliation.factory.ContainerContext;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.ProviderName;
import io.stackgres.operator.conciliation.factory.VolumeMountsProvider;
import io.stackgres.operator.conciliation.factory.cluster.StackGresClusterContainerContext;

public abstract class AbstractPostgresUtil
    implements ContainerFactory<StackGresClusterContainerContext> {

  protected VolumeMountsProvider<ContainerContext> postgresSocket;

  @Override
  public Map<String, String> getComponentVersions(StackGresClusterContainerContext context) {
    return ImmutableMap.of(
        StackGresContext.POSTGRES_VERSION_KEY,
        getPostgresFlavorComponent(context.getClusterContext().getCluster())
        .get(context.getClusterContext().getCluster())
        .getVersion(
            context.getClusterContext().getCluster().getSpec().getPostgres().getVersion()));
  }

  @Inject
  public void setPostgresSocket(
      @ProviderName(POSTGRES_SOCKET)
          VolumeMountsProvider<ContainerContext> postgresSocket) {
    this.postgresSocket = postgresSocket;
  }
}

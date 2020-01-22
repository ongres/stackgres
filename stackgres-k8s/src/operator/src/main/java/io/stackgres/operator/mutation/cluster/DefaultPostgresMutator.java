/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import javax.enterprise.context.ApplicationScoped;

import com.github.fge.jackson.jsonpointer.JsonPointer;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.customresource.sgpgconfig.StackGresPostgresConfig;

@ApplicationScoped
public class DefaultPostgresMutator
    extends AbstractDefaultResourceMutator<StackGresPostgresConfig>
    implements ClusterMutator {

  @Override
  protected String getTargetPropertyValue(StackGresCluster targetCluster) {
    return targetCluster.getSpec().getPostgresConfig();
  }

  @Override
  protected JsonPointer getTargetPointer() throws NoSuchFieldException {
    return getTargetPointer("postgresConfig");
  }

}

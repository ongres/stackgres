/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import javax.enterprise.context.ApplicationScoped;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.customresource.sgcluster.StackGresClusterSpec;
import io.stackgres.operator.sidecars.pgbouncer.customresources.StackGresPgbouncerConfig;

@ApplicationScoped
public class DefaultPgBouncerMutator
    extends AbstractDefaultResourceMutator<StackGresPgbouncerConfig>
    implements ClusterMutator {

  @Override
  protected String getTargetPropertyValue(StackGresCluster targetCluster) {
    return targetCluster.getSpec().getConnectionPoolingConfig();
  }

  @Override
  protected boolean applyDefault(StackGresCluster targetCluster) {
    return targetCluster.getSpec().getSidecars().contains("connection-pooling")
        && super.applyDefault(targetCluster);
  }

  @Override
  protected JsonPointer getTargetPointer() throws NoSuchFieldException {
    String jsonField = StackGresClusterSpec.class
        .getDeclaredField("connectionPoolingConfig")
        .getAnnotation(JsonProperty.class)
        .value();
    return CLUSTER_CONFIG_POINTER.append(jsonField);
  }
}

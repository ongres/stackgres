/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.github.fge.jackson.jsonpointer.JsonPointer;

import io.stackgres.operator.common.ArcUtil;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operator.resource.CustomResourceFinder;
import io.stackgres.operator.resource.CustomResourceScheduler;
import io.stackgres.operator.sidecars.pgbouncer.customresources.StackGresPgbouncerConfig;

@ApplicationScoped
public class DefaultPgBouncerMutator
    extends AbstractDefaultResourceMutator<StackGresPgbouncerConfig>
    implements ClusterMutator {

  @Inject
  public DefaultPgBouncerMutator(
      DefaultCustomResourceFactory<StackGresPgbouncerConfig> resourceFactory,
      CustomResourceFinder<StackGresPgbouncerConfig> finder,
      CustomResourceScheduler<StackGresPgbouncerConfig> scheduler) {
    super(resourceFactory, finder, scheduler);
  }

  public DefaultPgBouncerMutator() {
    super(null, null, null);
    ArcUtil.checkPublicNoArgsConstructorIsCalledFromArc();
  }

  @Override
  protected String getTargetPropertyValue(StackGresCluster targetCluster) {
    return targetCluster.getSpec().getConnectionPoolingConfig();
  }

  @Override
  protected boolean applyDefault(StackGresCluster targetCluster) {
    List<String> clusterSidecars = targetCluster.getSpec().getSidecars();
    return (clusterSidecars == null || clusterSidecars.contains("connection-pooling"))
        && super.applyDefault(targetCluster);
  }

  @Override
  protected JsonPointer getTargetPointer() throws NoSuchFieldException {
    return getTargetPointer("connectionPoolingConfig");
  }
}

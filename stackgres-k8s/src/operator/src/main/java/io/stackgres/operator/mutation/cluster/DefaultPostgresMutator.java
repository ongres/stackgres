/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.github.fge.jackson.jsonpointer.JsonPointer;

import io.stackgres.operator.common.ArcUtil;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.customresource.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operator.resource.CustomResourceFinder;
import io.stackgres.operator.resource.CustomResourceScheduler;

@ApplicationScoped
public class DefaultPostgresMutator
    extends AbstractDefaultResourceMutator<StackGresPostgresConfig>
    implements ClusterMutator {

  @Inject
  public DefaultPostgresMutator(
      DefaultCustomResourceFactory<StackGresPostgresConfig> resourceFactory,
      CustomResourceFinder<StackGresPostgresConfig> finder,
      CustomResourceScheduler<StackGresPostgresConfig> scheduler) {
    super(resourceFactory, finder, scheduler);
  }

  public DefaultPostgresMutator() {
    super(null, null, null);
    ArcUtil.checkPublicNoArgsConstructorIsCalledFromArc();
  }

  @Override
  protected String getTargetPropertyValue(StackGresCluster targetCluster) {
    return targetCluster.getSpec().getPostgresConfig();
  }

  @Override
  protected JsonPointer getTargetPointer() throws NoSuchFieldException {
    return getTargetPointer("postgresConfig");
  }

}

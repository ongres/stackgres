/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfiguration;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operator.mutation.AbstractDefaultResourceMutator;

public class DefaultPostgresMutator
    extends AbstractDefaultResourceMutator<StackGresPostgresConfig, StackGresCluster,
        StackGresClusterReview> {

  public DefaultPostgresMutator(
      DefaultCustomResourceFactory<StackGresPostgresConfig> resourceFactory,
      CustomResourceFinder<StackGresPostgresConfig> finder,
      CustomResourceScheduler<StackGresPostgresConfig> scheduler) {
    super(resourceFactory, finder, scheduler);
  }

  @Override
  protected void setValueSection(StackGresCluster resource) {
    if (resource.getSpec().getConfiguration() == null) {
      resource.getSpec().setConfiguration(new StackGresClusterConfiguration());
    }
  }

  @Override
  protected String getTargetPropertyValue(StackGresCluster resource) {
    return resource.getSpec().getConfiguration().getPostgresConfig();
  }

  @Override
  protected void setTargetProperty(StackGresCluster resource, String defaultResourceName) {
    resource.getSpec().getConfiguration().setPostgresConfig(defaultResourceName);
  }

}

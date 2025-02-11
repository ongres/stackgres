/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operator.mutation.AbstractDefaultResourceMutator;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DefaultPostgresConfigMutator
    extends AbstractDefaultResourceMutator<StackGresPostgresConfig, StackGresCluster,
        StackGresCluster, StackGresClusterReview>
    implements ClusterMutator {

  public DefaultPostgresConfigMutator(
      DefaultCustomResourceFactory<StackGresPostgresConfig, StackGresCluster> resourceFactory,
      CustomResourceFinder<StackGresPostgresConfig> finder,
      CustomResourceScheduler<StackGresPostgresConfig> scheduler) {
    super(resourceFactory, finder, scheduler);
  }

  @Override
  protected void setValueSection(StackGresCluster resource) {
    if (resource.getSpec().getConfigurations() == null) {
      resource.getSpec().setConfigurations(new StackGresClusterConfigurations());
    }
  }

  @Override
  protected String getTargetPropertyValue(StackGresCluster resource) {
    return resource.getSpec().getConfigurations().getSgPostgresConfig();
  }

  @Override
  protected void setTargetProperty(StackGresCluster resource, String defaultResourceName) {
    resource.getSpec().getConfigurations().setSgPostgresConfig(defaultResourceName);
  }

}

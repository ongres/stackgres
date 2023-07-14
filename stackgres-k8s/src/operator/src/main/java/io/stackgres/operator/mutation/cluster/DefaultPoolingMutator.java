/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfiguration;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operator.mutation.AbstractDefaultResourceMutator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DefaultPoolingMutator
    extends AbstractDefaultResourceMutator<StackGresPoolingConfig, StackGresCluster,
        StackGresClusterReview>
    implements ClusterMutator {

  @Inject
  public DefaultPoolingMutator(
      DefaultCustomResourceFactory<StackGresPoolingConfig> resourceFactory,
      CustomResourceFinder<StackGresPoolingConfig> finder,
      CustomResourceScheduler<StackGresPoolingConfig> scheduler) {
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
    return resource.getSpec().getConfiguration().getConnectionPoolingConfig();
  }

  @Override
  protected void setTargetProperty(StackGresCluster resource, String defaultResourceName) {
    resource.getSpec().getConfiguration().setConnectionPoolingConfig(defaultResourceName);
  }

}

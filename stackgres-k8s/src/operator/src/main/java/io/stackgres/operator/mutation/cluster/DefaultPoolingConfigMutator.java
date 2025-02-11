/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operator.mutation.AbstractDefaultResourceMutator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DefaultPoolingConfigMutator
    extends AbstractDefaultResourceMutator<StackGresPoolingConfig, HasMetadata,
        StackGresCluster, StackGresClusterReview>
    implements ClusterMutator {

  @Inject
  public DefaultPoolingConfigMutator(
      DefaultCustomResourceFactory<StackGresPoolingConfig, HasMetadata> resourceFactory,
      CustomResourceFinder<StackGresPoolingConfig> finder,
      CustomResourceScheduler<StackGresPoolingConfig> scheduler) {
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
    return resource.getSpec().getConfigurations().getSgPoolingConfig();
  }

  @Override
  protected void setTargetProperty(StackGresCluster resource, String defaultResourceName) {
    resource.getSpec().getConfigurations().setSgPoolingConfig(defaultResourceName);
  }

}

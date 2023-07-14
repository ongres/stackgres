/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operator.mutation.AbstractDefaultResourceMutator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DefaultProfileMutator
    extends AbstractDefaultResourceMutator<StackGresProfile, StackGresCluster,
        StackGresClusterReview>
    implements ClusterMutator {

  @Inject
  public DefaultProfileMutator(
      DefaultCustomResourceFactory<StackGresProfile> resourceFactory,
      CustomResourceFinder<StackGresProfile> finder,
      CustomResourceScheduler<StackGresProfile> scheduler) {
    super(resourceFactory, finder, scheduler);
  }

  @Override
  protected String getTargetPropertyValue(StackGresCluster resource) {
    return resource.getSpec().getResourceProfile();
  }

  @Override
  protected void setTargetProperty(StackGresCluster resource, String defaultResourceName) {
    resource.getSpec().setResourceProfile(defaultResourceName);
  }

}

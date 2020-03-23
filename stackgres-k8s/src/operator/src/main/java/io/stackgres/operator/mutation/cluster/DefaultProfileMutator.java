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
import io.stackgres.operator.customresource.sgprofile.StackGresProfile;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operator.resource.CustomResourceFinder;
import io.stackgres.operator.resource.CustomResourceScheduler;

@ApplicationScoped
public class DefaultProfileMutator extends AbstractDefaultResourceMutator<StackGresProfile>
    implements ClusterMutator {

  @Inject
  public DefaultProfileMutator(DefaultCustomResourceFactory<StackGresProfile> resourceFactory,
      CustomResourceFinder<StackGresProfile> finder,
      CustomResourceScheduler<StackGresProfile> scheduler) {
    super(resourceFactory, finder, scheduler);
  }

  public DefaultProfileMutator() {
    super(null, null, null);
    ArcUtil.checkPublicNoArgsConstructorIsCalledFromArc();
  }

  @Override
  protected String getTargetPropertyValue(StackGresCluster targetCluster) {
    return targetCluster.getSpec().getResourceProfile();
  }

  @Override
  protected JsonPointer getTargetPointer() throws NoSuchFieldException {
    return getTargetPointer("resourceProfile");
  }
}

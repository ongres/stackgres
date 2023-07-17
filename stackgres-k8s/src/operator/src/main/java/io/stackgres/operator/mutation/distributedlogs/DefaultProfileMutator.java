/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.distributedlogs;

import io.stackgres.common.CdiUtil;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.common.StackGresDistributedLogsReview;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operator.mutation.AbstractDefaultResourceMutator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DefaultProfileMutator
    extends AbstractDefaultResourceMutator<StackGresProfile,
        StackGresDistributedLogs, StackGresDistributedLogsReview>
    implements DistributedLogsMutator {

  @Inject
  public DefaultProfileMutator(
      DefaultCustomResourceFactory<StackGresProfile> resourceFactory,
      CustomResourceFinder<StackGresProfile> finder,
      CustomResourceScheduler<StackGresProfile> scheduler) {
    super(resourceFactory, finder, scheduler);
  }

  public DefaultProfileMutator() {
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
  }

  @Override
  protected String getTargetPropertyValue(StackGresDistributedLogs resource) {
    return resource.getSpec().getResourceProfile();
  }

  @Override
  protected void setTargetProperty(StackGresDistributedLogs resource, String value) {
    resource.getSpec().setResourceProfile(value);
  }

}

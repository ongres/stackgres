/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.distributedlogs;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.operator.common.StackGresDistributedLogsReview;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operator.mutation.AbstractDefaultResourceMutator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DefaultProfileMutator
    extends AbstractDefaultResourceMutator<StackGresProfile, HasMetadata,
        StackGresDistributedLogs, StackGresDistributedLogsReview>
    implements DistributedLogsMutator {

  @Inject
  public DefaultProfileMutator(
      DefaultCustomResourceFactory<StackGresProfile, HasMetadata> resourceFactory) {
    super(resourceFactory);
  }

  public DefaultProfileMutator() {
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
  }

  @Override
  protected String getTargetPropertyValue(StackGresDistributedLogs resource) {
    return resource.getSpec().getSgInstanceProfile();
  }

  @Override
  protected void setTargetProperty(StackGresDistributedLogs resource, String value) {
    resource.getSpec().setSgInstanceProfile(value);
  }

}

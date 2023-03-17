/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.distributedlogs;

import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsConfiguration;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.common.StackGresDistributedLogsReview;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operator.mutation.AbstractDefaultResourceMutator;

public class DefaultPostgresMutator
    extends AbstractDefaultResourceMutator<StackGresPostgresConfig,
        StackGresDistributedLogs, StackGresDistributedLogsReview> {

  public DefaultPostgresMutator(
      DefaultCustomResourceFactory<StackGresPostgresConfig> resourceFactory,
      CustomResourceFinder<StackGresPostgresConfig> finder,
      CustomResourceScheduler<StackGresPostgresConfig> scheduler) {
    super(resourceFactory, finder, scheduler);
  }

  @Override
  protected void setValueSection(StackGresDistributedLogs resource) {
    if (resource.getSpec().getConfiguration() == null) {
      resource.getSpec().setConfiguration(new StackGresDistributedLogsConfiguration());
    }
  }

  @Override
  protected String getTargetPropertyValue(StackGresDistributedLogs resource) {
    return resource.getSpec().getConfiguration().getPostgresConfig();
  }

  @Override
  protected void setTargetProperty(StackGresDistributedLogs resource, String value) {
    resource.getSpec().getConfiguration().setPostgresConfig(value);
  }

}

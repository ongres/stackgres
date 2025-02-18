/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.distributedlogs;

import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsConfigurations;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.common.StackGresDistributedLogsReview;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operator.mutation.AbstractDefaultResourceMutator;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DefaultPostgresConfigMutator
    extends AbstractDefaultResourceMutator<StackGresPostgresConfig, StackGresDistributedLogs,
        StackGresDistributedLogs, StackGresDistributedLogsReview>
    implements DistributedLogsMutator {

  public DefaultPostgresConfigMutator(
      DefaultCustomResourceFactory<StackGresPostgresConfig, StackGresDistributedLogs> resourceFactory) {
    super(resourceFactory);
  }

  @Override
  protected void setValueSection(StackGresDistributedLogs resource) {
    if (resource.getSpec().getConfigurations() == null) {
      resource.getSpec().setConfigurations(new StackGresDistributedLogsConfigurations());
    }
  }

  @Override
  protected String getTargetPropertyValue(StackGresDistributedLogs resource) {
    return resource.getSpec().getConfigurations().getSgPostgresConfig();
  }

  @Override
  protected void setTargetProperty(StackGresDistributedLogs resource, String value) {
    resource.getSpec().getConfigurations().setSgPostgresConfig(value);
  }

}

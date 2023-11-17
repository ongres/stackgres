/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs.patroni;

import java.util.List;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.patroni.StackGresPasswordKeys;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import io.stackgres.operator.conciliation.factory.AbstractPatroniEnvironmentVariablesFactory;
import io.stackgres.operator.conciliation.factory.FactoryName;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@FactoryName(DistributedLogsPatroniEnvironmentVariablesFactory.LATEST_PATRONI_ENV_VAR_FACTORY)
public class PatroniEnvironmentVariablesFactory
    extends AbstractPatroniEnvironmentVariablesFactory<StackGresDistributedLogsContext> {

  @Override
  public List<EnvVar> createResource(StackGresDistributedLogsContext context) {
    return ImmutableList.<EnvVar>builder()
        .addAll(DistributedLogsCommonEnvVars.getEnvVars())
        .add(new EnvVarBuilder()
            .withName("PATRONI_RESTAPI_LISTEN")
            .withValue("0.0.0.0:" + EnvoyUtil.PATRONI_ENTRY_PORT)
            .build())
        .add(new EnvVarBuilder()
            .withName(StackGresPasswordKeys.SUPERUSER_USERNAME_ENV)
            .withValue(StackGresPasswordKeys.SUPERUSER_USERNAME)
            .build())
        .add(new EnvVarBuilder()
            .withName(StackGresPasswordKeys.REPLICATION_USERNAME_ENV)
            .withValue(StackGresPasswordKeys.REPLICATION_USERNAME)
            .build())
        .addAll(createPatroniEnvVars(context.getSource()))
        .build();
  }

}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs.patroni;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import io.stackgres.operator.conciliation.factory.FactoryName;
import io.stackgres.operator.conciliation.factory.PatroniEnvironmentVariablesFactory;

@ApplicationScoped
@FactoryName(DistributedLogsEnvVarFactories.LATEST_PATRONI_ENV_VAR_FACTORY)
public class EnvVarFactory
    extends PatroniEnvironmentVariablesFactory<StackGresDistributedLogsContext> {

  @Override
  public List<EnvVar> createResource(StackGresDistributedLogsContext context) {
    return ImmutableList.<EnvVar>builder()
        .addAll(DistributedLogsCommonEnvVars.getEnvVars())
        .add(new EnvVarBuilder()
            .withName("PATRONI_RESTAPI_LISTEN")
            .withValue("0.0.0.0:" + EnvoyUtil.PATRONI_ENTRY_PORT)
            .build())
        .addAll(createPatroniEnvVars(context.getSource()))
        .build();
  }

}

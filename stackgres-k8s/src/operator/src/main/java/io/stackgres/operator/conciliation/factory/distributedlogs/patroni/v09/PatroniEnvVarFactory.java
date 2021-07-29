/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs.patroni.v09;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import io.stackgres.operator.conciliation.factory.FactoryName;
import io.stackgres.operator.conciliation.factory.PatroniEnvironmentVariablesFactory;
import io.stackgres.operator.conciliation.factory.ResourceFactory;
import io.stackgres.operator.conciliation.factory.distributedlogs.patroni.DistributedLogsEnvVarFactories;

@ApplicationScoped
@FactoryName(DistributedLogsEnvVarFactories.V09_PATRONI_ENV_VAR_FACTORY)
public class PatroniEnvVarFactory
    extends PatroniEnvironmentVariablesFactory<StackGresDistributedLogsContext> {

  @FactoryName(DistributedLogsEnvVarFactories.V09_COMMON_ENV_VAR_FACTORY)
  ResourceFactory<StackGresDistributedLogsContext, List<EnvVar>> commonEnvVarFactory;

  @Override
  public List<EnvVar> createResource(StackGresDistributedLogsContext context) {
    return ImmutableList.<EnvVar>builder()
        .addAll(commonEnvVarFactory.createResource(context))
        .add(new EnvVarBuilder()
            .withName("PATRONI_RESTAPI_LISTEN")
            .withValue("0.0.0.0:" + EnvoyUtil.PATRONI_ENTRY_PORT)
            .build())
        .addAll(createPatroniEnvVars(context.getSource()))
        .build();
  }
}

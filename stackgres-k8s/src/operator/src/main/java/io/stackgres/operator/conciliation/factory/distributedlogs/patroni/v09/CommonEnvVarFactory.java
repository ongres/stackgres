/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs.patroni.v09;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.stackgres.operator.conciliation.distributedlogs.DistributedLogsContext;
import io.stackgres.operator.conciliation.factory.FactoryName;
import io.stackgres.operator.conciliation.factory.ResourceFactory;
import io.stackgres.operator.conciliation.factory.distributedlogs.patroni.DistributedLogsEnvVarFactories;

@ApplicationScoped
@FactoryName(DistributedLogsEnvVarFactories.V09_COMMON_ENV_VAR_FACTORY)
public class CommonEnvVarFactory implements ResourceFactory<DistributedLogsContext, List<EnvVar>> {

  @Override
  public List<EnvVar> createResource(DistributedLogsContext source) {
    return ImmutableList.<EnvVar>builder()
        .addAll(StatefulSetEnvPaths.getEnvVars())
        .addAll(DistributedLogsCommonEnvVars.getEnvVars())
        .build();
  }
}

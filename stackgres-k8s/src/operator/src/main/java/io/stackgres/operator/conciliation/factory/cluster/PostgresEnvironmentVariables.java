/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import java.util.List;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.EnvVarProvider;
import io.stackgres.operator.conciliation.factory.cluster.patroni.PatroniSecret;
import jakarta.inject.Singleton;

@Singleton
public class PostgresEnvironmentVariables
    implements EnvVarProvider<StackGresClusterContext> {

  @Override
  public List<EnvVar> getEnvVars(StackGresClusterContext context) {
    return List.of(
        new EnvVarBuilder()
        .withName("PGUSER")
        .withNewValueFrom()
        .withNewSecretKeyRef()
        .withName(PatroniSecret.name(context.getCluster()))
        .withKey(PatroniSecret.SUPERUSER_USERNAME_KEY)
        .withOptional(false)
        .endSecretKeyRef()
        .endValueFrom()
        .build(),
        new EnvVarBuilder()
        .withName("PGDATABASE")
        .withValue("postgres")
        .build());

  }

}

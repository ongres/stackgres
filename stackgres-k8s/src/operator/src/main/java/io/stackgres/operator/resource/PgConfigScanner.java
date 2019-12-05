/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Doneable;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.operator.customresource.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.customresource.sgpgconfig.StackGresPostgresConfigDefinition;
import io.stackgres.operator.customresource.sgpgconfig.StackGresPostgresConfigDoneable;
import io.stackgres.operator.customresource.sgpgconfig.StackGresPostgresConfigList;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple5;

@ApplicationScoped
public class PgConfigScanner extends
    AbstractKubernetesCustomResourceScanner<StackGresPostgresConfig, StackGresPostgresConfigList> {

  private final KubernetesClient client;

  @Inject
  public PgConfigScanner(KubernetesClient client) {
    this.client = client;
  }

  @Override
  protected Tuple5<KubernetesClient, String,
      Class<StackGresPostgresConfig>,
      Class<StackGresPostgresConfigList>,
      Class<? extends Doneable<StackGresPostgresConfig>>> arguments() {

    return Tuple.tuple(client, StackGresPostgresConfigDefinition.NAME,
        StackGresPostgresConfig.class, StackGresPostgresConfigList.class,
        StackGresPostgresConfigDoneable.class);

  }
}

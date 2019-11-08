/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Doneable;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.stackgres.operator.app.KubernetesClientFactory;
import io.stackgres.operator.customresource.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.customresource.sgpgconfig.StackGresPostgresConfigDefinition;
import io.stackgres.operator.customresource.sgpgconfig.StackGresPostgresConfigDoneable;
import io.stackgres.operator.customresource.sgpgconfig.StackGresPostgresConfigList;

import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple5;

@ApplicationScoped
public class PostgresConfigFinder
    extends AbstractKubernetesCustomResourceFinder<StackGresPostgresConfig> {

  private final KubernetesClientFactory kubernetesClientFactory;

  /**
   * Create a {@code PostgresConfigFinder} instance.
   */
  @Inject
  public PostgresConfigFinder(KubernetesClientFactory kubernetesClientFactory) {
    this.kubernetesClientFactory = kubernetesClientFactory;
  }

  @Override
  protected Tuple5<KubernetesClientFactory, String, Class<StackGresPostgresConfig>,
      Class<? extends KubernetesResourceList<StackGresPostgresConfig>>,
          Class<? extends Doneable<StackGresPostgresConfig>>> arguments() {
    return Tuple.tuple(kubernetesClientFactory, StackGresPostgresConfigDefinition.NAME,
        StackGresPostgresConfig.class, StackGresPostgresConfigList.class,
        StackGresPostgresConfigDoneable.class);
  }

}

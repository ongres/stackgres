/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Doneable;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.operator.sidecars.pgbouncer.customresources.StackGresPgbouncerConfig;
import io.stackgres.operator.sidecars.pgbouncer.customresources.StackGresPgbouncerConfigDefinition;
import io.stackgres.operator.sidecars.pgbouncer.customresources.StackGresPgbouncerConfigDoneable;
import io.stackgres.operator.sidecars.pgbouncer.customresources.StackGresPgbouncerConfigList;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple5;

@ApplicationScoped
public class PgPoolingConfigScanner extends AbstractKubernetesCustomResourceScanner
    <StackGresPgbouncerConfig, StackGresPgbouncerConfigList> {

  private final KubernetesClient client;

  @Inject
  public PgPoolingConfigScanner(KubernetesClient client) {
    this.client = client;
  }

  @Override
  protected Tuple5<KubernetesClient,
      String,
      Class<StackGresPgbouncerConfig>,
      Class<StackGresPgbouncerConfigList>,
      Class<? extends Doneable<StackGresPgbouncerConfig>>> arguments() {

    return Tuple.tuple(client, StackGresPgbouncerConfigDefinition.NAME,
        StackGresPgbouncerConfig.class, StackGresPgbouncerConfigList.class,
        StackGresPgbouncerConfigDoneable.class);

  }
}

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
import io.stackgres.operator.sidecars.pgbouncer.customresources.StackGresPgbouncerConfig;
import io.stackgres.operator.sidecars.pgbouncer.customresources.StackGresPgbouncerConfigDefinition;
import io.stackgres.operator.sidecars.pgbouncer.customresources.StackGresPgbouncerConfigDoneable;
import io.stackgres.operator.sidecars.pgbouncer.customresources.StackGresPgbouncerConfigList;

import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple5;

@ApplicationScoped
public class PoolingConfigFinder
    extends AbstractKubernetesCustomResourceFinder<StackGresPgbouncerConfig> {

  private final KubernetesClientFactory kubernetesClientFactory;

  /**
   * Create a {@code PoolingConfigFinder} instance.
   */
  @Inject
  public PoolingConfigFinder(KubernetesClientFactory kubernetesClientFactory) {
    this.kubernetesClientFactory = kubernetesClientFactory;
  }

  @Override
  protected Tuple5<KubernetesClientFactory, String, Class<StackGresPgbouncerConfig>,
      Class<? extends KubernetesResourceList<StackGresPgbouncerConfig>>,
          Class<? extends Doneable<StackGresPgbouncerConfig>>> arguments() {
    return Tuple.tuple(kubernetesClientFactory, StackGresPgbouncerConfigDefinition.NAME,
        StackGresPgbouncerConfig.class, StackGresPgbouncerConfigList.class,
        StackGresPgbouncerConfigDoneable.class);
  }

}

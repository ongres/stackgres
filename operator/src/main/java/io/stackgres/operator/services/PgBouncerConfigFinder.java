/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.services;

import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.customresource.sgpgconfig.StackGresPostgresConfigDefinition;
import io.stackgres.common.resource.ResourceUtil;
import io.stackgres.operator.app.KubernetesClientFactory;
import io.stackgres.sidecars.pgbouncer.customresources.StackGresPgbouncerConfig;
import io.stackgres.sidecars.pgbouncer.customresources.StackGresPgbouncerConfigDoneable;
import io.stackgres.sidecars.pgbouncer.customresources.StackGresPgbouncerConfigList;

@ApplicationScoped
public class PgBouncerConfigFinder
    implements KubernetesCustomResourceFinder<StackGresPgbouncerConfig> {

  private KubernetesClientFactory kubClientFactory;

  @Inject
  public PgBouncerConfigFinder(KubernetesClientFactory kubClientFactory) {
    this.kubClientFactory = kubClientFactory;
  }

  @Override
  public Optional<StackGresPgbouncerConfig> findByNameAndNamespace(String name, String namespace) {
    try (KubernetesClient client = kubClientFactory.create()) {
      Optional<CustomResourceDefinition> crd =
          ResourceUtil.getCustomResource(client, StackGresPostgresConfigDefinition.NAME);
      if (crd.isPresent()) {

        return Optional.ofNullable(client
            .customResources(crd.get(),
                StackGresPgbouncerConfig.class,
                StackGresPgbouncerConfigList.class,
                StackGresPgbouncerConfigDoneable.class)
            .inNamespace(namespace)
            .withName(name)
            .get());
      }
    }
    return Optional.empty();
  }
}

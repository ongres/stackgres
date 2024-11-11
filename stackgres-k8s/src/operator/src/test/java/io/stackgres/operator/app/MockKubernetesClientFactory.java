/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.app;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.Mock;
import io.stackgres.common.CrdLoader;
import io.stackgres.common.YamlMapperProvider;
import io.stackgres.testutil.KubernetesServerSupplier;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@Mock
@ApplicationScoped
public class MockKubernetesClientFactory {

  private final KubernetesServerSupplier serverSupplier = new KubernetesServerSupplier();

  @PostConstruct
  public void setup() throws Exception {
    try (KubernetesClient client = serverSupplier.get().getClient()) {
      for (var crd : new CrdLoader(new YamlMapperProvider().get()).scanCrds()) {
        client.apiextensions().v1()
            .customResourceDefinitions()
            .resource(crd)
            .create();
      }
    }
  }

  @Produces
  public KubernetesClient create() {
    return serverSupplier.get().getClient();
  }

  @PreDestroy
  public void cleanUp() {
    if (serverSupplier.wasRetrieved()) {
      serverSupplier.get().after();
    }
  }

}

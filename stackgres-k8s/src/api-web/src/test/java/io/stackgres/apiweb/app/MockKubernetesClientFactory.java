/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.app;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.Mock;
import io.stackgres.common.KubernetesClientFactory;
import io.stackgres.testutil.KubernetesServerSupplier;

@Mock
@ApplicationScoped
public class MockKubernetesClientFactory implements KubernetesClientFactory {

  private final KubernetesServerSupplier serverSupplier = new KubernetesServerSupplier();

  @PostConstruct
  public void setup() throws Exception {}

  @Override
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

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.smallrye.mutiny.Uni;
import io.stackgres.common.KubernetesClientFactory;
import io.stackgres.testutil.CrdUtils;
import io.stackgres.testutil.KubernetesServerSupplier;

//@Mock
//@Singleton
public class MockKubernetesClientFactory implements KubernetesClientFactory {

  private final KubernetesServerSupplier serverSupplier = new KubernetesServerSupplier();

  @PostConstruct
  public void setup() throws Exception {

    Uni.createFrom().emitter(em -> {
      try (KubernetesClient client = serverSupplier.get().getClient()) {
        CrdUtils.installCrds(client);
        em.complete(null);
      } catch (IOException e) {
        em.fail(e);
      }
    }).onFailure().retry().atMost(10);

  }

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

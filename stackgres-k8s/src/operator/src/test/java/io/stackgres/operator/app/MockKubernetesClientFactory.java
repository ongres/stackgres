/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.app;

import java.io.File;
import java.util.Optional;
import java.util.function.Supplier;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.quarkus.test.Mock;
import io.stackgres.operator.AbstractStackGresOperatorIt;
import io.stackgres.operator.CrdMatchTest;
import io.stackgres.operator.common.StackGresUtil;


@Mock
@ApplicationScoped
public class MockKubernetesClientFactory extends KubernetesClientFactory {

  private KubernetesServerSupplier serverSupplier = new KubernetesServerSupplier();

  @Override
  public KubernetesClient create() {
    if (AbstractStackGresOperatorIt.isRunning()) {
      return new DefaultKubernetesClient(
          new ConfigBuilder()
          .withNamespace(StackGresUtil.OPERATOR_NAMESPACE)
          .build());
    }
    return serverSupplier.get().getClient();
  }

  @PreDestroy
  public void cleanUp(){
    if (serverSupplier.wasRetrieved()) {
      serverSupplier.get().after();
    }
  }

  private class KubernetesServerSupplier implements Supplier<KubernetesServer> {
    KubernetesServer server;

    public boolean wasRetrieved() {
      return server != null;
    }

    @Override
    public synchronized KubernetesServer get() {
      if (server == null) {
        server = new KubernetesServer(true, true);
        server.before();
        final NamespacedKubernetesClient client = server.getClient();

        File file = CrdMatchTest.getCrdsFolder();
        for (File crdFile: Optional.ofNullable(file.listFiles()).orElse(new File[0])) {
          CustomResourceDefinition crd = client.customResourceDefinitions().load(crdFile).get();
          client.customResourceDefinitions().create(crd);
        }
      }
      return server;
    }
  }
}

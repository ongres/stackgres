/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.testutil;

import java.io.File;
import java.util.Optional;
import java.util.function.Supplier;

import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;

public class KubernetesServerSupplier implements Supplier<KubernetesServer> {
  KubernetesServer server;

  public synchronized boolean wasRetrieved() {
    return server != null;
  }

  @Override
  public synchronized KubernetesServer get() {
    if (server == null) {
      server = new KubernetesServer(false, true);
      server.before();
      final NamespacedKubernetesClient client = server.getClient();

      File file = CrdUtils.getCrdsFolder();
      for (File crdFile : Optional.ofNullable(file.listFiles()).orElse(new File[0])) {
        if (!crdFile.getName().endsWith(".yaml")) {
          continue;
        }
        CustomResourceDefinition crd =
            client.apiextensions().v1().customResourceDefinitions().load(crdFile).get();
        client.apiextensions().v1().customResourceDefinitions().create(crd);
      }
    }
    return server;
  }
}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.function.Consumer;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.kubernetes.client.KubernetesServer;

public class KubernetesTestServerSetup implements Consumer<KubernetesServer> {

  @Override
  public void accept(KubernetesServer server) {
    final KubernetesClient client = server.getClient();
    for (var crd : new CrdLoader(new YamlMapperProvider().get()).scanCrds()) {
      client.apiextensions().v1()
          .customResourceDefinitions()
          .resource(crd)
          .create();
    }
  }

}

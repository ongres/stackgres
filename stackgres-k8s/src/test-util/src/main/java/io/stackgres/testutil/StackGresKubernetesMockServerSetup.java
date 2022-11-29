/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.testutil;

import java.io.File;
import java.util.Optional;
import java.util.function.Consumer;

import io.fabric8.kubernetes.client.server.mock.KubernetesServer;

public final class StackGresKubernetesMockServerSetup implements Consumer<KubernetesServer> {

  @Override
  public void accept(KubernetesServer server) {
    final var client = server.getClient();

    File file = CrdUtils.getCrdsFolder();
    for (File crdFile : Optional.ofNullable(file.listFiles()).orElse(new File[0])) {
      if (!crdFile.getName().endsWith(".yaml")) {
        continue;
      }
      var crd = client.apiextensions().v1()
          .customResourceDefinitions()
          .load(crdFile).get();
      client.apiextensions().v1()
          .customResourceDefinitions()
          .resource(crd)
          .create();
    }
  }

}

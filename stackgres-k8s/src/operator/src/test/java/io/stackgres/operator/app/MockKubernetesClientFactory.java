/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.app;

import java.io.File;
import java.util.Objects;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.quarkus.test.Mock;
import io.stackgres.operator.AbstractStackGresOperatorIt;
import io.stackgres.operator.CrdMatchTest;


@Mock
@ApplicationScoped
public class MockKubernetesClientFactory extends KubernetesClientFactory {

  private KubernetesServer server = new KubernetesServer(false, true);

  @PostConstruct
  public void init(){
    server = new KubernetesServer(true, true);
    server.before();
    final NamespacedKubernetesClient client = server.getClient();

    File file = CrdMatchTest.getCrdsFolder();
    for (File crdFile: Objects.requireNonNull(file.listFiles())){
      CustomResourceDefinition crd = client.customResourceDefinitions().load(crdFile).get();
      client.customResourceDefinitions().create(crd);
    }
  }

  @Override
  public KubernetesClient create() {
    if (AbstractStackGresOperatorIt.isRunning()) {
      return new DefaultKubernetesClient();
    }
    return server.getClient();
  }

  @PreDestroy
  public void cleanUp(){
    server.after();
  }
}

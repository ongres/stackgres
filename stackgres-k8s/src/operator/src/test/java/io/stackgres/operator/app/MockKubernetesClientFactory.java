/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.app;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.quarkus.test.Mock;
import io.stackgres.operator.AbstractStackGresOperatorIt;
import io.stackgres.operator.CrdMatchTest;
import org.jooq.lambda.Unchecked;


@Mock
@ApplicationScoped
public class MockKubernetesClientFactory extends KubernetesClientFactory {

  private KubernetesServerSupplier serverSupplier = new KubernetesServerSupplier();

  private Instant lastTokenUpdate = null;

  @Override
  public KubernetesClient create() {
    if (AbstractStackGresOperatorIt.isRunning()) {
      updateTokenPeriodically();
      return new DefaultKubernetesClient();
    }
    return serverSupplier.get().getClient();
  }

  public synchronized void updateTokenPeriodically() {
    if (lastTokenUpdate == null
        || lastTokenUpdate.isBefore(Instant.now().plus(10, ChronoUnit.SECONDS))) {
      List<String> operatorSecret = Unchecked.supplier(
          () -> AbstractStackGresOperatorIt.getContainer().execute("sh", "-l", "-c",
              "kubectl get secret -n stackgres -o yaml"
                  + " \"$(kubectl get secret -n stackgres"
                  + " | grep stackgres-operator-token-"
                  + " | sed 's/\\s\\+/ /g'"
                  + " | cut -d ' ' -f 1)\""))
          .get()
          .collect(Collectors.toList());
      System.setProperty(Config.KUBERNETES_CA_CERTIFICATE_DATA_SYSTEM_PROPERTY, operatorSecret.stream()
          .filter(line -> line.startsWith("  ca.crt: "))
          .map(line -> line.substring("  ca.crt: ".length()))
          .findAny().get());
      System.setProperty(Config.KUBERNETES_OAUTH_TOKEN_SYSTEM_PROPERTY, operatorSecret.stream()
          .filter(line -> line.startsWith("  token: "))
          .map(line -> line.substring("  token: ".length()))
          .map(secret -> new String(Base64.getDecoder().decode(secret), StandardCharsets.UTF_8))
          .findAny().get());
      lastTokenUpdate = Instant.now();
    }
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

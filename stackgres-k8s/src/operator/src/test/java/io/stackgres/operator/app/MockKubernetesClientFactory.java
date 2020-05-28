/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.app;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.quarkus.test.Mock;
import io.stackgres.common.KubernetesClientFactory;
import io.stackgres.operator.AbstractStackGresOperatorIt;
import io.stackgres.operator.CrdMatchTest;
import io.stackgres.common.StackGresUtil;
import org.jooq.lambda.Unchecked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Mock
@ApplicationScoped
public class MockKubernetesClientFactory extends KubernetesClientFactory {

  private final static Logger LOGGER = LoggerFactory.getLogger(MockKubernetesClientFactory.class);

  private KubernetesServerSupplier serverSupplier = new KubernetesServerSupplier();

  private AtomicReference<String[]> auth = new AtomicReference<>();

  private final ScheduledExecutorService executor;

  public MockKubernetesClientFactory() {
    executor = Executors.newSingleThreadScheduledExecutor();
    executor.schedule(this::updateTokenPeriodically, 0, TimeUnit.MILLISECONDS);
  }

  @Override
  public KubernetesClient create() {
    if (AbstractStackGresOperatorIt.isRunning()) {
      if (this.auth.get() == null) {
        updateToken();
      }
      String[] auth = this.auth.get();
      return new DefaultKubernetesClient(
          new ConfigBuilder()
          .withNamespace(StackGresUtil.OPERATOR_NAMESPACE)
          .withCaCertData(auth[0])
          .withOauthToken(auth[1])
          .build());
    }
    return serverSupplier.get().getClient();
  }

  private void updateTokenPeriodically() {
    try {
      updateToken();
    } catch (Exception ex) {
      LOGGER.warn("Error while updating the token {}", ex.getMessage());
    }
    if (!executor.isShutdown() && !executor.isTerminated()) {
      executor.schedule(this::updateTokenPeriodically, 1000, TimeUnit.MILLISECONDS);
    }
  }

  private void updateToken() {
    List<String> operatorSecret = Unchecked.supplier(
        () -> AbstractStackGresOperatorIt.getContainer().execute("sh", "-l", "-c",
            "kubectl get secret -n stackgres -o yaml"
                + " \"$(kubectl get secret -n stackgres"
                + " | grep stackgres-operator-token-"
                + " | sed 's/\\s\\+/ /g'"
                + " | cut -d ' ' -f 1)\""))
        .get()
        .collect(Collectors.toList());
    auth.set(new String[] {
        operatorSecret.stream()
        .filter(line -> line.startsWith("  ca.crt: "))
        .map(line -> line.substring("  ca.crt: ".length()))
        .findAny().get(),
        operatorSecret.stream()
        .filter(line -> line.startsWith("  token: "))
        .map(line -> line.substring("  token: ".length()))
        .map(secret -> new String(Base64.getDecoder().decode(secret), StandardCharsets.UTF_8))
        .findAny().get()
    });
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

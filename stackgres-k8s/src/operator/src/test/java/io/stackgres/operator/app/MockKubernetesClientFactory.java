/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.app;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.quarkus.test.Mock;
import io.stackgres.common.OperatorProperty;
import io.stackgres.operator.AbstractStackGresOperatorIt;
import io.stackgres.testutil.CrdUtils;
import io.stackgres.testutil.KubernetesServerSupplier;
import org.jooq.lambda.Unchecked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mock
@ApplicationScoped
public class MockKubernetesClientFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(MockKubernetesClientFactory.class);

  private final KubernetesServerSupplier serverSupplier = new KubernetesServerSupplier();

  private final AtomicReference<String[]> auth = new AtomicReference<>();

  private final ScheduledExecutorService executor;

  public MockKubernetesClientFactory() {
    executor = Executors.newSingleThreadScheduledExecutor();
  }

  @PostConstruct
  public void setup() throws Exception {
    if (AbstractStackGresOperatorIt.isRunning()) {
      updateTokenPeriodically();
      return;
    }
    try (KubernetesClient client = serverSupplier.get().getClient()) {
      CrdUtils.installCrds(client);
    }
  }

  @Produces
  public KubernetesClient create() {
    if (AbstractStackGresOperatorIt.isRunning()) {
      if (this.auth.get() == null) {
        updateToken();
      }
      String[] auth = this.auth.get();
      return new KubernetesClientBuilder()
          .withConfig(
              new ConfigBuilder()
              .withNamespace(OperatorProperty.OPERATOR_NAMESPACE.getString())
              .withCaCertData(auth[0])
              .withOauthToken(auth[1])
              .build())
          .build();
    }
    return serverSupplier.get().getClient();
  }

  private void updateTokenPeriodically() {
    if (AbstractStackGresOperatorIt.isRunning()) {
      try {
        updateToken();
      } catch (Exception ex) {
        LOGGER.warn("Error while updating the token {}", ex.getMessage());
      }
    }
    if (!executor.isShutdown() && !executor.isTerminated()) {
      executor.schedule(this::updateTokenPeriodically, 1000, TimeUnit.MILLISECONDS);
    }
  }

  private void updateToken() {
    List<String> operatorSecret = Unchecked.supplier(
        () -> AbstractStackGresOperatorIt.getContainer().execute("sh", "-l", "-c",
            "kubectl get secret -n " + OperatorProperty.OPERATOR_NAMESPACE.getString() + " -o yaml"
                + " \"$(kubectl get secret -n " + OperatorProperty.OPERATOR_NAMESPACE.getString()
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
  public void cleanUp() {
    if (serverSupplier.wasRetrieved()) {
      serverSupplier.get().after();
    }
  }

}

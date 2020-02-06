/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import com.ongres.junit.docker.Container;
import com.ongres.junit.docker.ContainerParam;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public abstract class AbstractStackGresOperatorIt extends AbstractIt {

  private static final Optional<Boolean> RESET_K8S = Optional.ofNullable(
      Optional.ofNullable(System.getenv("RESET_K8S"))
      .orElse(System.getProperty("it.resetK8s")))
      .map(Boolean::valueOf);
  private static final int OPERATOR_PORT = getFreePort();
  private static final int OPERATOR_SSL_PORT = getFreePort();

  protected final String namespace = getNamespace();
  protected final int k8sSize = getKindSize();

  private Closeable operatorClose;
  private WebTarget operatorClient;

  protected String getNamespace() {
    return "stackgres";
  }

  protected int getKindSize() {
    return 3;
  }

  @BeforeEach
  public void setupOperator(@ContainerParam("k8s") Container k8s) throws Exception {
    ItHelper.killUnwantedProcesses(k8s);
    ItHelper.copyResources(k8s);
    ItHelper.resetKind(k8s, k8sSize, !RESET_K8S.orElse(false));
    ItHelper.deleteStackGresOperatorHelmChartIfExists(k8s, namespace);
    ItHelper.deleteNamespaceIfExists(k8s, namespace);
    ItHelper.installStackGresOperatorHelmChart(k8s, namespace, OPERATOR_SSL_PORT, executor);
    OperatorRunner operatorRunner = ItHelper.createOperator(
        k8s, OPERATOR_PORT, OPERATOR_SSL_PORT, executor);
    CompletableFuture<Void> operator = runAsync(() -> operatorRunner.run());
    this.operatorClose = () -> {
      operatorRunner.close();
      operator.join();
    };
    operatorClient = ClientBuilder.newClient().target("http://localhost:" + OPERATOR_PORT);
    ItHelper.waitUntilOperatorIsReady(operator, operatorClient, k8s);
  }

  private static int getFreePort() {
    final int freePort;
    try (ServerSocket serverSocket = new ServerSocket(0)) {
      freePort = serverSocket.getLocalPort();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
    return freePort;
  }

  @AfterEach
  public void teardownOperator() throws Exception {
    if (operatorClose != null) {
      runAsync(() -> operatorClose.close()).get(10, TimeUnit.SECONDS);
    }
  }

}

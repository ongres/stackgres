/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import com.ongres.junit.docker.Container;
import com.ongres.junit.docker.ContainerParam;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public abstract class AbstractStackGresOperatorIt extends AbstractIt {

  protected final String namespace = getNamespace();

  private Closeable operator;
  private WebTarget operatorClient;

  protected String getNamespace() {
    return "stackgres";
  }

  @BeforeEach
  public void setupOperator(@ContainerParam("kind") Container kind) throws Exception {
    final int operatorPort = getFreePort();
    final int operatorSslPort = getFreePort();
    ItHelper.copyResources(kind);
    ItHelper.deleteStackGresOperatorHelmChartIfExists(kind);
    ItHelper.installStackGresOperatorHelmChart(kind, operatorSslPort, executor);
    OperatorRunner operatorRunner = ItHelper.createOperator(
        getClass(), kind, operatorPort, operatorSslPort);
    CompletableFuture<Void> operator = runAsync(() -> operatorRunner.run());
    this.operator = () -> {
      operatorRunner.close();
      operator.join();
    };
    operatorClient = ClientBuilder.newClient().target("http://localhost:" + operatorPort);
    ItHelper.waitUntilOperatorIsReady(operator, operatorClient, kind);
  }

  private int getFreePort() throws IOException {
    final int freePort;
    try (ServerSocket serverSocket = new ServerSocket(0)) {
      freePort = serverSocket.getLocalPort();
    }
    return freePort;
  }

  @AfterEach
  public void teardownOperator() throws Exception {
    if (operator != null) {
      operator.close();
    }
  }

}

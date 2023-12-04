/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.testutil;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.Supplier;

import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.fabric8.mockwebserver.dsl.MockServerExpectation;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class KubernetesServerSupplier implements Supplier<KubernetesServer> {
  KubernetesServer server;

  public synchronized boolean wasRetrieved() {
    return server != null;
  }

  @Override
  public synchronized KubernetesServer get() {
    if (server == null) {
      KubernetesServer k8sServer = new KubernetesServer(true, true);
      k8sServer.before();
      k8sServer = new WrappedKubernetesServer(k8sServer);
      this.server = k8sServer;
    }
    return server;
  }

  @SuppressWarnings("deprecation")
  static class WrappedKubernetesServer extends KubernetesServer {
    final KubernetesServer server;
    final NamespacedKubernetesClient client;

    WrappedKubernetesServer(KubernetesServer server) {
      this.server = server;
      this.client = (NamespacedKubernetesClient) Proxy
          .newProxyInstance(NamespacedKubernetesClient.class.getClassLoader(),
              new Class<?>[] { NamespacedKubernetesClient.class },
              new KubernetesClientInvocationHandler(server.getClient()));
    }

    @Override
    public Statement apply(Statement base, Description description) {
      return server.apply(base, description);
    }

    @Override
    public int hashCode() {
      return server.hashCode();
    }

    @Override
    public void before() {
      server.before();
    }

    @Override
    public void after() {
      server.after();
    }

    @Override
    public NamespacedKubernetesClient getClient() {
      return client;
    }

    @Override
    public MockServerExpectation expect() {
      return server.expect();
    }

    @Override
    public <T> void expectAndReturnAsJson(String path, int code, T body) {
      server.expectAndReturnAsJson(path, code, body);
    }

    @Override
    public <T> void expectAndReturnAsJson(String method, String path, int code, T body) {
      server.expectAndReturnAsJson(method, path, code, body);
    }

    @Override
    public void expectAndReturnAsString(String path, int code, String body) {
      server.expectAndReturnAsString(path, code, body);
    }

    @Override
    public void expectAndReturnAsString(String method, String path, int code, String body) {
      server.expectAndReturnAsString(method, path, code, body);
    }

    @Override
    public KubernetesMockServer getKubernetesMockServer() {
      return server.getKubernetesMockServer();
    }

    @Override
    public RecordedRequest getLastRequest() throws InterruptedException {
      return server.getLastRequest();
    }

    @Override
    public boolean equals(Object obj) {
      return server.equals(obj);
    }

    @Override
    public String toString() {
      return server.toString();
    }
  }

  static class KubernetesClientInvocationHandler implements InvocationHandler {

    final NamespacedKubernetesClient client;

    KubernetesClientInvocationHandler(NamespacedKubernetesClient client) {
      this.client = client;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      if ("close".equals(method.getName())) {
        return null;
      }
      try {
        return method.invoke(client, args);
      } catch (Exception ex) {
        if (ex instanceof InterruptedException) {
          Thread.currentThread().interrupt();
        }
        if (ex.getCause() != null) {
          throw ex.getCause();
        } else {
          throw ex;
        }
      }
    }
  }
}

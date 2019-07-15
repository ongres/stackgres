/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.app;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.google.common.io.Resources;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.stackgres.crd.sgcluster.StackGresCluster;
import io.stackgres.crd.sgcluster.StackGresClusterDefinition;
import io.stackgres.crd.sgcluster.StackGresClusterDoneable;
import io.stackgres.crd.sgcluster.StackGresClusterList;
import io.stackgres.util.ResourceUtils;
import io.stackgres.watcher.StackGresClusterWatcher;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class StackGresOperatorApp {

  private static final Logger LOGGER = LoggerFactory.getLogger(StackGresOperatorApp.class);

  @ConfigProperty(name = "stackgres.namespace", defaultValue = "stackgres")
  String namespace;

  @Inject
  KubernetesClientFactory kubClientFactory;

  @Inject
  StackGresClusterWatcher watcher;

  void onStart(@Observes StartupEvent ev) {
    printArt();
    try (KubernetesClient client = kubClientFactory.retrieveKubernetesClient()) {
      log(client);
      // Create a namespace for all our stuff
      Namespace ns = new NamespaceBuilder()
          .withNewMetadata()
          .withName(namespace)
          .endMetadata()
          .build();
      client.namespaces().createOrReplace(ns);
      LOGGER.debug("Created or replaced namespace: {}", ns);
      startClusterCrdWatcher(client);
    } catch (KubernetesClientException e) {
      if (e.getCause() instanceof SocketTimeoutException) {
        LOGGER.error("Kubernetes cluster is not reachable, check your connection.");
      }
      throw e;
    }
  }

  void onStop(@Observes ShutdownEvent ev) {
    LOGGER.info("The application is stopping...");
  }

  private void startClusterCrdWatcher(KubernetesClient client) {
    LOGGER.info("CRD Watcher: {}", StackGresClusterDefinition.NAME);
    createCrdIfNotExists(client, StackGresClusterDefinition.CR_DEFINITION,
        StackGresClusterDefinition.NAME);

    KubernetesDeserializer.registerCustomKind(StackGresClusterDefinition.APIVERSION,
        StackGresClusterDefinition.KIND, StackGresCluster.class);

    kubClientFactory.retrieveKubernetesClient()
        .customResources(StackGresClusterDefinition.CR_DEFINITION,
            StackGresCluster.class,
            StackGresClusterList.class,
            StackGresClusterDoneable.class)
        .inNamespace(namespace)
        .watch(watcher);
  }

  private static void createCrdIfNotExists(KubernetesClient client,
      CustomResourceDefinition definition, String name) {
    boolean exists = ResourceUtils.exists(client.customResourceDefinitions().list().getItems(),
        name);

    if (!exists) {
      client.customResourceDefinitions().create(definition);
    }
  }

  private static void log(KubernetesClient client) {
    LOGGER.info("Kubernetes version: {}", client.getVersion().getGitVersion());
    LOGGER.info("URL of this Kubernetes cluster: {}", client.getMasterUrl());
    LOGGER.info("Default namespace selected: {}", client.getNamespace());
  }

  private void printArt() {
    try {
      System.out.println(Resources.toString(
          Resources.getResource(StackGresOperatorApp.class, "/META-INF/banner.txt"),
          StandardCharsets.UTF_8));
    } catch (IOException ignored) {
      // ignored, not important if we can't print the ASCII-art.
    }
  }

}

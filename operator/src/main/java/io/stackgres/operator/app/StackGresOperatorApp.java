/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.app;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.google.common.io.Resources;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.stackgres.common.ResourceUtils;
import io.stackgres.operator.customresources.pgconfig.StackGresPostgresConfig;
import io.stackgres.operator.customresources.pgconfig.StackGresPostgresConfigDefinition;
import io.stackgres.operator.customresources.sgcluster.StackGresCluster;
import io.stackgres.operator.customresources.sgcluster.StackGresClusterDefinition;
import io.stackgres.operator.customresources.sgcluster.StackGresClusterDoneable;
import io.stackgres.operator.customresources.sgcluster.StackGresClusterList;
import io.stackgres.operator.customresources.sgprofile.StackGresProfile;
import io.stackgres.operator.customresources.sgprofile.StackGresProfileDefinition;
import io.stackgres.operator.watcher.StackGresClusterWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class StackGresOperatorApp {

  private static final Logger LOGGER = LoggerFactory.getLogger(StackGresOperatorApp.class);

  @Inject
  KubernetesClientFactory kubeClient;

  @Inject
  StackGresClusterWatcher watcher;

  void onStart(@Observes StartupEvent ev) {
    printArt();
    try (KubernetesClient client = kubeClient.retrieveKubernetesClient()) {
      LOGGER.info("Kubernetes version: {}", client.getVersion().getGitVersion());
      LOGGER.info("URL of this Kubernetes cluster: {}", client.getMasterUrl());
      if (!hasCustomResource(client, StackGresProfileDefinition.NAME)
          || !hasCustomResource(client, StackGresPostgresConfigDefinition.NAME)
          || !hasCustomResource(client, StackGresClusterDefinition.NAME)) {
        throw new RuntimeException("Some required CRDs does not exists");
      }
      registerCustomResources();
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
    ResourceUtils.getCustomResource(client, StackGresClusterDefinition.NAME)
        .ifPresent(crd -> kubeClient.retrieveKubernetesClient()
            .customResources(crd,
                StackGresCluster.class,
                StackGresClusterList.class,
                StackGresClusterDoneable.class)
            .inAnyNamespace()
            .watch(watcher));
  }

  private void registerCustomResources() {
    KubernetesDeserializer.registerCustomKind(StackGresClusterDefinition.APIVERSION,
        StackGresClusterDefinition.KIND, StackGresCluster.class);

    KubernetesDeserializer.registerCustomKind(StackGresPostgresConfigDefinition.APIVERSION,
        StackGresPostgresConfigDefinition.KIND, StackGresPostgresConfig.class);

    KubernetesDeserializer.registerCustomKind(StackGresProfileDefinition.APIVERSION,
        StackGresProfileDefinition.KIND, StackGresProfile.class);
  }

  private boolean hasCustomResource(KubernetesClient client, String crdName) {
    if (!ResourceUtils.getCustomResource(client, crdName).isPresent()) {
      LOGGER.error("CRD not found, please create it first: {}", crdName);
      return false;
    }
    return true;
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

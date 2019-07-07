/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.app;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@SuppressWarnings("initialization.fields.uninitialized")
@ApplicationScoped
public class KubernetesClientFactory {

  @ConfigProperty(name = "stackgres.namespace", defaultValue = "stackgres")
  String namespace;

  /**
   * Create a default Kubernetes Client.
   */
  public KubernetesClient retrieveKubernetesClient() {
    Config config = new ConfigBuilder().build();
    config.setNamespace(namespace);
    return new DefaultKubernetesClient(config);
  }

}

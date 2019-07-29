/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.app;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

@ApplicationScoped
public class KubernetesClientFactory {

  public KubernetesClient retrieveKubernetesClient() {
    return new DefaultKubernetesClient();
  }

}

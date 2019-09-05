/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.app;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

@ApplicationScoped
public class KubernetesClientFactory {

  public KubernetesClient create() {
    return new DefaultKubernetesClient();
  }

}

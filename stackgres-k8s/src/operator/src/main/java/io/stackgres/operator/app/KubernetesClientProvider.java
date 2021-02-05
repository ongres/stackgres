/*
 * Copyright (C) 2020 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.app;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.KubernetesClientFactory;
import io.stackgres.common.workaround.DefaultKubernetesClient;

@ApplicationScoped
public class KubernetesClientProvider implements KubernetesClientFactory {

  @Override
  public KubernetesClient create() {
    return new DefaultKubernetesClient();
  }

}

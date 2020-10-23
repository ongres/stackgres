/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.common;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

public class KubernetesClientFactoryImpl implements KubernetesClientFactory {

  @Override
  public KubernetesClient create() {
    return new DefaultKubernetesClient();
  }
}

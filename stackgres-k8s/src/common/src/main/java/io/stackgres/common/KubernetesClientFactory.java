/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.function.Function;

import io.fabric8.kubernetes.client.KubernetesClient;

public interface KubernetesClientFactory {

  KubernetesClient create();

  default <T> T withNewClient(Function<KubernetesClient, T> func) {
    try (var client = create()) {
      return func.apply(client);
    }
  }

}

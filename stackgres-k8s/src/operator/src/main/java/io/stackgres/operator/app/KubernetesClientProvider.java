/*
 * Copyright (C) 2020 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.app;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.common.StackGresKubernetesClient;
import io.stackgres.common.StackGresKubernetesClientFactory;

@ApplicationScoped
public class KubernetesClientProvider implements StackGresKubernetesClientFactory {

  @Override
  public StackGresKubernetesClient create() {
    return new StackGresKubernetesClient();
  }

}

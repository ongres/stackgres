/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory;

import java.util.function.Supplier;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.VersionInfo;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class KubernetesVersionProvider implements Supplier<VersionInfo> {

  @Inject
  KubernetesClient client;

  @Override
  public VersionInfo get() {
    return client.getKubernetesVersion();
  }

}

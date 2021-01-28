/*
 * Copyright (C) 2020 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.app;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.KubernetesClientFactory;
import io.stackgres.common.kubernetes.DefaultKubernetesClient;
import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.jwt.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequestScoped
public class KubernetesClientProvider implements KubernetesClientFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesClientProvider.class);

  @Inject
  @Claim(standard = Claims.sub)
  String impersonate;

  @Override
  public KubernetesClient create() {
    LOGGER.debug("Impersonate user {}", impersonate);
    Config config;
    if (null != impersonate) {
      config = new ConfigBuilder()
          .withImpersonateUsername(impersonate)
          .withImpersonateGroup("system:authenticated")
          .build();
    } else {
      config = new ConfigBuilder().build();
    }

    return new DefaultKubernetesClient(config);
  }

}

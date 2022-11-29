/*
 * Copyright (C) 2020 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.app;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.quarkus.arc.Priority;
import io.quarkus.security.identity.SecurityIdentity;
import org.eclipse.microprofile.jwt.Claim;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequestScoped
public class KubernetesClientProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesClientProvider.class);

  @Inject
  SecurityIdentity identity;

  @Inject
  @Claim("stackgres_k8s_username")
  String k8sUsername;

  @Produces
  @RequestScoped
  @Alternative
  @Priority(1)
  public KubernetesClient create() {
    Config config;

    if (!identity.isAnonymous()) {
      String impersonated = k8sUsername != null ? k8sUsername : identity.getPrincipal().getName();
      LOGGER.debug("Impersonate user {}", impersonated);
      config = new ConfigBuilder()
          .withImpersonateUsername(impersonated)
          .withImpersonateGroups("system:authenticated")
          .build();
    } else {
      config = new ConfigBuilder().build();
    }

    return new KubernetesClientBuilder().withConfig(config).build();
  }

  public KubernetesClient createDefault() {
    return new KubernetesClientBuilder().build();
  }

}

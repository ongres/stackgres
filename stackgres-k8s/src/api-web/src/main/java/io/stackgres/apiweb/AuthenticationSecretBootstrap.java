/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.runtime.StartupEvent;
import io.stackgres.apiweb.config.WebApiContext;
import io.stackgres.apiweb.config.WebApiProperty;
import io.stackgres.common.KubernetesClientFactory;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.resource.ResourceUtil;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class AuthenticationSecretBootstrap {

  private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationSecretBootstrap.class);

  private KubernetesClientFactory kubeClient;
  private WebApiContext context;

  @ConfigProperty(name = "stackgres.restapiNamespace")
  String namespace;

  public void init(@Observes StartupEvent ev) {

    LOGGER.info("Initializing authentication secret");
    String secretName = context.get(WebApiProperty.AUTHENTICATION_SECRET_NAME);
    try (KubernetesClient client = kubeClient.create()) {
      if (client.secrets().inNamespace(namespace).withName(secretName).get() != null) {
        LOGGER.info("Authentication secret found. Skipping creation.");
        return;
      }
      LOGGER.info("No rest api secret found, creating a new one");
      final String randUser = ResourceUtil.encodeSecret(ResourceUtil.generateRandom(20));
      final String randPassword = ResourceUtil
          .encodeSecret(ResourceUtil.generateRandom(40));
      Secret secret = new SecretBuilder()
          .withNewMetadata()
          .withName(secretName)
          .withNamespace(namespace)
          .endMetadata()
          .addToData(StackGresContext.REST_APIUSER_KEY, randUser)
          .addToData(StackGresContext.REST_PASSWORD_KEY, randPassword)
          .build();

      client.secrets().create(secret);
    }
  }

  @Inject
  public void setKubeClient(KubernetesClientFactory kubeClient) {
    this.kubeClient = kubeClient;
  }

  @Inject
  public void setContext(WebApiContext context) {
    this.context = context;
  }
}

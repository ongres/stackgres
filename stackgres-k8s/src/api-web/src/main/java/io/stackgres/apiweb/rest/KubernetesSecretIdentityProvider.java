/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.security.Principal;
import java.util.Arrays;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.credential.PasswordCredential;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.stackgres.apiweb.AuthenticationSecretBootstrap;
import io.stackgres.apiweb.config.WebApiContext;
import io.stackgres.apiweb.config.WebApiProperty;
import io.stackgres.common.KubernetesClientFactory;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.resource.ResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class KubernetesSecretIdentityProvider
    implements IdentityProvider<UsernamePasswordAuthenticationRequest> {

  private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationSecretBootstrap.class);

  private final KubernetesClientFactory clientFactory;
  private final WebApiContext webApiContext;
  private final Principal principal = new QuarkusPrincipal(RestAuthenticationRoles.REALM_NAME);

  @Inject
  public KubernetesSecretIdentityProvider(KubernetesClientFactory clientFactory,
                                          WebApiContext webApiContext) {
    this.clientFactory = clientFactory;
    this.webApiContext = webApiContext;
  }

  @Override
  public Class<UsernamePasswordAuthenticationRequest> getRequestType() {
    return UsernamePasswordAuthenticationRequest.class;
  }

  @Override
  public CompletionStage<SecurityIdentity> authenticate(
      UsernamePasswordAuthenticationRequest request, AuthenticationRequestContext context) {
    String secretName = webApiContext.get(WebApiProperty.AUTHENTICATION_SECRET_NAME);

    return context.runBlocking(() -> {
      try (KubernetesClient client = clientFactory.create()) {
        final Secret secret = client
            .secrets()
            .withName(secretName).get();
        if (secret == null) {
          LOGGER.error("Authentication secret not found");
          throw new AuthenticationFailedException();
        }

        String storedUsername = getStoredUsername(secret);
        if (!request.getUsername().equals(storedUsername)) {
          LOGGER.debug("Invalid username " + request.getUsername());
          throw new AuthenticationFailedException();

        }

        char[] storedPassword = getStoredPassword(secret);
        if (!Arrays.equals(request.getPassword().getPassword(), storedPassword)) {
          LOGGER.debug("Invalid password " + Arrays.toString(request.getPassword().getPassword()));
          throw new AuthenticationFailedException();
        }

        return QuarkusSecurityIdentity.builder().setPrincipal(principal)
            .addRole(RestAuthenticationRoles.ADMIN)
            .addCredential(new PasswordCredential(storedPassword))
            .build();
      }
    });
  }

  private char[] getStoredPassword(Secret secret) {
    return ResourceUtil.dencodeSecret(secret.getData().get(StackGresContext.REST_PASSWORD_KEY))
        .toCharArray();
  }

  private String getStoredUsername(Secret secret) {
    return ResourceUtil.dencodeSecret(secret.getData().get(StackGresContext.REST_USER_KEY));
  }
}

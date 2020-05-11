/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest;

import java.security.Principal;
import java.util.Arrays;
import java.util.Optional;
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
import io.stackgres.operator.app.KubernetesClientFactory;
import io.stackgres.operator.common.ConfigLoader;
import io.stackgres.operator.common.ConfigProperty;
import io.stackgres.operator.common.StackGresUtil;
import io.stackgres.operatorframework.resource.ResourceUtil;

@ApplicationScoped
public class KubernetesSecretIdentityProvider
    implements IdentityProvider<UsernamePasswordAuthenticationRequest> {

  private final KubernetesClientFactory clientFactory;
  private final String operatorNamespace;
  private final String secretName;
  private final Principal principal = new QuarkusPrincipal(RestAuthenticationRoles.REALM_NAME);

  @Inject
  public KubernetesSecretIdentityProvider(KubernetesClientFactory clientFactory,
      ConfigLoader configLoader) {
    super();
    this.clientFactory = clientFactory;
    this.operatorNamespace = configLoader.get(ConfigProperty.OPERATOR_NAMESPACE);
    this.secretName = configLoader.get(ConfigProperty.AUTHENTICATION_SECRET_NAME);
  }

  @Override
  public Class<UsernamePasswordAuthenticationRequest> getRequestType() {
    return UsernamePasswordAuthenticationRequest.class;
  }

  @Override
  public CompletionStage<SecurityIdentity> authenticate(
      UsernamePasswordAuthenticationRequest request, AuthenticationRequestContext context) {
    return context.runBlocking(() -> {
      try (KubernetesClient client = clientFactory.create()) {
        return Optional
            .ofNullable(
                client.secrets().inNamespace(operatorNamespace).withName(secretName).get())
            .map(Secret::getData)
            .filter(data -> Optional.ofNullable(data.get(StackGresUtil.REST_USER_KEY))
                .map(ResourceUtil::dencodeSecret).map(request.getUsername()::equals)
                .orElse(false))
            .map(data -> data.get(StackGresUtil.REST_PASSWORD_KEY))
            .map(ResourceUtil::dencodeSecret).map(String::toCharArray)
            .filter(password -> Arrays.equals(request.getPassword().getPassword(), password))
            .map(password -> QuarkusSecurityIdentity.builder().setPrincipal(principal)
                .addRole(RestAuthenticationRoles.ADMIN)
                .addCredential(new PasswordCredential(password)).build())
            .orElseThrow(AuthenticationFailedException::new);
      }
    });
  }
}

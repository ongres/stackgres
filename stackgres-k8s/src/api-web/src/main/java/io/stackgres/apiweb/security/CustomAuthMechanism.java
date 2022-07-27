/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.security;

import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;

import io.quarkus.arc.Priority;
import io.quarkus.oidc.runtime.OidcAuthenticationMechanism;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.smallrye.jwt.runtime.auth.JWTAuthMechanism;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

/**
 * Allows to switch between local JWT authentication and OIDC authentication.
 */
@Alternative
@Priority(1)
@ApplicationScoped
public class CustomAuthMechanism implements HttpAuthenticationMechanism {

  @Inject
  AuthConfig config;

  @Inject
  JWTAuthMechanism jwt;

  @Inject
  OidcAuthenticationMechanism oidc;

  @Override
  public Uni<SecurityIdentity> authenticate(RoutingContext context,
      IdentityProviderManager identityProviderManager) {
    return selectAuthMechanism().authenticate(context, identityProviderManager);
  }

  @Override
  public Uni<ChallengeData> getChallenge(RoutingContext context) {
    return selectAuthMechanism().getChallenge(context);
  }

  @Override
  public Set<Class<? extends AuthenticationRequest>> getCredentialTypes() {
    return selectAuthMechanism().getCredentialTypes();
  }

  @Override
  public int getPriority() {
    return HttpAuthenticationMechanism.DEFAULT_PRIORITY + 100;
  }

  private HttpAuthenticationMechanism selectAuthMechanism() {
    AuthType type = config.type();
    return switch (type) {
      case JWT -> jwt;
      case OIDC -> oidc;
    };
  }

}

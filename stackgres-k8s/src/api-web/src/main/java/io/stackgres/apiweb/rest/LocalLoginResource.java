/*
 * Copyright (C) 2020 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import io.quarkus.security.AuthenticationFailedException;
import io.stackgres.apiweb.security.SecretVerification;
import io.stackgres.apiweb.security.TokenResponse;
import io.stackgres.apiweb.security.TokenUtils;
import io.stackgres.apiweb.security.UserPassword;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/stackgres/auth")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LocalLoginResource {

  private static final Logger LOGGER = LoggerFactory.getLogger(LocalLoginResource.class);

  @Inject
  SecretVerification verify;

  private static final long DURATION = 28800; // 8h

  @POST
  @Path("login")
  public Response login(@Valid UserPassword credentials) {
    try {
      String k8sUsername =
          verify.verifyCredentials(credentials.getUsername(), credentials.getPassword());
      LOGGER.info("Kubernetes user: {}", k8sUsername);
      String accessToken =
          TokenUtils.generateTokenString(k8sUsername, credentials.getUsername(), DURATION,
              "/etc/operator/certs/jwt-rsa.key");

      TokenResponse tokenResponse = new TokenResponse();
      tokenResponse.setAccessToken(accessToken);
      tokenResponse.setExpiresIn(DURATION);
      tokenResponse.setTokenType("Bearer");

      return Response.ok(tokenResponse)
          .cacheControl(noCache())
          .build();
    } catch (AuthenticationFailedException e) {
      return Response.status(Status.FORBIDDEN)
          .cacheControl(noCache())
          .build();
    }
  }

  private CacheControl noCache() {
    CacheControl cc = new CacheControl();
    cc.setPrivate(true);
    cc.setNoCache(true);
    cc.setNoStore(true);
    return cc;
  }

}

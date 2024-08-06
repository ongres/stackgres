/*
 * Copyright (C) 2020 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest.auth;

import java.net.URI;
import java.util.Map;

import io.quarkus.oidc.OidcSession;
import io.quarkus.security.Authenticated;
import io.quarkus.security.AuthenticationFailedException;
import io.stackgres.apiweb.exception.ErrorResponse;
import io.stackgres.apiweb.security.AuthConfig;
import io.stackgres.apiweb.security.AuthType;
import io.stackgres.apiweb.security.SecretVerification;
import io.stackgres.apiweb.security.TokenResponse;
import io.stackgres.apiweb.security.TokenUtils;
import io.stackgres.apiweb.security.UserPassword;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("auth")
@RequestScoped
@Tag(name = "auth")
@APIResponse(responseCode = "400", description = "Bad Request",
    content = {@Content(
        mediaType = "application/json",
        schema = @Schema(implementation = ErrorResponse.class))})
@APIResponse(responseCode = "401", description = "Unauthorized",
    content = {@Content(
        mediaType = "application/json",
        schema = @Schema(implementation = ErrorResponse.class))})
@APIResponse(responseCode = "403", description = "Forbidden",
    content = {@Content(
        mediaType = "application/json",
        schema = @Schema(implementation = ErrorResponse.class))})
@APIResponse(responseCode = "500", description = "Internal Server Error",
    content = {@Content(
        mediaType = "application/json",
        schema = @Schema(implementation = ErrorResponse.class))})
public class AuthResource {

  private static final Logger LOGGER = LoggerFactory.getLogger(AuthResource.class);

  private static final CacheControl NO_CACHE = noCache();

  @Inject
  SecretVerification verify;

  @ConfigProperty(name = "smallrye.jwt.new-token.lifespan", defaultValue = "28800")
  long duration;

  @Inject
  AuthConfig config;

  @Inject
  OidcSession oidcSession;

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(implementation = TokenResponse.class,
              description = "The JWT bearer token that is needed to authenticate a user request."))})
  @Operation(summary = "Login", description = """
      Log in a user and returns a JWT token.

      ### RBAC permissions required

      None
      """)
  @POST
  @Path("login")
  public Response login(@Valid UserPassword credentials) {
    try {
      String k8sUsername =
          verify.verifyCredentials(credentials.username(), credentials.password());
      LOGGER.info("Kubernetes user: {}", k8sUsername);
      String accessToken = TokenUtils.generateTokenString(k8sUsername, credentials.username());

      TokenResponse tokenResponse = new TokenResponse();
      tokenResponse.setAccessToken(accessToken);
      tokenResponse.setTokenType("Bearer");
      tokenResponse.setExpiresIn(duration);

      return Response.ok(tokenResponse)
          .cacheControl(NO_CACHE)
          .build();
    } catch (AuthenticationFailedException e) {
      return Response.status(Status.FORBIDDEN)
          .cacheControl(NO_CACHE)
          .build();
    }
  }

  @APIResponse(responseCode = "200", description = "OK")
  @Operation(summary = "Logout", description = """
      Log out an active OIDC session.

      ### RBAC permissions required

      None
      """)
  @GET
  @Path("logout")
  @Authenticated
  public Response logout() {
    if (config.type() == AuthType.OIDC) {
      // Invalidate the session
      oidcSession.logout().await().indefinitely();
    }
    // Redirect to the post-logout page
    return Response.ok().build();
  }

  @APIResponse(responseCode = "200", description = "OK")
  @APIResponse(responseCode = "307", description = "Redirect")
  @Operation(summary = "External authentication URL", description = """
      Return the URL for external authentication.

      ### RBAC permissions required

      None
      """)
  @GET
  @Path("external")
  @Authenticated
  public Response externalRedirect(@QueryParam("redirectTo") URI redirectTo) {
    return Response.temporaryRedirect(redirectTo)
        .cacheControl(NO_CACHE)
        .build();
  }

  @APIResponse(responseCode = "200", description = "OK")
  @Operation(summary = "Type of authentication", description = """
      Return the configured auth mechanism type.

      ### RBAC permissions required

      None
      """)
  @GET
  @Path("type")
  public Response type(@QueryParam("redirectTo") URI redirectTo) {
    return Response.ok(Map.of("type", config.type()))
        .cacheControl(NO_CACHE)
        .header("WWW-Authenticate", config.type())
        .build();
  }

  private static final CacheControl noCache() {
    CacheControl cc = new CacheControl();
    cc.setPrivate(true);
    cc.setNoCache(true);
    cc.setNoStore(true);
    return cc;
  }

}

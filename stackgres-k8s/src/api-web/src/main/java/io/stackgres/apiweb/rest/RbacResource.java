/*
 * Copyright (C) 2020 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.util.Optional;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import io.fabric8.kubernetes.api.model.authorization.SubjectAccessReview;
import io.fabric8.kubernetes.api.model.authorization.SubjectAccessReviewBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.StackGresProperty;
import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.jwt.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/stackgres/auth/rbac")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RbacResource {

  private static final Logger LOGGER = LoggerFactory.getLogger(RbacResource.class);

  @Inject
  @Claim(standard = Claims.sub)
  String user;

  @GET
  @Path("/can-i/{verb}/{resource}")
  public Response verb(@PathParam("verb") String verb, @PathParam("resource") String resource,
      @QueryParam("namespace") String namespace, @QueryParam("group") Optional<String> group) {
    LOGGER.debug("User to review access {}", user);
    try (KubernetesClient client = new DefaultKubernetesClient()) {

      SubjectAccessReview review = new SubjectAccessReviewBuilder()
          .withNewSpec()
          .withUser(user)
          .withNewResourceAttributes()
          .withNamespace(namespace)
          .withGroup(group.orElse(StackGresProperty.CRD_GROUP.getString()))
          .withResource(resource)
          .withVerb(verb)
          .endResourceAttributes()
          .endSpec()
          .build();

      review = client.subjectAccessReviewAuth()
          .create(review);

      LOGGER.debug("{}", review);

      if (Boolean.TRUE.equals(review.getStatus().getAllowed())) {
        return Response.ok(review.getStatus()).build();
      } else {
        return Response.status(Status.FORBIDDEN).entity(review.getStatus()).build();
      }

    }
  }

}

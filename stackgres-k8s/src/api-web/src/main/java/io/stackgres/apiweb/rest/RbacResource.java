/*
 * Copyright (C) 2020 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.authorization.SubjectAccessReview;
import io.fabric8.kubernetes.api.model.authorization.SubjectAccessReviewBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.apiweb.dto.PermissionsListDto;
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

  NamespaceResource namespaces;

  @Inject
  public RbacResource(NamespaceResource namespaces) {
    super();
    this.namespaces = namespaces;
  }

  @GET
  @Path("/can-i/{verb}/{resource}")
  public Response verb(@PathParam("verb") String verb, @PathParam("resource") String resource,
      @QueryParam("namespace") String namespace, @QueryParam("group") Optional<String> group) {
    LOGGER.debug("User to review access {}", user);
    // Connect with the serviceaccount permissions
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

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/can-i")
  public Response caniList() {
    // Connect with the serviceaccount permissions
    try (KubernetesClient client = new DefaultKubernetesClient()) {

      List<String> verbs = ImmutableList.of("get", "list", "create", "patch", "delete");
      List<String> resourcesNamespaced = ImmutableList.of("pods", "secrets",
          "sgbackupconfigs.stackgres.io", "sgbackups.stackgres.io", "sgclusters.stackgres.io",
          "sgdistributedlogs.stackgres.io", "sginstanceprofiles.stackgres.io",
          "sgpgconfigs.stackgres.io", "sgpoolconfigs.stackgres.io");
      List<String> resourcesUnnamespaced =
          ImmutableList.of("namespaces", "storageclasses.storage.k8s.io");

      PermissionsListDto permissionsList = new PermissionsListDto();
      List<PermissionsListDto.Namespaced> listNamespaced = new ArrayList<>();
      for (String ns : namespaces.get()) {
        PermissionsListDto.Namespaced permisionsNamespaced = new PermissionsListDto.Namespaced();
        Map<String, List<String>> resourceNamespace = new HashMap<>();
        for (String rsNamespaced : resourcesNamespaced) {
          String[] resource = rsNamespaced.split("\\.", 2);
          List<String> allowed = accessReview(client, user, ns, resource[0],
              resource.length == 2 ? resource[1] : "", verbs);
          resourceNamespace.put(resource[0], allowed);
        }
        permisionsNamespaced.setNamespace(ns);
        permisionsNamespaced.setResources(resourceNamespace);
        listNamespaced.add(permisionsNamespaced);
      }

      Map<String, List<String>> resourceUnamespace = new HashMap<>();
      for (String rsUnnamespaced : resourcesUnnamespaced) {
        String[] resource = rsUnnamespaced.split("\\.", 2);
        List<String> allowed = accessReview(client, user, null, resource[0],
            resource.length == 2 ? resource[1] : "", verbs);
        resourceUnamespace.put(resource[0], allowed);
      }

      permissionsList.setNamespaced(listNamespaced);
      permissionsList.setUnnamespaced(resourceUnamespace);

      return Response.ok(permissionsList).build();
    }
  }

  private List<String> accessReview(KubernetesClient client, String user, String namespace,
      String resource, String group, List<String> verbs) {
    List<String> allowed = new ArrayList<>();
    for (String verb : verbs) {
      SubjectAccessReview review = new SubjectAccessReviewBuilder()
          .withNewSpec()
          .withUser(user)
          .withNewResourceAttributes()
          .withNamespace(namespace)
          .withResource(resource)
          .withGroup(group)
          .withVerb(verb)
          .endResourceAttributes()
          .endSpec()
          .build();
      review = client.subjectAccessReviewAuth()
          .create(review);

      if (Boolean.TRUE.equals(review.getStatus().getAllowed())) {
        allowed.add(verb);
      }
    }
    return allowed;
  }

}

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
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.authorization.v1.SubjectAccessReview;
import io.fabric8.kubernetes.api.model.authorization.v1.SubjectAccessReviewBuilder;
import io.fabric8.kubernetes.api.model.authorization.v1.SubjectAccessReviewStatus;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.apiweb.dto.PermissionsListDto;
import io.stackgres.apiweb.rest.utils.CommonApiResponses;
import io.stackgres.common.crd.CommonDefinition;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.jwt.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("auth/rbac")
@RequestScoped
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

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK",
              content = {@Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = SubjectAccessReviewStatus.class))})
      })
  @CommonApiResponses
  @GET
  @Path("can-i/{verb}/{resource}")
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
          .withGroup(group.orElse(CommonDefinition.GROUP))
          .withResource(resource)
          .withVerb(verb)
          .endResourceAttributes()
          .endSpec()
          .build();

      review = client.authorization().v1().subjectAccessReview()
          .create(review);

      LOGGER.debug("{}", review);

      if (Boolean.TRUE.equals(review.getStatus().getAllowed())) {
        return Response.ok(review.getStatus()).build();
      } else {
        return Response.status(Status.FORBIDDEN).entity(review.getStatus()).build();
      }

    }
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK",
              content = {@Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = PermissionsListDto.class))})
      })
  @CommonApiResponses
  @GET
  @Path("can-i")
  public Response caniList() {
    // Connect with the serviceaccount permissions
    try (KubernetesClient client = new DefaultKubernetesClient()) {

      List<String> verbs = List.of("get", "list", "create", "patch", "delete");
      List<String> resourcesNamespaced = List.of("pods", "secrets", "configmaps",
          HasMetadata.getFullResourceName(StackGresObjectStorage.class),
          HasMetadata.getFullResourceName(StackGresBackupConfig.class),
          HasMetadata.getFullResourceName(StackGresBackup.class),
          HasMetadata.getFullResourceName(StackGresCluster.class),
          HasMetadata.getFullResourceName(StackGresDistributedLogs.class),
          HasMetadata.getFullResourceName(StackGresProfile.class),
          HasMetadata.getFullResourceName(StackGresDbOps.class),
          HasMetadata.getFullResourceName(StackGresPostgresConfig.class),
          HasMetadata.getFullResourceName(StackGresPoolingConfig.class));
      List<String> resourcesUnnamespaced =
          List.of("namespaces", "storageclasses.storage.k8s.io");

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
      review = client.authorization().v1().subjectAccessReview()
          .create(review);

      if (Boolean.TRUE.equals(review.getStatus().getAllowed())) {
        allowed.add(verb);
      }
    }
    return allowed;
  }

}

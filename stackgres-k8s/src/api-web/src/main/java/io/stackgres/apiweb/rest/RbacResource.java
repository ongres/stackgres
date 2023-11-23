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

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.authorization.v1.SubjectAccessReview;
import io.fabric8.kubernetes.api.model.authorization.v1.SubjectAccessReviewBuilder;
import io.fabric8.kubernetes.api.model.authorization.v1.SubjectAccessReviewStatus;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import io.stackgres.apiweb.app.KubernetesClientProvider;
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
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.eclipse.microprofile.jwt.Claim;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("auth/rbac")
@RequestScoped
@Authenticated
public class RbacResource {

  private static final Logger LOGGER = LoggerFactory.getLogger(RbacResource.class);

  @Inject
  SecurityIdentity identity;

  @Inject
  @Claim("stackgres_k8s_username")
  String k8sUsername;

  @Inject
  NamespaceResource namespaces;

  @Inject
  KubernetesClientProvider kubernetesClientProvider;

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

    String impersonated = k8sUsername != null ? k8sUsername : identity.getPrincipal().getName();
    LOGGER.debug("User to review access {}", impersonated);
    // Connect with the serviceaccount permissions
    try (KubernetesClient client = kubernetesClientProvider.createDefault()) {
      SubjectAccessReview review = new SubjectAccessReviewBuilder()
          .withNewSpec()
          .withUser(impersonated)
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
    String impersonated = k8sUsername != null ? k8sUsername : identity.getPrincipal().getName();
    LOGGER.debug("User to review access {}", impersonated);
    try (KubernetesClient client = kubernetesClientProvider.createDefault()) {
      return Response.ok(new PermissionsListDto(
          buildUnnamespacedPermissionList(client, impersonated),
          buildNamespacedPermissionList(client, impersonated)))
          .build();
    }
  }

  private Map<String, List<String>> buildUnnamespacedPermissionList(
      KubernetesClient client, String user) {
    Map<String, List<String>> resourceUnamespace = new HashMap<>();

    for (String rsUnnamespaced : getResourcesUnnamespaced()) {
      String[] resource = rsUnnamespaced.split("\\.", 2);
      List<String> allowed = accessReview(client, user, null, resource[0],
          resource.length == 2 ? resource[1] : "", getVerbs());
      resourceUnamespace.put(resource[0], allowed);
    }

    return resourceUnamespace;
  }

  private List<PermissionsListDto.Namespaced> buildNamespacedPermissionList(
      KubernetesClient client, String user) {
    List<PermissionsListDto.Namespaced> listNamespaced = new ArrayList<>();

    for (String ns : namespaces.get()) {
      Map<String, List<String>> resourceNamespace = new HashMap<>();
      for (String rsNamespaced : getResourcesNamespaced()) {
        String[] resource = rsNamespaced.split("\\.", 2);
        List<String> allowed = accessReview(client, user, ns, resource[0],
            resource.length == 2 ? resource[1] : "", getVerbs());
        resourceNamespace.put(resource[0], allowed);
      }

      listNamespaced.add(new PermissionsListDto.Namespaced(ns, resourceNamespace));
    }
    return listNamespaced;
  }

  protected List<String> getResourcesUnnamespaced() {
    return List.of(
        "namespaces",
        "storageclasses.storage.k8s.io",
        "clusterroles.rbac.authorization.k8s.io",
        "clusterrolebindings.rbac.authorization.k8s.io");
  }

  protected List<String> getResourcesNamespaced() {
    return List.of(
        "pods",
        "secrets",
        "configmaps",
        "events",
        "pods/exec",
        "roles.rbac.authorization.k8s.io",
        "rolebindings.rbac.authorization.k8s.io",
        HasMetadata.getFullResourceName(StackGresScript.class),
        HasMetadata.getFullResourceName(StackGresObjectStorage.class),
        HasMetadata.getFullResourceName(StackGresBackupConfig.class),
        HasMetadata.getFullResourceName(StackGresBackup.class),
        HasMetadata.getFullResourceName(StackGresCluster.class),
        HasMetadata.getFullResourceName(StackGresDistributedLogs.class),
        HasMetadata.getFullResourceName(StackGresProfile.class),
        HasMetadata.getFullResourceName(StackGresDbOps.class),
        HasMetadata.getFullResourceName(StackGresPostgresConfig.class),
        HasMetadata.getFullResourceName(StackGresPoolingConfig.class),
        HasMetadata.getFullResourceName(StackGresShardedCluster.class));
  }

  private List<String> getVerbs() {
    return List.of("get", "list", "create", "patch", "delete");
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

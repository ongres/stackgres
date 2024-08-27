/*
 * Copyright (C) 2020 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest.auth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Event;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.authorization.v1.SubjectAccessReview;
import io.fabric8.kubernetes.api.model.authorization.v1.SubjectAccessReviewBuilder;
import io.fabric8.kubernetes.api.model.authorization.v1.SubjectAccessReviewStatus;
import io.fabric8.kubernetes.api.model.authorization.v1.SubjectAccessReviewStatusBuilder;
import io.fabric8.kubernetes.api.model.rbac.ClusterRole;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBinding;
import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.api.model.storage.StorageClass;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import io.stackgres.apiweb.app.KubernetesClientProvider;
import io.stackgres.apiweb.dto.PermissionsListDto;
import io.stackgres.apiweb.exception.ErrorResponse;
import io.stackgres.apiweb.rest.misc.NamespaceResource;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.crd.CommonDefinition;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.common.crd.sgstream.StackGresStream;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("auth/rbac")
@RequestScoped
@Authenticated
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
public class RbacResource {

  private static final Logger LOGGER = LoggerFactory.getLogger(RbacResource.class);

  private static final List<String> NAMESPACED_RESOURCES = getResourcesNamespaced();
  private static final List<String> UNNAMESPACED_RESOURCES = getResourcesUnnamespaced();

  private final boolean clusterRoleDisabled = OperatorProperty.CLUSTER_ROLE_DISABLED.getBoolean();

  @Inject
  SecurityIdentity identity;

  @Inject
  @Claim("stackgres_k8s_username")
  String k8sUsername;

  @Inject
  NamespaceResource namespaces;

  @Inject
  KubernetesClientProvider kubernetesClientProvider;

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(implementation = SubjectAccessReviewStatus.class))})
  @Operation(summary = "Can-i <verb> over <resource>", description = """
      Check if an user can do specified `<verb>` on specified `<resource>`.

      ### RBAC permissions required

      None
      """)
  @GET
  @Path("can-i/{verb}/{resource}")
  public Response verb(@PathParam("verb") String verb, @PathParam("resource") String resource,
      @QueryParam("namespace") String namespace, @QueryParam("group") Optional<String> group) {
    String impersonated = k8sUsername != null ? k8sUsername : identity.getPrincipal().getName();
    if (clusterRoleDisabled) {
      LOGGER.debug("ClusterRole are disabled, skipping review access for User {}", impersonated);
      return Response.ok(
          new SubjectAccessReviewStatusBuilder()
          .withAllowed(true)
          .build()).build();
    }
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

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(implementation = PermissionsListDto.class))})
  @Operation(summary = "Can-i list", description = """
      Return a list of namespaced and unnamespaced permissions a user has granted.

      ### RBAC permissions required

      None
      """)
  @GET
  @Path("can-i")
  public PermissionsListDto caniList() {
    String impersonated = k8sUsername != null ? k8sUsername : identity.getPrincipal().getName();
    if (clusterRoleDisabled) {
      LOGGER.debug("ClusterRole are disabled, skipping review access for User {}", impersonated);
    } else {
      LOGGER.debug("User to review access {}", impersonated);
    }
    try (KubernetesClient client = kubernetesClientProvider.createDefault()) {
      return new PermissionsListDto(
          buildUnnamespacedPermissionList(client, impersonated),
          buildNamespacedPermissionList(client, impersonated));
    }
  }

  private Map<String, List<String>> buildUnnamespacedPermissionList(
      KubernetesClient client, String user) {
    Map<String, List<String>> resourceUnamespace = new HashMap<>();

    for (String rsUnnamespaced : UNNAMESPACED_RESOURCES) {
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

    for (String namespace : namespaces.get()) {
      Map<String, List<String>> resourceNamespace = new HashMap<>();
      for (String rsNamespaced : NAMESPACED_RESOURCES) {
        String[] resource = rsNamespaced.split("\\.", 2);
        List<String> allowed = accessReview(client, user, namespace, resource[0],
            resource.length == 2 ? resource[1] : "", getVerbs());
        resourceNamespace.put(resource[0], allowed);
      }

      listNamespaced.add(new PermissionsListDto.Namespaced(namespace, resourceNamespace));
    }
    return listNamespaced;
  }

  public static final List<String> getResourcesUnnamespaced() {
    return List.of(
        HasMetadata.getFullResourceName(Namespace.class),
        HasMetadata.getFullResourceName(StorageClass.class),
        HasMetadata.getFullResourceName(ClusterRole.class),
        HasMetadata.getFullResourceName(ClusterRoleBinding.class));
  }

  public static final List<String> getResourcesNamespaced() {
    return List.of(
        HasMetadata.getFullResourceName(Secret.class),
        HasMetadata.getFullResourceName(ConfigMap.class),
        HasMetadata.getFullResourceName(Event.class),
        HasMetadata.getFullResourceName(Pod.class),
        HasMetadata.getFullResourceName(Pod.class) + "/exec",
        HasMetadata.getFullResourceName(Role.class),
        HasMetadata.getFullResourceName(RoleBinding.class),
        HasMetadata.getFullResourceName(StackGresConfig.class),
        HasMetadata.getFullResourceName(StackGresScript.class),
        HasMetadata.getFullResourceName(StackGresObjectStorage.class),
        HasMetadata.getFullResourceName(StackGresBackup.class),
        HasMetadata.getFullResourceName(StackGresCluster.class),
        HasMetadata.getFullResourceName(StackGresDistributedLogs.class),
        HasMetadata.getFullResourceName(StackGresProfile.class),
        HasMetadata.getFullResourceName(StackGresDbOps.class),
        HasMetadata.getFullResourceName(StackGresPostgresConfig.class),
        HasMetadata.getFullResourceName(StackGresPoolingConfig.class),
        HasMetadata.getFullResourceName(StackGresShardedCluster.class),
        HasMetadata.getFullResourceName(StackGresShardedBackup.class),
        HasMetadata.getFullResourceName(StackGresShardedDbOps.class),
        HasMetadata.getFullResourceName(StackGresStream.class));
  }

  private static List<String> getVerbs() {
    return List.of("get", "list", "create", "patch", "delete");
  }

  private List<String> accessReview(KubernetesClient client, String user, String namespace,
      String resource, String group, List<String> verbs) {
    List<String> allowed = new ArrayList<>();
    for (String verb : verbs) {
      if (clusterRoleDisabled) {
        allowed.add(verb);
        continue;
      }

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

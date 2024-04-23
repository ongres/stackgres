/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest.sgcluster;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.event.EventDto;
import io.stackgres.apiweb.dto.event.ObjectReference;
import io.stackgres.apiweb.exception.ErrorResponse;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.Seq;

@Path("namespaces/{namespace:[a-z0-9]([-a-z0-9]*[a-z0-9])?}/sgclusters")
@RequestScoped
@Authenticated
@Tag(name = "sgcluster")
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
public class NamespacedClusterEventsResource {

  private final ResourceScanner<EventDto> scanner;
  private final CustomResourceScanner<StackGresDbOps> dbOpsScanner;

  @Inject
  public NamespacedClusterEventsResource(ResourceScanner<EventDto> scanner,
      CustomResourceScanner<StackGresDbOps> dbOpsScanner) {
    this.scanner = scanner;
    this.dbOpsScanner = dbOpsScanner;
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(type = SchemaType.ARRAY, implementation = EventDto.class))})
  @Operation(summary = "Get events related to an sgcluster", description = """
      Get events related to an sgcluster including `StatefulSet`, `Pod`s and `SGDbOps`.

      ### RBAC permissions required

      * events list
      * sgdbops list
      """)
  @GET
  @Path("{name}/events")
  public List<EventDto> list(@PathParam("namespace") String namespace,
      @PathParam("name") String name) {
    Map<String, List<ObjectMeta>> relatedResources = new HashMap<>();
    relatedResources.put(StackGresDbOps.KIND,
        Seq.seq(dbOpsScanner.getResources(namespace))
            .filter(dbOps -> Objects.equals(dbOps.getSpec().getSgCluster(), name))
            .map(StackGresDbOps::getMetadata)
            .toList());
    return Seq.seq(scanner.findResourcesInNamespace(namespace))
        .filter(event -> isClusterEvent(event, namespace, name, relatedResources))
        .sorted(this::orderByLastTimestamp)
        .toList();
  }

  private boolean isClusterEvent(EventDto event, String namespace, @NotNull String name,
      Map<String, List<ObjectMeta>> relatedResources) {
    Pattern namePattern = ResourceUtil.getNameWithIndexPattern(name);
    ObjectReference involvedObject = event.getInvolvedObject();
    return Objects.equals(involvedObject.getNamespace(), namespace)
        && ((Objects.equals(involvedObject.getKind(), StackGresCluster.KIND)
            && Objects.equals(involvedObject.getName(), name))
            || (Objects.equals(involvedObject.getKind(), "StatefulSet")
                && Objects.equals(involvedObject.getName(), name))
            || (Objects.equals(involvedObject.getKind(), "Pod")
                && involvedObject.getName() != null
                && namePattern.matcher(involvedObject.getName()).matches())
            || (Optional.ofNullable(relatedResources.get(involvedObject.getKind()))
                .stream().flatMap(Collection::stream)
                .anyMatch(relatedResource -> Objects
                    .equals(relatedResource.getNamespace(), involvedObject.getNamespace())
                    && Objects.equals(relatedResource.getName(), involvedObject.getName())
                    && Objects.equals(relatedResource.getUid(), involvedObject.getUid()))));
  }

  private int orderByLastTimestamp(EventDto e1, EventDto e2) {
    Optional<Instant> lt1 = Optional.ofNullable(e1.getLastTimestamp()).map(Instant::parse);
    Optional<Instant> lt2 = Optional.ofNullable(e2.getLastTimestamp()).map(Instant::parse);
    if (lt1.isPresent() && lt2.isPresent()) {
      return lt1.get().compareTo(lt2.get());
    }
    if (lt1.isPresent()) {
      return 1;
    }
    if (lt2.isPresent()) {
      return -1;
    }
    return 0;
  }

}

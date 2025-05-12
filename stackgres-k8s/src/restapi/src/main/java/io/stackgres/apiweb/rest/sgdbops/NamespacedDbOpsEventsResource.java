/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest.sgdbops;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.event.EventDto;
import io.stackgres.apiweb.dto.event.ObjectReference;
import io.stackgres.apiweb.exception.ErrorResponse;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.resource.ResourceScanner;
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
import org.jooq.lambda.Seq;

@Path("namespaces/{namespace:[a-z0-9]([-a-z0-9]*[a-z0-9])?}/sgdbops")
@RequestScoped
@Authenticated
@Tag(name = "sgdbops")
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
public class NamespacedDbOpsEventsResource {

  private final ResourceScanner<EventDto> scanner;
  private final ResourceScanner<Job> jobScanner;
  private final ResourceScanner<Pod> podScanner;

  @Inject
  public NamespacedDbOpsEventsResource(ResourceScanner<EventDto> scanner,
      ResourceScanner<Job> jobScanner,
      ResourceScanner<Pod> podScanner) {
    this.scanner = scanner;
    this.jobScanner = jobScanner;
    this.podScanner = podScanner;
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
            mediaType = "application/json",
            schema = @Schema(type = SchemaType.ARRAY, implementation = EventDto.class))})
  @Operation(summary = "Get events related to a sgdbops", description = """
      Get events related to a sgdbops including `Pod`s and `Job`s.

      ### RBAC permissions required

      * events list
      """)
  @Path("/{name}/events")
  @GET
  public List<EventDto> list(@PathParam("namespace") String namespace,
      @PathParam("name") String name) {
    Map<String, List<ObjectMeta>> relatedResources = new HashMap<>();
    relatedResources.put("Job",
        Seq.seq(jobScanner.getResourcesInNamespace(namespace))
            .filter(job -> job.getMetadata().getOwnerReferences().stream()
                .anyMatch(resourceReference -> Objects
                    .equals(resourceReference.getKind(), StackGresDbOps.KIND)
                    && Objects.equals(resourceReference.getName(), name)))
            .map(Job::getMetadata)
            .toList());
    relatedResources.put("Pod",
        Seq.seq(podScanner.getResourcesInNamespace(namespace))
            .filter(pod -> pod.getMetadata().getOwnerReferences().stream()
                .anyMatch(resourceReference -> Objects.equals(resourceReference.getKind(), "Job")
                    && relatedResources.get("Job").stream().anyMatch(jobMetadata -> Objects
                        .equals(jobMetadata.getName(), resourceReference.getName()))))
            .map(Pod::getMetadata)
            .toList());
    return Seq.seq(scanner.getResourcesInNamespace(namespace))
        .filter(event -> isDbOpsEvent(event, namespace, name, relatedResources))
        .sorted(this::orderByLastTimestamp)
        .toList();
  }

  private boolean isDbOpsEvent(EventDto event, String namespace, String name,
      Map<String, List<ObjectMeta>> relatedResources) {
    ObjectReference involvedObject = event.getInvolvedObject();
    return Objects.equals(involvedObject.getNamespace(), namespace)
        && ((Objects.equals(involvedObject.getKind(), StackGresDbOps.KIND)
            && Objects.equals(involvedObject.getName(), name))
            || (Optional.ofNullable(relatedResources.get(involvedObject.getKind()))
                .stream().flatMap(List::stream)
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

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

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
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.resource.ResourceScanner;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.jooq.lambda.Seq;

@Path("namespaces/{namespace:[a-z0-9]([-a-z0-9]*[a-z0-9])?}/sgdbops")
@RequestScoped
@Authenticated
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

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK",
              content = { @Content(
                  mediaType = "application/json",
                  array = @ArraySchema(schema = @Schema(implementation = EventDto.class))) })
      })
  @Path("/{name}/events")
  @GET
  public List<EventDto> list(@PathParam("namespace") String namespace,
      @PathParam("name") String name) {
    Map<String, List<ObjectMeta>> relatedResources = new HashMap<>();
    relatedResources.put("Job",
        Seq.seq(jobScanner.findResourcesInNamespace(namespace))
        .filter(job -> job.getMetadata().getOwnerReferences().stream()
            .anyMatch(resourceReference -> Objects
                .equals(resourceReference.getKind(), StackGresDbOps.KIND)
                && Objects.equals(resourceReference.getName(), name)))
        .map(Job::getMetadata)
        .toList());
    relatedResources.put("Pod",
        Seq.seq(podScanner.findResourcesInNamespace(namespace))
        .filter(pod -> pod.getMetadata().getOwnerReferences().stream()
            .anyMatch(resourceReference -> Objects.equals(resourceReference.getKind(), "Job")
                && relatedResources.get("Job").stream().anyMatch(jobMetadata -> Objects
                    .equals(jobMetadata.getName(), resourceReference.getName()))))
        .map(Pod::getMetadata)
        .toList());
    return Seq.seq(scanner.findResourcesInNamespace(namespace))
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

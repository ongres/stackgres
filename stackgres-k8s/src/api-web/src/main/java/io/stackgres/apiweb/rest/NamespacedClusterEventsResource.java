/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.event.EventDto;
import io.stackgres.apiweb.dto.event.ObjectReference;
import io.stackgres.apiweb.rest.utils.CommonApiResponses;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.operatorframework.resource.ResourceUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.Seq;

@Path("namespaces/{namespace:[a-z0-9]([-a-z0-9]*[a-z0-9])?}/sgclusters")
@RequestScoped
@Authenticated
public class NamespacedClusterEventsResource {

  private final ResourceScanner<EventDto> scanner;
  private final CustomResourceScanner<StackGresDbOps> dbOpsScanner;

  @Inject
  public NamespacedClusterEventsResource(ResourceScanner<EventDto> scanner,
      CustomResourceScanner<StackGresDbOps> dbOpsScanner) {
    this.scanner = scanner;
    this.dbOpsScanner = dbOpsScanner;
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK",
              content = {@Content(
                  mediaType = "application/json",
                  array = @ArraySchema(schema = @Schema(implementation = EventDto.class)))})
      })
  @CommonApiResponses
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

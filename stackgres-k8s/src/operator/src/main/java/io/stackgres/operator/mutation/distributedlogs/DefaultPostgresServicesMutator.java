/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.distributedlogs;

import static io.stackgres.common.crd.postgres.service.StackGresPostgresServiceType.CLUSTER_IP;

import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonpatch.JsonPatchOperation;
import com.google.common.collect.ImmutableList;
import io.stackgres.common.crd.postgres.service.StackGresPostgresService;
import io.stackgres.common.crd.postgres.service.StackGresPostgresServiceType;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsPostgresServices;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsSpec;
import io.stackgres.operator.common.StackGresDistributedLogsReview;
import io.stackgres.operatorframework.admissionwebhook.AdmissionRequest;
import io.stackgres.operatorframework.admissionwebhook.Operation;

@ApplicationScoped
public class DefaultPostgresServicesMutator implements DistributedLogsMutator {

  private ObjectMapper jsonMapper;

  private JsonPointer postgresServicesPointer;

  @PostConstruct
  public void init() throws NoSuchFieldException {
    String postgresServicesJson = getJsonMappingField("postgresServices",
        StackGresDistributedLogsSpec.class);

    postgresServicesPointer = SPEC_POINTER
        .append(postgresServicesJson);
  }

  @Override
  public List<JsonPatchOperation> mutate(StackGresDistributedLogsReview review) {
    ImmutableList.Builder<JsonPatchOperation> operations = ImmutableList.builder();
    return Optional.ofNullable(review)
        .map(StackGresDistributedLogsReview::getRequest)
        .map(AdmissionRequest::getOperation)
        .map(operation -> mutatePgServices(operations, review))
        .orElse(operations.build());
  }

  private ImmutableList<JsonPatchOperation> mutatePgServices(
      ImmutableList.Builder<JsonPatchOperation> operations, StackGresDistributedLogsReview review) {

    if (review.getRequest().getOperation() != Operation.CREATE
        && review.getRequest().getOperation() != Operation.UPDATE) {
      return operations.build();
    }

    return Optional.ofNullable(review.getRequest().getObject())
        .map(StackGresDistributedLogs::getSpec)
        .map(spec -> validatePgServices(operations, spec))
        .orElse(operations.build());

  }

  private ImmutableList<JsonPatchOperation> validatePgServices(
      ImmutableList.Builder<JsonPatchOperation> operations, StackGresDistributedLogsSpec spec) {

    return Optional.ofNullable(spec.getPostgresServices())
        .map(pgServices -> {
          mapPgPrimaryService(pgServices);
          mapPgReplicasService(pgServices);
          JsonNode target = jsonMapper.valueToTree(pgServices);
          operations.add(applyReplaceValue(postgresServicesPointer, target));
          return operations.build();
        }).orElseGet(() -> {
          StackGresDistributedLogsPostgresServices pgServices =
              new StackGresDistributedLogsPostgresServices();
          pgServices.setPrimary(createPostgresServicePrimary());
          pgServices.setReplicas(createPostgresServiceReplicas());
          JsonNode target = jsonMapper.valueToTree(pgServices);
          operations.add(applyAddValue(postgresServicesPointer, target));
          return operations.build();
        });
  }

  private void mapPgPrimaryService(StackGresDistributedLogsPostgresServices postgresServices) {
    if (postgresServices.getPrimary() == null) {
      postgresServices.setPrimary(createNewPostgresService(null));
      return;
    }
    postgresServices.getPrimary().setEnabled(null);
    if (postgresServices.getPrimary().getType() == null) {
      postgresServices.getPrimary()
          .setType(StackGresPostgresServiceType.CLUSTER_IP.toString());
    }
  }

  private void mapPgReplicasService(StackGresDistributedLogsPostgresServices postgresServices) {
    if (postgresServices.getReplicas() == null) {
      postgresServices.setReplicas(createPostgresServiceReplicas());
      return;
    }

    if (postgresServices.getReplicas().getEnabled() == null) {
      postgresServices.getReplicas().setEnabled(Boolean.TRUE);
    }
    if (postgresServices.getReplicas().getType() == null) {
      postgresServices.getReplicas()
          .setType(StackGresPostgresServiceType.CLUSTER_IP.toString());
    }
  }

  private StackGresPostgresService createNewPostgresService(Boolean enabled) {
    StackGresPostgresService service = new StackGresPostgresService();
    service.setEnabled(enabled);
    service.setType(CLUSTER_IP.toString());
    return service;
  }

  private StackGresPostgresService createPostgresServiceReplicas() {
    return createNewPostgresService(Boolean.TRUE);
  }

  private StackGresPostgresService createPostgresServicePrimary() {
    return createNewPostgresService(null);
  }

  @Inject
  public void setObjectMapper(ObjectMapper jsonMapper) {
    this.jsonMapper = jsonMapper;
  }
}

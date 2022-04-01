/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import java.util.List;

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
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgresServices;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;

@ApplicationScoped
public class DefaultPostgresServicesMutator implements ClusterMutator {

  private ObjectMapper jsonMapper;

  private JsonPointer postgresServicesPointer;

  @PostConstruct
  public void init() throws NoSuchFieldException {
    String postgresServicesJson = getJsonMappingField("postgresServices",
        StackGresClusterSpec.class);

    postgresServicesPointer = ClusterMutator.SPEC_POINTER
        .append(postgresServicesJson);
  }

  @Override
  public List<JsonPatchOperation> mutate(StackGresClusterReview review) {
    final StackGresClusterPostgresServices postgresServices =
        review.getRequest().getObject().getSpec().getPostgresServices();

    if (review.getRequest().getOperation() == Operation.CREATE
        || review.getRequest().getOperation() == Operation.UPDATE) {

      if (postgresServices != null) {
        if (postgresServices.getPrimary() != null) {
          if (postgresServices.getPrimary().getEnabled() == null) {
            postgresServices.getPrimary().setEnabled(Boolean.TRUE);
          }
          if (postgresServices.getPrimary().getType() == null) {
            postgresServices.getPrimary()
                .setType(StackGresPostgresServiceType.CLUSTER_IP.toString());
          }
        } else {
          postgresServices.setPrimary(createNewPostgresService());
        }

        if (postgresServices.getReplicas() != null) {
          if (postgresServices.getReplicas().getEnabled() == null) {
            postgresServices.getReplicas().setEnabled(Boolean.TRUE);
          }
          if (postgresServices.getReplicas().getType() == null) {
            postgresServices.getReplicas()
                .setType(StackGresPostgresServiceType.CLUSTER_IP.toString());
          }
        } else {
          postgresServices.setReplicas(createNewPostgresService());
        }

        JsonNode target = jsonMapper.valueToTree(postgresServices);
        ImmutableList.Builder<JsonPatchOperation> operations = ImmutableList.builder();
        operations.add(applyReplaceValue(postgresServicesPointer, target));

        return operations.build();
      } else {
        StackGresClusterPostgresServices pgServices = new StackGresClusterPostgresServices();
        pgServices.setPrimary(createNewPostgresService());
        pgServices.setReplicas(createNewPostgresService());

        JsonNode target = jsonMapper.valueToTree(pgServices);
        ImmutableList.Builder<JsonPatchOperation> operations = ImmutableList.builder();
        operations.add(applyAddValue(postgresServicesPointer, target));

        return operations.build();
      }
    }

    return List.of();
  }

  private StackGresPostgresService createNewPostgresService() {
    StackGresPostgresService service = new StackGresPostgresService();
    service.setEnabled(Boolean.TRUE);
    service.setType(StackGresPostgresServiceType.CLUSTER_IP.toString());
    return service;
  }

  @Inject
  public void setObjectMapper(ObjectMapper jsonMapper) {
    this.jsonMapper = jsonMapper;
  }

}

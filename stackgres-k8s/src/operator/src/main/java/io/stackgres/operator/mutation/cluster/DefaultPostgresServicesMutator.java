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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.stackgres.common.crd.postgres.service.StackGresPostgresServiceType;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgresService;
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
  @SuppressFBWarnings(value = "BC_UNCONFIRMED_CAST_OF_RETURN_VALUE",
      justification = "False positive")
  public List<JsonPatchOperation> mutate(StackGresClusterReview review) {
    final StackGresClusterPostgresServices postgresServices =
        review.getRequest().getObject().getSpec().getPostgresServices();

    if (isNotCreationOrUpdate(review)) {
      return List.of();
    }

    if (postgresServices == null) {
      return createNewClusterPostgresServices();
    }

    postgresServices.setPrimary(definePostgresServiceInfoFor(postgresServices.getPrimary()));
    postgresServices.setReplicas(definePostgresServiceInfoFor(postgresServices.getReplicas()));

    JsonNode target = FACTORY.pojoNode(postgresServices);
    ImmutableList.Builder<JsonPatchOperation> operations = ImmutableList.builder();
    operations.add(applyReplaceValue(postgresServicesPointer, target));

    return operations.build();
  }

  private boolean isNotCreationOrUpdate(StackGresClusterReview review) {
    boolean isNotCreationOrUpdate = !(review.getRequest().getOperation() == Operation.CREATE
        || review.getRequest().getOperation() == Operation.UPDATE);
    return isNotCreationOrUpdate;
  }

  private StackGresClusterPostgresService definePostgresServiceInfoFor(
      StackGresClusterPostgresService pgPrimary) {

    if (pgPrimary == null) {
      return createNewPostgresService();
    }

    if (pgPrimary.getEnabled() == null) {
      pgPrimary.setEnabled(Boolean.TRUE);
    }

    if (pgPrimary.getType() == null) {
      pgPrimary.setType(StackGresPostgresServiceType.CLUSTER_IP.toString());
    }

    return pgPrimary;
  }

  private List<JsonPatchOperation> createNewClusterPostgresServices() {
    StackGresClusterPostgresServices pgServices = new StackGresClusterPostgresServices();
    pgServices.setPrimary(createNewPostgresService());
    pgServices.setReplicas(createNewPostgresService());

    JsonNode target = jsonMapper.valueToTree(pgServices);
    ImmutableList.Builder<JsonPatchOperation> operations = ImmutableList.builder();
    operations.add(applyAddValue(postgresServicesPointer, target));

    return operations.build();
  }

  private StackGresClusterPostgresService createNewPostgresService() {
    StackGresClusterPostgresService service = new StackGresClusterPostgresService();
    service.setEnabled(Boolean.TRUE);
    service.setType(StackGresPostgresServiceType.CLUSTER_IP.toString());
    return service;
  }

  @Inject
  public void setObjectMapper(ObjectMapper jsonMapper) {
    this.jsonMapper = jsonMapper;
  }

}

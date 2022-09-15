/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.distributedlogs;

import static io.stackgres.common.crd.postgres.service.StackGresPostgresServiceType.CLUSTER_IP;
import static io.stackgres.common.crd.postgres.service.StackGresPostgresServiceType.LOAD_BALANCER;
import static io.stackgres.common.crd.postgres.service.StackGresPostgresServiceType.NODE_PORT;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.github.fge.jsonpatch.JsonPatchOperation;
import io.stackgres.common.crd.postgres.service.StackGresPostgresService;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsPostgresServices;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsPostgresServicesBuilder;
import io.stackgres.operator.common.StackGresDistributedLogsReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

class DefaultPostgresServicesMutatorTest {

  protected static final JsonMapper JSON_MAPPER = JsonUtil.jsonMapper();

  protected static final JavaPropsMapper PROPS_MAPPER = new JavaPropsMapper();

  private StackGresDistributedLogsReview review;

  private DefaultPostgresServicesMutator mutator;

  private StackGresDistributedLogsPostgresServices sgDistributedLogs;

  @BeforeEach
  void setUp() throws NoSuchFieldException, IOException {
    review = AdmissionReviewFixtures.distributedLogs().loadCreate().get();

    mutator = new DefaultPostgresServicesMutator();
    mutator.setObjectMapper(JSON_MAPPER);
    mutator.init();
  }

  @Test
  void clusterWithPostgresService_shouldSetNothing() {

    setupDistributedLogsPgServices(
        withPgPrimaryService(TRUE, NODE_PORT.toString()),
        withPgReplicasService(TRUE, NODE_PORT.toString()));

    StackGresDistributedLogsPostgresServices pgServicesMuted = mutate(review);

    assertNull(pgServicesMuted.getPrimary().getEnabled());
    assertEquals(sgDistributedLogs.getPrimary().getType(),
        pgServicesMuted.getPrimary().getType());
    assertEquals(sgDistributedLogs.getReplicas().getEnabled(),
        pgServicesMuted.getReplicas().getEnabled());
    assertEquals(sgDistributedLogs.getReplicas().getType(),
        pgServicesMuted.getReplicas().getType());
  }

  @Test
  void clusterWithPostgresServiceNoType_shouldSetClusterIP() {
    setupDistributedLogsPgServices(
        withPgPrimaryService(TRUE, null),
        withPgReplicasService(FALSE, null));

    StackGresDistributedLogsPostgresServices pgServicesMuted = mutate(review);

    assertNull(pgServicesMuted.getPrimary().getEnabled());
    assertEquals(CLUSTER_IP.toString(), pgServicesMuted.getPrimary().getType());
    assertEquals(Boolean.FALSE, pgServicesMuted.getReplicas().getEnabled());
    assertEquals(CLUSTER_IP.toString(), pgServicesMuted.getReplicas().getType());
  }

  @Test
  void clusterWithNoPostgresService_shouldBeSetup() {
    review.getRequest().getObject().getSpec().setPostgresServices(null);

    StackGresDistributedLogsPostgresServices pgServicesMuted = mutate(review);

    assertNull(pgServicesMuted.getPrimary().getEnabled());
    assertEquals(CLUSTER_IP.toString(), pgServicesMuted.getPrimary().getType());
    assertEquals(Boolean.TRUE, pgServicesMuted.getReplicas().getEnabled());
    assertEquals(CLUSTER_IP.toString(), pgServicesMuted.getReplicas().getType());
  }

  @Test
  void clusterWithNoPgServicePrimary_shouldbeSetup() {
    setupDistributedLogsPgServices(
        withNoPgPrimaryService(),
        withPgReplicasService(FALSE, LOAD_BALANCER.toString()));

    StackGresDistributedLogsPostgresServices pgServicesMuted = mutate(review);

    assertNull(pgServicesMuted.getPrimary().getEnabled());
    assertEquals(CLUSTER_IP.toString(), pgServicesMuted.getPrimary().getType());
    assertEquals(Boolean.FALSE, pgServicesMuted.getReplicas().getEnabled());
    assertEquals(LOAD_BALANCER.toString(), pgServicesMuted.getReplicas().getType());
  }

  @Test
  void clusterWithNoPgServiceReplicas_shouldSetValue() {
    setupDistributedLogsPgServices(
        withPgPrimaryService(FALSE, LOAD_BALANCER.toString()),
        withNoPgReplicasService());

    StackGresDistributedLogsPostgresServices pgServicesMuted = mutate(review);

    assertNull(pgServicesMuted.getPrimary().getEnabled());
    assertEquals(LOAD_BALANCER.toString(), pgServicesMuted.getPrimary().getType());
    assertEquals(Boolean.TRUE, pgServicesMuted.getReplicas().getEnabled());
    assertEquals(CLUSTER_IP.toString(), pgServicesMuted.getReplicas().getType());
  }

  @Test
  void clusterWithPostgresServiceNoEnabled_shouldSetValue() {
    setupDistributedLogsPgServices(
        withPgPrimaryService(null, LOAD_BALANCER.toString()),
        withNoPgReplicasService());

    StackGresDistributedLogsPostgresServices pgServicesMuted = mutate(review);

    assertNull(pgServicesMuted.getPrimary().getEnabled());
    assertEquals(LOAD_BALANCER.toString(), pgServicesMuted.getPrimary().getType());
    assertEquals(Boolean.TRUE, pgServicesMuted.getReplicas().getEnabled());
    assertEquals(CLUSTER_IP.toString(), pgServicesMuted.getReplicas().getType());
  }

  @Test
  void clusterWithPgServiceReplicaNoEnabled_shouldBeSetup() {
    setupDistributedLogsPgServices(
        withPgPrimaryService(TRUE, LOAD_BALANCER.toString()),
        withPgReplicasService(null, LOAD_BALANCER.toString()));

    StackGresDistributedLogsPostgresServices pgServicesMuted = mutate(review);

    assertNull(pgServicesMuted.getPrimary().getEnabled());
    assertEquals(LOAD_BALANCER.toString(), pgServicesMuted.getPrimary().getType());
    assertEquals(Boolean.TRUE, pgServicesMuted.getReplicas().getEnabled());
    assertEquals(LOAD_BALANCER.toString(), pgServicesMuted.getReplicas().getType());
  }

  private StackGresPostgresService withPgPrimaryService(Boolean enabled, String serviceType) {
    return withPgPrimaryService(enabled, serviceType, Arrays.asList());
  }

  private StackGresPostgresService withPgPrimaryService(Boolean enabled, String serviceType,
      List<String> externalIPs) {
    return new StackGresPostgresService(enabled, serviceType, externalIPs, EMPTY);
  }

  private StackGresPostgresService withPgReplicasService(Boolean enabled, String serviceType) {
    return new StackGresPostgresService(enabled, serviceType, asList(), EMPTY);
  }

  private void setupDistributedLogsPgServices(StackGresPostgresService primary,
      StackGresPostgresService replicas) {
    this.sgDistributedLogs = new StackGresDistributedLogsPostgresServicesBuilder()
        .withPrimary(primary)
        .withReplicas(replicas)
        .build();
    review.getRequest().getObject().getSpec().setPostgresServices(sgDistributedLogs);
  }

  private StackGresPostgresService withNoPgPrimaryService() {
    return null;
  }

  private StackGresPostgresService withNoPgReplicasService() {
    return null;
  }

  private StackGresDistributedLogsPostgresServices mutate(StackGresDistributedLogsReview review) {
    try {
      List<JsonPatchOperation> operations = mutator.mutate(review);
      JsonNode crJson = JSON_MAPPER.valueToTree(review.getRequest().getObject());
      JsonNode newConfig = new JsonPatch(operations).apply(crJson);
      return JSON_MAPPER.treeToValue(newConfig, StackGresDistributedLogs.class).getSpec()
          .getPostgresServices();
    } catch (JsonPatchException | JsonProcessingException | IllegalArgumentException e) {
      throw new AssertionFailedError(e.getMessage(), e);
    }
  }

}

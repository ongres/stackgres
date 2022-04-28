/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni.v11;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.ClusterLabelFactory;
import io.stackgres.common.ClusterLabelMapper;
import io.stackgres.common.ClusterStatefulSetEnvVars;
import io.stackgres.common.LabelFactoryForCluster;
import io.stackgres.common.StringUtil;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigStatus;
import io.stackgres.common.patroni.PatroniConfig;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.cluster.patroni.AbstractPatroniConfigEndpoints;
import io.stackgres.operator.conciliation.factory.cluster.patroni.parameters.PostgresBlocklist;
import io.stackgres.operator.conciliation.factory.cluster.patroni.parameters.PostgresDefaultValues;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PatroniConfigEndpointsTest {

  private static final ObjectMapper MAPPER = JsonUtil.JSON_MAPPER;
  private final LabelFactoryForCluster<StackGresCluster> labelFactory = new ClusterLabelFactory(
      new ClusterLabelMapper());
  @Mock
  private StackGresClusterContext context;
  private PatroniConfigEndpoints generator;
  private StackGresCluster cluster;
  private StackGresBackupConfig backupConfig;
  private StackGresPostgresConfig postgresConfig;

  @BeforeEach
  void setUp() {
    generator = new PatroniConfigEndpoints(MAPPER, labelFactory);

    cluster = JsonUtil
        .readFromJson("stackgres_cluster/default.json", StackGresCluster.class);
    cluster.getSpec().setDistributedLogs(null);
    backupConfig = JsonUtil.readFromJson("backup_config/default.json", StackGresBackupConfig.class);
    postgresConfig = JsonUtil.readFromJson("postgres_config/default_postgres.json",
        StackGresPostgresConfig.class);
    postgresConfig.setStatus(new StackGresPostgresConfigStatus());
    setDefaultParameters(postgresConfig);
  }

  private void setDefaultParameters(StackGresPostgresConfig postgresConfig) {
    final String version = postgresConfig.getSpec().getPostgresVersion();
    postgresConfig.getStatus()
        .setDefaultParameters(PostgresDefaultValues.getDefaultValues(version));
  }

  @Test
  void getPostgresConfigValues_shouldConfigureBackupParametersIfArePresent() {
    when(context.getBackupConfig()).thenReturn(Optional.of(backupConfig));
    when(context.getPostgresConfig()).thenReturn(postgresConfig);
    when(context.getSource()).thenReturn(cluster);
    when(context.getCluster()).thenReturn(cluster);

    Map<String, String> pgParams = generator.getPostgresConfigValues(context);

    assertTrue(pgParams.containsKey("archive_command"));
    final String expected = "exec-with-env '" + ClusterStatefulSetEnvVars.BACKUP_ENV.value(cluster)
        + "' -- wal-g wal-push %p";
    assertEquals(expected, pgParams.get("archive_command"));
  }

  @Test
  void getPostgresRecoveryConfigValues_shouldConfigureBackupParametersIfArePresent() {
    when(context.getBackupConfig()).thenReturn(Optional.of(backupConfig));
    when(context.getPostgresConfig()).thenReturn(postgresConfig);
    when(context.getSource()).thenReturn(cluster);

    Map<String, String> pgRecoveryParams = generator.getPostgresRecoveryConfigValues(context);

    assertTrue(pgRecoveryParams.containsKey("restore_command"));
    final String expected = "exec-with-env '" + ClusterStatefulSetEnvVars.BACKUP_ENV.value(cluster)
        + "' -- wal-g wal-fetch %f %p";
    assertEquals(expected, pgRecoveryParams.get("restore_command"));
  }

  @Test
  void getPostgresConfigValues_shouldNotConfigureBackupParametersIfAreNotPresent() {
    when(context.getBackupConfig()).thenReturn(Optional.empty());
    when(context.getPostgresConfig()).thenReturn(postgresConfig);
    when(context.getSource()).thenReturn(cluster);
    when(context.getCluster()).thenReturn(cluster);

    Map<String, String> pgParams = generator.getPostgresConfigValues(context);

    assertTrue(pgParams.containsKey("archive_command"));
    assertEquals("/bin/true", pgParams.get("archive_command"));
  }

  @Test
  void getPostgresConfigValues_shouldConfigurePgParameters() {
    when(context.getBackupConfig()).thenReturn(Optional.of(backupConfig));
    when(context.getPostgresConfig()).thenReturn(postgresConfig);
    when(context.getSource()).thenReturn(cluster);
    when(context.getCluster()).thenReturn(cluster);

    Map<String, String> pgParams = generator.getPostgresConfigValues(context);

    postgresConfig.getSpec().getPostgresqlConf().forEach((key, value) -> {
      assertTrue(pgParams.containsKey(key));
      assertEquals(value, pgParams.get(key));
    });
  }

  @Test
  void getPostgresConfigValues_shouldNotModifyBlockedValuesIfArePresent() {
    when(context.getBackupConfig()).thenReturn(Optional.empty());
    when(context.getPostgresConfig()).thenReturn(postgresConfig);
    when(context.getSource()).thenReturn(cluster);
    when(context.getCluster()).thenReturn(cluster);

    final String version = postgresConfig.getSpec().getPostgresVersion();
    Map<String, String> defValues = PostgresDefaultValues.getDefaultValues(version);

    defValues.forEach((key, value) -> {
      postgresConfig.getSpec().getPostgresqlConf().put(key, StringUtil.generateRandom());
    });

    Map<String, String> pgParams = generator.getPostgresConfigValues(context);

    Set<String> blocklistedKeys = PostgresBlocklist.getBlocklistParameters();
    defValues.forEach((key, value) -> {
      assertTrue(pgParams.containsKey(key));
      if (blocklistedKeys.contains(key)) {
        assertEquals(value, pgParams.get(key), "Blocklisted parameter " + key + " with value "
            + value + " has been modified with value " + pgParams.get(key));
      }
    });
  }

  @Test
  void generateResource_shouldSetLabelsFromLabelFactory() {
    Endpoints endpoints = generateEndpoint();
    assertEquals(labelFactory.patroniClusterLabels(cluster), endpoints.getMetadata().getLabels());
  }

  @Test
  void generatedEndpoint_shouldBeAnnotatedWithPatroniKeyAndAValidPostgresConfig()
      throws JsonProcessingException {
    Endpoints endpoints = generateEndpoint();

    final Map<String, String> annotations = endpoints.getMetadata().getAnnotations();
    assertTrue(annotations.containsKey(AbstractPatroniConfigEndpoints.PATRONI_CONFIG_KEY));

    PatroniConfig patroniConfig = MAPPER
        .readValue(annotations.get(AbstractPatroniConfigEndpoints.PATRONI_CONFIG_KEY),
            PatroniConfig.class);
    final String version = postgresConfig.getSpec().getPostgresVersion();
    PostgresDefaultValues.getDefaultValues(version).forEach(
        (key, value) -> assertTrue(patroniConfig.getPostgresql().getParameters().containsKey(key)));
    assertEquals(30, patroniConfig.getTtl());
    assertEquals(10, patroniConfig.getLoopWait());
    assertEquals(10, patroniConfig.getRetryTimeout());
    assertTrue(patroniConfig.getPostgresql().getUsePgRewind());
    assertNull(patroniConfig.getPostgresql().getUseSlots());
  }

  private Endpoints generateEndpoint() {
    when(context.getSource()).thenReturn(cluster);
    when(context.getCluster()).thenReturn(cluster);
    when(context.getBackupConfig()).thenReturn(Optional.of(backupConfig));
    when(context.getPostgresConfig()).thenReturn(postgresConfig);

    List<HasMetadata> endpoints = generator.generateResource(context)
        .collect(Collectors.toUnmodifiableList());

    assertFalse(endpoints.isEmpty());

    final Endpoints endpoint = (Endpoints) endpoints.get(0);
    assertNotNull(endpoint.getMetadata());
    assertNotNull(endpoint.getMetadata().getLabels());
    assertNotNull(endpoint.getMetadata().getAnnotations());
    return endpoint;
  }

  @Test
  void generatedConfig_shouldNotChangeTooMuchFromPreviousVersion()
      throws JsonProcessingException {
    var cluster = JsonUtil
          .readFromJson("upgrade/sgcluster.json", StackGresCluster.class);
    var postgresConfig = JsonUtil.readFromJson("upgrade/sgpgconfig.json",
        StackGresPostgresConfig.class);
    var backupConfig = JsonUtil.readFromJson("upgrade/sgbackupconfig.json",
        StackGresBackupConfig.class);
    when(context.getSource()).thenReturn(cluster);
    when(context.getCluster()).thenReturn(cluster);
    when(context.getPostgresConfig()).thenReturn(postgresConfig);
    when(context.getBackupConfig()).thenReturn(Optional.of(backupConfig));

    var patroniConfig = generator.getPatroniConfig(context);

    JsonUtil.assertJsonEquals(
        JsonUtil.readFromJsonAsJson("upgrade/v1.1/patroni.json"),
        MAPPER.valueToTree(patroniConfig));
  }

}

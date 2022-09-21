/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import static io.stackgres.common.PatroniUtil.REPLICATION_SERVICE_PORT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.ClusterLabelFactory;
import io.stackgres.common.ClusterLabelMapper;
import io.stackgres.common.ClusterStatefulSetEnvVars;
import io.stackgres.common.LabelFactoryForCluster;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.StringUtil;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFrom;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFromExternal;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFromInstance;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFromStorage;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigStatus;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.patroni.PatroniConfig;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
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

  private static final JsonMapper JSON_MAPPER = JsonUtil.jsonMapper();
  private final LabelFactoryForCluster<StackGresCluster> labelFactory = new ClusterLabelFactory(
      new ClusterLabelMapper());
  @Mock
  private StackGresClusterContext context;
  private AbstractPatroniConfigEndpoints generator;
  private StackGresCluster cluster;
  private StackGresBackupConfig backupConfig;
  private StackGresPostgresConfig postgresConfig;

  @BeforeEach
  void setUp() {
    generator = new PatroniConfigEndpoints(JSON_MAPPER, labelFactory);

    cluster = Fixtures.cluster().loadDefault().get();
    cluster.getMetadata().getAnnotations()
        .put(StackGresContext.VERSION_KEY, StackGresVersion.LATEST.getVersion());
    cluster.getSpec().setDistributedLogs(null);
    backupConfig = Fixtures.backupConfig().loadDefault().get();
    postgresConfig = Fixtures.postgresConfig().loadDefault().get();
    postgresConfig.setStatus(new StackGresPostgresConfigStatus());
    setDefaultParameters(postgresConfig);

    lenient().when(context.getBackupConfig()).thenReturn(Optional.of(backupConfig));
    lenient().when(context.getPostgresConfig()).thenReturn(postgresConfig);
  }

  private void setDefaultParameters(StackGresPostgresConfig postgresConfig) {
    final String version = postgresConfig.getSpec().getPostgresVersion();
    postgresConfig.getStatus()
        .setDefaultParameters(PostgresDefaultValues.getDefaultValues(version));
  }

  @Test
  void getPostgresConfigValues_shouldConfigureBackupParametersIfArePresent() {
    when(context.getBackupConfig()).thenReturn(Optional.of(backupConfig));
    when(context.getBackupStorage()).thenCallRealMethod();
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
    when(context.getBackupStorage()).thenCallRealMethod();
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
    when(context.getBackupStorage()).thenReturn(Optional.empty());
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
    when(context.getBackupStorage()).thenCallRealMethod();
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
    when(context.getBackupStorage()).thenReturn(Optional.empty());
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

    PatroniConfig patroniConfig = JSON_MAPPER
        .readValue(annotations.get(AbstractPatroniConfigEndpoints.PATRONI_CONFIG_KEY),
            PatroniConfig.class);
    final String version = postgresConfig.getSpec().getPostgresVersion();
    PostgresDefaultValues.getDefaultValues(version).forEach(
        (key, value) -> assertTrue(patroniConfig.getPostgresql().getParameters().containsKey(key),
            "Patroni config for postgres does not contain parameter " + key));
    assertEquals(30, patroniConfig.getTtl());
    assertEquals(10, patroniConfig.getLoopWait());
    assertEquals(10, patroniConfig.getRetryTimeout());
    assertTrue(patroniConfig.getPostgresql().getUsePgRewind());
    assertTrue(patroniConfig.getPostgresql().getUseSlots());
    assertNull(patroniConfig.getStandbyCluster());
  }

  @Test
  void generatedEndpointWithReplicateFromExternal_shouldBeConfiguredAccordingly()
      throws JsonProcessingException {
    cluster.getSpec().setReplicateFrom(new StackGresClusterReplicateFrom());
    cluster.getSpec().getReplicateFrom().setInstance(new StackGresClusterReplicateFromInstance());
    cluster.getSpec().getReplicateFrom().getInstance()
        .setExternal(new StackGresClusterReplicateFromExternal());
    cluster.getSpec().getReplicateFrom().getInstance().getExternal()
        .setHost("test");
    cluster.getSpec().getReplicateFrom().getInstance().getExternal()
        .setPort(5433);
    Endpoints endpoints = generateEndpoint();

    final Map<String, String> annotations = endpoints.getMetadata().getAnnotations();
    assertTrue(annotations.containsKey(AbstractPatroniConfigEndpoints.PATRONI_CONFIG_KEY));

    PatroniConfig patroniConfig = JSON_MAPPER
        .readValue(annotations.get(AbstractPatroniConfigEndpoints.PATRONI_CONFIG_KEY),
            PatroniConfig.class);
    final String version = postgresConfig.getSpec().getPostgresVersion();
    PostgresDefaultValues.getDefaultValues(version).forEach(
        (key, value) -> assertTrue(patroniConfig.getPostgresql().getParameters().containsKey(key),
            "Patroni config for postgres does not contain parameter " + key));
    assertEquals(30, patroniConfig.getTtl());
    assertEquals(10, patroniConfig.getLoopWait());
    assertEquals(10, patroniConfig.getRetryTimeout());
    assertTrue(patroniConfig.getPostgresql().getUsePgRewind());
    assertTrue(patroniConfig.getPostgresql().getUseSlots());
    assertNotNull(patroniConfig.getStandbyCluster());
    assertEquals("test", patroniConfig.getStandbyCluster().getHost());
    assertEquals("5433", patroniConfig.getStandbyCluster().getPort());
    assertNull(patroniConfig.getStandbyCluster().getCreateReplicaMethods());
    assertNull(patroniConfig.getStandbyCluster().getArchiveCleanupCommand());
    assertNull(patroniConfig.getStandbyCluster().getPrimarySlotName());
    assertNull(patroniConfig.getStandbyCluster().getRecoveryMinApplyDelay());
    assertNull(patroniConfig.getStandbyCluster().getRestoreCommand());
  }

  @Test
  void generatedEndpointWithReplicateFromSgCluster_shouldBeConfiguredAccordingly()
      throws JsonProcessingException {
    cluster.getSpec().setReplicateFrom(new StackGresClusterReplicateFrom());
    cluster.getSpec().getReplicateFrom().setInstance(new StackGresClusterReplicateFromInstance());
    cluster.getSpec().getReplicateFrom().getInstance()
        .setSgCluster("test");
    Endpoints endpoints = generateEndpoint();

    final Map<String, String> annotations = endpoints.getMetadata().getAnnotations();
    assertTrue(annotations.containsKey(AbstractPatroniConfigEndpoints.PATRONI_CONFIG_KEY));

    PatroniConfig patroniConfig = JSON_MAPPER
        .readValue(annotations.get(AbstractPatroniConfigEndpoints.PATRONI_CONFIG_KEY),
            PatroniConfig.class);
    final String version = postgresConfig.getSpec().getPostgresVersion();
    PostgresDefaultValues.getDefaultValues(version).forEach(
        (key, value) -> assertTrue(patroniConfig.getPostgresql().getParameters().containsKey(key),
            "Patroni config for postgres does not contain parameter " + key));
    assertEquals(30, patroniConfig.getTtl());
    assertEquals(10, patroniConfig.getLoopWait());
    assertEquals(10, patroniConfig.getRetryTimeout());
    assertTrue(patroniConfig.getPostgresql().getUsePgRewind());
    assertTrue(patroniConfig.getPostgresql().getUseSlots());
    assertNotNull(patroniConfig.getStandbyCluster());
    assertEquals("test", patroniConfig.getStandbyCluster().getHost());
    assertEquals(String.valueOf(PatroniUtil.REPLICATION_SERVICE_PORT),
        patroniConfig.getStandbyCluster().getPort());
    assertEquals(List.of("replicate", "basebackup"),
        patroniConfig.getStandbyCluster().getCreateReplicaMethods());
    assertNull(patroniConfig.getStandbyCluster().getArchiveCleanupCommand());
    assertNull(patroniConfig.getStandbyCluster().getPrimarySlotName());
    assertNull(patroniConfig.getStandbyCluster().getRecoveryMinApplyDelay());
    assertEquals("exec-with-env 'replicate' -- wal-g wal-fetch %f %p",
        patroniConfig.getStandbyCluster().getRestoreCommand());
  }

  @Test
  void generatedEndpointWithReplicateFromStorage_shouldBeConfiguredAccordingly()
      throws JsonProcessingException {
    cluster.getSpec().setReplicateFrom(new StackGresClusterReplicateFrom());
    cluster.getSpec().getReplicateFrom().setStorage(new StackGresClusterReplicateFromStorage());
    cluster.getSpec().getReplicateFrom().getStorage()
        .setSgObjectStorage("test");
    cluster.getSpec().getReplicateFrom().getStorage()
        .setPath("test");
    Endpoints endpoints = generateEndpoint();

    final Map<String, String> annotations = endpoints.getMetadata().getAnnotations();
    assertTrue(annotations.containsKey(AbstractPatroniConfigEndpoints.PATRONI_CONFIG_KEY));

    PatroniConfig patroniConfig = JSON_MAPPER
        .readValue(annotations.get(AbstractPatroniConfigEndpoints.PATRONI_CONFIG_KEY),
            PatroniConfig.class);
    final String version = postgresConfig.getSpec().getPostgresVersion();
    PostgresDefaultValues.getDefaultValues(version).forEach(
        (key, value) -> assertTrue(patroniConfig.getPostgresql().getParameters().containsKey(key),
            "Patroni config for postgres does not contain parameter " + key));
    assertEquals(30, patroniConfig.getTtl());
    assertEquals(10, patroniConfig.getLoopWait());
    assertEquals(10, patroniConfig.getRetryTimeout());
    assertTrue(patroniConfig.getPostgresql().getUsePgRewind());
    assertTrue(patroniConfig.getPostgresql().getUseSlots());
    assertNotNull(patroniConfig.getStandbyCluster());
    assertEquals(PatroniServices.readWriteName(context),
        patroniConfig.getStandbyCluster().getHost());
    assertEquals(String.valueOf(String.valueOf(REPLICATION_SERVICE_PORT)),
        patroniConfig.getStandbyCluster().getPort());
    assertEquals(List.of("replicate"), patroniConfig.getStandbyCluster().getCreateReplicaMethods());
    assertNull(patroniConfig.getStandbyCluster().getArchiveCleanupCommand());
    assertNull(patroniConfig.getStandbyCluster().getPrimarySlotName());
    assertNull(patroniConfig.getStandbyCluster().getRecoveryMinApplyDelay());
    assertEquals("exec-with-env 'replicate' -- wal-g wal-fetch %f %p",
        patroniConfig.getStandbyCluster().getRestoreCommand());
  }

  @Test
  void generatedEndpointWithReplicateFromStorageAfterBootstrap_shouldBeConfiguredAccordingly()
      throws JsonProcessingException {
    cluster.getSpec().setReplicateFrom(new StackGresClusterReplicateFrom());
    cluster.getSpec().getReplicateFrom().setStorage(new StackGresClusterReplicateFromStorage());
    cluster.getSpec().getReplicateFrom().getStorage()
        .setSgObjectStorage("test");
    cluster.getSpec().getReplicateFrom().getStorage()
        .setPath("test");
    cluster.setStatus(new StackGresClusterStatus());
    cluster.getStatus().setOs("linux");
    cluster.getStatus().setArch("x86_64");
    Endpoints endpoints = generateEndpoint();

    final Map<String, String> annotations = endpoints.getMetadata().getAnnotations();
    assertTrue(annotations.containsKey(AbstractPatroniConfigEndpoints.PATRONI_CONFIG_KEY));

    PatroniConfig patroniConfig = JSON_MAPPER
        .readValue(annotations.get(AbstractPatroniConfigEndpoints.PATRONI_CONFIG_KEY),
            PatroniConfig.class);
    final String version = postgresConfig.getSpec().getPostgresVersion();
    PostgresDefaultValues.getDefaultValues(version).forEach(
        (key, value) -> assertTrue(patroniConfig.getPostgresql().getParameters().containsKey(key),
            "Patroni config for postgres does not contain parameter " + key));
    assertEquals(30, patroniConfig.getTtl());
    assertEquals(10, patroniConfig.getLoopWait());
    assertEquals(10, patroniConfig.getRetryTimeout());
    assertTrue(patroniConfig.getPostgresql().getUsePgRewind());
    assertTrue(patroniConfig.getPostgresql().getUseSlots());
    assertNotNull(patroniConfig.getStandbyCluster());
    assertNull(patroniConfig.getStandbyCluster().getHost());
    assertNull(patroniConfig.getStandbyCluster().getPort());
    assertEquals(List.of("replicate"), patroniConfig.getStandbyCluster().getCreateReplicaMethods());
    assertNull(patroniConfig.getStandbyCluster().getArchiveCleanupCommand());
    assertNull(patroniConfig.getStandbyCluster().getPrimarySlotName());
    assertNull(patroniConfig.getStandbyCluster().getRecoveryMinApplyDelay());
    assertEquals("exec-with-env 'replicate' -- wal-g wal-fetch %f %p",
        patroniConfig.getStandbyCluster().getRestoreCommand());
  }

  @Test
  void generatedEndpointWithReplicateFromExternalAndStorage_shouldBeConfiguredAccordingly()
      throws JsonProcessingException {
    cluster.getSpec().setReplicateFrom(new StackGresClusterReplicateFrom());
    cluster.getSpec().getReplicateFrom().setInstance(new StackGresClusterReplicateFromInstance());
    cluster.getSpec().getReplicateFrom().getInstance()
        .setExternal(new StackGresClusterReplicateFromExternal());
    cluster.getSpec().getReplicateFrom().getInstance().getExternal()
        .setHost("test");
    cluster.getSpec().getReplicateFrom().getInstance().getExternal()
        .setPort(5433);
    cluster.getSpec().getReplicateFrom().setStorage(new StackGresClusterReplicateFromStorage());
    cluster.getSpec().getReplicateFrom().getStorage()
        .setSgObjectStorage("test");
    cluster.getSpec().getReplicateFrom().getStorage()
        .setPath("test");
    Endpoints endpoints = generateEndpoint();

    final Map<String, String> annotations = endpoints.getMetadata().getAnnotations();
    assertTrue(annotations.containsKey(AbstractPatroniConfigEndpoints.PATRONI_CONFIG_KEY));

    PatroniConfig patroniConfig = JSON_MAPPER
        .readValue(annotations.get(AbstractPatroniConfigEndpoints.PATRONI_CONFIG_KEY),
            PatroniConfig.class);
    final String version = postgresConfig.getSpec().getPostgresVersion();
    PostgresDefaultValues.getDefaultValues(version).forEach(
        (key, value) -> assertTrue(patroniConfig.getPostgresql().getParameters().containsKey(key),
            "Patroni config for postgres does not contain parameter " + key));
    assertEquals(30, patroniConfig.getTtl());
    assertEquals(10, patroniConfig.getLoopWait());
    assertEquals(10, patroniConfig.getRetryTimeout());
    assertTrue(patroniConfig.getPostgresql().getUsePgRewind());
    assertTrue(patroniConfig.getPostgresql().getUseSlots());
    assertNotNull(patroniConfig.getStandbyCluster());
    assertEquals("test", patroniConfig.getStandbyCluster().getHost());
    assertEquals("5433", patroniConfig.getStandbyCluster().getPort());
    assertEquals(List.of("replicate", "basebackup"),
        patroniConfig.getStandbyCluster().getCreateReplicaMethods());
    assertNull(patroniConfig.getStandbyCluster().getArchiveCleanupCommand());
    assertNull(patroniConfig.getStandbyCluster().getPrimarySlotName());
    assertNull(patroniConfig.getStandbyCluster().getRecoveryMinApplyDelay());
    assertEquals("exec-with-env 'replicate' -- wal-g wal-fetch %f %p",
        patroniConfig.getStandbyCluster().getRestoreCommand());
  }

  private Endpoints generateEndpoint() {
    when(context.getSource()).thenReturn(cluster);
    when(context.getCluster()).thenReturn(cluster);
    when(context.getBackupConfig()).thenReturn(Optional.of(backupConfig));
    when(context.getBackupStorage()).thenCallRealMethod();
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

}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs.patroni;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.YamlMapperProvider;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.labels.LabelMapperForCluster;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import io.stackgres.testutil.JsonUtil;
import org.jboss.logmanager.Level;
import org.jboss.logmanager.LogContext;
import org.jboss.logmanager.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PatroniConfigMapTest {
  @Mock
  private LabelFactoryForCluster<StackGresDistributedLogs> labelFactoryForCluster;

  @Mock
  private LabelMapperForCluster<StackGresDistributedLogs> labelMapperForCluster;

  private ObjectMapper objectMapper = JsonUtil.jsonMapper();

  private YamlMapperProvider yamlMapperProvider = new YamlMapperProvider();

  @Mock
  private StackGresDistributedLogsContext context;

  private StackGresDistributedLogs stackGresDistributedLogs;

  private PatroniConfigMap patroniConfigMap;

  private List<String> expectedEnvVarsConfigMap = List.of("PATRONI_CONFIG_FILE",
      "PATRONI_INITIAL_CONFIG", "PATRONI_POSTGRESQL_LISTEN",
      "PATRONI_POSTGRESQL_CONNECT_ADDRESS", "PATRONI_RESTAPI_LISTEN",
      "PATRONI_POSTGRESQL_DATA_DIR", "PATRONI_POSTGRESQL_BIN_DIR",
      "PATRONI_POSTGRES_UNIX_SOCKET_DIRECTORY", "PATRONI_SCRIPTS", StackGresUtil.MD5SUM_KEY);

  private static final String PATRONI_LOG_LEVEL_KEY = "PATRONI_LOG_LEVEL";

  private static final String PATRONI_LOGGER_NAME = "io.stackgres.patroni";

  @BeforeEach
  void setUp() {
    patroniConfigMap = new PatroniConfigMap(labelFactoryForCluster, objectMapper, yamlMapperProvider);
    stackGresDistributedLogs =  Fixtures.distributedLogs().loadDefault().get();
    when(context.getSource()).thenReturn(stackGresDistributedLogs);
    when(labelFactoryForCluster.labelMapper()).thenReturn(labelMapperForCluster);
    when(labelMapperForCluster.clusterScopeKey(stackGresDistributedLogs)).thenReturn("test");
  }

  @Test
  void createDefaultConfigMapWithPatroniLogLevelEnvVar() {
    this.updateLevelLogger(PATRONI_LOGGER_NAME, Level.TRACE);
    HasMetadata configMap = patroniConfigMap.buildSource(context);
    Map<String, String> data = ((ConfigMap) configMap).getData();
    assertNotNull(configMap);
    assertNotNull(data);
    assertFalse(data.isEmpty());

    List<String> expectedEnvVars = new ArrayList<>(expectedEnvVarsConfigMap);
    expectedEnvVars.add(PATRONI_LOG_LEVEL_KEY);
    expectedEnvVars.stream().forEach(
        envVar -> assertNotNull(data.get(envVar), "Excepted not null value for " + envVar));
  }

  @Test
  void createDefaultConfigMapWithoutPatroniLogLevelEnvVar() {
    this.updateLevelLogger(PATRONI_LOGGER_NAME, Level.INFO);
    HasMetadata configMap = patroniConfigMap.buildSource(context);
    Map<String, String> data = ((ConfigMap) configMap).getData();
    assertNotNull(configMap);
    assertNotNull(data);
    assertFalse(data.isEmpty());
    expectedEnvVarsConfigMap.stream().forEach(
        envVar -> assertNotNull(data.get(envVar), "Excepted not null value for " + envVar));
    assertNull(data.get(PATRONI_LOG_LEVEL_KEY), "Excepted null value for " + PATRONI_LOG_LEVEL_KEY);
  }

  private void updateLevelLogger(String loggerName, Level newLevel) {
    LogContext logContext = LogContext.getLogContext();
    Logger logger = logContext.getLogger(loggerName);
    logger.setLevel(newLevel);
  }
}

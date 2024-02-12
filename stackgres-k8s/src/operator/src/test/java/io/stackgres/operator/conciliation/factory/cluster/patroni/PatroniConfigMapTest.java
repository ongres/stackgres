/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.json.JsonMapper;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.stackgres.common.ClusterPath;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.YamlMapperProvider;
import io.stackgres.common.crd.CustomContainer;
import io.stackgres.common.crd.CustomServicePort;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPatroni;
import io.stackgres.common.crd.sgcluster.StackGresClusterPatroniConfig;
import io.stackgres.common.crd.sgcluster.StackGresPostgresFlavor;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.ClusterLabelFactory;
import io.stackgres.common.labels.ClusterLabelMapper;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.testutil.JsonUtil;
import io.stackgres.testutil.ModelTestUtil;
import org.jboss.logmanager.Level;
import org.jboss.logmanager.LogContext;
import org.jboss.logmanager.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PatroniConfigMapTest {

  private static final JsonMapper JSON_MAPPER = JsonUtil.jsonMapper();
  private final LabelFactoryForCluster<StackGresCluster> labelFactory = new ClusterLabelFactory(
      new ClusterLabelMapper());
  @Mock
  private StackGresClusterContext context;
  private PatroniConfigMap generator;
  private StackGresCluster cluster;

  @BeforeEach
  void setUp() {
    generator = new PatroniConfigMap(labelFactory, JSON_MAPPER, new YamlMapperProvider());
    cluster = Fixtures.cluster().loadDefault().get();
    when(context.getSource()).thenReturn(cluster);
    cluster.getMetadata().getAnnotations()
        .put(StackGresContext.VERSION_KEY, StackGresVersion.LATEST.getVersion());
    cluster.getSpec().getConfigurations().setPatroni(new StackGresClusterPatroni());
    cluster.getSpec().getConfigurations().getPatroni()
        .setInitialConfig(new StackGresClusterPatroniConfig());
  }

  @Test
  void getConfigMapWithoutScope_shouldReturnExpectedEnvVars() throws Exception {
    when(context.getCluster()).thenReturn(cluster);
    ConfigMap configMap = generator.buildSource(context);

    assertEquals("""
            scope: "stackgres"
            kubernetes:
              namespace: "stackgres"
              labels:
                app: "StackGresCluster"
                stackgres.io/cluster: "true"
                stackgres.io/cluster-scope: "stackgres"
              use_endpoints: true
              scope_label: "stackgres.io/cluster-scope"
              ports:
              - name: "pgport"
                port: 7432
                protocol: "TCP"
              - name: "pgreplication"
                port: 7433
                protocol: "TCP"
            """,
        configMap.getData().get("PATRONI_INITIAL_CONFIG"));
  }

  @Test
  void getConfigMapWithoutEmptyPatroniConfig_shouldReturnExpectedEnvVars() throws Exception {
    when(context.getCluster()).thenReturn(cluster);
    ConfigMap configMap = generator.buildSource(context);

    assertEquals("""
            scope: "stackgres"
            kubernetes:
              namespace: "stackgres"
              labels:
                app: "StackGresCluster"
                stackgres.io/cluster: "true"
                stackgres.io/cluster-scope: "stackgres"
              use_endpoints: true
              scope_label: "stackgres.io/cluster-scope"
              ports:
              - name: "pgport"
                port: 7432
                protocol: "TCP"
              - name: "pgreplication"
                port: 7433
                protocol: "TCP"
            """,
        configMap.getData().get("PATRONI_INITIAL_CONFIG"));
  }

  @Test
  void getConfigMapWithScope_shouldReturnExpectedEnvVars() throws Exception {
    when(context.getCluster()).thenReturn(cluster);
    cluster.getSpec().getConfigurations().getPatroni()
        .getInitialConfig().put("scope", "test");
    cluster.getSpec().getConfigurations().getPatroni()
        .getInitialConfig().put("test", true);
    ConfigMap configMap = generator.buildSource(context);

    assertEquals("""
          test: true
          scope: "test"
          kubernetes:
            namespace: "stackgres"
            labels:
              app: "StackGresCluster"
              stackgres.io/cluster: "true"
              stackgres.io/cluster-scope: "test"
            use_endpoints: true
            scope_label: "stackgres.io/cluster-scope"
            ports:
            - name: "pgport"
              port: 7432
              protocol: "TCP"
            - name: "pgreplication"
              port: 7433
              protocol: "TCP"
            """,
        configMap.getData().get("PATRONI_INITIAL_CONFIG"));
  }

  @Test
  void verifyExclusionBlockListConfigKeys() {
    when(context.getCluster()).thenReturn(cluster);
    cluster.getSpec().getConfigurations().getPatroni()
        .getInitialConfig().put("validKey1", true);
    cluster.getSpec().getConfigurations().getPatroni()
        .getInitialConfig().put("validKey2", 1);
    cluster.getSpec().getConfigurations().getPatroni()
        .getInitialConfig().put("validKey3", "stringValue");
    cluster.getSpec().getPostgres().setVersion(
        StackGresComponent.BABELFISH.getLatest().streamOrderedVersions().findFirst().get());
    cluster.getSpec().getPostgres().setFlavor(StackGresPostgresFlavor.BABELFISH.toString());
    PatroniUtil.PATRONI_BLOCKLIST_CONFIG_KEYS.forEach(
        key ->  cluster.getSpec().getConfigurations().getPatroni()
        .getInitialConfig().put(key, ModelTestUtil.createWithRandomData(String.class)));
    ConfigMap configMap = generator.buildSource(context);

    assertEquals("""
            validKey1: true
            validKey2: 1
            validKey3: "stringValue"
            scope: "%1$s"
            kubernetes:
              namespace: "stackgres"
              labels:
                app: "StackGresCluster"
                stackgres.io/cluster: "true"
                stackgres.io/cluster-scope: "%1$s"
              use_endpoints: true
              scope_label: "stackgres.io/cluster-scope"
              ports:
              - name: "pgport"
                port: 7432
                protocol: "TCP"
              - name: "pgreplication"
                port: 7433
                protocol: "TCP"
              - name: "babelfish"
                port: 7434
                protocol: "TCP"
            """.formatted(PatroniUtil.clusterScope(cluster)),
        configMap.getData().get("PATRONI_INITIAL_CONFIG"));
  }

  @Test
  void getConfigMapWithBabelfishFlavor_shouldReturnBabelfishInformationPort() throws Exception {
    when(context.getCluster()).thenReturn(cluster);
    cluster.getSpec().getPostgres().setVersion(
        StackGresComponent.BABELFISH.getLatest().streamOrderedVersions().findFirst().get());
    cluster.getSpec().getPostgres().setFlavor(StackGresPostgresFlavor.BABELFISH.toString());

    ConfigMap configMap = generator.buildSource(context);
    @SuppressWarnings("unchecked")
    List<Map<String, String>> kubernetesPorts = JsonUtil.fromJson(JsonUtil.yamlMapper()
        .readTree(configMap.getData().get("PATRONI_INITIAL_CONFIG"))
        .get("kubernetes")
        .get("ports"), List.class);
    Optional<Map<String, String>> babelfishEndpointPort = kubernetesPorts.stream()
        .filter(ep -> StackGresPostgresFlavor.BABELFISH.toString().equals(ep.get("name")))
        .findFirst();

    assertTrue(babelfishEndpointPort.isPresent());
    assertEquals(babelfishEndpointPort.get().get("port"), EnvoyUtil.BF_ENTRY_PORT);
  }

  @Test
  void getConfigMapWhenLogLevelTraceIsActive_shouldSetLogLevelOnDebugMode() {
    when(context.getCluster()).thenReturn(cluster);
    LogContext logContext = LogContext.getLogContext();
    Logger logger = logContext.getLogger("io.stackgres.patroni");
    logger.setLevel(Level.TRACE);

    ConfigMap configMap = generator.buildSource(context);
    assertEquals(Level.DEBUG.getName(), configMap.getData().get("PATRONI_LOG_LEVEL"));
  }

  @Test
  void getConfigMapWhenDistributedLogsSpecIsNotPresent_shouldNotContainLogInformationEnvVars() {
    when(context.getCluster()).thenReturn(cluster);
    cluster.getSpec().getDistributedLogs().setSgDistributedLogs(null);
    ConfigMap configMap = generator.buildSource(context);
    assertNull(configMap.getData().get("PATRONI_LOG_DIR"));
    assertNull(configMap.getData().get("PATRONI_LOG_FILE_NUM"));
    assertNull(configMap.getData().get("PATRONI_LOG_FILE_SIZE"));
  }

  @Test
  void getConfigMapWhenDistributedLogsSpecIsPresent_shouldContainLogInformationEnvVars() {
    when(context.getCluster()).thenReturn(cluster);
    ConfigMap configMap = generator.buildSource(context);
    assertEquals(ClusterPath.PG_LOG_PATH.path(),
        configMap.getData().get("PATRONI_LOG_DIR"));
    assertEquals("2", configMap.getData().get("PATRONI_LOG_FILE_NUM"));
    assertEquals(String.valueOf(PatroniConfigMap.PATRONI_LOG_FILE_SIZE),
        configMap.getData().get("PATRONI_LOG_FILE_SIZE"));
  }

  @Test
  void getConfigMapWhenExistOneCustomServiceWithIntegerTypePort() throws Exception {
    when(context.getCluster()).thenReturn(cluster);
    CustomServicePort csPort = ModelTestUtil.createWithRandomData(CustomServicePort.class);
    csPort.setTargetPort(new IntOrString(Integer.valueOf("8080")));
    cluster.getSpec().getPostgresServices().getPrimary().setCustomPorts(List.of(csPort));

    ConfigMap configMap = generator.buildSource(context);
    @SuppressWarnings("unchecked")
    List<Map<String, String>> kubernetesPorts = JsonUtil.fromJson(JsonUtil.yamlMapper()
        .readTree(configMap.getData().get("PATRONI_INITIAL_CONFIG"))
        .get("kubernetes")
        .get("ports"), List.class);
    Optional<Map<String, String>> customPortIntValue = kubernetesPorts.stream()
        .filter(ep -> "custom-".concat(csPort.getName()).equals(ep.get("name").toString()))
        .findFirst();
    assertTrue(customPortIntValue.isPresent());
  }

  @Test
  void throwAnIllegalArgumentExceptionWhenPodDoesNotContainCustomServicesStringTypePort() {
    CustomServicePort csPort = ModelTestUtil.createWithRandomData(CustomServicePort.class);
    csPort.setTargetPort(new IntOrString(new String("80")));
    cluster.getSpec().getPostgresServices().getPrimary().setCustomPorts(List.of(csPort));
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> generator.buildSource(context));
    assertEquals(
        "Can not find any custom container with port named " + csPort.getTargetPort().getStrVal(),
        exception.getMessage());
  }

  @Test
  void getConfigMapWhenExistOneCustomServiceWithStringTypePort() throws Exception {
    when(context.getCluster()).thenReturn(cluster);
    String customContainerPortName = "portName";
    ContainerPort containerPort = ModelTestUtil.createWithRandomData(ContainerPort.class);
    containerPort.setName(customContainerPortName);
    CustomContainer customContainer = ModelTestUtil.createWithRandomData(CustomContainer.class);
    customContainer.setPorts(List.of(containerPort));

    context.getCluster().getSpec().getPods().setCustomContainers(List.of(customContainer));
    CustomServicePort csPort = ModelTestUtil.createWithRandomData(CustomServicePort.class);
    csPort.setTargetPort(new IntOrString(customContainerPortName));
    cluster.getSpec().getPostgresServices().getPrimary().setCustomPorts(List.of(csPort));

    ConfigMap configMap = generator.buildSource(context);
    @SuppressWarnings("unchecked")
    List<Map<String, String>> kubernetesPorts = JsonUtil.fromJson(JsonUtil.yamlMapper()
        .readTree(configMap.getData().get("PATRONI_INITIAL_CONFIG"))
        .get("kubernetes")
        .get("ports"), List.class);
    Optional<Map<String, String>> customPortIntValue = kubernetesPorts.stream()
        .filter(ep -> "custom-".concat(csPort.getName()).equals(ep.get("name").toString()))
        .findFirst();
    assertTrue(customPortIntValue.isPresent());
  }
}

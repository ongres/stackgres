/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.ClusterPath;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.DistributedLogsLabelFactory;
import io.stackgres.common.labels.DistributedLogsLabelMapper;
import io.stackgres.common.labels.LabelFactoryForDistributedLogs;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DistributedLogsTemplatesConfigMapTest {

  private final LabelFactoryForDistributedLogs labelFactory =
      new DistributedLogsLabelFactory(new DistributedLogsLabelMapper());

  @Mock
  private StackGresDistributedLogsContext context;

  private DistributedLogsTemplatesConfigMap distributedLogsTemplatesConfigMap;

  private StackGresDistributedLogs distributedLogs;

  @BeforeEach
  void setUp() {
    distributedLogsTemplatesConfigMap = new DistributedLogsTemplatesConfigMap(labelFactory);
    distributedLogs = Fixtures.distributedLogs().loadDefault().get();
    when(context.getSource()).thenReturn(distributedLogs);
  }

  @Test
  void generateResource_shouldGenerateConfigMapWithTemplateDataKeys() {
    List<HasMetadata> resources =
        distributedLogsTemplatesConfigMap.generateResource(context).toList();

    assertEquals(1, resources.size());
    assertTrue(resources.getFirst() instanceof ConfigMap);
    ConfigMap configMap = (ConfigMap) resources.getFirst();
    assertNotNull(configMap.getData());

    assertTrue(configMap.getData().containsKey(
            ClusterPath.LOCAL_BIN_START_FLUENTD_SH_PATH.filename()),
        "Expected ConfigMap to contain key "
        + ClusterPath.LOCAL_BIN_START_FLUENTD_SH_PATH.filename());
    assertTrue(configMap.getData().containsKey(
            ClusterPath.LOCAL_BIN_SHELL_UTILS_PATH.filename()),
        "Expected ConfigMap to contain key "
        + ClusterPath.LOCAL_BIN_SHELL_UTILS_PATH.filename());

    assertNotNull(configMap.getData().get(
            ClusterPath.LOCAL_BIN_START_FLUENTD_SH_PATH.filename()),
        "Expected non-null content for "
        + ClusterPath.LOCAL_BIN_START_FLUENTD_SH_PATH.filename());
    assertNotNull(configMap.getData().get(
            ClusterPath.LOCAL_BIN_SHELL_UTILS_PATH.filename()),
        "Expected non-null content for "
        + ClusterPath.LOCAL_BIN_SHELL_UTILS_PATH.filename());
  }

  @Test
  void generateResource_shouldHaveCorrectNamespace() {
    List<HasMetadata> resources =
        distributedLogsTemplatesConfigMap.generateResource(context).toList();

    ConfigMap configMap = (ConfigMap) resources.getFirst();
    assertEquals(DistributedLogsTemplatesConfigMap.templatesName(distributedLogs),
        configMap.getMetadata().getName());
    assertEquals("distributed-logs", configMap.getMetadata().getNamespace());
  }

}

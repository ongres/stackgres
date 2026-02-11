/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.ClusterPath;
import io.stackgres.common.StackGresVolume;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.ClusterLabelFactory;
import io.stackgres.common.labels.ClusterLabelMapper;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.AbstractTemplatesConfigMap;
import io.stackgres.operator.conciliation.factory.VolumePair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TemplatesConfigMapTest {

  private final LabelFactoryForCluster labelFactory =
      new ClusterLabelFactory(new ClusterLabelMapper());

  @Mock
  private StackGresClusterContext context;

  private TemplatesConfigMap templatesConfigMap;

  private StackGresCluster cluster;

  @BeforeEach
  void setUp() {
    templatesConfigMap = new TemplatesConfigMap();
    templatesConfigMap.setLabelFactory(labelFactory);
    cluster = Fixtures.cluster().loadDefault().get();
    when(context.getSource()).thenReturn(cluster);
    when(context.getCluster()).thenReturn(cluster);
  }

  @Test
  void buildSource_shouldGenerateConfigMapWithTemplateData() {
    HasMetadata source = templatesConfigMap.buildSource(context);

    assertNotNull(source);
    assertInstanceOfConfigMap(source);
    ConfigMap configMap = (ConfigMap) source;
    assertNotNull(configMap.getData());
    assertFalse(configMap.getData().isEmpty());
  }

  @Test
  void buildSource_shouldHaveCorrectNamespaceAndName() {
    HasMetadata source = templatesConfigMap.buildSource(context);

    ConfigMap configMap = (ConfigMap) source;
    assertEquals(cluster.getMetadata().getNamespace(), configMap.getMetadata().getNamespace());
    String expectedName = StackGresVolume.SCRIPT_TEMPLATES
        .getResourceName(cluster.getMetadata().getName());
    assertEquals(expectedName, configMap.getMetadata().getName());
  }

  @Test
  void buildSource_shouldContainAllClusterTemplateKeys() {
    HasMetadata source = templatesConfigMap.buildSource(context);

    ConfigMap configMap = (ConfigMap) source;
    Map<String, String> data = configMap.getData();
    for (ClusterPath templatePath : AbstractTemplatesConfigMap.CLUSTER_TEMPLATE_PATHS) {
      assertTrue(data.containsKey(templatePath.filename()),
          "Missing template key: " + templatePath.filename());
    }
  }

  @Test
  void buildVolumes_shouldReturnSingleVolumePair() {
    List<VolumePair> volumePairs = templatesConfigMap.buildVolumes(context).toList();

    assertEquals(1, volumePairs.size());
    VolumePair volumePair = volumePairs.getFirst();
    assertEquals(StackGresVolume.SCRIPT_TEMPLATES.getName(),
        volumePair.getVolume().getName());
    assertTrue(volumePair.getSource().isPresent());
  }

  @Test
  void buildVolume_shouldHaveCorrectConfigMapReference() {
    reset(context);
    when(context.getCluster()).thenReturn(cluster);
    var volume = templatesConfigMap.buildVolume(context);

    assertEquals(StackGresVolume.SCRIPT_TEMPLATES.getName(), volume.getName());
    assertNotNull(volume.getConfigMap());
    String expectedName = StackGresVolume.SCRIPT_TEMPLATES
        .getResourceName(cluster.getMetadata().getName());
    assertEquals(expectedName, volume.getConfigMap().getName());
    assertEquals(0444, volume.getConfigMap().getDefaultMode());
  }

  @Test
  void generateResource_shouldReturnConfigMapWithData() {
    List<HasMetadata> resources = templatesConfigMap.generateResource(context).toList();

    assertEquals(1, resources.size());
    assertInstanceOfConfigMap(resources.getFirst());
    ConfigMap configMap = (ConfigMap) resources.getFirst();
    assertFalse(configMap.getData().isEmpty());
    assertEquals(cluster.getMetadata().getNamespace(),
        configMap.getMetadata().getNamespace());
  }

  @Test
  void buildSource_shouldHaveLabels() {
    HasMetadata source = templatesConfigMap.buildSource(context);

    ConfigMap configMap = (ConfigMap) source;
    assertNotNull(configMap.getMetadata().getLabels());
    assertFalse(configMap.getMetadata().getLabels().isEmpty());
  }

  private void assertInstanceOfConfigMap(HasMetadata resource) {
    assertTrue(resource instanceof ConfigMap,
        "Expected ConfigMap but got " + resource.getClass().getSimpleName());
  }

}

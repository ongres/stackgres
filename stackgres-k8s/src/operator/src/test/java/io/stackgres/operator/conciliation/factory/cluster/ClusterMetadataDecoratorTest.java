/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresProperty;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecAnnotations;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecLabels;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecMetadata;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClusterMetadataDecoratorTest {

  private final ClusterMetadataDecorator decorator = new ClusterMetadataDecorator();

  @Mock
  private StackGresClusterContext context;

  private StackGresCluster cluster;

  @BeforeEach
  void setUp() {
    cluster = Fixtures.cluster().loadDefault().get();
    when(context.getSource()).thenReturn(cluster);
  }

  @Test
  void decorate_whenAllResourcesAnnotationsSet_shouldPropagateToResource() {
    cluster.getSpec().getMetadata().setAnnotations(new StackGresClusterSpecAnnotations());
    cluster.getSpec().getMetadata().getAnnotations()
        .setAllResources(Map.of("custom-annotation", "custom-value"));

    HasMetadata resource = new ConfigMapBuilder()
        .withNewMetadata().withName("test").withNamespace("test-ns").endMetadata()
        .build();

    decorator.decorate(context, resource);

    assertNotNull(resource.getMetadata().getAnnotations());
    assertTrue(resource.getMetadata().getAnnotations().containsKey("custom-annotation"));
    assertEquals("custom-value", resource.getMetadata().getAnnotations().get("custom-annotation"));
  }

  @Test
  void decorate_whenAllResourcesAnnotationsSet_shouldIncludeVersionAnnotation() {
    cluster.getSpec().getMetadata().setAnnotations(new StackGresClusterSpecAnnotations());
    cluster.getSpec().getMetadata().getAnnotations()
        .setAllResources(Map.of("custom-annotation", "custom-value"));

    HasMetadata resource = new ServiceBuilder()
        .withNewMetadata().withName("test").withNamespace("test-ns").endMetadata()
        .build();

    decorator.decorate(context, resource);

    assertTrue(resource.getMetadata().getAnnotations()
        .containsKey(StackGresContext.VERSION_KEY));
  }

  @Test
  void decorate_whenAllResourcesLabelsSet_shouldPropagateToResource() {
    cluster.getSpec().getMetadata().setLabels(new StackGresClusterSpecLabels());
    cluster.getSpec().getMetadata().getLabels()
        .setAllResources(Map.of("custom-label", "label-value"));

    HasMetadata resource = new ConfigMapBuilder()
        .withNewMetadata().withName("test").withNamespace("test-ns").endMetadata()
        .build();

    decorator.decorate(context, resource);

    assertNotNull(resource.getMetadata().getLabels());
    assertTrue(resource.getMetadata().getLabels().containsKey("custom-label"));
    assertEquals("label-value", resource.getMetadata().getLabels().get("custom-label"));
  }

  @Test
  void decorate_whenNullSpecMetadata_shouldNotThrow() {
    cluster.getSpec().setMetadata(null);

    HasMetadata resource = new ConfigMapBuilder()
        .withNewMetadata().withName("test").withNamespace("test-ns").endMetadata()
        .build();

    decorator.decorate(context, resource);

    assertNotNull(resource.getMetadata().getAnnotations());
    assertTrue(resource.getMetadata().getAnnotations()
        .containsKey(StackGresContext.VERSION_KEY));
  }

  @Test
  void decorate_whenNullAnnotationsInSpecMetadata_shouldHandleGracefully() {
    StackGresClusterSpecMetadata specMetadata = new StackGresClusterSpecMetadata();
    cluster.getSpec().setMetadata(specMetadata);

    HasMetadata resource = new ConfigMapBuilder()
        .withNewMetadata().withName("test").withNamespace("test-ns").endMetadata()
        .build();

    decorator.decorate(context, resource);

    assertNotNull(resource.getMetadata().getAnnotations());
    assertTrue(resource.getMetadata().getAnnotations()
        .containsKey(StackGresContext.VERSION_KEY));
  }

  @Test
  void decorate_whenStatefulSet_shouldPropagateToPodTemplateAndVolumeClaimTemplates() {
    cluster.getSpec().getMetadata().setAnnotations(new StackGresClusterSpecAnnotations());
    cluster.getSpec().getMetadata().getAnnotations()
        .setAllResources(Map.of("sts-annotation", "sts-value"));

    StatefulSet sts = new StatefulSetBuilder()
        .withNewMetadata().withName("test-sts").withNamespace("test-ns").endMetadata()
        .withNewSpec()
        .withNewTemplate()
        .withNewMetadata().endMetadata()
        .endTemplate()
        .addNewVolumeClaimTemplate()
        .withNewMetadata().withName("data").endMetadata()
        .withNewSpec().withAccessModes("ReadWriteOnce").endSpec()
        .endVolumeClaimTemplate()
        .endSpec()
        .build();

    decorator.decorate(context, sts);

    assertNotNull(sts.getMetadata().getAnnotations());
    assertTrue(sts.getMetadata().getAnnotations().containsKey("sts-annotation"));

    ObjectMeta podTemplateMeta = sts.getSpec().getTemplate().getMetadata();
    assertNotNull(podTemplateMeta.getAnnotations());
    assertTrue(podTemplateMeta.getAnnotations().containsKey("sts-annotation"));

    ObjectMeta pvcMeta = sts.getSpec().getVolumeClaimTemplates()
        .getFirst().getMetadata();
    assertNotNull(pvcMeta.getAnnotations());
    assertTrue(pvcMeta.getAnnotations().containsKey("sts-annotation"));
  }

  @Test
  void decorate_whenExistingAnnotationsOnResource_shouldPreserveAndMerge() {
    cluster.getSpec().getMetadata().setAnnotations(new StackGresClusterSpecAnnotations());
    cluster.getSpec().getMetadata().getAnnotations()
        .setAllResources(Map.of("new-annotation", "new-value"));

    HasMetadata resource = new ConfigMapBuilder()
        .withNewMetadata()
        .withName("test")
        .withNamespace("test-ns")
        .withAnnotations(new HashMap<>(Map.of("existing", "preserved")))
        .endMetadata()
        .build();

    decorator.decorate(context, resource);

    assertEquals("preserved", resource.getMetadata().getAnnotations().get("existing"));
    assertEquals("new-value", resource.getMetadata().getAnnotations().get("new-annotation"));
  }

  @Test
  void decorate_whenVersionKeyInClusterAnnotations_shouldUseClusterVersion() {
    String clusterVersion = cluster.getMetadata().getAnnotations()
        .get(StackGresContext.VERSION_KEY);

    HasMetadata resource = new ConfigMapBuilder()
        .withNewMetadata().withName("test").withNamespace("test-ns").endMetadata()
        .build();

    decorator.decorate(context, resource);

    assertEquals(clusterVersion,
        resource.getMetadata().getAnnotations().get(StackGresContext.VERSION_KEY));
  }

  @Test
  void decorate_whenNoVersionKeyInClusterAnnotations_shouldUseFallbackVersion() {
    cluster.getMetadata().getAnnotations().remove(StackGresContext.VERSION_KEY);

    HasMetadata resource = new ConfigMapBuilder()
        .withNewMetadata().withName("test").withNamespace("test-ns").endMetadata()
        .build();

    decorator.decorate(context, resource);

    assertEquals(StackGresProperty.OPERATOR_VERSION.getString(),
        resource.getMetadata().getAnnotations().get(StackGresContext.VERSION_KEY));
  }

}

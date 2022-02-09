/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.stackgres.common.ClusterLabelFactory;
import io.stackgres.common.ClusterLabelMapper;
import io.stackgres.common.StringUtil;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.testutil.JsonUtil;
import io.stackgres.testutil.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClusterStatefulSetComparatorTest {

  @Mock
  private ResourceScanner<Pod> podScanner;

  private ClusterLabelFactory labelFactory;

  private ClusterStatefulSetComparator comparator;

  private StatefulSet required;
  private StatefulSet deployed;

  @BeforeEach
  void setUp() {
    labelFactory = new ClusterLabelFactory(new ClusterLabelMapper());
    comparator = new ClusterStatefulSetComparator(podScanner, labelFactory);
    required = JsonUtil.readFromJson("statefulset/required.json",
        StatefulSet.class);
    deployed = JsonUtil.readFromJson("statefulset/deployed.json",
        StatefulSet.class);
  }

  @Test
  void generatedResourceAndRequiredResource_shouldHaveNoDifference() {
    setupPrimaryPod();
    var isContentEqual = comparator.isResourceContentEqual(required, deployed);

    assertTrue(isContentEqual);
    verify(podScanner).findByLabelsAndNamespace(any(), any());
  }

  @Test
  void annotationChanges_shouldBeDetected() {
    setupPrimaryPod();
    required.getMetadata().setAnnotations(
        ImmutableMap.of(StringUtil.generateRandom(), StringUtil.generateRandom()));

    var isContentEqual = comparator.isResourceContentEqual(required, deployed);

    assertFalse(isContentEqual);
    verify(podScanner).findByLabelsAndNamespace(any(), any());
  }

  @Test
  void volumeClaimAnnotationChanges_shouldNotBeDetected() {
    setupPrimaryPod();
    required.getSpec().getVolumeClaimTemplates().forEach(vct -> vct.getMetadata().setAnnotations(
        ImmutableMap.of(StringUtils.getRandomString(), StringUtils.getRandomString())));

    var isContentEqual = comparator.isResourceContentEqual(required, deployed);

    assertTrue(isContentEqual);

    deployed.getSpec().getVolumeClaimTemplates().forEach(vct -> vct.getMetadata().setAnnotations(
        ImmutableMap.of(StringUtils.getRandomString(), StringUtils.getRandomString())));

    isContentEqual = comparator.isResourceContentEqual(required, deployed);
    assertTrue(isContentEqual);

    required.getSpec().getVolumeClaimTemplates().forEach(vct -> vct.getMetadata().setAnnotations(
        ImmutableMap.of("test-property", StringUtils.getRandomString())));

    deployed.getSpec().getVolumeClaimTemplates().forEach(vct -> vct.getMetadata().setAnnotations(
        ImmutableMap.of("test-property", StringUtils.getRandomString())));

    isContentEqual = comparator.isResourceContentEqual(required, deployed);
    assertTrue(isContentEqual);
    verify(podScanner, times(3)).findByLabelsAndNamespace(any(), any());
  }

  @Test
  void containerImageChanges_shouldBeDetected() {
    setupPrimaryPod();
    required.getSpec().getTemplate().getSpec().getContainers()
        .get(0).setImage("docker.io/ongres/patroni:v1.6.5-pg12.3-build-5.2");

    deployed.getSpec().getTemplate().getSpec().getContainers()
        .get(0).setImage("docker.io/ongres/patroni:v1.6.5-pg12.3-build-5.1");

    var isContentEqual = comparator.isResourceContentEqual(required, deployed);

    assertFalse(isContentEqual);
    verify(podScanner).findByLabelsAndNamespace(any(), any());
  }

  @Test
  void missingPrimary_shouldBeDetected() {
    when(podScanner.findByLabelsAndNamespace(any(), any()))
        .thenReturn(ImmutableList.of());

    var isContentEqual = comparator.isResourceContentEqual(required, deployed);

    assertFalse(isContentEqual);
    verify(podScanner).findByLabelsAndNamespace(any(), any());
  }

  private void setupPrimaryPod() {
    when(podScanner.findByLabelsAndNamespace(any(), any()))
        .thenReturn(ImmutableList.of(new PodBuilder()
            .build()));
  }

}

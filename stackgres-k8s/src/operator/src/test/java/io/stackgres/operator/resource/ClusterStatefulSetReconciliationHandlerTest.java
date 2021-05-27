/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StringUtil;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.common.resource.ResourceWriter;
import io.stackgres.testutil.JsonUtil;
import io.stackgres.testutil.StringUtils;
import io.stackgres.operator.conciliation.cluster.ClusterStatefulSetReconciliationHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

@ExtendWith(MockitoExtension.class)
class ClusterStatefulSetReconciliationHandlerTest {

  @Mock
  private ResourceWriter<StatefulSet> statefulSetWriter;

  @Mock
  private ResourceScanner<Pod> podScanner;

  @Mock
  private ResourceWriter<Pod> podWriter;

  @Mock
  private ResourceFinder<StatefulSet> statefulSetFinder;

  private ClusterStatefulSetReconciliationHandler handler;

  private StatefulSet requiredStatefulSet;

  private StatefulSet deployedStatefulSet;

  @BeforeEach
  void setUp() {
    handler = new ClusterStatefulSetReconciliationHandler(statefulSetWriter, podScanner, podWriter, statefulSetFinder);
    requiredStatefulSet = JsonUtil
        .readFromJson("statefulset/required.json", StatefulSet.class);

    deployedStatefulSet = JsonUtil
        .readFromJson("statefulset/deployed.json", StatefulSet.class);
    lenient().when(statefulSetFinder.findByNameAndNamespace(
        requiredStatefulSet.getMetadata().getName(),
        requiredStatefulSet.getMetadata().getNamespace())
    ).thenReturn(Optional.of(deployedStatefulSet));
  }

  @Test
  void createResource_shouldValidateTheResourceType() {

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> handler.create(new SecretBuilder()
            .addToData(StringUtil.generateRandom(), StringUtil.generateRandom())
            .build()));

    assertEquals("Resource must be a StatefulSet instance", ex.getMessage());

    verify(statefulSetWriter, never()).create(any(StatefulSet.class));
  }

  @Test
  void createResource_shouldCreateTheResource() {

    when(statefulSetWriter.create(requiredStatefulSet)).thenReturn(requiredStatefulSet);

    HasMetadata sts = handler.create(requiredStatefulSet);

    assertEquals(requiredStatefulSet, sts);
  }

  @Test
  void scalingDownStatefulSetWithoutNonDisruptablePods_shouldResultInTheSameNumberOfDesiredReplicas() {

    final int desiredReplicas = setUpDownscale(0, 0, MasterPosition.FIRST);

    StatefulSet sts = (StatefulSet) handler.replace(requiredStatefulSet);

    assertEquals(desiredReplicas, sts.getSpec().getReplicas());

    verify(podScanner).findByLabelsAndNamespace(anyString(), anyMap());
    verify(statefulSetWriter).update(any(StatefulSet.class));
    verify(podWriter, never()).update(any(Pod.class));

  }

  @Test
  void scalingUpStatefulSetWithoutNonDisputablePods_shouldResultInTheSameNumberOfDesiredReplicas() {

    final int desiredReplicas = setUpUpscale(0, 0, MasterPosition.FIRST);

    StatefulSet sts = (StatefulSet) handler.replace(requiredStatefulSet);

    assertEquals(desiredReplicas, sts.getSpec().getReplicas());

    verify(podScanner).findByLabelsAndNamespace(anyString(), anyMap());
    verify(statefulSetWriter).update(any(StatefulSet.class));
    verify(podWriter, never()).update(any(Pod.class));
  }

  @Test
  void scalingDownStatefulSetWithNonDisruptablePods_shouldResultInTheNumberOfDesiredReplicasMinusTheDisruptablePods() {

    final int desiredReplicas = setUpDownscale(1, 0, MasterPosition.FIRST);

    StatefulSet sts = (StatefulSet) handler.replace(requiredStatefulSet);

    assertEquals(desiredReplicas - 1, sts.getSpec().getReplicas());

    verify(podScanner).findByLabelsAndNamespace(anyString(), anyMap());
    verify(statefulSetWriter).update(any(StatefulSet.class));
    verify(podWriter, never()).update(any(Pod.class));

  }

  @Test
  void scalingUpStatefulSetWithNonDisputablePodsWithIndexBiggerThanReplicasCount_shouldResultInTheSameNumberOfDesiredReplicasMinusTheDisruptablePods() {

    final int desiredReplicas = setUpUpscale(1, 1, MasterPosition.FIRST);

    StatefulSet sts = (StatefulSet) handler.replace(requiredStatefulSet);

    assertEquals(desiredReplicas - 1, sts.getSpec().getReplicas());

    verify(podScanner).findByLabelsAndNamespace(anyString(), anyMap());
    verify(statefulSetWriter).update(any(StatefulSet.class));
    verify(podWriter, never()).update(any(Pod.class));
  }

  @Test
  void scalingUpStatefulSetWithNonDisputablePodsWithIndexLowerThanReplicasCount_shouldResultInTheSameNumberOfDesiredReplicasAndFixDisruptableLabel() {

    final int desiredReplicas = setUpUpscale(1, -1, MasterPosition.FIRST_NONDISRUPTABLE);

    ArgumentCaptor<Pod> podArgumentCaptor = ArgumentCaptor.forClass(Pod.class);

    when(podWriter.update(podArgumentCaptor.capture()))
        .then((Answer<Pod>) invocationOnMock -> invocationOnMock.getArgument(0));

    StatefulSet sts = (StatefulSet) handler.replace(requiredStatefulSet);

    assertEquals(desiredReplicas, sts.getSpec().getReplicas());

    var updatedPod = podArgumentCaptor.getValue();

    String disruptableValue = updatedPod.getMetadata().getLabels()
        .get(StackGresContext.DISRUPTIBLE_KEY);

    assertEquals(StackGresContext.RIGHT_VALUE, disruptableValue);

    verify(podScanner).findByLabelsAndNamespace(anyString(), anyMap());
    verify(statefulSetWriter).update(any(StatefulSet.class));
    verify(podWriter).update(any(Pod.class));

  }

  @Test
  void scalingDownStatefulSetWithoutNonDisputablePodsAndMasterNodeAboutToBeDisrupt_shouldResultInTheNumberOfDesiredReplicasMinusOneAndMakeTheMasterNodeNonDisruptable() {

    final int desiredReplicas = setUpDownscale(0, 0, MasterPosition.LAST_DISRUPTABLE);

    ArgumentCaptor<Pod> podArgumentCaptor = ArgumentCaptor.forClass(Pod.class);

    when(podWriter.update(any(Pod.class)))
        .then((Answer<Pod>) invocationOnMock -> invocationOnMock.getArgument(0));

    StatefulSet sts = (StatefulSet) handler.replace(requiredStatefulSet);

    assertEquals(desiredReplicas - 1, sts.getSpec().getReplicas());

    verify(podWriter).update(podArgumentCaptor.capture());
    var updatedPod = podArgumentCaptor.getValue();

    String disruptableValue = updatedPod.getMetadata().getLabels()
        .get(StackGresContext.DISRUPTIBLE_KEY);
    String podRole = updatedPod.getMetadata().getLabels()
        .get(StackGresContext.ROLE_KEY);

    assertEquals(StackGresContext.WRONG_VALUE, disruptableValue);
    assertEquals(StackGresContext.PRIMARY_ROLE, podRole);

    verify(podScanner).findByLabelsAndNamespace(anyString(), anyMap());
    verify(statefulSetWriter).update(any(StatefulSet.class));

  }

  @Test
  void scalingDownStatefulSetWithNonDisputablePodsAndMasterNodeNonDisruptible_shouldResultInTheNumberOfDesiredReplicasMinusTheDisruptablePods() {

    final int desiredReplicas = setUpDownscale(1, 0, MasterPosition.FIRST_NONDISRUPTABLE);

    StatefulSet sts = (StatefulSet) handler.replace(requiredStatefulSet);

    assertEquals(desiredReplicas - 1, sts.getSpec().getReplicas());

    verify(podScanner).findByLabelsAndNamespace(anyString(), anyMap());
    verify(statefulSetWriter).update(any(StatefulSet.class));
    verify(podWriter, never()).update(any(Pod.class));
  }

  @Test
  void scalingDownStatefulSetWithNonDisputablePodsAndMasterNodeNonDisruptibleAndDistanceBiggerThan0_shouldResultInTheNumberOfDesiredReplicasMinusTheDisruptablePods() {

    final int desiredReplicas = setUpDownscale(1, 1, MasterPosition.FIRST_NONDISRUPTABLE);

    StatefulSet sts = (StatefulSet) handler.replace(requiredStatefulSet);

    assertEquals(desiredReplicas - 1, sts.getSpec().getReplicas());

    verify(podScanner).findByLabelsAndNamespace(anyString(), anyMap());
    verify(statefulSetWriter).update(any(StatefulSet.class));
    verify(podWriter, never()).update(any(Pod.class));
  }

  @Test
  void delete_shouldNotFail() {

    doNothing().when(statefulSetWriter).delete(requiredStatefulSet);

    handler.delete(requiredStatefulSet);

  }

  @Test
  void givenDisruptablePods_decorateShouldFixReplicasValue() {

    final Map<String, String> commonPodLabels = requiredStatefulSet.getSpec().getSelector()
        .getMatchLabels();
    Map<String, String> nonDisruptablePodLabels = new HashMap<>(commonPodLabels);
    nonDisruptablePodLabels.put(StackGresContext.DISRUPTIBLE_KEY, StackGresContext.WRONG_VALUE);

    final String namespace = requiredStatefulSet.getMetadata().getNamespace();
    final String name = requiredStatefulSet.getMetadata().getName();
    when(podScanner.findByLabelsAndNamespace(namespace, nonDisruptablePodLabels))
        .thenReturn(ImmutableList.of(new PodBuilder()
            .withNewMetadata()
            .withNamespace(namespace)
            .withName(name)
            .withLabels(nonDisruptablePodLabels)
            .endMetadata()
            .build()));

    int replicas = deployedStatefulSet.getSpec().getReplicas();

    handler.decorate(deployedStatefulSet);

    assertEquals(replicas + 1, deployedStatefulSet.getSpec().getReplicas());

  }

  @Test
  void givenAnyDisruptablePods_decorateShouldNotUpdateReplicasValue() {

    final Map<String, String> commonPodLabels = requiredStatefulSet.getSpec().getSelector()
        .getMatchLabels();
    Map<String, String> nonDisruptablePodLabels = new HashMap<>(commonPodLabels);
    nonDisruptablePodLabels.put(StackGresContext.DISRUPTIBLE_KEY, StackGresContext.WRONG_VALUE);

    final String namespace = requiredStatefulSet.getMetadata().getNamespace();
    when(podScanner.findByLabelsAndNamespace(namespace, nonDisruptablePodLabels))
        .thenReturn(ImmutableList.of());

    int replicas = deployedStatefulSet.getSpec().getReplicas();

    handler.decorate(deployedStatefulSet);

    assertEquals(replicas, deployedStatefulSet.getSpec().getReplicas());

  }

  @Test
  void givenVolumeClaimTemplateAnnotationChanges_shouldNotBeApplied() {

    setUpUpscale(0, 0, MasterPosition.FIRST);
    requiredStatefulSet.getSpec().getVolumeClaimTemplates().forEach(vct -> vct
        .getMetadata().setAnnotations(
            Map.of(StringUtils.getRandomString(), StringUtils.getRandomString())));

    final Map<String, String> deployedAnnotations = Map
        .of(StringUtils.getRandomString(), StringUtils.getRandomString());

    deployedStatefulSet.getSpec().getVolumeClaimTemplates().forEach(vct -> vct.getMetadata()
        .setAnnotations(deployedAnnotations));

    ArgumentCaptor<StatefulSet> patchedStatefulSetCaptor = ArgumentCaptor.forClass(StatefulSet.class);

    when(statefulSetWriter.update(patchedStatefulSetCaptor.capture())).thenReturn(requiredStatefulSet);

    handler.replace(requiredStatefulSet);

    var patchedStatefulSet = patchedStatefulSetCaptor.getValue();
    patchedStatefulSet.getSpec().getVolumeClaimTemplates()
        .forEach(vct -> assertEquals(deployedAnnotations, vct.getMetadata().getAnnotations()));

  }

  @Test
  void givenPodAnnotationChanges_shouldBeAppliedDirectlyToPods() {
    setUpUpscale(0, 0, MasterPosition.FIRST);

    final Map<String, String> requiredAnnotations = Map
        .of(StringUtils.getRandomString(), StringUtils.getRandomString());
    requiredStatefulSet.getSpec().getTemplate().getMetadata().setAnnotations(requiredAnnotations);

    when(statefulSetWriter.update(any())).thenReturn(requiredStatefulSet);
    ArgumentCaptor<Pod> podArgumentCaptor = ArgumentCaptor.forClass(Pod.class);

    when(podWriter.update(podArgumentCaptor.capture()))
        .then((Answer<Pod>) invocationOnMock -> invocationOnMock.getArgument(0));

    handler.replace(requiredStatefulSet);

    podArgumentCaptor.getAllValues().forEach(pod -> {
      assertEquals(requiredAnnotations, pod.getMetadata().getAnnotations());
    });

  }

  private int setUpDownscale(int distuptiblePods, int distance, MasterPosition masterPosition) {
    final int desiredReplicas = new Random().nextInt(10) + 1;
    requiredStatefulSet.getSpec().setReplicas(desiredReplicas);

    int masterIndex = getMasterIndex(desiredReplicas + 1, distuptiblePods, masterPosition);

    setUpPods(desiredReplicas + 1, distuptiblePods, distance, masterIndex);

    lenient().when(statefulSetWriter.update(requiredStatefulSet)).thenReturn(requiredStatefulSet);

    return desiredReplicas;
  }

  private int setUpUpscale(int distuptiblePods, int distance, MasterPosition masterPosition) {
    final int desiredReplicas = new Random().nextInt(10) + 1;
    requiredStatefulSet.getSpec().setReplicas(desiredReplicas);

    int masterIndex = getMasterIndex(desiredReplicas, distuptiblePods, masterPosition);

    setUpPods(desiredReplicas - 1, distuptiblePods, distance, masterIndex);

    lenient().when(statefulSetWriter.update(requiredStatefulSet)).thenReturn(requiredStatefulSet);

    return desiredReplicas;
  }

  private int getMasterIndex(int desiredReplicas, int distuptiblePods, MasterPosition masterPosition) {
    int masterIndex = 0;

    switch (masterPosition) {
      case LAST_DISRUPTABLE:
        masterIndex = desiredReplicas - distuptiblePods - 1;
        break;
      case FIRST_NONDISRUPTABLE:
        masterIndex = desiredReplicas - distuptiblePods;
        break;
    }
    return masterIndex;
  }

  private void setUpPods(int pods, int distuptiblePods, int distance, int masterIndex) {
    final ObjectMeta requiredStatefulSetMetadata = requiredStatefulSet.getMetadata();
    final Map<String, String> commonPodLables = requiredStatefulSet.getSpec().getSelector()
        .getMatchLabels();
    commonPodLables.remove(StackGresContext.DISRUPTIBLE_KEY);

    Map<String, String> disruptablePodLabels = new HashMap<>(commonPodLables);
    disruptablePodLabels.put(StackGresContext.DISRUPTIBLE_KEY, StackGresContext.RIGHT_VALUE);

    Map<String, String> nonDisruptablePodLables = new HashMap<>(disruptablePodLabels);
    nonDisruptablePodLables.put(StackGresContext.DISRUPTIBLE_KEY, StackGresContext.WRONG_VALUE);

    var listBuilder = ImmutableList.<Pod>builder();
    final int startPodIndex = pods - distuptiblePods;
    final int endPodIndex = startPodIndex + distuptiblePods;

    for (int i = 0; i < startPodIndex; i++) {
      listBuilder.add(new PodBuilder()
          .withNewMetadata()
          .withGenerateName(requiredStatefulSet.getMetadata().getName() + "-")
          .withNamespace(requiredStatefulSet.getMetadata().getNamespace())
          .withName(requiredStatefulSet.getMetadata().getName() + "-" + i)
          .withLabels(ImmutableMap.<String, String>builder()
              .putAll(disruptablePodLabels)
              .put(StackGresContext.ROLE_KEY,
                  i == masterIndex ? StackGresContext.PRIMARY_ROLE : StackGresContext.REPLICA_ROLE)
              .build())
          .endMetadata()
          .build());
    }


    for (int i = startPodIndex + distance; i < endPodIndex + distance; i++) {
      listBuilder.add(new PodBuilder()
          .withNewMetadata()
          .withGenerateName(requiredStatefulSet.getMetadata().getName() + "-")
          .withNamespace(requiredStatefulSet.getMetadata().getNamespace())
          .withName(requiredStatefulSet.getMetadata().getName() + "-" + i)
          .withLabels(ImmutableMap.<String, String>builder()
              .putAll(nonDisruptablePodLables)
              .put(StackGresContext.ROLE_KEY,
                  i == masterIndex ? StackGresContext.PRIMARY_ROLE : StackGresContext.REPLICA_ROLE)
              .build())
          .endMetadata()
          .build());
    }

    when(podScanner
        .findByLabelsAndNamespace(requiredStatefulSetMetadata.getNamespace(), commonPodLables))
        .thenReturn(listBuilder.build());
  }

  private enum MasterPosition {
    FIRST,
    LAST_DISRUPTABLE,
    FIRST_NONDISRUPTABLE;
  }
}

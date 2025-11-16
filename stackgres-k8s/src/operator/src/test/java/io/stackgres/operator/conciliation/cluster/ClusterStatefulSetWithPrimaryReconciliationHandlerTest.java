/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import static io.stackgres.operator.conciliation.cluster.ClusterStatefulSetWithPrimaryReconciliationHandler.PLACEHOLDER_NODE_SELECTOR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StringUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.ClusterLabelFactory;
import io.stackgres.common.labels.ClusterLabelMapper;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.patroni.PatroniCtl;
import io.stackgres.common.patroni.PatroniCtlInstance;
import io.stackgres.common.patroni.PatroniHistoryEntry;
import io.stackgres.common.patroni.PatroniMember;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.operatorframework.resource.ResourceUtil;
import io.stackgres.testutil.JsonUtil;
import io.stackgres.testutil.StringUtils;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class ClusterStatefulSetWithPrimaryReconciliationHandlerTest {

  protected static final Logger LOGGER = LoggerFactory.getLogger(
      ClusterStatefulSetWithPrimaryReconciliationHandlerTest.class);

  private final LabelFactoryForCluster labelFactory =
      new ClusterLabelFactory(new ClusterLabelMapper());

  @Mock
  private ResourceScanner<Pod> podScanner;

  @Mock
  private ResourceScanner<PersistentVolumeClaim> pvcScanner;

  @Mock
  private ResourceFinder<StatefulSet> statefulSetFinder;

  @Mock
  private ResourceFinder<Secret> secretFinder;

  @Mock
  private PatroniCtl patroniCtl;

  @Mock
  private PatroniCtlInstance patroniCtlInstance;

  @Mock
  private ClusterDefaultReconciliationHandler defaultHandler;

  @Mock
  private ClusterDefaultReconciliationHandler protectHandler;

  private ObjectMapper objectMapper = JsonUtil.jsonMapper();

  private ClusterStatefulSetWithPrimaryReconciliationHandler handler;

  private StackGresCluster cluster;

  private StatefulSet requiredStatefulSet;

  private StatefulSet deployedStatefulSet;

  private List<Pod> podList = new ArrayList<>();

  private List<PersistentVolumeClaim> pvcList = new ArrayList<>();

  @BeforeEach
  void setUp() {
    handler = new ClusterStatefulSetWithPrimaryReconciliationHandler(
        defaultHandler, protectHandler, labelFactory, statefulSetFinder,
        podScanner, pvcScanner, secretFinder, patroniCtl, objectMapper);
    requiredStatefulSet = Fixtures.statefulSet().loadRequired().get();

    cluster = new StackGresCluster();
    cluster.setMetadata(new ObjectMeta());
    cluster.getMetadata().setNamespace(requiredStatefulSet.getMetadata().getNamespace());
    cluster.getMetadata().setName(requiredStatefulSet.getMetadata().getName());
    cluster.setSpec(new StackGresClusterSpec());

    deployedStatefulSet = Fixtures.statefulSet().loadDeployed().get();
    lenient().when(defaultHandler.patch(any(), any(StatefulSet.class), any()))
        .then(invocationOnMock -> invocationOnMock.getArgument(1));
    lenient().when(patroniCtl.instanceFor(any()))
        .thenReturn(patroniCtlInstance);
  }

  @Test
  void createResource_shouldValidateTheResourceType() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> handler.create(cluster, new SecretBuilder()
            .addToData(StringUtil.generateRandom(), StringUtil.generateRandom())
            .build()));

    assertEquals("Resource must be a StatefulSet instance", ex.getMessage());

    verify(defaultHandler, never()).create(any(), any(StatefulSet.class));
    verify(defaultHandler, never()).patch(any(), any(Pod.class), any());
    verify(defaultHandler, never()).patch(any(), any(PersistentVolumeClaim.class), any());
  }

  @Test
  void createResource_shouldCreateTheResource() {
    when(defaultHandler.create(any(), any(StatefulSet.class)))
        .thenReturn(requiredStatefulSet);

    HasMetadata sts = handler.create(cluster, requiredStatefulSet);

    assertEquals(requiredStatefulSet, sts);
  }

  @Test
  @DisplayName("Scaling down StatefulSet without non disrputable Pod should result in the same"
      + " number of desired replicas")
  void scaleDownStatefulSetWithoutNonDisruptablePods_shouldResultInSameNumberOfDesiredReplicas() {
    final int desiredReplicas = setUpDownscale(1, false, 0, PrimaryPosition.FIRST);

    StatefulSet sts = (StatefulSet) handler.patch(
        cluster, requiredStatefulSet, deployedStatefulSet);

    assertEquals(desiredReplicas, sts.getSpec().getReplicas());

    verify(podScanner, times(4)).getResourcesInNamespaceWithLabels(anyString(), anyMap());
    verify(defaultHandler).patch(any(), any(StatefulSet.class), any());
    verify(defaultHandler, never()).patch(any(), any(Pod.class), any());
    verify(defaultHandler, never()).delete(any(), any(StatefulSet.class));
    verify(defaultHandler, never()).patch(any(), any(PersistentVolumeClaim.class), any());
  }

  @Test
  @DisplayName("Scaling up StatefulSet without non disrputable Pod should result in the same"
      + " number of desired replicas")
  void scaleUpWithoutNonDisrputablePods_shouldResultInTheSameNumberOfDesiredReplicas() {
    final int desiredReplicas = setUpUpscale(1, false, 0, PrimaryPosition.FIRST);

    StatefulSet sts = (StatefulSet) handler.patch(
        cluster, requiredStatefulSet, deployedStatefulSet);

    assertEquals(desiredReplicas, sts.getSpec().getReplicas());

    verify(podScanner, times(4)).getResourcesInNamespaceWithLabels(anyString(), anyMap());
    verify(defaultHandler).patch(any(), any(StatefulSet.class), any());
    verify(defaultHandler, never()).patch(any(), any(Pod.class), any());
    verify(defaultHandler, never()).delete(any(), any(StatefulSet.class));
    verify(defaultHandler, never()).patch(any(), any(PersistentVolumeClaim.class), any());
  }

  public static class Source implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context)
        throws Exception {
      return Seq.range(0, 100).map(i -> Arguments.of(i));
    }
  }

  @Test
  @DisplayName("Scaling up StatefulSet with non disrputable Pod with index lower than replicas"
      + " count should result in the same number of desired replicas and fix disruptable Label")
  void scaleUpWithIndexLowerThanReplicasCount_DesiredReplicasAndFixDisruptableLabel() {
    final int desiredReplicas = setUpUpscale(3, true, 0, PrimaryPosition.FIRST_NONDISRUPTABLE);

    ArgumentCaptor<HasMetadata> podArgumentCaptor = ArgumentCaptor.forClass(HasMetadata.class);

    lenient().when(defaultHandler.patch(any(), any(Pod.class), any()))
        .then(invocationOnMock -> invocationOnMock.getArgument(1));

    var history = List.of(new PatroniHistoryEntry());
    history.get(0).setNewLeader(
        this.podList.stream()
        .filter(pod -> pod.getMetadata().getLabels().get(PatroniUtil.ROLE_KEY)
            .equals(PatroniUtil.PRIMARY_ROLE))
        .findFirst().get().getMetadata().getName());
    when(patroniCtlInstance.history())
        .thenReturn(history);

    StatefulSet sts = (StatefulSet) handler.patch(
        cluster, requiredStatefulSet, deployedStatefulSet);

    assertEquals(desiredReplicas, sts.getSpec().getReplicas());

    verify(defaultHandler, atLeastOnce()).patch(any(), podArgumentCaptor.capture(), any());
    for (var updatedPod : podArgumentCaptor.getAllValues()
        .stream().filter(Pod.class::isInstance).toList()) {
      String disruptableValue = updatedPod.getMetadata().getLabels()
          .get(labelFactory.labelMapper().disruptableKey(cluster));

      assertEquals(StackGresContext.RIGHT_VALUE, disruptableValue);
    }

    verify(podScanner, times(5)).getResourcesInNamespaceWithLabels(anyString(), anyMap());
    verify(defaultHandler).patch(any(), any(StatefulSet.class), any());
    verify(defaultHandler, atMostOnce()).patch(any(), any(Pod.class), any());
    verify(defaultHandler, never()).delete(any(), any(StatefulSet.class));
    verify(defaultHandler, never()).patch(any(), any(PersistentVolumeClaim.class), any());
  }

  @Test
  @DisplayName("Scaling down StatefulSet without non disrputable Pod and primary Pod about"
      + " to be disrupted should result in the number of desired replicas minus one and make"
      + " the primary Pod non disruptable")
  void scaleDownPods_shouldResultDesiredReplicasMinusOneThePrimaryPodNonDisruptable() {
    final int desiredReplicas = setUpDownscale(1, false, 0, PrimaryPosition.LAST_DISRUPTABLE);

    when(defaultHandler.patch(any(), any(Pod.class), any()))
        .then(invocationOnMock -> invocationOnMock.getArgument(1));

    StatefulSet sts = (StatefulSet) handler.patch(
        cluster, requiredStatefulSet, deployedStatefulSet);

    assertEquals(desiredReplicas, sts.getSpec().getReplicas());

    verify(defaultHandler, times(1)).patch(any(), any(Pod.class), any());
    ArgumentCaptor<HasMetadata> podArgumentCaptor = ArgumentCaptor.forClass(HasMetadata.class);
    verify(defaultHandler, atLeastOnce()).patch(any(), podArgumentCaptor.capture(), any());
    var updatedPod = podArgumentCaptor.getAllValues().stream()
        .filter(Pod.class::isInstance).map(Pod.class::cast).findFirst().orElseThrow();

    String disruptableValue = updatedPod.getMetadata().getLabels()
        .get(labelFactory.labelMapper().disruptableKey(cluster));
    String podRole = updatedPod.getMetadata().getLabels()
        .get(PatroniUtil.ROLE_KEY);

    assertEquals(StackGresContext.WRONG_VALUE, disruptableValue);
    assertEquals(PatroniUtil.PRIMARY_ROLE, podRole);

    verify(podScanner, times(5)).getResourcesInNamespaceWithLabels(anyString(), anyMap());
    verify(defaultHandler).patch(any(), any(StatefulSet.class), any());
    verify(defaultHandler, never()).delete(any(), any(StatefulSet.class));
    verify(defaultHandler, never()).patch(any(), any(PersistentVolumeClaim.class), any());
  }

  @Test
  @DisplayName("Missing primary Pod with index bigger than replicas in patroni history"
      + " and distance of 1 should result in the number of desired replicas minus one and"
      + " make the primary Pod non disruptable")
  void missingPrimaryPod_shouldResultDesiredReplicasMinusOneThePrimaryPodNonDisruptable() {
    final int desiredReplicas = setUpNoScale(
        1, true, 1, PrimaryPosition.FIRST_NONDISRUPTABLE_MISSING);

    var history = List.of(new PatroniHistoryEntry());
    history.get(0).setNewLeader(
        requiredStatefulSet.getMetadata().getName()
        + "-" + (desiredReplicas));
    when(patroniCtlInstance.history())
        .thenReturn(history);
    when(defaultHandler.patch(any(), any(StatefulSet.class), any()))
        .then(invocationOnMock -> {
          int podIndex = desiredReplicas - 1;
          addPlaceholderPod(podIndex, false);
          return invocationOnMock.getArgument(1);
        })
        .then(invocationOnMock -> {
          int podIndex = desiredReplicas;
          addPrimaryPod(podIndex, false);
          return invocationOnMock.getArgument(1);
        })
        .then(invocationOnMock -> invocationOnMock.getArgument(1));

    StatefulSet sts = (StatefulSet) handler.patch(
        cluster, requiredStatefulSet, deployedStatefulSet);

    assertEquals(desiredReplicas - 1, sts.getSpec().getReplicas());

    verify(podScanner, times(7)).getResourcesInNamespaceWithLabels(anyString(), anyMap());
    verify(defaultHandler, times(3)).patch(any(), any(StatefulSet.class), any());
    verify(defaultHandler, times(1)).patch(any(), any(Pod.class), any());
    verify(defaultHandler, never()).delete(any(), any(StatefulSet.class));
    verify(defaultHandler, never()).patch(any(), any(PersistentVolumeClaim.class), any());
  }

  @Test
  @DisplayName("Primary Pod with index bigger than replicas and distance of 1 with a placeholder"
      + " Pod should result in the number of desired replicas and make the primary Pod"
      + " non disruptable")
  void primaryPodWithPlchldrPods_shouldResultDesiredReplicasMinusOneThePrimaryPodNonDisruptable() {
    final int desiredReplicas =
        setUpNoScaleWithPlaceholders(1, true, 1, PrimaryPosition.FIRST_NONDISRUPTABLE);

    var history = List.of(new PatroniHistoryEntry());
    history.get(0).setNewLeader(
        requiredStatefulSet.getMetadata().getName()
        + "-" + (desiredReplicas));
    when(patroniCtlInstance.history())
        .thenReturn(history);

    StatefulSet sts = (StatefulSet) handler.patch(
        cluster, requiredStatefulSet, deployedStatefulSet);

    assertEquals(desiredReplicas, sts.getSpec().getReplicas());

    verify(podScanner, times(5)).getResourcesInNamespaceWithLabels(anyString(), anyMap());
    verify(defaultHandler, times(1)).patch(any(), any(StatefulSet.class), any());
    verify(defaultHandler, times(1)).patch(any(), any(Pod.class), any());
    verify(defaultHandler, times(1)).delete(any(), any(Pod.class));
    verify(defaultHandler, never()).patch(any(), any(PersistentVolumeClaim.class), any());
  }

  @Test
  @DisplayName("Scaling down StatefulSet with non disrputable Pod and primary Pod non"
      + " disruptable about to be disrupted should result in the number of desired replicas"
      + " minus the disruptable Pod")
  void scaleDownNonDisrputablePodsPrimaryPodNonDisruptable_DesiredReplicasMinusDisruptablePods() {
    final int desiredReplicas = setUpDownscale(1, true, 0, PrimaryPosition.FIRST_NONDISRUPTABLE);

    StatefulSet sts = (StatefulSet) handler.patch(
        cluster, requiredStatefulSet, deployedStatefulSet);

    assertEquals(desiredReplicas, sts.getSpec().getReplicas());

    verify(podScanner, times(5)).getResourcesInNamespaceWithLabels(anyString(), anyMap());
    verify(defaultHandler).patch(any(), any(StatefulSet.class), any());
    verify(defaultHandler, never()).patch(any(), any(Pod.class), any());
    verify(defaultHandler, never()).delete(any(), any(StatefulSet.class));
    verify(defaultHandler, never()).patch(any(), any(PersistentVolumeClaim.class), any());
  }

  @Test
  @DisplayName("Scaling down StatefulSet with non disrputable Pod and primary Pod non"
      + " disruptable and distance bigger than 0 should result in the number of desired replicas"
      + " minus the disruptable Pod")
  void scaleDownNonDisputPodsPrimaryPodNonDisrupDistBig0_DesiredReplicasMinusTheDisruptablePods() {
    final int desiredReplicas = setUpDownscale(1, true, 1, PrimaryPosition.FIRST_NONDISRUPTABLE);

    StatefulSet sts = (StatefulSet) handler.patch(
        cluster, requiredStatefulSet, deployedStatefulSet);

    assertEquals(desiredReplicas, sts.getSpec().getReplicas());

    verify(podScanner, times(5)).getResourcesInNamespaceWithLabels(anyString(), anyMap());
    verify(defaultHandler).patch(any(), any(StatefulSet.class), any());
    verify(defaultHandler, never()).patch(any(), any(Pod.class), any());
    verify(defaultHandler, never()).delete(any(), any(StatefulSet.class));
    verify(defaultHandler, never()).patch(any(), any(PersistentVolumeClaim.class), any());
  }

  @Test
  void delete_shouldNotFail() {
    doNothing().when(defaultHandler).delete(any(), any(StatefulSet.class));

    handler.delete(cluster, requiredStatefulSet);

    verify(defaultHandler, times(1)).delete(any(), any(StatefulSet.class));
  }

  @Test
  void givenPodAnnotationChanges_shouldBeAppliedDirectlyToPods() {
    final int replicas = setUpNoScale(1, false, 0, PrimaryPosition.FIRST);

    final Map<String, String> requiredAnnotations = Map
        .of(StringUtils.getRandomString(), StringUtils.getRandomString(),
            "same-key", "new-value");
    requiredStatefulSet.getSpec().getTemplate().getMetadata().setAnnotations(requiredAnnotations);
    final var deployedAnnotations = Seq.seq(podList).map(pod -> Tuple
        .tuple(pod, Map
            .of(StringUtils.getRandomString(), StringUtils.getRandomString(),
                "same-key", "old-value")))
        .map(t -> {
          t.v1.getMetadata().setAnnotations(t.v2);
          return t;
        })
        .map(t -> t.map1(pod -> pod.getMetadata().getName()))
        .collect(ImmutableMap.toImmutableMap(Tuple2::v1, Tuple2::v2));

    when(defaultHandler.patch(any(), any(StatefulSet.class), any()))
        .thenReturn(requiredStatefulSet);

    handler.patch(cluster, requiredStatefulSet, deployedStatefulSet);

    verify(defaultHandler).patch(any(), any(StatefulSet.class), any());
    ArgumentCaptor<HasMetadata> statefulSetArgumentCaptor =
        ArgumentCaptor.forClass(HasMetadata.class);
    verify(defaultHandler, atLeastOnce()).patch(any(), statefulSetArgumentCaptor.capture(), any());
    assertEquals(requiredAnnotations,
        statefulSetArgumentCaptor.getAllValues().stream()
        .filter(StatefulSet.class::isInstance).map(StatefulSet.class::cast)
        .findFirst().orElseThrow().getSpec().getTemplate()
        .getMetadata().getAnnotations());

    verify(defaultHandler, times(replicas)).patch(any(), any(Pod.class), any());
    ArgumentCaptor<HasMetadata> podArgumentCaptor = ArgumentCaptor.forClass(HasMetadata.class);
    verify(defaultHandler, atLeastOnce()).patch(any(), podArgumentCaptor.capture(), any());
    podArgumentCaptor.getAllValues().stream().filter(Pod.class::isInstance).forEach(pod -> {
      assertEquals(Seq.seq(deployedAnnotations.get(pod.getMetadata().getName()))
          .filter(annotation -> !requiredAnnotations.containsKey(annotation.v1))
          .append(Seq.seq(requiredAnnotations))
          .collect(ImmutableMap.toImmutableMap(Tuple2::v1, Tuple2::v2)),
          pod.getMetadata().getAnnotations());
    });
  }

  @Test
  void givenPodLabelsChanges_shouldBeAppliedDirectlyToPods() {
    final int replicas = setUpNoScale(1, false, 0, PrimaryPosition.FIRST);

    final Map<String, String> requiredLabels = Seq.seq(Map
        .of(StringUtils.getRandomString(), StringUtils.getRandomString(),
            "same-key", "new-value"))
        .append(Seq.seq(requiredStatefulSet.getSpec().getSelector().getMatchLabels()))
        .toMap(Tuple2::v1, Tuple2::v2);
    requiredStatefulSet.getSpec().getSelector().setMatchLabels(requiredLabels);
    final var deployedLabels = Seq.seq(podList).map(pod -> Tuple
        .tuple(pod, Seq.seq(Map
            .of(StringUtils.getRandomString(), StringUtils.getRandomString(),
                "same-key", "old-value"))
            .append(Seq.seq(pod.getMetadata().getLabels()))
            .toMap(Tuple2::v1, Tuple2::v2)))
        .map(t -> {
          t.v1.getMetadata().setLabels(t.v2);
          return t;
        })
        .map(t -> t.map1(pod -> pod.getMetadata().getName()))
        .collect(ImmutableMap.toImmutableMap(Tuple2::v1, Tuple2::v2));

    when(defaultHandler.patch(any(), any(StatefulSet.class), any()))
        .thenReturn(requiredStatefulSet);

    handler.patch(cluster, requiredStatefulSet, deployedStatefulSet);

    verify(defaultHandler).patch(any(), any(StatefulSet.class), any());

    verify(defaultHandler, times(replicas)).patch(any(), any(Pod.class), any());
    ArgumentCaptor<HasMetadata> podArgumentCaptor = ArgumentCaptor.forClass(HasMetadata.class);
    verify(defaultHandler, atLeastOnce()).patch(any(), podArgumentCaptor.capture(), any());
    podArgumentCaptor.getAllValues().stream().filter(Pod.class::isInstance).forEach(pod -> {
      assertEquals(Seq.seq(deployedLabels.get(pod.getMetadata().getName()))
          .filter(label -> !requiredLabels.containsKey(label.v1))
          .append(Seq.seq(requiredLabels))
          .collect(ImmutableMap.toImmutableMap(Tuple2::v1, Tuple2::v2)),
          pod.getMetadata().getLabels());
    });
  }

  @Test
  void givenPodOwnerReferenceChanges_shouldBeAppliedDirectlyToPods(TestInfo testInfo) {
    setUpUpscale(2, false, 0, PrimaryPosition.FIRST);

    deployedStatefulSet.getMetadata().setUid(StringUtils.getRandomString());
    final List<OwnerReference> requiredOwnerReferences = List.of(
        ResourceUtil.getControllerOwnerReference(deployedStatefulSet));

    when(defaultHandler.patch(any(), any(StatefulSet.class), any()))
        .thenReturn(deployedStatefulSet);

    handler.patch(cluster, requiredStatefulSet, deployedStatefulSet);

    ArgumentCaptor<Pod> podArgumentCaptor = ArgumentCaptor.forClass(Pod.class);
    podArgumentCaptor.getAllValues().forEach(pod -> {
      assertEquals(requiredOwnerReferences, pod.getMetadata().getOwnerReferences());
    });
  }

  @Test
  void givenPvcAnnotationChanges_shouldBeAppliedDirectlyToPvcs() {
    final Map<String, String> requiredAnnotations = Map
        .of(StringUtils.getRandomString(), StringUtils.getRandomString(),
            "same-key", "new-value");

    final int desiredReplicas = setUpNoScale(1, false, 0, PrimaryPosition.FIRST);
    requiredStatefulSet.getSpec().getVolumeClaimTemplates().forEach(pvc -> pvc
        .getMetadata().setAnnotations(requiredAnnotations));
    final var deployedAnnotations = Seq.seq(pvcList).map(pvc -> Tuple
        .tuple(pvc, Map
            .of(StringUtils.getRandomString(), StringUtils.getRandomString(),
                "same-key", "old-value")))
        .map(t -> {
          t.v1.getMetadata().setAnnotations(t.v2);
          return t;
        })
        .map(t -> t.map1(pvc -> pvc.getMetadata().getName()))
        .collect(ImmutableMap.toImmutableMap(Tuple2::v1, Tuple2::v2));

    deployedStatefulSet.getSpec().getVolumeClaimTemplates().forEach(pvc -> pvc.getMetadata()
        .setAnnotations(
            Map.of(StringUtils.getRandomString(), StringUtils.getRandomString())));

    handler.patch(cluster, requiredStatefulSet, deployedStatefulSet);

    verify(defaultHandler).patch(any(), any(StatefulSet.class), any());
    ArgumentCaptor<HasMetadata> statefulSetArgumentCaptor =
        ArgumentCaptor.forClass(HasMetadata.class);
    verify(defaultHandler, atLeastOnce()).patch(any(), statefulSetArgumentCaptor.capture(), any());
    statefulSetArgumentCaptor.getAllValues().stream()
        .filter(StatefulSet.class::isInstance).map(StatefulSet.class::cast)
        .findFirst().orElseThrow().getSpec().getVolumeClaimTemplates()
        .forEach(pvc -> assertEquals(requiredAnnotations, pvc.getMetadata().getAnnotations()));

    verify(defaultHandler, times(desiredReplicas))
        .patch(any(), any(PersistentVolumeClaim.class), any());
    ArgumentCaptor<HasMetadata> pvcArgumentCaptor =
        ArgumentCaptor.forClass(HasMetadata.class);
    verify(defaultHandler, atLeastOnce()).patch(any(), pvcArgumentCaptor.capture(), any());
    pvcArgumentCaptor.getAllValues().stream().filter(PersistentVolumeClaim.class::isInstance)
        .forEach(pvc -> {
          assertEquals(Seq.seq(deployedAnnotations.get(pvc.getMetadata().getName()))
              .filter(annotation -> !requiredAnnotations.containsKey(annotation.v1))
              .append(Seq.seq(requiredAnnotations))
              .collect(ImmutableMap.toImmutableMap(Tuple2::v1, Tuple2::v2)),
              pvc.getMetadata().getAnnotations());
        });
  }

  @Test
  void givenPvcLabelsChanges_shouldBeAppliedDirectlyToPvcs() {
    final Map<String, String> requiredLabels = Map
        .of(StringUtils.getRandomString(), StringUtils.getRandomString(),
            "same-key", "new-value");

    final int desiredReplicas = setUpNoScale(1, false, 0, PrimaryPosition.FIRST);
    requiredStatefulSet.getSpec().getVolumeClaimTemplates().forEach(pvc -> pvc
        .getMetadata().setLabels(requiredLabels));
    final var deployedLabels = Seq.seq(pvcList).map(pvc -> Tuple
        .tuple(pvc, Seq.seq(Map
            .of(StringUtils.getRandomString(), StringUtils.getRandomString(),
                "same-key", "old-value"))
            .append(Seq.seq(pvc.getMetadata().getLabels()))
            .toMap(Tuple2::v1, Tuple2::v2)))
        .map(t -> {
          t.v1.getMetadata().setLabels(t.v2);
          return t;
        })
        .map(t -> t.map1(pvc -> pvc.getMetadata().getName()))
        .collect(ImmutableMap.toImmutableMap(Tuple2::v1, Tuple2::v2));

    deployedStatefulSet.getSpec().getVolumeClaimTemplates().forEach(pvc -> pvc.getMetadata()
        .setLabels(
            Map.of(StringUtils.getRandomString(), StringUtils.getRandomString())));

    handler.patch(cluster, requiredStatefulSet, deployedStatefulSet);

    verify(defaultHandler).patch(any(), any(StatefulSet.class), any());
    ArgumentCaptor<HasMetadata> statefulSetArgumentCaptor =
        ArgumentCaptor.forClass(HasMetadata.class);
    verify(defaultHandler, atLeastOnce()).patch(any(), statefulSetArgumentCaptor.capture(), any());
    statefulSetArgumentCaptor.getAllValues().stream()
        .filter(StatefulSet.class::isInstance).map(StatefulSet.class::cast)
        .findFirst().orElseThrow().getSpec().getVolumeClaimTemplates()
        .forEach(pvc -> assertEquals(requiredLabels, pvc.getMetadata().getLabels()));

    verify(defaultHandler, times(desiredReplicas))
        .patch(any(), any(PersistentVolumeClaim.class), any());
    ArgumentCaptor<HasMetadata> pvcArgumentCaptor =
        ArgumentCaptor.forClass(HasMetadata.class);
    verify(defaultHandler, atLeastOnce()).patch(any(), pvcArgumentCaptor.capture(), any());
    pvcArgumentCaptor.getAllValues().stream().filter(PersistentVolumeClaim.class::isInstance)
        .forEach(pvc -> {
          assertEquals(Seq.seq(deployedLabels.get(pvc.getMetadata().getName()))
              .filter(label -> !requiredLabels.containsKey(label.v1))
              .append(Seq.seq(requiredLabels))
              .collect(ImmutableMap.toImmutableMap(Tuple2::v1, Tuple2::v2)),
              pvc.getMetadata().getLabels());
        });
  }

  private int setUpNoScale(int min, boolean nonDisruptablePod, int distance,
      PrimaryPosition primaryPosition) {
    final int replicas = getRandomDesiredReplicas(min);

    setUpPods(replicas, nonDisruptablePod, true, distance, primaryPosition,
        false);

    setStatefulSetMocks(replicas, true);

    return replicas;
  }

  private int setUpNoScaleWithPlaceholders(int min, boolean nonDisruptablePod, int distance,
      PrimaryPosition primaryPosition) {
    final int replicas = getRandomDesiredReplicas(min);

    setUpPods(replicas + distance, nonDisruptablePod, false, distance,
        primaryPosition, true);

    setStatefulSetMocks(replicas, false);

    return replicas;
  }

  private int setUpDownscale(int min, boolean nonDisruptablePod, int distance,
      PrimaryPosition primaryPosition) {
    final int desiredReplicas = getRandomDesiredReplicas(min);

    setUpPods(desiredReplicas + 1, nonDisruptablePod, true, distance,
        primaryPosition, false);

    setStatefulSetMocks(desiredReplicas, false);

    return desiredReplicas;
  }

  private int setUpUpscale(int min, boolean nonDisruptablePod, int distance,
      PrimaryPosition primaryPosition) {
    final int desiredReplicas = getRandomDesiredReplicas(min);

    setUpPods(desiredReplicas - 1, nonDisruptablePod, true, distance,
        primaryPosition, false);

    setStatefulSetMocks(desiredReplicas, false);

    return desiredReplicas;
  }

  private void setStatefulSetMocks(final int desiredReplicas, boolean returnRequiredStatefulSet) {
    lenient().when(statefulSetFinder.findByNameAndNamespace(
        eq(requiredStatefulSet.getMetadata().getName()),
        eq(requiredStatefulSet.getMetadata().getNamespace())))
        .thenReturn(Optional.of(
            returnRequiredStatefulSet ? requiredStatefulSet : deployedStatefulSet));
    requiredStatefulSet.getSpec().setReplicas(desiredReplicas);
    lenient().when(defaultHandler.create(any(), any(StatefulSet.class)))
        .thenReturn(requiredStatefulSet);
    lenient().when(defaultHandler.patch(any(), any(StatefulSet.class), any()))
        .thenReturn(requiredStatefulSet);
  }

  private int getRandomDesiredReplicas(int min) {
    return new Random().nextInt(10) + min;
  }

  @SuppressWarnings("unchecked")
  private void setUpPods(int currentReplicas, boolean nonDisruptablePod,
      boolean afterDistanceNonDisruptable, int distance, PrimaryPosition primaryPosition,
      boolean withPlaceholders) {
    final int primaryIndex = getPrimaryIndex(
        currentReplicas, nonDisruptablePod, distance, primaryPosition);
    final Map<String, String> commonPodLabels = new HashMap<>(
        requiredStatefulSet.getSpec().getSelector().getMatchLabels());

    Map<String, String> disruptablePodLabels = new HashMap<>(commonPodLabels);
    disruptablePodLabels.put(
        labelFactory.labelMapper().disruptableKey(cluster), StackGresContext.RIGHT_VALUE);

    Map<String, String> nonDisruptablePodLabels = new HashMap<>(commonPodLabels);
    nonDisruptablePodLabels.put(
        labelFactory.labelMapper().disruptableKey(cluster), StackGresContext.WRONG_VALUE);

    podList.clear();
    final int placeholderStart = currentReplicas
        - (primaryPosition == PrimaryPosition.FIRST_NONDISRUPTABLE_MISSING ? distance : 0);
    final int placeholderEnd = currentReplicas + distance - 1;
    final int endPodIndex = nonDisruptablePod
        ? currentReplicas + distance : placeholderEnd;

    for (int podIndex = 0; podIndex <= endPodIndex; podIndex++) {
      if (!withPlaceholders
          && podIndex >= placeholderStart && podIndex <= placeholderEnd) {
        continue;
      }
      if (podIndex == primaryIndex
          && primaryPosition == PrimaryPosition.FIRST_NONDISRUPTABLE_MISSING) {
        continue;
      }
      addPod(podIndex, podIndex == primaryIndex,
          afterDistanceNonDisruptable && podIndex >= currentReplicas,
          withPlaceholders
          && podIndex > currentReplicas - 1 && podIndex <= placeholderEnd,
          true);
      addPvcs(podIndex);
    }

    lenient().when(podScanner
        .getResourcesInNamespaceWithLabels(any(), any()))
            .then(arguments -> {
              return podList
                  .stream()
                  .filter(pod -> ((Map<String, String>) arguments.getArgument(1))
                      .entrySet().stream().allMatch(label -> pod.getMetadata().getLabels()
                          .entrySet().stream().anyMatch(
                              podLabel -> podLabel.getKey().equals(label.getKey())
                              && podLabel.getValue().equals(label.getValue()))))
                  .toList();
            });

    lenient().when(pvcScanner
        .getResourcesInNamespaceWithLabels(any(), any()))
            .then(arguments -> {
              return pvcList
                  .stream()
                  .filter(pvc -> ((Map<String, String>) arguments.getArgument(1))
                      .entrySet().stream().allMatch(label -> pvc.getMetadata().getLabels()
                          .entrySet().stream().anyMatch(label::equals)))
                  .toList();
            });

    lenient().doAnswer(arguments -> {
      podList.remove(arguments.getArgument(0));
      return null;
    }).when(defaultHandler).delete(any(), any(Pod.class));

    lenient().when(patroniCtlInstance.list()).thenReturn(
        podList
        .stream()
        .map(pod -> {
          var member = new PatroniMember();
          member.setMember(pod.getMetadata().getName());
          if (Objects.equals(
              PatroniUtil.PRIMARY_ROLE,
              pod.getMetadata().getLabels().get(PatroniUtil.ROLE_KEY))) {
            member.setRole(PatroniMember.LEADER);
          }
          if (Objects.equals(
              PatroniUtil.REPLICA_ROLE,
              pod.getMetadata().getLabels().get(PatroniUtil.ROLE_KEY))) {
            member.setRole(PatroniMember.REPLICA);
          }
          return member;
        })
        .toList());
  }

  private int getPrimaryIndex(int desiredReplicas, boolean nonDisruptablePod,
      int distance, PrimaryPosition primaryPosition) {
    switch (primaryPosition) {
      case FIRST:
        return 0;
      case LAST_DISRUPTABLE:
        return desiredReplicas - 1;
      case FIRST_NONDISRUPTABLE:
      case FIRST_NONDISRUPTABLE_MISSING:
        return nonDisruptablePod
            ? desiredReplicas + distance : desiredReplicas + distance - 1;
      default:
        throw new IllegalArgumentException("Primary position " + primaryPosition + " unrecognized");
    }
  }

  private void addPlaceholderPod(int podIndex, boolean disruptable) {
    addPod(podIndex, false, disruptable, true, false);
  }

  private void addPrimaryPod(int podIndex, boolean disruptable) {
    addPod(podIndex, true, disruptable, false, false);
  }

  private void addPod(int podIndex, boolean primary, boolean nonDisruptable, boolean placeholder,
      boolean setRole) {
    LOGGER.info("Pod {} ({}{}{})",
        podIndex,
        primary ? "primary" : "replica",
        nonDisruptable ? " non disruptable" : "",
        placeholder ? " placeholder" : "");
    final Map<String, String> podLabels = new HashMap<>(
        requiredStatefulSet.getSpec().getSelector().getMatchLabels());
    podLabels.put(labelFactory.labelMapper().disruptableKey(cluster),
        nonDisruptable ? StackGresContext.WRONG_VALUE : StackGresContext.RIGHT_VALUE);
    if (!placeholder && setRole) {
      podLabels.put(PatroniUtil.ROLE_KEY,
          primary ? PatroniUtil.PRIMARY_ROLE : PatroniUtil.REPLICA_ROLE);
    }
    podList.add(new PodBuilder()
        .withNewMetadata()
        .withGenerateName(requiredStatefulSet.getMetadata().getName() + "-")
        .withNamespace(requiredStatefulSet.getMetadata().getNamespace())
        .withName(requiredStatefulSet.getMetadata().getName() + "-" + podIndex)
        .withLabels(ImmutableMap.<String, String>builder()
            .putAll(podLabels)
            .build())
        .withOwnerReferences(List.of(
            ResourceUtil.getControllerOwnerReference(requiredStatefulSet),
            ResourceUtil.getOwnerReference(cluster)))
        .endMetadata()
        .withNewSpec()
        .withNodeSelector(
            placeholder ? PLACEHOLDER_NODE_SELECTOR : ImmutableMap.of())
        .endSpec()
        .build());
  }

  private void addPvcs(int podIndex) {
    requiredStatefulSet.getSpec().getVolumeClaimTemplates()
        .forEach(pvc -> addPvc(podIndex, pvc.getMetadata()));
  }

  private void addPvc(int podIndex, ObjectMeta pvcMetadata) {
    pvcList.add(new PersistentVolumeClaimBuilder()
        .withNewMetadata()
        .withNamespace(requiredStatefulSet.getMetadata().getNamespace())
        .withName(pvcMetadata.getName() + "-"
            + requiredStatefulSet.getMetadata().getName() + "-" + podIndex)
        .withLabels(ImmutableMap.<String, String>builder()
            .putAll(pvcMetadata.getLabels())
            .build())
        .withOwnerReferences(List.of(
            ResourceUtil.getControllerOwnerReference(requiredStatefulSet),
            ResourceUtil.getOwnerReference(cluster)))
        .endMetadata()
        .build());
  }

  private enum PrimaryPosition {
    FIRST,
    LAST_DISRUPTABLE,
    FIRST_NONDISRUPTABLE,
    FIRST_NONDISRUPTABLE_MISSING;
  }
}

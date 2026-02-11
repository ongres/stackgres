/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;

import java.util.List;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.stackgres.common.crd.Condition;
import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.crd.sgstream.StackGresStreamStatus;
import io.stackgres.common.crd.sgstream.StreamStatusCondition;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.LabelFactoryForStream;
import io.stackgres.common.labels.StreamLabelFactory;
import io.stackgres.common.labels.StreamLabelMapper;
import io.stackgres.operator.conciliation.stream.StackGresStreamContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StreamPersistentVolumeClaimTest {

  private final LabelFactoryForStream labelFactory =
      new StreamLabelFactory(new StreamLabelMapper());

  @Mock
  private StackGresStreamContext context;

  private StreamPersistentVolumeClaim streamPersistentVolumeClaim;

  private StackGresStream stream;

  @BeforeEach
  void setUp() {
    streamPersistentVolumeClaim = new StreamPersistentVolumeClaim(labelFactory);
    stream = Fixtures.stream().loadSgClusterToCloudEvent().get();
    lenient().when(context.getSource()).thenReturn(stream);
  }

  @Test
  void generateResource_whenStreamNotCompleted_shouldGeneratePvc() {
    List<HasMetadata> resources = streamPersistentVolumeClaim.generateResource(context).toList();

    assertEquals(1, resources.size());
    assertTrue(resources.get(0) instanceof PersistentVolumeClaim);
  }

  @Test
  void generateResource_whenStreamAlreadyCompleted_shouldReturnEmpty() {
    StackGresStreamStatus status = new StackGresStreamStatus();
    Condition completedCondition = new Condition(
        StreamStatusCondition.Type.COMPLETED.getType(),
        StreamStatusCondition.Status.TRUE.getStatus(),
        "OperationCompleted");
    status.setConditions(List.of(completedCondition));
    stream.setStatus(status);

    List<HasMetadata> resources = streamPersistentVolumeClaim.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

  @Test
  void generateResource_whenStreamFailed_shouldReturnEmpty() {
    StackGresStreamStatus status = new StackGresStreamStatus();
    Condition failedCondition = new Condition(
        StreamStatusCondition.Type.FAILED.getType(),
        StreamStatusCondition.Status.TRUE.getStatus(),
        "OperationFailed");
    status.setConditions(List.of(failedCondition));
    stream.setStatus(status);

    List<HasMetadata> resources = streamPersistentVolumeClaim.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

  @Test
  void generateResource_shouldUseCorrectSizeFromSpec() {
    stream.getSpec().getPods().getPersistentVolume().setSize("5Gi");

    List<HasMetadata> resources = streamPersistentVolumeClaim.generateResource(context).toList();

    assertEquals(1, resources.size());
    PersistentVolumeClaim pvc = (PersistentVolumeClaim) resources.get(0);
    assertNotNull(pvc.getSpec().getResources());
    String storageRequest = pvc.getSpec().getResources().getRequests()
        .get("storage").toString();
    assertEquals("5Gi", storageRequest);
  }

  @Test
  void generateResource_shouldSetStorageClassWhenProvided() {
    stream.getSpec().getPods().getPersistentVolume().setSize("1Gi");
    stream.getSpec().getPods().getPersistentVolume().setStorageClass("fast-storage");

    List<HasMetadata> resources = streamPersistentVolumeClaim.generateResource(context).toList();

    assertEquals(1, resources.size());
    PersistentVolumeClaim pvc = (PersistentVolumeClaim) resources.get(0);
    assertEquals("fast-storage", pvc.getSpec().getStorageClassName());
  }

  @Test
  void generateResource_shouldSetCorrectNameAndNamespace() {
    List<HasMetadata> resources = streamPersistentVolumeClaim.generateResource(context).toList();

    assertEquals(1, resources.size());
    PersistentVolumeClaim pvc = (PersistentVolumeClaim) resources.get(0);
    assertEquals(stream.getMetadata().getNamespace(), pvc.getMetadata().getNamespace());
    assertEquals(stream.getMetadata().getName(), pvc.getMetadata().getName());
  }

  @Test
  void generateResource_shouldSetAccessModeToReadWriteOnce() {
    List<HasMetadata> resources = streamPersistentVolumeClaim.generateResource(context).toList();

    assertEquals(1, resources.size());
    PersistentVolumeClaim pvc = (PersistentVolumeClaim) resources.get(0);
    assertTrue(pvc.getSpec().getAccessModes().contains("ReadWriteOnce"));
  }

}

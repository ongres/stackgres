/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.replicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StackGresVolume;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.ClusterLabelFactory;
import io.stackgres.common.labels.ClusterLabelMapper;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.VolumePair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReplicateConfigMapTest {

  private final LabelFactoryForCluster labelFactory =
      new ClusterLabelFactory(new ClusterLabelMapper());

  @Mock
  private StackGresClusterContext context;

  private ReplicateConfigMap replicateConfigMap;

  private StackGresCluster cluster;

  @BeforeEach
  void setUp() {
    replicateConfigMap = new ReplicateConfigMap();
    replicateConfigMap.setLabelFactory(labelFactory);
    cluster = Fixtures.cluster().loadDefault().get();
    lenient().when(context.getSource()).thenReturn(cluster);
    lenient().when(context.getCluster()).thenReturn(cluster);
    lenient().when(context.getReplicateStorage()).thenReturn(Optional.empty());
    lenient().when(context.getReplicateConfiguration()).thenReturn(Optional.empty());
    lenient().when(context.getPodDataPersistentVolumeNames()).thenReturn(Map.of());
  }

  @Test
  void buildVolumes_whenReplicateStorageEmpty_shouldReturnConfigMapWithMinimalData() {
    when(context.getReplicateStorage()).thenReturn(Optional.empty());
    when(context.getReplicateConfiguration()).thenReturn(Optional.empty());

    List<VolumePair> pairs = replicateConfigMap.buildVolumes(context).toList();

    assertEquals(1, pairs.size());
    VolumePair pair = pairs.get(0);
    assertTrue(pair.getSource().isPresent());
    ConfigMap configMap = (ConfigMap) pair.getSource().get();
    assertNotNull(configMap.getData());
    assertTrue(configMap.getData().containsKey(StackGresUtil.MD5SUM_KEY));
    assertTrue(configMap.getData().containsKey(StackGresUtil.MD5SUM_2_KEY));
  }

  @Test
  void buildVolumes_shouldReturnVolumePairWithCorrectConfigMapName() {
    List<VolumePair> pairs = replicateConfigMap.buildVolumes(context).toList();

    assertEquals(1, pairs.size());
    VolumePair pair = pairs.get(0);

    String expectedName =
        StackGresVolume.REPLICATE_ENV.getResourceName(cluster.getMetadata().getName());
    assertEquals(StackGresVolume.REPLICATE_ENV.getName(), pair.getVolume().getName());
    assertEquals(expectedName, pair.getVolume().getConfigMap().getName());

    assertTrue(pair.getSource().isPresent());
    ConfigMap configMap = (ConfigMap) pair.getSource().get();
    assertEquals(expectedName, configMap.getMetadata().getName());
    assertEquals(cluster.getMetadata().getNamespace(),
        configMap.getMetadata().getNamespace());
    assertFalse(configMap.getMetadata().getLabels().isEmpty());
  }
}

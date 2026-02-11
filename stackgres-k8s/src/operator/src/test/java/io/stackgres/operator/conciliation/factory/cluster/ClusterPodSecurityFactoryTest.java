/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.fabric8.kubernetes.api.model.PodSecurityContext;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.PodSecurityFactory;
import io.stackgres.operator.configuration.OperatorPropertyContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClusterPodSecurityFactoryTest {

  @Mock
  private OperatorPropertyContext operatorContext;

  @Mock
  private StackGresClusterContext clusterContext;

  private StackGresCluster cluster;

  @BeforeEach
  void setUp() {
    cluster = Fixtures.cluster().loadDefault().get();
    when(clusterContext.getSource()).thenReturn(cluster);
  }

  @Test
  void createResource_whenNotUsingArbitraryUser_shouldSetRunAsNonRoot() {
    when(operatorContext.getBoolean(eq(OperatorProperty.USE_ARBITRARY_USER))).thenReturn(false);

    ClusterPodSecurityFactory factory = new ClusterPodSecurityFactory(operatorContext);
    PodSecurityContext podSecurityContext = factory.createResource(clusterContext);

    assertNotNull(podSecurityContext);
    assertTrue(podSecurityContext.getRunAsNonRoot());
  }

  @Test
  void createResource_whenNotUsingArbitraryUser_shouldSetUserAndGroupTo999() {
    when(operatorContext.getBoolean(eq(OperatorProperty.USE_ARBITRARY_USER))).thenReturn(false);

    ClusterPodSecurityFactory factory = new ClusterPodSecurityFactory(operatorContext);
    PodSecurityContext podSecurityContext = factory.createResource(clusterContext);

    assertEquals(PodSecurityFactory.USER, podSecurityContext.getRunAsUser());
    assertEquals(PodSecurityFactory.GROUP, podSecurityContext.getRunAsGroup());
    assertEquals(PodSecurityFactory.GROUP, podSecurityContext.getFsGroup());
  }

  @Test
  void createResource_whenNotUsingArbitraryUser_shouldUseExpectedUidGid() {
    when(operatorContext.getBoolean(eq(OperatorProperty.USE_ARBITRARY_USER))).thenReturn(false);

    ClusterPodSecurityFactory factory = new ClusterPodSecurityFactory(operatorContext);
    PodSecurityContext podSecurityContext = factory.createResource(clusterContext);

    assertEquals(999L, podSecurityContext.getRunAsUser());
    assertEquals(999L, podSecurityContext.getRunAsGroup());
    assertEquals(999L, podSecurityContext.getFsGroup());
  }

  @Test
  void createResource_whenUsingArbitraryUser_shouldSetRunAsNonRoot() {
    when(operatorContext.getBoolean(eq(OperatorProperty.USE_ARBITRARY_USER))).thenReturn(true);

    ClusterPodSecurityFactory factory = new ClusterPodSecurityFactory(operatorContext);
    PodSecurityContext podSecurityContext = factory.createResource(clusterContext);

    assertNotNull(podSecurityContext);
    assertTrue(podSecurityContext.getRunAsNonRoot());
  }

  @Test
  void createResource_whenUsingArbitraryUser_shouldNotSetUserGroupOrFsGroup() {
    when(operatorContext.getBoolean(eq(OperatorProperty.USE_ARBITRARY_USER))).thenReturn(true);

    ClusterPodSecurityFactory factory = new ClusterPodSecurityFactory(operatorContext);
    PodSecurityContext podSecurityContext = factory.createResource(clusterContext);

    assertNull(podSecurityContext.getRunAsUser());
    assertNull(podSecurityContext.getRunAsGroup());
    assertNull(podSecurityContext.getFsGroup());
  }

}

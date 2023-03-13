/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.MockitoAnnotations.openMocks;

import io.fabric8.kubernetes.api.model.PodSecurityContext;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.labels.LabelMapperForCluster;
import io.stackgres.operator.conciliation.ContainerFactoryDiscoverer;
import io.stackgres.operator.conciliation.InitContainerFactoryDiscover;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import io.stackgres.operator.conciliation.factory.ResourceFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class DistributedLogsPodTemplateSpecFactoryTest {

  @Mock
  ResourceFactory<StackGresDistributedLogsContext, PodSecurityContext> podSecurityContext;
  @Mock
  LabelFactoryForCluster<StackGresDistributedLogs> labelFactory;
  @Mock
  ContainerFactoryDiscoverer<DistributedLogsContainerContext> containerFactoryDiscoverer;
  @Mock
  InitContainerFactoryDiscover<DistributedLogsContainerContext> initContainerFactoryDiscoverer;
  @Mock
  private DistributedLogsContainerContext context;
  @Mock
  private StackGresDistributedLogsContext sgDistributedLogContext;
  @Mock
  private LabelMapperForCluster<StackGresDistributedLogs> labelMapperForCluster;
  private StackGresDistributedLogs distributedLogs;
  private DistributedLogsPodTemplateSpecFactory distributedLogPodTemplateSpecFactory;

  @BeforeEach
  public void setup() {
    openMocks(this);
    distributedLogs = Fixtures.distributedLogs().loadDefault().get();

    distributedLogPodTemplateSpecFactory =
        new DistributedLogsPodTemplateSpecFactory(podSecurityContext, labelFactory,
            containerFactoryDiscoverer, initContainerFactoryDiscoverer);
  }

  @Test
  public void shouldDistributedLogPodSpecHasNodeSelector_onceDistributedLogsHasNodeSelectors() {
    given(context.getDistributedLogsContext()).willReturn(sgDistributedLogContext);
    given(sgDistributedLogContext.getSource()).willReturn(distributedLogs);
    given(labelFactory.labelMapper()).willReturn(labelMapperForCluster);

    var podTemplateSpec = distributedLogPodTemplateSpecFactory.getPodTemplateSpec(context);
    assertEquals(2, podTemplateSpec.getSpec().getSpec().getNodeSelector().size());
  }

  @Test
  public void shouldDistributedLogPodSpecHasNodeAffinity_onceDistributedLogsHasNodeAffinity() {
    given(context.getDistributedLogsContext()).willReturn(sgDistributedLogContext);
    given(sgDistributedLogContext.getSource()).willReturn(distributedLogs);
    given(labelFactory.labelMapper()).willReturn(labelMapperForCluster);

    var podTemplateSpec = distributedLogPodTemplateSpecFactory.getPodTemplateSpec(context);
    var nodeAffinity = podTemplateSpec.getSpec().getSpec().getAffinity().getNodeAffinity();
    assertEquals(1, nodeAffinity.getPreferredDuringSchedulingIgnoredDuringExecution().size());
    assertEquals(1, nodeAffinity.getRequiredDuringSchedulingIgnoredDuringExecution()
        .getNodeSelectorTerms().size());
  }
}

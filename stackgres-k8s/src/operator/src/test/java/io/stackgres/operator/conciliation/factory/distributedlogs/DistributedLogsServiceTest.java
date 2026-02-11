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
import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Service;
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
class DistributedLogsServiceTest {

  private final LabelFactoryForDistributedLogs labelFactory =
      new DistributedLogsLabelFactory(new DistributedLogsLabelMapper());

  @Mock
  private StackGresDistributedLogsContext context;

  private DistributedLogsService distributedLogsService;

  private StackGresDistributedLogs distributedLogs;

  @BeforeEach
  void setUp() {
    distributedLogsService = new DistributedLogsService(labelFactory);
    distributedLogs = Fixtures.distributedLogs().loadDefault().get();
    when(context.getSource()).thenReturn(distributedLogs);
    when(context.getCluster()).thenReturn(Optional.empty());
  }

  @Test
  void generateResource_shouldGenerateServiceWithExternalNameType() {
    List<HasMetadata> resources =
        distributedLogsService.generateResource(context).toList();

    assertEquals(1, resources.size());
    assertTrue(resources.getFirst() instanceof Service);
    Service service = (Service) resources.getFirst();
    assertEquals("ExternalName", service.getSpec().getType());
  }

  @Test
  void generateResource_shouldHaveCorrectExternalNameFormat() {
    List<HasMetadata> resources =
        distributedLogsService.generateResource(context).toList();

    Service service = (Service) resources.getFirst();
    assertNotNull(service.getSpec().getExternalName());
    String externalName = service.getSpec().getExternalName();
    assertTrue(externalName.contains("distributed-logs"),
        "Expected external name to contain the namespace 'distributed-logs' but was: "
        + externalName);
    assertTrue(externalName.contains("distributedlogs"),
        "Expected external name to reference the cluster name but was: "
        + externalName);
  }

  @Test
  void generateResource_shouldHaveCorrectServiceName() {
    List<HasMetadata> resources =
        distributedLogsService.generateResource(context).toList();

    Service service = (Service) resources.getFirst();
    assertEquals(DistributedLogsService.serviceName(distributedLogs),
        service.getMetadata().getName());
    assertEquals("distributed-logs", service.getMetadata().getNamespace());
  }

}

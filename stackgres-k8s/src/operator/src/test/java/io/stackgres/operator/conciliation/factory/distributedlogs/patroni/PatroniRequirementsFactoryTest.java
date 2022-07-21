/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs.patroni;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Random;

import io.fabric8.kubernetes.api.model.Quantity;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsNonProduction;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgprofile.StackGresProfileHugePages;
import io.stackgres.common.crd.sgprofile.StackGresProfileRequests;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PatroniRequirementsFactoryTest {

  private PatroniRequirementsFactory patroniRequirementsFactory;

  @Mock
  private StackGresDistributedLogsContext context;

  private StackGresDistributedLogs distributedLogs;

  private StackGresProfile profile;

  @BeforeEach
  void setUp() {
    patroniRequirementsFactory = new PatroniRequirementsFactory();
    distributedLogs = JsonUtil.readFromJson("distributedlogs/default.json",
        StackGresDistributedLogs.class);
    profile = JsonUtil.readFromJson("stackgres_profiles/size-s.json", StackGresProfile.class);
  }

  @Test
  void givenACluster_itShouldCreateTheResourceWithCpuAndMemory() {
    when(context.getSource()).thenReturn(distributedLogs);
    when(context.getProfile()).thenReturn(profile);

    var requirements = patroniRequirementsFactory.createResource(context);

    final Map<String, Quantity> limits = requirements.getLimits();
    assertTrue(limits.containsKey("cpu"));
    assertTrue(limits.containsKey("memory"));

    assertEquals(new Quantity(profile.getSpec().getCpu()), limits.get("cpu"));
    assertEquals(new Quantity(profile.getSpec().getMemory()), limits.get("memory"));

    final Map<String, Quantity> requests = requirements.getRequests();
    assertTrue(requests.containsKey("cpu"));
    assertTrue(requests.containsKey("memory"));

    assertEquals(new Quantity(profile.getSpec().getCpu()), requests.get("cpu"));
    assertEquals(new Quantity(profile.getSpec().getMemory()), requests.get("memory"));
  }

  @Test
  void givenAClusterWithDisabledResource_itShouldCreateTheResourceWithoutCpuAndMemory() {
    distributedLogs.getSpec().setNonProductionOptions(new StackGresDistributedLogsNonProduction());
    distributedLogs.getSpec().getNonProductionOptions().setDisablePatroniResourceRequirements(true);
    when(context.getSource()).thenReturn(distributedLogs);

    var requirements = patroniRequirementsFactory.createResource(context);

    assertNull(requirements);
  }

  @Test
  void givenAClusterWithEnabledSetResources_itShouldCreateTheResourceWithCpuAndMemory() {
    distributedLogs.getSpec().setNonProductionOptions(new StackGresDistributedLogsNonProduction());
    distributedLogs.getSpec().getNonProductionOptions().setEnableSetPatroniCpuRequests(true);
    distributedLogs.getSpec().getNonProductionOptions().setEnableSetPatroniMemoryRequests(true);
    profile.getSpec().setRequests(new StackGresProfileRequests());
    profile.getSpec().getRequests().setCpu(new Random().nextInt(32000) + "m");
    profile.getSpec().getRequests().setMemory(new Random().nextInt(32) + "Gi");
    when(context.getSource()).thenReturn(distributedLogs);
    when(context.getProfile()).thenReturn(profile);

    var requirements = patroniRequirementsFactory.createResource(context);

    final Map<String, Quantity> limits = requirements.getLimits();
    assertTrue(limits.containsKey("cpu"));
    assertTrue(limits.containsKey("memory"));

    assertEquals(new Quantity(profile.getSpec().getCpu()), limits.get("cpu"));
    assertEquals(new Quantity(profile.getSpec().getMemory()), limits.get("memory"));

    final Map<String, Quantity> requests = requirements.getRequests();
    assertTrue(requests.containsKey("cpu"));
    assertTrue(requests.containsKey("memory"));

    assertEquals(new Quantity(profile.getSpec().getRequests().getCpu()), requests.get("cpu"));
    assertEquals(new Quantity(profile.getSpec().getRequests().getMemory()), requests.get("memory"));
  }

  @Test
  void givenAClusterWithAProfileWithHugePages_itShouldCreateTheResourceWithHugePages() {
    profile.getSpec().setHugePages(new StackGresProfileHugePages());
    profile.getSpec().getHugePages().setHugepages2Mi("2Mi");
    profile.getSpec().getHugePages().setHugepages1Gi("1Gi");
    when(context.getSource()).thenReturn(distributedLogs);
    when(context.getProfile()).thenReturn(profile);

    var requirements = patroniRequirementsFactory.createResource(context);

    final Map<String, Quantity> limits = requirements.getLimits();
    assertTrue(limits.containsKey("hugepages-2Mi"));
    assertTrue(limits.containsKey("hugepages-1Gi"));

    assertEquals(new Quantity(profile.getSpec().getHugePages().getHugepages2Mi()),
        limits.get("hugepages-2Mi"));
    assertEquals(new Quantity(profile.getSpec().getHugePages().getHugepages1Gi()),
        limits.get("hugepages-1Gi"));

    final Map<String, Quantity> requests = requirements.getRequests();
    assertTrue(requests.containsKey("hugepages-2Mi"));
    assertTrue(requests.containsKey("hugepages-1Gi"));

    assertEquals(new Quantity(profile.getSpec().getHugePages().getHugepages2Mi()),
        requests.get("hugepages-2Mi"));
    assertEquals(new Quantity(profile.getSpec().getHugePages().getHugepages1Gi()),
        requests.get("hugepages-1Gi"));
  }

}

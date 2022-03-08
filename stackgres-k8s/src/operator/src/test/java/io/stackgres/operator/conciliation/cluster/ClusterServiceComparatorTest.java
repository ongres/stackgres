/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.stackgres.common.fixture.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClusterServiceComparatorTest {

  private final ClusterServiceComparator comparator = new ClusterServiceComparator();

  private Service required;
  private Service deployed;

  @BeforeEach
  void setUp() {
    required = Fixtures.service().loadRequired().get();
    deployed = Fixtures.service().loadDeployed().get();
  }

  @Test
  void generatedResourceAndRequiredResource_shouldHaveNoDifference() {

    var isContentEqual = comparator.isResourceContentEqual(required, deployed);

    assertTrue(isContentEqual);
  }

  @Test
  void exactlyTheSameContent_shouldReturnTrue() {

    boolean isTheSameContent = comparator.isResourceContentEqual(
        new ServiceBuilder()
            .withNewMetadata().withNamespace("test")
            .withName("test")
            .endMetadata()
            .withNewSpec()
            .withClusterIP("None")
            .withSessionAffinity("None")
            .withType("ClusterIP")
            .endSpec()
            .build(),
        new ServiceBuilder()
            .withNewMetadata().withNamespace("test")
            .withName("test")
            .endMetadata()
            .withNewSpec()
            .withClusterIP("None")
            .withSessionAffinity("None")
            .withType("ClusterIP")
            .endSpec()
            .build());

    assertTrue(isTheSameContent);

  }

  @Test
  void ifHasNoType_shouldIgnoreIt() {

    boolean isTheSameContent = comparator.isResourceContentEqual(
        new ServiceBuilder()
            .withNewMetadata().withNamespace("test")
            .withName("test")
            .endMetadata()
            .withNewSpec()
            .withClusterIP("None")
            .withSessionAffinity("None")
            .endSpec()
            .build(),
        new ServiceBuilder()
            .withNewMetadata().withNamespace("test")
            .withName("test")
            .endMetadata()
            .withNewSpec()
            .withClusterIP("None")
            .withSessionAffinity("None")
            .withType("ClusterIP")
            .endSpec()
            .build());

    assertTrue(isTheSameContent);

  }

  @Test
  void ifSessionAffinityIsNone_shouldIgnoreIt() {

    boolean isTheSameContent = comparator.isResourceContentEqual(
        new ServiceBuilder()
            .withNewMetadata().withNamespace("test")
            .withName("test")
            .endMetadata()
            .withNewSpec()
            .withClusterIP("None")
            .withType("ClusterIP")
            .endSpec()
            .build(),
        new ServiceBuilder()
            .withNewMetadata().withNamespace("test")
            .withName("test")
            .endMetadata()
            .withNewSpec()
            .withClusterIP("None")
            .withType("ClusterIP")
            .withSessionAffinity("None")
            .endSpec()
            .build());

    assertTrue(isTheSameContent);

  }

  @Test
  void ifClusterIpIsNone_shouldNotIgnoreIt() {

    boolean isTheSameContent = comparator.isResourceContentEqual(
        new ServiceBuilder()
            .withNewMetadata().withNamespace("test")
            .withName("test")
            .endMetadata()
            .withNewSpec()
            .withSessionAffinity("None")
            .withType("ClusterIP")
            .endSpec()
            .build(),
        new ServiceBuilder()
            .withNewMetadata().withNamespace("test")
            .withName("test")
            .endMetadata()
            .withNewSpec()
            .withClusterIP("None")
            .withSessionAffinity("None")
            .withType("ClusterIP")
            .endSpec()
            .build());

    assertFalse(isTheSameContent);
  }

  @Test
  void ifClusterIpHasAnIp_shouldBeIgnoredIfNotSet() {

    boolean isTheSameContent = comparator.isResourceContentEqual(
        new ServiceBuilder()
            .withNewMetadata().withNamespace("test")
            .withName("test")
            .endMetadata()
            .withNewSpec()
            .withSessionAffinity("None")
            .withType("ClusterIP")
            .endSpec()
            .build(),
        new ServiceBuilder()
            .withNewMetadata().withNamespace("test")
            .withName("test")
            .endMetadata()
            .withNewSpec()
            .withClusterIP("127.0.0.1")
            .withSessionAffinity("None")
            .withType("ClusterIP")
            .endSpec()
            .build());

    assertTrue(isTheSameContent);

  }

  @Test
  void ifClusterIpHasAnIp_shouldNotBeIgnored() {

    boolean isTheSameContent = comparator.isResourceContentEqual(
        new ServiceBuilder()
            .withNewMetadata().withNamespace("test")
            .withName("test")
            .endMetadata()
            .withNewSpec()
            .withClusterIP("127.0.0.1")
            .withSessionAffinity("None")
            .withType("ClusterIP")
            .endSpec()
            .build(),
        new ServiceBuilder()
            .withNewMetadata().withNamespace("test")
            .withName("test")
            .endMetadata()
            .withNewSpec()
            .withClusterIP("127.0.0.2")
            .withSessionAffinity("None")
            .withType("ClusterIP")
            .endSpec()
            .build());

    assertFalse(isTheSameContent);

  }

  @Test
  void serviceStatus_shouldBeIgnored() {

    boolean isTheSameContent = comparator.isResourceContentEqual(
        new ServiceBuilder()
            .withNewMetadata().withNamespace("test")
            .withName("test")
            .endMetadata()
            .withNewSpec()
            .withClusterIP("None")
            .withSessionAffinity("None")
            .withType("ClusterIP")
            .endSpec()
            .build(),
        new ServiceBuilder()
            .withNewMetadata().withNamespace("test")
            .withName("test")
            .endMetadata()
            .withNewSpec()
            .withClusterIP("None")
            .withSessionAffinity("None")
            .withType("ClusterIP")
            .endSpec()
            .withNewStatus().withNewLoadBalancer().endLoadBalancer().endStatus()
            .build());

    assertTrue(isTheSameContent);
  }

  @Test
  void semanticallyDifferentServices_shouldBeReturnFalse() {

    boolean isTheSameContent = comparator.isResourceContentEqual(
        new ServiceBuilder()
            .withNewMetadata().withNamespace("test")
            .withName("test")
            .endMetadata()
            .withNewSpec()
            .withClusterIP("None")
            .withSessionAffinity("None")
            .withType("LoadBalancer")
            .endSpec()
            .withNewStatus().withNewLoadBalancer().endLoadBalancer().endStatus()
            .build(),
        new ServiceBuilder()
            .withNewMetadata().withNamespace("test")
            .withName("test")
            .endMetadata()
            .withNewSpec()
            .withClusterIP("None")
            .withSessionAffinity("None")
            .withType("ClusterIP")
            .endSpec()
            .build());

    assertFalse(isTheSameContent);
  }

  @Test
  void isResourceContentEqual_shouldNotModifyItsParameters() {

    var service = new ServiceBuilder()
        .withNewMetadata().withNamespace("test")
        .withName("test")
        .endMetadata()
        .withNewSpec()
        .withClusterIP("None")
        .endSpec()
        .withNewStatus().withNewLoadBalancer().endLoadBalancer().endStatus()
        .build();

    var serviceHash = service.hashCode();

    comparator.isResourceContentEqual(service, service);

    var afterServiceHash = service.hashCode();

    assertEquals(serviceHash, afterServiceHash,
        "Parameters of the comparator should not be altered");
  }
}
